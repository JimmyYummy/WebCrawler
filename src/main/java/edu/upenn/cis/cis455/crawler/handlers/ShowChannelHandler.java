package edu.upenn.cis.cis455.crawler.handlers;

import edu.upenn.cis.cis455.model.ChannelMeta;

import static spark.Spark.halt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Request;
import spark.Response;
import spark.Route;

public class ShowChannelHandler implements Route {
	private static Logger logger = LogManager.getLogger(ShowChannelHandler.class);

	private StorageInterface db;

	public ShowChannelHandler(StorageInterface db) {
		this.db = db;
	}
	
	@Override
	public Object handle(Request request, Response response) throws Exception {
		// get the channel name
		String channelName = request.queryParams("channel");
		logger.debug("received request on channel: " + channelName);
		response.type("text/html");
		// map the channel name to the channel number
		logger.debug("getting channel no");
		int chNo = db.getChannelNo(channelName);
		if (chNo == -1) halt("channel does not exist");
		logger.debug("getting channel detail");
		// get the channel detail using the number
		ChannelMeta ch = db.getChannelDetail(chNo);
		// write the html file
		logger.debug("generating response html");
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html><html lang=\"en\">");
		sb.append("<body>");
		sb.append("<div class=\"channelheader\">");
		sb.append("Channel name: " + ch.getChannelName());
		sb.append(" created by " + ch.getChannelCreater());
		sb.append("</div>");
		
		sb.append("<ul class=\"docs\">");
		// write each xml file to the channel
		for (String url : ch.getUrls()) {
			logger.debug("Getting url doc: " + url);
			appendDoc(url, sb);
		}
		// close blocks
		sb.append("</ul");
		sb.append("</body>");
		sb.append("</html>");
		logger.debug("returning the html to client");
		return sb.toString();
	}

	private void appendDoc(String url, StringBuilder sb) {
		String crawledTime = db.getCraweledTime(url);
		String doc = db.getDocument(url);
		sb.append("<li>");
		sb.append(String.format("<p>Crawled on: %s</p>", crawledTime));
		sb.append(String.format("<p>Location: %s</p>", url));
		sb.append("<div class=”document”>");
		sb.append(doc);
		sb.append("</div>");
		sb.append("</li>");
	}

}
