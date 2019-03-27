package edu.upenn.cis.cis455.crawler.handlers;

import spark.Request;

import static spark.Spark.halt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.model.User;
import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Response;
import spark.Route;

public class CreateChannelHandler implements Route {
	private static Logger logger = LogManager.getLogger(CreateChannelHandler.class);

	private StorageInterface db;

	public CreateChannelHandler(StorageInterface db) {
		this.db = db;
	}

	@Override
	public Object handle(Request request, Response response) throws Exception {
		// read in the input parameters
		String channelName = request.params("name");
		String channelPath = request.queryParams("xpath");
		if (channelPath == null) halt(400, "XPath not specified");
		logger.debug("Reveived create channel request with name: " + channelName + " XPath: " + channelPath);
		// get the user's name from the request
		User user = (User) request.session().attribute("userModel");
		response.header("content-type", "text/html");
		String channelCreater = user.getFirstName() + " " + user.getLastName();
		// integrity check
		if (channelName == null || channelPath == null)
			halt(400, "wrong request format");
		// write the channel to the db
		logger.debug("adding the channel");
		if (db.addChannel(channelName, channelCreater, channelPath)) {
			logger.debug("channel added");
			return String.format("Successfully created channel %s by %s.", channelName, channelCreater);
		}
		halt(400, "Channel name already in use");
		return "";
	}

}
