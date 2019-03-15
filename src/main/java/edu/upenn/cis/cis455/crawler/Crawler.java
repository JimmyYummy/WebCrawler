package edu.upenn.cis.cis455.crawler;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

import edu.upenn.cis.cis455.crawler.info.RobotsTxtInfo;
import edu.upenn.cis.cis455.crawler.info.URLInfo;
import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class Crawler implements CrawlMaster {
	static final int NUM_WORKERS = 10;

	private int maxSize;
	private int maxCount;
	private AtomicInteger docCount;
	private StorageInterface db;
	private BlockingQueue<String> q;
	private Collection<CrawlerWorker> pool;
	private int exitedWorkerCount;
	private AtomicInteger workingWorkers;
	
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
			e.printStackTrace();
		}
		pool = new ArrayList<>();
		for (int i = 0; i < NUM_WORKERS; i++) {
			CrawlerWorker worker = new CrawlerWorker();
			pool.add(worker);
		}
		maxSize = size;
		maxCount = count;
		docCount = new AtomicInteger(0);
		workingWorkers = new AtomicInteger(0);
		
		robotMap = new HashMap<>();
		signatures = new HashSet<>();
		
	}

	///// TODO: you'll need to flesh all of this out. You'll need to build a thread
	// pool of CrawlerWorkers etc. and to implement the functions below which are
	// stubs to compile

	/**
	 * Main thread
	 */
	public void start() {
		for (CrawlerWorker worker : pool) {
			worker.start();
		}
	}

	/**
	 * Returns true if it's permissible to access the site right now eg due to
	 * robots, etc.
	 */ 
	public boolean isOKtoCrawl(String site, int port, boolean isSecure) {
		
		String url = CrawlerUtils.genURL(site, port, isSecure);
		if (! robotMap.containsKey(url)) {
			synchronized (this) {
				if (! robotMap.containsKey(url)) {
					robotMap.put(url, new RobotResolver(url, isSecure));
				}
			}
		}
		return robotMap.get(url).isWebsiteOK();
	}

	/**
	 * Returns true if the crawl delay says we should wait
	 */
	// when to wait? 
	// 1. the robot.txt's delay has not been reached 2. workingWorkers + current docCount >= maxCount
	public boolean deferCrawl(String url) {
		if (workingWorkers.get() + docCount.get() >= maxCount) return true;
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

	/**
	 * Returns true if the document content looks worthy of indexing, eg that it
	 * doesn't have a known signature
	 */
	public synchronized boolean isIndexable(String content) {
		return ! signatures.contains(CrawlerUtils.gentMD5Sign(content));
	}
	
	public synchronized void addSignature(String signature) {
		signatures.add(signature);
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
		if (docCount.get() >= maxCount) return true;
		if (q.isEmpty() && workingWorkers.get() == 0) return true;
		return false;
	}

	/**
	 * Workers should notify when they are processing an URL
	 */
	public synchronized void setWorking(boolean working) {
		if (working) workingWorkers.incrementAndGet();
		else workingWorkers.decrementAndGet();
	}

	/**
	 * Workers should call this when they exit, so the master knows when it can shut
	 * down
	 */
	public synchronized void notifyThreadExited() {
		exitedWorkerCount--;
	}
	
	public synchronized boolean shutDownMainThread() {
		return exitedWorkerCount == NUM_WORKERS;
	}
	

	/**
	 * Main program: init database, start crawler, wait for it to notify that it is
	 * done, then close.
	 */
	public static void main(String args[]) {
		if (args.length < 3 || args.length > 5) {
			System.out.println(
					"Usage: Crawler {start URL} {database environment path} {max doc size in MB} {number of files to index}");
			System.exit(1);
		}

		System.out.println("Crawler starting");
		String startUrl = args[0];
		String envPath = args[1];
		Integer size = Integer.valueOf(args[2]);
		Integer count = args.length == 4 ? Integer.valueOf(args[3]) : 100;

		StorageInterface db = StorageFactory.getDatabaseInstance(envPath);

		Crawler crawler = new Crawler(startUrl, db, size, count);

		System.out.println("Starting crawl of " + count + " documents, starting at " + startUrl);
		crawler.start();

		while (!crawler.shutDownMainThread())
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		// TODO: final shutdown

		System.out.println("Done crawling!");
	}
}
