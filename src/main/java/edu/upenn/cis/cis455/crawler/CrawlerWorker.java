package edu.upenn.cis.cis455.crawler;

import static spark.Spark.halt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import edu.upenn.cis.cis455.crawler.info.URLInfo;
import edu.upenn.cis.cis455.model.URLDetail;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class CrawlerWorker extends Thread {
	private static Logger logger = LogManager.getLogger(CrawlerWorker.class);
	
	private BlockingQueue<String> q;
	private Crawler c;
	private StorageInterface db;
	private boolean working;

	public CrawlerWorker(BlockingQueue<String> q, Crawler c, StorageInterface db) {
		this.q = q;
		this.c = c;
		this.db = db;
		working = false;
	}

	@Override
	public void run() {
		while (true) {
			setUnworking();
			if (c.isDone())
				break;
			// get the url
			String urlStr = null;
			try {
				while (true) {
					System.out.println("xxxx");
					urlStr = q.poll(5, TimeUnit.MILLISECONDS);
					if (urlStr != null) break;
					if (c.isDone()) break;
				}
			} catch (InterruptedException e1) {
				logger.catching(e1);
			}
			if (urlStr == null) break;
			setWorking();
			URLInfo url = new URLInfo(urlStr);
			// check website
			if (!c.isOKtoCrawl(url.getHostName(), url.getPortNo(), url.isSecure()))
				continue;
			// check defer
			String hostStr = CrawlerUtils.genURL(url.getHostName(), url.getPortNo(), url.isSecure());
			while (c.deferCrawl(hostStr)) {
				if (c.isDone())
					break;

			}
			// check url ok to crawl
			if (!c.isOKtoParse(url))
				continue;
			// check if the url is fetched already & no need to update
			long lastModified = 0;
			String doc = null;
			boolean isHtml = false;
			URLDetail detail = db.getUrlDetial(url);
			if (detail != null)
				lastModified = detail.getEpochSecond();
//			send head request
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
					// if status == 301/302 put the new link in the queue and quit
					// TODO: take care of different types of URLs
//					q.put(conn.getHeaderField("Location"));
					conn.disconnect();
					continue;
				} else if (statusCode == HttpURLConnection.HTTP_ACCEPTED) {
					// if status == 200 check size and type
					if (!c.isQualifiedDoc(conn.getContentLength(), conn.getContentType())) {
						conn.disconnect();
						continue;
					}
					// if qualified -> send GET request
					conn.disconnect();
					conn = createConnection(urlStr, url.isSecure(), "GET");
					logger.info(urlStr + ": Downloading");
					if (!(conn.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED)) {
						System.err.println(conn.getResponseCode());
						System.err.println("Unexpected Connection Failure when sending GET request: " + urlStr);
					}
					// if 200
					// get doc
					doc = readContent(conn);
					isHtml = conn.getContentType().equals("text/html");
					// save doc / (or just increment count)
					String docId = db.addDocument(doc, conn.getContentType());
					// add or change UrlDetail
					db.addUrlDetail(new URLDetail(urlStr, docId, conn.getLastModified()));
					// remove old doc if necessary
					if (detail != null) {
						db.decreUrlCount(detail.getDocId());
					}
				} else {
					// any other status code -> quit
					System.err.println(conn.getResponseCode());
					System.err.println("Unexpected Connection Failure when sending HEAD request: " + urlStr);
					conn.disconnect();
					continue;
				}
				conn.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
//			if html, check signature
			if (!isHtml)
				continue;
			if (!c.isIndexable(doc))
				continue;
			// extract links
			Document htmlDoc = Jsoup.parse(doc);
			htmlDoc.setBaseUri(CrawlerUtils.genURL(url.getHostName(), url.getPortNo(), url.isSecure()));
			for (Element ele : htmlDoc.getElementsByAttribute("href")) {
				String nextUrlStr = ele.absUrl("href");
				try {
					q.put(nextUrlStr);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		c.notifyThreadExited();
	}

	public synchronized boolean isWorking() {
		return working;
	}

	private synchronized void setWorking() {
		working = true;
		c.setWorking(working);
	}

	private synchronized void setUnworking() {
		working = false;
		c.setWorking(working);
	}

	private static HttpURLConnection createConnection(String urlStr, boolean isSecure, String method) {

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
			e.printStackTrace();
			halt(500);
		}
		return null;
	}

	private String readContent(HttpURLConnection conn) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		return response.toString();
	}
}