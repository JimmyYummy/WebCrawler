package edu.upenn.cis.cis455.stormLiteCrawler;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;
import junit.framework.TestCase;

public class UrlSpoutTest extends TestCase {
	StorageInterface db;
	
	@Before
	public void setUp() throws Exception {
		String envPath = "CrawlerTestDB";
		db = StorageFactory.getDatabaseInstance(envPath);
		Crawler.createCrawler("http://google.com", db, 10, 10);
	}

	@After
	public void tearDown() throws Exception {
		db.close();
	}

	@Test
	public void test() {
		BlockingQueue<String> q = Crawler.getCrawler().getBlockingQueue();
		assertEquals(1, q.size());
		UrlSpout spout = new UrlSpout();
		try {
			spout.open(new HashMap<>(), null  , null);
			spout.nextTuple();
		} catch (NullPointerException e) {
			
		}
		assertEquals(0, q.size());
	}

}
