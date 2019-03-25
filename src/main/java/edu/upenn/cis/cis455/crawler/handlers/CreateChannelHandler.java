package edu.upenn.cis.cis455.crawler.handlers;

import spark.Request;

import static spark.Spark.halt;

import edu.upenn.cis.cis455.model.User;
import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Response;
import spark.Route;

public class CreateChannelHandler implements Route {

	StorageInterface db;

	public CreateChannelHandler(StorageInterface db) {
		this.db = db;
	}

	@Override
	public Object handle(Request request, Response response) throws Exception {
		String channelName = request.params("name");
		String channelPath = request.queryParams("xpath");
		User user = (User) request.session().attribute("userModel");
		response.header("content-type", "text/html");
		String channelCreater = user.getFirstName() + " " + user.getLastName();
		if (channelName == null || channelPath == null)
			halt(400, "wrong request format");
		if (db.addChannel(channelName, channelCreater, channelPath))
			return String.format("Successfully created channel %s by %s.", channelName, channelCreater);
		return halt(400, "Channel name already in use");
	}

}
