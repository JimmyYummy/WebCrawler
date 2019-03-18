package edu.upenn.cis.cis455.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StorageFactory {
	private static Logger logger = LogManager.getLogger(StorageFactory.class);

	static StorageInterface storage = null;
	
    public static synchronized StorageInterface getDatabaseInstance(String directory) {
	// TODO: factory object, instantiate your storage server
    	if (!Files.exists(Paths.get(directory))) {
            try {
                Files.createDirectory(Paths.get(directory));
            } catch (IOException e) {
            	logger.catching(Level.DEBUG, e);
            }
        }
    	
    	if (storage == null || storage.isClosed()) {
    		storage = new StorageInstance(directory);
    	}
        return storage; 
    }
}
