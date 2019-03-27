package edu.upenn.cis.cis455.crawler;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;
import junit.framework.TestCase;

public class CrawlerTest extends TestCase {
	private String dbPath = "crawl_test_db";
	private String url = "https://dbappserv.cis.upenn.edu/crawltest/nytimes/Africa.xml";
	private StorageInterface db = null;

	@Before
	public synchronized void setup() throws Exception {
		StorageFactory.getDatabaseInstance(dbPath).close();
	}
	
	@After
	public void tearDown() throws Exception {
		db = null;
	}

	@Test
	public synchronized void testCrawl1() {
		db = StorageFactory.getDatabaseInstance(dbPath);
		Crawler.crawl(url, db, 10000, 1);
		db = StorageFactory.getDatabaseInstance(dbPath);
		assertEquals(1, db.getCorpusSize());
		db.close();
	}
	
	@Test
	public synchronized void testCrawl2() {
		db = StorageFactory.getDatabaseInstance(dbPath);
		Crawler.crawl(url + "xxx", db, 1, 10000);
		db = StorageFactory.getDatabaseInstance(dbPath);
		assertEquals(0, db.getCorpusSize());	
		db.close();
	}
	
	@Test
	public synchronized void testCrawl3() {
		db = StorageFactory.getDatabaseInstance(dbPath);
		Crawler.crawl(url, db, 10000, 1);
		db = StorageFactory.getDatabaseInstance(dbPath);
		Crawler.crawl(url, db, 10000, 1);
		db = StorageFactory.getDatabaseInstance(dbPath);
		assertEquals(1, db.getCorpusSize());	
		db.close();
	}

}
