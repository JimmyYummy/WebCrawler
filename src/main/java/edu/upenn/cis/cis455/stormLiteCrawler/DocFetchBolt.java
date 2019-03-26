package edu.upenn.cis.cis455.stormLiteCrawler;

import static spark.Spark.halt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.stormLiteCrawler.Crawler;
import edu.upenn.cis.cis455.crawler.CrawlerUtils;
import edu.upenn.cis.cis455.crawler.info.URLInfo;
import edu.upenn.cis.cis455.model.URLDetail;
import edu.upenn.cis.cis455.storage.StorageInterface;
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.bolt.IRichBolt;
import edu.upenn.cis.stormlite.bolt.OutputCollector;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;

public class DocFetchBolt implements IRichBolt {
	static Logger log = LogManager.getLogger(DocFetchBolt.class);
	static Logger logger = log;
	private Crawler c;
	private StorageInterface db;
	private boolean working;
	private boolean processing;

	Fields schema = new Fields("doc", "url", "doctype");

	/**
	 * To make it easier to debug: we have a unique ID for each instance of the
	 * WordCounter, aka each "executor"
	 */
	String executorId = UUID.randomUUID().toString();

	/**
	 * This is where we send our output stream
	 */
	private OutputCollector collector;

	/**
	 * Initialization, just saves the output stream destination
	 */
	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.c = Crawler.getCrawler();
		this.db = c.getDB();
		working = false;
		processing = false;
	}

	/**
	 * Process a tuple received from the stream, incrementing our counter and
	 * outputting a result
	 */
	@Override
	public void execute(Tuple input) {
		while (true) {
			setWorking(true);
			String urlStr = input.getStringByField("url");
			log.debug(getExecutorId() + " received " + urlStr);
			if (urlStr == null)
				break;
			if (!urlStr.startsWith("http")) {
				logger.debug("Unsupported url type: " + urlStr);
				continue;
			}
			URLInfo url = new URLInfo(urlStr);
			logger.debug("Raw url: " + urlStr);
			urlStr = CrawlerUtils.genURL(url.getHostName(), url.getPortNo(), url.isSecure(), url.getFilePath());
			// check website
			logger.debug("Processing url:" + urlStr);
			if (!c.isOKtoCrawl(url.getHostName(), url.getPortNo(), url.isSecure()))
				continue;
			// check defer
			logger.debug("Check whether defer: " + urlStr);
			String hostStr = CrawlerUtils.genURL(url.getHostName(), url.getPortNo(), url.isSecure());
			while (c.deferCrawl(hostStr)) {
				if (c.isDone()) {
					logger.debug("crawling finished while the worker is waiting on the delay clearance");
					break;
				}
			}
			if (c.isDone())
				break;
			logger.debug("defer clearance retrieved: " + urlStr);
			setProcessing(true);
			// check url ok to crawl
			if (!c.isOKtoParse(url)) {
				logger.debug("Not OK to parse url (closed host/url visited before): " + urlStr);
				continue;
			}
			// check if the url is fetched already & no need to update
			long lastModified = 0;
			String doc = null;
			boolean isHtml = false;
			URLDetail detail = db.getUrlDetial(url);
			if (detail != null)
				lastModified = detail.getEpochSecond();
//	send head request
			try {
				HttpURLConnection conn = createConnection(urlStr, url.isSecure(), "HEAD");
				conn.setIfModifiedSince(lastModified);
				int statusCode = conn.getResponseCode();
				// if status == NOT_MODIFIED -> skip to fetching doc
				if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
					logger.info(urlStr + ": Not modified");
					doc = db.getDocument(urlStr);
					isHtml = db.isHtmlDoc(urlStr);
				} else if (statusCode == HttpURLConnection.HTTP_MOVED_PERM
						|| statusCode == HttpURLConnection.HTTP_MOVED_TEMP) {
					break;
				} else if (statusCode == HttpURLConnection.HTTP_OK) {
					// if status == 200 check size and type
					String type = conn.getContentType();
					type = type.split(";")[0].toLowerCase().trim();
					if (!c.isQualifiedDoc(conn.getContentLength(), type)) {
						conn.disconnect();
						String docId = db.removeUrlDetail(urlStr);
						if (docId != null)
							db.decreUrlCount(docId);
						break;
					}
					// if qualified -> send GET request
					conn.disconnect();
					conn = createConnection(urlStr, url.isSecure(), "GET");
					logger.info(urlStr + ": Downloading");
					if (!(conn.getResponseCode() == HttpURLConnection.HTTP_OK)) {
						String docId = db.removeUrlDetail(urlStr);
						if (docId != null)
							db.decreUrlCount(docId);
						System.err.println(conn.getResponseCode());
						System.err.println("Unexpected Connection Failure when sending GET request: " + urlStr);
					}
					// if 200
					// get doc
					doc = readContent(conn);
					isHtml = type.equals("text/html");
					// save doc / (or just increment count)
					if (!db.hasDocument(doc)) {
						logger.debug("new doc");
						c.incCount();
					}
					String docId = db.addDocument(doc, type);

					// add or change UrlDetail
					db.addUrlDetail(new URLDetail(urlStr, docId, conn.getLastModified()));
					// remove old doc if necessary
					if (detail != null) {
						db.decreUrlCount(detail.getDocId());
					}
				} else {
					// any other status code -> quit
					logger.debug("Unexpected Connection Failure when sending HEAD request: " + urlStr + " with code: "
							+ conn.getResponseCode());
					conn.disconnect();
					break;
				}
				conn.disconnect();
			} catch (IOException e) {
				logger.catching(Level.DEBUG, e);
				break;
			} finally {
				setWorking(false);
				setProcessing(false);
			}
			if (doc != null) {
				if (isHtml) {
					collector.emit(new Values<Object>(doc, urlStr, "html"));
				} else {
					collector.emit(new Values<Object>(doc, urlStr, "xml"));
				}
			}
			break;
		}
	}

	/**
	 * Shutdown, just frees memory
	 */
	@Override
	public void cleanup() {

	}

	/**
	 * Lets the downstream operators know our schema
	 */
	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(schema);
	}

	/**
	 * Used for debug purposes, shows our exeuctor/operator's unique ID
	 */
	@Override
	public String getExecutorId() {
		return executorId;
	}

	/**
	 * Called during topology setup, sets the router to the next bolt
	 */
	@Override
	public void setRouter(IStreamRouter router) {
		this.collector.setRouter(router);
	}

	/**
	 * The fields (schema) of our output stream
	 */
	@Override
	public Fields getSchema() {
		return schema;
	}

	public synchronized void setProcessing(boolean b) {
		if (this.processing != b) {
			c.setProcessing(b);
		}
		this.processing = b;

	}

	public synchronized boolean isWorking() {
		return working;
	}

	public synchronized void setWorking(boolean b) {
		if (this.working != b) {
			c.setWorking(b);
		}
		this.working = b;
	}

	public static HttpURLConnection createConnection(String urlStr, boolean isSecure, String method) {

		URL url = null;
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			halt(500);
		}
		try {

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(method);
			conn.setRequestProperty("User-Agent", "cis455crawler");
			return conn;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.catching(Level.DEBUG, e);
			halt(500);
		}
		return null;
	}

	public String readContent(HttpURLConnection conn) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		int size = conn.getContentLength();
		if (size == -1)
			size = c.maxSize();
		int bytesRead = 0;
		char[] buffer = new char[size];
		while (bytesRead != size) {
			bytesRead += in.read(buffer, bytesRead, size - bytesRead);
		}
		in.close();
		return new String(buffer);
	}
}
