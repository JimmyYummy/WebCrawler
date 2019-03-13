package edu.upenn.cis.cis455.storage;

public class StorageFactory {
	static StorageInterface storage = null;
	
    public static StorageInterface getDatabaseInstance(String directory) {
	// TODO: factory object, instantiate your storage server
    	if (storage == null) {
    		storage = new StorageInstance(directory);
    	}
        return storage; 
    }
}
