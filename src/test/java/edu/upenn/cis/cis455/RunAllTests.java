package edu.upenn.cis.cis455;

import edu.upenn.cis.cis455.crawler.CrawlerTest;
import edu.upenn.cis.cis455.storage.StorageInterfaceTest;
import edu.upenn.cis.cis455.stormLiteCrawler.ChannelDocBoltTest;
import edu.upenn.cis.cis455.stormLiteCrawler.LinkExtractBoltTest;
import edu.upenn.cis.cis455.stormLiteCrawler.UrlSpoutTest;
import edu.upenn.cis.cis455.xpathengine.XPathEngineTest;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class RunAllTests extends TestCase {
	public static Test suite() {
		try {
			Class[] testClasses = {
					/* TODO: Add the names of your unit test classes here */
					// Class.forName("your.class.name.here")
					CrawlerTest.class,
					StorageInterfaceTest.class,
					ChannelDocBoltTest.class,
					LinkExtractBoltTest.class,
					UrlSpoutTest.class,
					XPathEngineTest.class
			};

			return new TestSuite(testClasses);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
