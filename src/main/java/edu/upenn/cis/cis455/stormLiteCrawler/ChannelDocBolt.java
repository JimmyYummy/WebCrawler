package edu.upenn.cis.cis455.stormLiteCrawler;

import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.storage.StorageInterface;
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.bolt.IRichBolt;
import edu.upenn.cis.stormlite.bolt.OutputCollector;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;

public class ChannelDocBolt implements IRichBolt {
	static Logger log = LogManager.getLogger(ChannelDocBolt.class);

	Fields myFields = new Fields();

	StorageInterface db;

	/**
	 * To make it easier to debug: we have a unique ID for each instance of the
	 * PrintBolt, aka each "executor"
	 */
	String executorId = UUID.randomUUID().toString();

	@Override
	public void cleanup() {
		// Do nothing

	}

	@Override
	public void execute(Tuple input) {
		// get the params and write the mapping to db
		String url = input.getStringByField("url");
		int channelNo = input.getIntegerByField("channelNo");
		log.debug(String.format("adding doc [%s] to channel [%d].", url, channelNo));
		db.addUrlToChannel(channelNo, url);
	}

	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
		// Do nothing
		this.db = Crawler.getCrawler().getDB();
	}

	@Override
	public String getExecutorId() {
		return executorId;
	}

	@Override
	public void setRouter(IStreamRouter router) {
		// Do nothing
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(myFields);
	}

	@Override
	public Fields getSchema() {
		return myFields;
	}
}
