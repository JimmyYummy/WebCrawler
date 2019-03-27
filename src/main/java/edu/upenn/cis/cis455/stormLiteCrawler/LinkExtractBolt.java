package edu.upenn.cis.cis455.stormLiteCrawler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.bolt.IRichBolt;
import edu.upenn.cis.stormlite.bolt.OutputCollector;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;

public class LinkExtractBolt implements IRichBolt {
	static Logger log = LogManager.getLogger(LinkExtractBolt.class);
	static Logger logger = log;
	BlockingQueue<String> q;
	private Crawler c;

	Fields schema = new Fields();

	/**
	 * To make it easier to debug: we have a unique ID for each instance of the
	 * WordCounter, aka each "executor"
	 */
	String executorId = UUID.randomUUID().toString();

	/**
	 * Initialization, just saves the output stream destination
	 */
	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
		this.c = Crawler.getCrawler();
		this.q = c.getBlockingQueue();
	}

	/**
	 * Process a tuple received from the stream, incrementing our counter and
	 * outputting a result
	 */
	@Override
	public void execute(Tuple input) {
		// check if the doc is a html
		String docType = input.getStringByField("doctype");
		log.debug("new input received, type: " + docType);
		if ("html".equals(docType)) {
			String doc = input.getStringByField("doc");
			if (!c.isIndexable(doc)) {
				log.debug("unindexable doc");
				return;
			}
			// extract links
			String baseUrl = input.getStringByField("url");
			Document htmlDoc = Jsoup.parse(doc);
			htmlDoc.setBaseUri(baseUrl);
			for (Element ele : htmlDoc.getElementsByAttribute("href")) {
				String nextUrlStr = ele.absUrl("href");
				try {
					log.debug("New URL added to the queue: " + nextUrlStr);
					q.put(nextUrlStr);
				} catch (InterruptedException e) {
					logger.catching(Level.DEBUG, e);
				}
			}
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
	}

	/**
	 * The fields (schema) of our output stream
	 */
	@Override
	public Fields getSchema() {
		return schema;
	}
}
