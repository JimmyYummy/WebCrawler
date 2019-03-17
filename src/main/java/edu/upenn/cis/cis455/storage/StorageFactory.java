package edu.upenn.cis.cis455.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StorageFactory {
	static StorageInterface storage = null;
	
    public static StorageInterface getDatabaseInstance(String directory) {
	// TODO: factory object, instantiate your storage server
    	if (!Files.exists(Paths.get(directory))) {
            try {
                Files.createDirectory(Paths.get(directory));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    	
    	if (storage == null) {
    		storage = new StorageInstance(directory);
    	}
        return storage; 
    }
}
