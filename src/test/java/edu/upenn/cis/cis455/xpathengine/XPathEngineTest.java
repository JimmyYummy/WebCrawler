package edu.upenn.cis.cis455.xpathengine;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.model.OccurrenceEvent;
import junit.framework.TestCase;

public class XPathEngineTest extends TestCase {
	private XPathEngine eng;

	@Before
	public void setUp() throws Exception {
		eng = XPathEngineFactory.getXPathEngine();
	}

	@After
	public void tearDown() throws Exception {
		eng = null;
	}

	@Test
	public void testIsValid1() {
		eng.setXPaths(new String[] {"/html"});
		assertTrue(eng.isValid(0));
	}
	
	@Test
	public void testIsValid2() {
		eng.setXPaths(new String[] {"/html/"});
		assertFalse(eng.isValid(0));
	}
	
	@Test
	public void testIsValid3() {
		eng.setXPaths(new String[] {"/html[text() = \"text()\\\"\"]"});
		assertTrue(eng.isValid(0));
	}
	
	@Test
	public void testIsValid4() {
		eng.setXPaths(new String[] {"/html[text() = \"text()\\\"\"]"});
		assertTrue(eng.isValid(0));
	}
	
	@Test
	public void testIsValid5() {
		eng.setXPaths(new String[]{"html", "/html", "/xml[contains(text(), \"hello\")]"});
		assertFalse(eng.isValid(0));
		assertTrue(eng.isValid(1));
		assertTrue(eng.isValid(2));
	}

	@Test
	public void testEvaluateEvent1() {
		eng.setXPaths(new String[]{"html", "/html", "/html[text()  = \"hello\"]", "/html[contains(text(), \"hello\")]"});
		String doc = "<html>hello world</html>";
		boolean[] results = eng.evaluateEvent(new OccurrenceEvent(doc));
		assertFalse(results[0]);
		assertTrue(results[1]);
		assertFalse(results[2]);
		assertTrue(results[3]);
	}
	
	@Test
	public void testEvaluateEvent2() {
		eng.setXPaths(new String[]{"/html", "/html/body", "/html/body/div"});
		String doc = "<html><body>hello world</body></html>";
		boolean[] results = eng.evaluateEvent(new OccurrenceEvent(doc));
		assertTrue(results[0]);
		assertTrue(results[1]);
		assertFalse(results[2]);
	}
	
	@Test
	public void testEvaluateEvent3() {
		eng.setXPaths(new String[]{"/html[text()  = \"hello\"]", "/html[contains(text(), \"hello\")]"});
		String doc = "<html>hello</html>";
		boolean[] results = eng.evaluateEvent(new OccurrenceEvent(doc));
		assertTrue(results[0]);
		assertTrue(results[1]);
	}
	
	@Test
	public void testEvaluateEvent4() {
		eng.setXPaths(new String[]{"/html[contains(text(), \"hello\")][contains(text(), \"world\")]"});
		String doc = "<html>hello world</html>";
		boolean[] results = eng.evaluateEvent(new OccurrenceEvent(doc));
		assertTrue(results[0]);
		doc = "<html>world hello</html>";
		results = eng.evaluateEvent(new OccurrenceEvent(doc));
		assertTrue(results[0]);
	}

}
