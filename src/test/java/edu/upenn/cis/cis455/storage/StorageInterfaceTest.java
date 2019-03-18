package edu.upenn.cis.cis455.storage;

import static org.junit.Assert.*;
import static spark.Spark.halt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.model.URLDetail;
import edu.upenn.cis.cis455.model.User;

public class StorageInterfaceTest {
	private StorageInterface db;
	@Before
	public void setUp() throws Exception {
		db = StorageFactory.getDatabaseInstance("test_db");
	}

	@After
	public void tearDown() throws Exception {
		db.close();
		db = null;
	}
	
	@Test
	public synchronized void testAddDocument() {
		String docId = db.addDocument("test_data", "test_type");
		db.addUrlDetail(new URLDetail("test_url", docId, 1));
		assertEquals("test_data", db.getDocument("test_url"));
		assertEquals("test_type", db.getDocType("test_url"));

	}
	
	@Test
	public synchronized void testAddDocumentChangeDoc() {
		String docId1 = db.addDocument("test_data1", "test_type");
		db.addUrlDetail(new URLDetail("test_url", docId1, 1));
		
		String docId2 = db.addDocument("test_data2", "test_type");
		db.addUrlDetail(new URLDetail("test_url", docId2, 1));
		db.decreUrlCount(docId1);
		assertEquals("test_data2", db.getDocument("test_url"));
		assertEquals(1, db.getCorpusSize());
	}

	@Test
	public synchronized void testGetCorpusSize() {
		assertEquals(0, db.getCorpusSize());
		db.addDocument("test_data1", "test_type");
		assertEquals(1, db.getCorpusSize());
	}

	@Test
	public synchronized void testDocLinkCount() {
		String docId = db.addDocument("test_data", "test_type");
		assertEquals(1, db.docLinkCount(docId));
		db.addDocument("test_data", "test_type");
		assertEquals(2, db.docLinkCount(docId));
		db.decreUrlCount(docId);
		assertEquals(1, db.docLinkCount(docId));
		db.decreUrlCount(docId);
		assertEquals(0, db.docLinkCount(docId));
		assertFalse(db.hasDocument("test_data"));
	}
	
	@Test
	public synchronized void testUserLogin() {
		String username = "testuser";
		String password = "testpassword";
		MessageDigest md;
		String hashedPassword = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
			hashedPassword = new String(md.digest(password.getBytes()));
		} catch (NoSuchAlgorithmException e) {
		}
		assertEquals(null, db.getSessionForUser(username, password));
		User user = new User(username, hashedPassword, "first", "last");
		assertEquals(0, db.addUser(user));
		assertEquals(1, db.addUser(user));
		User retrievedUser = db.getSessionForUser(username, password);
		assertNotEquals(null, retrievedUser);
	}

}
