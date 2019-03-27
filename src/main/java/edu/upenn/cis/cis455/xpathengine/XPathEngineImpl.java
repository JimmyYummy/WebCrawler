package edu.upenn.cis.cis455.xpathengine;

import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.upenn.cis.cis455.model.OccurrenceEvent;

public class XPathEngineImpl implements XPathEngine {
	private static Logger logger = LogManager.getLogger(XPathEngineImpl.class);

	private static PathStep invalidIndicator = new PathStep("INVALID_INDICATOR");
	private PathStep[] heads;
	private boolean[] isValidPaths;
	private int len;

	@Override
	public void setXPaths(String[] expressions) {
		// read the expressions, and parse them to XPath linked list (OathStep), save
		// the head
		if (expressions == null)
			return;
		len = expressions.length;
		heads = new PathStep[len];
		isValidPaths = new boolean[len];
		// get the list head for each XPath
		for (int i = 0; i < len; i++) {
			PathStep head = null;
			try {
				head = genStepList(expressions[i]);
			} catch (Exception e) {
				logger.catching(Level.DEBUG, e);
			}

			logger.debug(String.format("%dth XPath: %s", i, head));
			// if the head is null or invalid, then the XPath must be an invalid expression
			if (head != null && head != invalidIndicator) {
				heads[i] = head;
				isValidPaths[i] = true;
			}
		}
	}

	@Override
	public boolean isValid(int i) {
		return isValidPaths[i];
	}

	@Override
	public boolean[] evaluateEvent(OccurrenceEvent event) {
		// for a given event, check if it match the valid XPaths
		Element root = event.getRootNode();
		boolean[] results = new boolean[len];
		for (int i = 0; i < len; i++) {
			if (isValidPaths[i]) {
				results[i] = checkMatch(root, heads[i]);
			}
		}
		return results;
	}

	/*
	 * do the DFS on the element to check if match
	 */
	private boolean checkMatch(Element docNode, PathStep pathStep) {
		if (pathStep == null)
			return true;
		logger.debug("checking node: " + docNode.nodeName());
		Elements children = docNode.children();
		int childSize = children.size();
		// check if the current doc node's child matches the current step
		for (int i = 0; i < childSize; i++) {
			Element child = children.get(i);
			if (child.nodeName().toLowerCase().equals(pathStep.getNodeName().toLowerCase())) {
				String childText = child.ownText();
				if (checkTests(childText, pathStep) && checkMatch(child, pathStep.next))
					return true;
			}
		}
		return false;
	}

	/**
	 * check if the text matches the text()/contains() functions of the step
	 * 
	 * @param childText
	 * @param pathStep
	 * @return
	 */
	private boolean checkTests(String childText, PathStep pathStep) {
		for (String text : pathStep.getTexts()) {
			if (childText == null)
				return false;
			if (!childText.equals(text))
				return false;
		}
		for (String partialText : pathStep.getContains()) {
			if (childText == null)
				return false;
			if (!childText.contains(partialText))
				return false;
		}
		return true;
	}

	/**
	 * return the head of the XPath if the expression is valid, else return the
	 * invalidIndicator
	 * 
	 * @param exp
	 * @return
	 */
	private PathStep genStepList(String exp) {
		// integrity check
		if (exp == null)
			return invalidIndicator;
		if (exp.length() == 0) {
			logger.debug("reached end");
			return null;
		}
		if (!exp.startsWith("/"))
			return invalidIndicator;
		exp = exp.substring(1);
		if (exp.length() == 0)
			return invalidIndicator;
//		logger.debug("remaining exp is: " + exp);
		// generate the current node
		Deque<Character> stack = new ArrayDeque<>();
		char[] arr = exp.toCharArray();
		int[] ptr = new int[1];
		PathStep head = generateNode(arr, ptr, stack);
		if (head == invalidIndicator)
			return head;
		if (!stack.isEmpty())
			return invalidIndicator;
		if (exp.length() <= ptr[0])
			return head;
		// get the next node and connect it with the current one
		exp = exp.substring(ptr[0]);
		logger.debug("generating the next node with exp: " + exp);
		PathStep next = genStepList(exp);
		if (next == invalidIndicator)
			return next;
		head.next = next;
		return head;
	}

	/**
	 * get the current node, move the ptr to the position of the next node
	 * 
	 * @param arr
	 * @param ptr
	 * @param stack
	 * @return
	 */
	private PathStep generateNode(char[] arr, int[] ptr, Deque<Character> stack) {
		// the stack should be empty initially
		if (!stack.isEmpty())
			return invalidIndicator;
		PathStep step = null;
		// read chars from the expression iteratively
		while (ptr[0] <= arr.length) {
			// if seeing an delimiter between the node name and the tests/next step,
			// generate the node with node name
			if (step == null && (ptr[0] >= arr.length || arr[ptr[0]] == '/' || arr[ptr[0]] == '[')) {
				step = new PathStep(new String(arr, 0, ptr[0]));

			}
			// if the node ends, return
			if (ptr[0] >= arr.length || arr[ptr[0]] == '/') {
				break;
			}

			// if seeing the start of the tests, put all the test strings to the node's
			// local variable
			if (arr[ptr[0]] == '[') {
				// keep checking the start sign of test
				while (arr[ptr[0]] == '[') {
					if (step == null)
						return invalidIndicator;
					logger.debug("checking functions");
					stack.offerFirst('[');
					ptr[0]++;
					try {
						generateTest(arr, ptr, stack, step);
					} catch (IllegalArgumentException e) {
						throw e;
//						System.err.println(new String(arr, 0, ptr[0]));
//						return invalidIndicator;
					}
					if (ptr[0] >= arr.length)
						break;
				}
				ptr[0]--;
			}
			// integrity check
			if (!stack.isEmpty()) {
				logger.debug("stack not cleaned");
				return invalidIndicator;
			}
			ptr[0]++;
		}
		return step;
	}

	/**
	 * extract the tests of the current step, and save them in the PathStep node
	 * 
	 * @param arr
	 * @param ptr
	 * @param stack
	 * @param step
	 */
	private void generateTest(char[] arr, int[] ptr, Deque<Character> stack, PathStep step) {
		// function: contains -> check if the inner is text, if has a valid str
		// text -> check if has a valid str
		// others -> return exception
		// status code (binary)
		// 00 -> undefined x1: text is defined 1x: contains is defined,
		// thus 10 -> 11 is legal, but 01 -> 11 is not
		int status = 0;
		// the number of equals signs should be 0 for contains(text(), ), and 1 for
		// text()
		int numOfEqualSigns = 0;
		String textStr = null;
		// mark the start of the function name
		int funcNameStart = ptr[0];
		while (ptr[0] < arr.length) {
			// if meet the start sign of the string
			if (arr[ptr[0]] == '"') {
				if ((status & 0b01) == 1) {
					if (textStr != null) {
						throw new IllegalArgumentException("string defined too early");
					}
					ptr[0]++;
					// get the string
					textStr = getTextStr(arr, ptr);
					if ((status & 0b10) == 0b10) {
						// check if there is an , before the string
						if (!stack.isEmpty() && stack.peekFirst() == ',') {
							stack.pollFirst();
						} else {
							throw new IllegalArgumentException("string defined too early");
						}
					}
				} else {
					throw new IllegalArgumentException("text term not been met");
				}
				// if meet the end sign of the test function block
			} else if (arr[ptr[0]] == ']') {
				if (stack.peekFirst() == '[') {
					stack.pollFirst();
					// post-processing
					if ((status & 0b01) == 0 || textStr == null) {
						throw new IllegalArgumentException("func term end without seeing the text() or string");
					}
					// add the function to the step's corresponding list
					if ((status & 0b10) == 0b10) {
						step.addContain(textStr);
					} else {
						step.addText(textStr);
					}
					ptr[0]++;
					return;
				} else {
					throw new IllegalArgumentException("expectiing '[', ill status of stack: " + stack);
				}
				// if meet the end sign for the function name, extract it
			} else if (arr[ptr[0]] == '(') {
				// if text() is defined already, no further functions is allowed
				if ((status & 0b01) == 1) {
					throw new IllegalArgumentException("text() has seen, not '(' allowed");
				}
				String funcName = new String(arr, funcNameStart, ptr[0] - funcNameStart).replaceAll(" ", "");
				// if contains() is already seen, only text() is allowed 
				if ((status & 0b10) == 0b10) {
					// check text
					if ("text".equals(funcName)) {
						status = status | 0b01;
					} else {
						throw new IllegalArgumentException("the function name is not text but " + funcName);
					}
					// else either contains() or text() is fine
				} else {
					if ("text".equals(funcName)) {
						status = status | 0b01;
					} else if ("contains".equals(funcName)) {
						status = status | 0b10;
					} else {
						throw new IllegalArgumentException("the function name is not text/contains but " + funcName);
					}
					funcNameStart = ptr[0] + 1;
				}
				stack.offerFirst('(');
				// if seeing the end of the function, check the stack
			} else if (arr[ptr[0]] == ')') {
				if (stack.peekFirst() == '(') {
					stack.pollFirst();
				} else {
					throw new IllegalArgumentException("pushing '(', expecting '(' in stack but is " + stack);
				}
				// if seeing a ',', check if it is after the term "contains(text()" 
			} else if (arr[ptr[0]] == ',') {
				if (stack.peekFirst() != '(' || status != 0b11) {
					throw new IllegalArgumentException("pushing ',', expecting '(' in stack but is " + stack);
				} else {
					stack.offerFirst(',');
				}
				// if seeing a '=', check if it is after the term text() and no contains function is defined
			} else if (arr[ptr[0]] == '=') {
				// check if text, if contains
				if ((status & 0b10) == 0b10) {
					throw new IllegalArgumentException("meet '=' with seeing contains");
				}
				if ((status & 0b01) != 1) {
					throw new IllegalArgumentException("meet '=' without seeing text");
				}
				if (numOfEqualSigns > 0) {
					throw new IllegalArgumentException("too many '='");
				}
				numOfEqualSigns++;
				// chars other than space, double quote, right brackets are disallowed after seeing "text("
			} else if ((status & 0b01) == 1 && arr[ptr[0]] != ' ') {
				throw new IllegalArgumentException("illegal char after seeing text: " + arr[ptr[0]]);
			}
			ptr[0]++;
		}
	}

	/**
	 * get the string quoted in "", or throw an error if the right double quote is
	 * not found
	 * 
	 * @param arr
	 * @param ptr
	 * @return
	 */
	private String getTextStr(char[] arr, int[] ptr) {
		int start = ptr[0];
		while (ptr[0] < arr.length) {
			if (arr[ptr[0]] == '\\') {
				ptr[0] += 2;
			} else if (arr[ptr[0]] == '"') {
				return new String(arr, start, ptr[0] - start);
			} else {
				ptr[0]++;
			}
		}
		throw new IllegalArgumentException();
	}

//	public static void main(String[] args) {
//		XPathEngineImpl eng = new XPathEngineImpl();
//		eng.setXPaths(new String[] {"/html/head/title[contains(text(), \"ccc\")]"});
//		System.out.println(eng.heads[0]);
//		System.out.println(eng.isValid(0));
////		OccurrenceEvent event = new OccurrenceEvent("<html>432<rss>xxx</rss></html>");
//		OccurrenceEvent event = new OccurrenceEvent("<HTML><HEAD><TITLE>CSE455/CIS555 HW2 Sample Data</TITLE></HEAD><BODY>\n" + 
//				"<H2 ALIGN=center>CSE455/CIS555 HW2 Sample Data</H2>\n" + 
//				"\n" + 
//				"<P>This page contains some sample data for your second homework \n" + 
//				"assignment. The HTML pages do not contain external links, so you shouldn't \n" + 
//				"have to worry about your crawler &ldquo;escaping&rdquo; to the outside \n" + 
//				"web. The XML files do, however, contain links to external URLs, so \n" + 
//				"you'll need to make sure your crawler does not follow links in XML \n" + 
//				"documents.</P>\n" + 
//				"\n" + 
//				"<H3>RSS Feeds</H3>\n" + 
//				"<UL>\n" + 
//				"<LI><A HREF=\"crawltest/nytimes/\">The New York Times</A></LI>\n" + 
//				"<LI><A HREF=\"crawltest/bbc/\">BBC News</A></LI>\n" + 
//				"<LI><A HREF=\"crawltest/cnn/\">CNN</A></LI>\n" + 
//				"<LI><A HREF=\"crawltest/international/\">News in foreign \n" + 
//				"languages</A></LI>\n" + 
//				"</UL>\n" + 
//				"\n" + 
//				"<H3>Other XML data</H3>\n" + 
//				"<UL>\n" + 
//				"<LI><A HREF=\"crawltest/misc/weather.xml\">Weather data</A></LI>\n" + 
//				"<LI><A HREF=\"crawltest/misc/eurofxref-daily.xml\">Current Euro exchange \n" + 
//				"rate data</A></LI>\n" + 
//				"<LI><A HREF=\"crawltest/misc/eurofxref-hist.xml\">Historical Euro exchange \n" + 
//				"rate data</A></LI>\n" + 
//				"</UL>\n" + 
//				"\n" + 
//				"<H3>Marie's XML data</H3>\n" + 
//				"<UL>\n" + 
//				"<LI><A HREF=\"crawltest/marie/\">More data</A></LI>\n" + 
//				"<LI><A HREF=\"crawltest/marie/private\">Private</A></LI>\n" + 
//				"</UL>\n" + 
//				"\n" + 
//				"\n" + 
//				"</BODY></HTML>");
////		for (Element e :event.getRootNode().getAllElements() ) {
////			System.out.println(e.nodeName());
////		}
//		System.out.println(eng.evaluateEvent(event)[0]);
//	}

}
