package edu.upenn.cis.cis455.crawler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static spark.Spark.*;
import edu.upenn.cis.cis455.crawler.handlers.*;
import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class WebInterface {
	private static Logger logger = LogManager.getLogger(WebInterface.class);

    public static void main(String args[]) {
        org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.DEBUG);

        if (args.length < 1 || args.length > 2) {
            System.out.println("Syntax: WebInterface {path} {root}");
            System.exit(1);
        }
        
        if (!Files.exists(Paths.get(args[0]))) {
            try {
                Files.createDirectory(Paths.get(args[0]));
            } catch (IOException e) {
            	logger.catching(Level.DEBUG, e);
            }
        }
        
        port(8080);
        StorageInterface database = StorageFactory.getDatabaseInstance(args[0]);
        
        LoginFilter testIfLoggedIn = new LoginFilter(database);
        
        if (args.length == 2) {
            staticFiles.externalLocation(args[1]);
            staticFileLocation(args[1]);
        }

            
        before("/*", testIfLoggedIn);
        // TODO:  add /register, /logout, /index.html, /, /lookup
        post("/register", new RegisterHandler(database));
        post("/login", new LoginHandler(database));
        
        get("/", new IndexHandler(database));
        get("/index.html", new IndexHandler(database));
        
        get("/logout", (req, resp) -> {
        	req.session(false).invalidate();
        	resp.redirect("/");
        	return "";
        });
        get("/lookup", new LookupHandler(database));
        
        
        awaitInitialization();
    }
}
