package edu.upenn.cis.cis455.stormLiteCrawler;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;

public class LinkExtractBoltTest {

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
	public void test1() {
		LinkExtractBolt bolt = new LinkExtractBolt();
		bolt.prepare(null, null, null);
		List<Object> list = new ArrayList<>();
		list.add("<a href=\"http://www.test.com\">test url</a>");
		list.add("http://google.com");
		list.add("html");
		Tuple input = new Tuple(new Fields("doc", "url", "doctype"), list);
		
		assertEquals(1, Crawler.getCrawler().getBlockingQueue().size());
		bolt.execute(input);
		assertEquals(2, Crawler.getCrawler().getBlockingQueue().size());
	}
	
	@Test
	public void test2() {
		LinkExtractBolt bolt = new LinkExtractBolt();
		bolt.prepare(null, null, null);
		List<Object> list = new ArrayList<>();
		list.add("<a href=\"http://www.test.com\">test url</a>");
		list.add("http://google.com");
		list.add("xml");
		Tuple input = new Tuple(new Fields("doc", "url", "doctype"), list);
		
		assertEquals(1, Crawler.getCrawler().getBlockingQueue().size());
		bolt.execute(input);
		assertEquals(1, Crawler.getCrawler().getBlockingQueue().size());
	}
	
	@Test
	public void test3() {
		LinkExtractBolt bolt = new LinkExtractBolt();
		bolt.prepare(null, null, null);
		List<Object> list = new ArrayList<>();
		list.add("<img src=\"http://www.test.com\">test url</img>");
		list.add("http://google.com");
		list.add("html");
		Tuple input = new Tuple(new Fields("doc", "url", "doctype"), list);
		
		assertEquals(1, Crawler.getCrawler().getBlockingQueue().size());
		bolt.execute(input);
		assertEquals(1, Crawler.getCrawler().getBlockingQueue().size());
	}

}
