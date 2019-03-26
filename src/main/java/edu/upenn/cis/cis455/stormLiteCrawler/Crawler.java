package edu.upenn.cis.cis455.stormLiteCrawler;

import java.io.Serializable;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.upenn.cis.cis455.crawler.CrawlMaster;
import edu.upenn.cis.cis455.crawler.CrawlerUtils;
import edu.upenn.cis.cis455.crawler.RobotResolver;
import edu.upenn.cis.cis455.crawler.info.URLInfo;
import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;
import edu.upenn.cis.stormlite.Config;
import edu.upenn.cis.stormlite.LocalCluster;
import edu.upenn.cis.stormlite.Topology;
import edu.upenn.cis.stormlite.TopologyBuilder;
import edu.upenn.cis.stormlite.tuple.Fields;

public class Crawler implements CrawlMaster, Serializable {
	private static Logger logger = LogManager.getLogger(Crawler.class);
	static final int NUM_WORKERS = 10;
	private static Crawler crawler;

	private int maxSize;
	private int maxCount;
	private AtomicInteger docCount;
	private StorageInterface db;
	private BlockingQueue<String> q;
	private int exitedWorkerCount;
	private AtomicInteger workingWorkers;
	private AtomicInteger processingWorkers;

	private Set<String> signatures;
	private Map<String, RobotResolver> robotMap;
	private LocalCluster cluster;

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

		// TODO:
		this.db = db;
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
		Config config = new Config();

		UrlSpout spout = new UrlSpout();
		DocFetchBolt dfBolt = new DocFetchBolt();
		LinkExtractBolt leBolt = new LinkExtractBolt();
		XPathMatchingBolt xpmBolt = new XPathMatchingBolt();
		ChannelDocBolt cdBolt = new ChannelDocBolt();

		TopologyBuilder builder = new TopologyBuilder();

		builder.setSpout("urlSpout", spout, 1);

		builder.setBolt("dfBolt", dfBolt, 1).shuffleGrouping("urlSpout");
		builder.setBolt("leBolt", leBolt, 1).shuffleGrouping("dfBolt");
		builder.setBolt("xpmBolt", xpmBolt, 1).shuffleGrouping("dfBolt");
		builder.setBolt("cdBolt", cdBolt, 1).fieldsGrouping("xpmBolt", new Fields("channelNo"));

		cluster = new LocalCluster();
		Topology topo = builder.createTopology();

		ObjectMapper mapper = new ObjectMapper();
		try {
			String str = mapper.writeValueAsString(topo);

			logger.debug("The StormLite topology is:\n" + str);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		cluster.submitTopology("crawler", config, builder.createTopology());

		while (!shutDownMainThread()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				logger.catching(Level.DEBUG, e);
			}
		}
		this.cluster.killTopology("crawler");
		this.cluster.shutdown();

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

	public synchronized StorageInterface getDB() {
		if (db == null) throw new IllegalStateException();
		return this.db;
	}
	
	public synchronized BlockingQueue<String> getBlockingQueue() {
		if (q == null) throw new IllegalStateException();
		return this.q;
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
		return q.size() == 0 && workingWorkers.get() == 0;
	}

	public int maxSize() {
		return maxSize;
	}

	public synchronized boolean couldEmit() {
		return processingWorkers.get() + docCount.get() < maxCount;
	}

	public static void crawl(String startUrl, StorageInterface db, int size, int count) {

		crawler = new Crawler(startUrl, db, size, count);

		logger.debug("Starting crawl of " + count + " documents, starting at " + startUrl);
		crawler.startCrawling();

		logger.debug("Done crawling!");
		db.closeWithoutFlushing();
		System.exit(0);

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
		crawl(startUrl, db, size, count);
	}
	
	public static synchronized Crawler getCrawler() {
		if (crawler == null) throw new IllegalStateException();
		return crawler;
	}
}
