package edu.upenn.cis.cis455.stormLiteCrawler;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.model.OccurrenceEvent;
import edu.upenn.cis.cis455.storage.StorageInterface;
import edu.upenn.cis.cis455.xpathengine.XPathEngine;
import edu.upenn.cis.cis455.xpathengine.XPathEngineFactory;
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.bolt.IRichBolt;
import edu.upenn.cis.stormlite.bolt.OutputCollector;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;

public class XPathMatchingBolt implements IRichBolt {
	static Logger log = LogManager.getLogger(XPathMatchingBolt.class);

	private Fields schema = new Fields("channelNo", "url");

	/**
	 * To make it easier to debug: we have a unique ID for each instance of the
	 * PrintBolt, aka each "executor"
	 */
	private String executorId = UUID.randomUUID().toString();

    private OutputCollector collector;
    private StorageInterface db;
    private XPathEngine parser;
    
	@Override
	public String getExecutorId() {
		return executorId;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(schema);
	}

	@Override
	public void cleanup() {
		// TODO Auto-generated method stub

	}

	@Override
	public void execute(Tuple input) {
		// TODO Auto-generated method stub
		// First: clean the previous channel
		String url = input.getStringByField("url");
		String doc = input.getStringByField("doc");
		log.debug("Remove the doc from all channels: " + url);
		db.removeUrlFromAllChannels(url);
		// Second: generate new channel tags
		OccurrenceEvent event = null;
		log.debug("Parsing the doc " + url);
		try {
			event = new OccurrenceEvent(doc);
		} catch (Exception e) {
			
		}
		if (event == null) {
			log.debug("parsing failed, return, url = " + url);
			return;
		}
		
		log.debug("assigning the doc to channels");
		String[] expressions = db.getXPathExpressions();
		
		parser.setXPaths(expressions);
		boolean[] results = parser.evaluateEvent(event);
		log.debug("evaluation result: " + Arrays.toString(results));
		for (int i = 0; i < results.length; i++) {
			if (results[i]) {
				log.debug(String.format("doc [%s] is emmited to channel %d.", url, i));
				collector.emit(new Values<Object>(i, url));
			}
		}
	}


	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.db = Crawler.getCrawler().getDB();
    	this.parser = XPathEngineFactory.getXPathEngine();
	}

	@Override
	public void setRouter(IStreamRouter router) {
		this.collector.setRouter(router);
	}

	@Override
	public Fields getSchema() {
		return schema;
	}
}
