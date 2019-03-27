package edu.upenn.cis.cis455.crawler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.info.URLInfo;
import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class Crawler implements CrawlMaster {
	private static Logger logger = LogManager.getLogger(Crawler.class);
	static final int NUM_WORKERS = 10;

	private int maxSize;
	private int maxCount;
	private AtomicInteger docCount;
	@SuppressWarnings("unused")
	private StorageInterface db;
	private BlockingQueue<String> q;
	private Collection<CrawlerWorker> pool;
	private int exitedWorkerCount;
	private AtomicInteger workingWorkers;
	private AtomicInteger processingWorkers;

	private Set<String> signatures;
	private Map<String, RobotResolver> robotMap;

	public Crawler(String startUrl, StorageInterface db, int size, int count) {
		// TODO: initialize
		if (startUrl == null || db == null || size <= 0 || count <= 0)
			throw new IllegalArgumentException("Illegal input arguments");
		q = new LinkedBlockingQueue<>();
		try {
			q.put(startUrl);
		} catch (InterruptedException e) {
			logger.catching(Level.DEBUG, e);
		}
		pool = new ArrayList<>();
		for (int i = 0; i < NUM_WORKERS; i++) {
			CrawlerWorker worker = new CrawlerWorker(q, this, db);
			pool.add(worker);
		}
		maxSize = size;
		maxCount = count;
		docCount = new AtomicInteger(0);
		workingWorkers = new AtomicInteger(0);
		processingWorkers = new AtomicInteger(0);

		robotMap = new HashMap<>();
		signatures = new HashSet<>();
	}

	///// TODO: you'll need to flesh all of this out. You'll need to build a thread
	// pool of CrawlerWorkers etc. and to implement the functions below which are
	// stubs to compile

	/**
	 * Main thread
	 */
	public void startCrawling() {
		for (CrawlerWorker worker : pool) {
			worker.start();
		}
		
		while (!shutDownMainThread())
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				logger.catching(Level.DEBUG, e);
			}

		// TODO: final shutdown
		for (CrawlerWorker worker : pool) {
			try {
				worker.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logger.catching(Level.DEBUG, e);
			}
		}
		logger.debug("" + docCount.get() + " new docs crawled");
	}

	/**
	 * Returns true if it's permissible to access the site right now eg due to
	 * robots, etc.
	 */
	public boolean isOKtoCrawl(String site, int port, boolean isSecure) {
		String url = CrawlerUtils.genURL(site, port, isSecure);
		if (!robotMap.containsKey(url)) {
			synchronized (this) {
				if (!robotMap.containsKey(url)) {
					robotMap.put(url, new RobotResolver(url));
				}
			}
		}
		return robotMap.get(url).isWebsiteOK();
	}

	/**
	 * Returns true if the crawl delay says we should wait
	 */
	// when to wait?
	// 1. the robot.txt's delay has not been reached 2. workingWorkers + current
	// docCount >= maxCount
	public synchronized boolean deferCrawl(String url) {
		if (processingWorkers.get() + docCount.get() >= maxCount)
			return true;
		return robotMap.get(url).shouldDefer();
	}

	/**
	 * Returns true if it's permissible to fetch the content, eg that it satisfies
	 * the path restrictions from robots.txt
	 */
	// FETCH
	public boolean isOKtoParse(URLInfo url) {
		String urlName = CrawlerUtils.genURL(url.getHostName(), url.getPortNo(), url.isSecure());
		return robotMap.get(urlName).isOKtoParse(url.getFilePath());
	}

	public boolean isQualifiedDoc(int length, String type) {
		if (length > maxSize) {
			logger.debug("too large");
			return false;
		}
		if (type == null) {
			logger.debug("wrong type");
			return false;
		}
		type = type.toLowerCase();
		if ("text/html".equals(type.toLowerCase()))
			return true;
		if ("text/xml".equals(type.toLowerCase()))
			return true;
		if ("application/xml".equals(type.toLowerCase()))
			return true;
		if (type.endsWith("+xml"))
			return true;
		logger.debug("unsuppored type: " + type);
		return false;
	}

	/**
	 * Returns true if the document content looks worthy of indexing, eg that it
	 * doesn't have a known signature
	 */
	public synchronized boolean isIndexable(String content) {
		return signatures.add(CrawlerUtils.gentMD5Sign(content));
	}

	/**
	 * We've indexed another document
	 */
	public synchronized void incCount() {
		docCount.incrementAndGet();
	}

	/**
	 * Workers can poll this to see if they should exit, ie the crawl is done
	 */
	public synchronized boolean isDone() {
		if (docCount.get() >= maxCount)
			return true;
		if (q.isEmpty() && workingWorkers.get() <= 0)
			return true;
		return false;
	}

	/**
	 * Workers should notify when they are processing an URL
	 */
	public synchronized void setWorking(boolean working) {
		if (working)
			workingWorkers.incrementAndGet();
		else
			workingWorkers.decrementAndGet();
	}

	public synchronized void setProcessing(boolean processing) {
		if (processing)
			processingWorkers.incrementAndGet();
		else
			processingWorkers.decrementAndGet();
	}

	/**
	 * Workers should call this when they exit, so the master knows when it can shut
	 * down
	 */
	public synchronized void notifyThreadExited() {
		exitedWorkerCount++;
		logger.debug("new thread exited, total number: " + exitedWorkerCount);
	}

	public synchronized boolean shutDownMainThread() {
		return exitedWorkerCount == NUM_WORKERS;
	}
	
	public int maxSize() {
		return maxSize;
	}

	public static void crawl(String startUrl, StorageInterface db, int size, int count) {

		Crawler crawler = new Crawler(startUrl, db, size, count);

		logger.debug("Starting crawl of " + count + " documents, starting at " + startUrl);
		crawler.startCrawling();

		logger.debug("Done crawling!");
		db.closeWithoutFlushing();
	}
	
	/**
	 * Main program: init database, start crawler, wait for it to notify that it is
	 * done, then close.
	 */
	public static void main(String args[]) {
		org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.INFO);
		if (args.length < 3 || args.length > 5) {
			logger.debug(
					"Usage: Crawler {start URL} {database environment path} {max doc size in MB} {number of files to index}");
			System.exit(1);
		}

		logger.debug("Crawler starting");
		String startUrl = args[0];
		String envPath = args[1];
		int size = Integer.valueOf(args[2]) * 1024 * 1024;
		int count = args.length == 4 ? Integer.valueOf(args[3]) : 100;

		StorageInterface db = StorageFactory.getDatabaseInstance(envPath);
		edu.upenn.cis.cis455.stormLiteCrawler.Crawler.crawl(startUrl, db, size, count);
	}
}
