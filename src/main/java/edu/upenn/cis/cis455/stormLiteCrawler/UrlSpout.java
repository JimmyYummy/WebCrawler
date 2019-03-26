package edu.upenn.cis.cis455.stormLiteCrawler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.spout.IRichSpout;
import edu.upenn.cis.stormlite.spout.SpoutOutputCollector;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Values;

public class UrlSpout implements IRichSpout {
	static Logger log = LogManager.getLogger(UrlSpout.class);

	/**
	 * To make it easier to debug: we have a unique ID for each instance of the
	 * WordSpout, aka each "executor"
	 */
	String executorId = UUID.randomUUID().toString();

	/**
	 * The collector is the destination for tuples; you "emit" tuples there
	 */
	SpoutOutputCollector collector;

	/**
	 * This is a simple file reader for words.txt
	 */

	private BlockingQueue<String> q;
	private Crawler c;

//	public UrlSpout(BlockingQueue<String> q, Crawler c) {
//		log.debug("Starting spout");
//		this.q = q;
//		this.c = c;
//	}

	/**
	 * Initializes the instance of the spout (note that there can be multiple
	 * objects instantiated)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
		this.collector = collector;
		c = Crawler.getCrawler();
		q = c.getBlockingQueue();
	}

	/**
	 * Shut down the spout
	 */
	@Override
	public void close() {

	}

	/**
	 * The real work happens here, in incremental fashion. We process and output the
	 * next item(s). They get fed to the collector, which routes them to targets
	 */
	@Override
	public void nextTuple() {
		if (q != null) {
			String url = null;
//			try {
//				url = q.take();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
			while (url == null) {
				try {
					url = q.poll(10, TimeUnit.MICROSECONDS);
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (url != null) {
				log.debug(getExecutorId() + " emitting " + url);
				while (!c.couldEmit());
				this.collector.emit(new Values<Object>(url));
			}
		}
		Thread.yield();
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("url"));
	}

	@Override
	public String getExecutorId() {

		return executorId;
	}

	@Override
	public void setRouter(IStreamRouter router) {
		this.collector.setRouter(router);
	}

}
