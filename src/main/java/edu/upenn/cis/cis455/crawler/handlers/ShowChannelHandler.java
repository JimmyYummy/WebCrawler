package edu.upenn.cis.cis455.crawler.handlers;

import edu.upenn.cis.cis455.model.ChannelMeta;

import static spark.Spark.halt;
import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Request;
import spark.Response;
import spark.Route;

public class ShowChannelHandler implements Route {

	StorageInterface db;

	public ShowChannelHandler(StorageInterface db) {
		this.db = db;
	}
	
	@Override
	public Object handle(Request request, Response response) throws Exception {
		String channelName = request.queryParams("channel");
		response.type("text/html");
		int chNo = db.getChannelNo(channelName);
		if (chNo == -1) halt("channel does not exist");
		ChannelMeta ch = db.getChannelDetail(chNo);
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html><html lang=\"en\">");
		sb.append("<body>");
		sb.append("<div class=\"channelheader\">");
		sb.append("Channel name: " + ch.getChannelName());
		sb.append(" created by " + ch.getChannelCreater());
		sb.append("</div");
		
		sb.append("<ul class=\"docs\">");
		for (String url : ch.getUrls()) {
			appendDoc(url, sb);
		}
		sb.append("</ul");
		sb.append("</body>");
		sb.append("</html>");
		
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
