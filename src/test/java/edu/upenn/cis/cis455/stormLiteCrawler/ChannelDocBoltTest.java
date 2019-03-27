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

public class ChannelDocBoltTest {

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
		ChannelDocBolt bolt = new ChannelDocBolt();
		bolt.prepare(null, null, null);
		List<Object> list = new ArrayList<>();
		list.add(0);
		list.add("test-url");
		Tuple input = new Tuple(new Fields("channelNo", "url"), list);
		
		db.addChannel("testChannel", "testAdmin", "/test");
		assertEquals(0, db.getChannelDetail(0).getUrls().size());
		bolt.execute(input);
		assertEquals(1, db.getChannelDetail(0).getUrls().size());
	}
	
	@Test
	public void test2() {
		ChannelDocBolt bolt = new ChannelDocBolt();
		bolt.prepare(null, null, null);
		List<Object> list = new ArrayList<>();
		list.add(1);
		list.add("test-url");
		Tuple input = new Tuple(new Fields("channelNo", "url"), list);
		
		db.addChannel("testChannel", "testAdmin", "/test");
		assertEquals(0, db.getChannelDetail(0).getUrls().size());
		bolt.execute(input);
		assertEquals(0, db.getChannelDetail(0).getUrls().size());
	}

}
