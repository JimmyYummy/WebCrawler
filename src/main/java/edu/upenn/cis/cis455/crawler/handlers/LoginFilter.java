package edu.upenn.cis.cis455.crawler.handlers;

import org.apache.logging.log4j.LogManager;

import static spark.Spark.halt;
import org.apache.logging.log4j.Logger;
import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Request;
import spark.Filter;
import spark.Response;

public class LoginFilter implements Filter {
	private static Logger logger = LogManager.getLogger(LoginFilter.class);

	public LoginFilter(StorageInterface db) {

	}

	@Override
	public void handle(Request req, Response response) throws Exception {
		if (!req.pathInfo().equals("/login-form.html") && !req.pathInfo().equals("/login")
				&& !req.pathInfo().equals("/register") && !req.pathInfo().equals("/register.html")) {
			logger.debug("Request is NOT login/registration");
			if (req.session(false) == null) {
//                logger.debug
				logger.debug("Not logged in - redirecting!");
				response.redirect("/login-form.html");
				halt();
			} else {
//                logger.debug
				logger.debug("Logged in!");
				req.attribute("user", req.session().attribute("user"));
			}

		} else {
//            logger.debug
			logger.debug("Request is LOGIN FORM");
			if (req.session(false) == null) {
//              logger.debug
				logger.debug("Not logged in");
				
			} else {
//              logger.debug
				logger.debug("Logged in!");
				response.redirect("/");
				halt();
			}
		}

	}
}
