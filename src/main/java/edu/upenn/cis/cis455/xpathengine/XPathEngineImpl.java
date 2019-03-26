package edu.upenn.cis.cis455.xpathengine;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import edu.upenn.cis.cis455.model.OccurrenceEvent;
import edu.upenn.cis.cis455.stormLiteCrawler.XPathMatchingBolt;

public class XPathEngineImpl implements XPathEngine {
	private static Logger logger = LogManager.getLogger(XPathEngineImpl.class);

	private static PathStep invalidIndicator = new PathStep("INVALID_INDICATOR");
	private PathStep[] heads;
	private boolean[] isValidPaths;
	private int len;

	@Override
	public void setXPaths(String[] expressions) {
		// TODO Auto-generated method stub
		len = expressions.length;
		heads = new PathStep[len];
		isValidPaths = new boolean[len];
		for (int i = 0; i < len; i++) {
			PathStep head = null;
			try {
				head = genStepList(expressions[i]);
			} catch (Exception e){
				logger.catching(Level.DEBUG, e);
			}
			
			logger.debug(String.format("%dth XPath: %s", i, head.toString()));
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
		Element root = event.getRootNode();
		boolean[] results = new boolean[len];
		for (int i = 0; i < len; i++) {
			if (isValidPaths[i]) {
				results[i] = checkMatch(root, heads[i]);
			}
		}
		return results;
	}

	private boolean checkMatch(Element docNode, PathStep pathStep) {
		if (pathStep == null) return true;
		logger.debug("checking node: " + docNode.nodeName());
		Elements children = docNode.children();
		int childSize = children.size();
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

	/*
	 * return the head of the XPath if the expression is valid, else return null
	 */
	private PathStep genStepList(String exp) {
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
		Deque<Character> stack = new ArrayDeque<>();
		char[] arr = exp.toCharArray();
		int[] ptr = new int[1];
		PathStep head = generateNode(arr, ptr, stack);
		if (head == invalidIndicator) return head;
		if (!stack.isEmpty())
			return invalidIndicator;
		if (exp.length() <= ptr[0]) return head;
		exp = exp.substring(ptr[0]);
		logger.debug("generating the next node with exp: " + exp);
		PathStep next = genStepList(exp);
		if (next == invalidIndicator)
			return next;
		head.next = next;
		return head;
	}

	private PathStep generateNode(char[] arr, int[] ptr, Deque<Character> stack) {
		if (!stack.isEmpty())
			return invalidIndicator;
		PathStep step = null;
		while (ptr[0] <= arr.length) {
			if (step == null && (ptr[0] >= arr.length || arr[ptr[0]] == '/' || arr[ptr[0]] == '[' )) {
				step = new PathStep(new String(arr, 0, ptr[0]));
				
			}
			if (ptr[0] >= arr.length || arr[ptr[0]] == '/') {
				break;
			}
			
			if (arr[ptr[0]] == '[') {
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
					if (ptr[0] >= arr.length) break;
				}
				ptr[0]--;
			}
			if (!stack.isEmpty()) {
				logger.debug("stack not cleaned");
				return invalidIndicator;
			}
			ptr[0]++;
		}
		return step;
	}

	private void generateTest(char[] arr, int[] ptr, Deque<Character> stack, PathStep step) {
		// function: contains -> check if the inner is text, if has a valid str
		// text -> check if has a valid str
		// others -> return exception
		// status code (binary) 00 -> undefined x1: text 1x: contains, thus 10 -> 11 is
		// legal, but 01 -> 11 is not
//		System.out.println(new String(arr, ptr[0], arr.length - ptr[0]));
		int status = 0;
		int numOfEqualSigns = 0;
		String textStr = null;
		int funcNameStart = ptr[0];
		while (ptr[0] < arr.length) {
			if (arr[ptr[0]] == '"') {
				if ((status & 0b01) == 1) {
					if (textStr != null) {
						throw new IllegalArgumentException("string defined too early");
					}
					ptr[0]++;
					textStr = getTextStr(arr, ptr);
					if ((status & 0b10) == 0b10) {						
						if (!stack.isEmpty() && stack.peekFirst() == ',') {
							stack.pollFirst();
						} else {
							throw new IllegalArgumentException("string defined too early");
						}
					}
				} else {
					throw new IllegalArgumentException("text term not been met");
				}
			} else if (arr[ptr[0]] == ']') {
				if (stack.peekFirst() == '[') {
					stack.pollFirst();
					// post-processing
					if ((status & 0b01) == 0 || textStr == null) {
						throw new IllegalArgumentException("func term end without seeing the text() or string");
					}
					// add the function to the test list
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
			} else if (arr[ptr[0]] == '(') {
				if ((status & 0b01) == 1) {
					throw new IllegalArgumentException("text() has seen, not '(' allowed");
				}
				if ((status & 0b10) == 0b10) {
					// check text
					if ("text".equals(new String(arr, funcNameStart, ptr[0] - funcNameStart).replaceAll(" ", ""))) {
						status = status | 0b01;
					} else {
						throw new IllegalArgumentException("the function name is not text but " 
								+ new String(arr, funcNameStart, ptr[0] - funcNameStart).replaceAll(" ", ""));
					}
				} else {
					if ("text".equals(new String(arr, funcNameStart, ptr[0] - funcNameStart).replaceAll(" ", ""))) {
						status = status | 0b01;
					} else if ("contains".equals(new String(arr, funcNameStart, ptr[0] - funcNameStart).replaceAll(" ", ""))) {
						status = status | 0b10;
					} else {
						throw new IllegalArgumentException("the function name is not text/contains but " 
								+ new String(arr, funcNameStart, ptr[0] - funcNameStart).replaceAll(" ", ""));
					}
					funcNameStart = ptr[0] + 1;
				}
				stack.offerFirst('(');
			} else if (arr[ptr[0]] == ')') {
				if (stack.peekFirst() == '(') {
					stack.pollFirst();
				} else {
					throw new IllegalArgumentException("pushing '(', expecting '(' in stack but is " + stack);
				}
			} else if (arr[ptr[0]] == ',') {
				if (stack.peekFirst() != '(' || status != 0b11) {
					throw new IllegalArgumentException("pushing ',', expecting '(' in stack but is " + stack);
				} else {
					stack.offerFirst(',');
				}
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
			} else if ((status & 0b01) == 1 && arr[ptr[0]] != ' ') {
				throw new IllegalArgumentException("illegal char after seeing text: " + arr[ptr[0]]);
			}
			ptr[0]++;
		}
	}

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
	
	public static void main(String[] args) {
		XPathEngineImpl eng = new XPathEngineImpl();
		eng.setXPaths(new String[] {"/html/head/title[contains(text(), \"ccc\")]"});
		System.out.println(eng.heads[0]);
		System.out.println(eng.isValid(0));
//		OccurrenceEvent event = new OccurrenceEvent("<html>432<rss>xxx</rss></html>");
		OccurrenceEvent event = new OccurrenceEvent("<HTML><HEAD><TITLE>CSE455/CIS555 HW2 Sample Data</TITLE></HEAD><BODY>\n" + 
				"<H2 ALIGN=center>CSE455/CIS555 HW2 Sample Data</H2>\n" + 
				"\n" + 
				"<P>This page contains some sample data for your second homework \n" + 
				"assignment. The HTML pages do not contain external links, so you shouldn't \n" + 
				"have to worry about your crawler &ldquo;escaping&rdquo; to the outside \n" + 
				"web. The XML files do, however, contain links to external URLs, so \n" + 
				"you'll need to make sure your crawler does not follow links in XML \n" + 
				"documents.</P>\n" + 
				"\n" + 
				"<H3>RSS Feeds</H3>\n" + 
				"<UL>\n" + 
				"<LI><A HREF=\"crawltest/nytimes/\">The New York Times</A></LI>\n" + 
				"<LI><A HREF=\"crawltest/bbc/\">BBC News</A></LI>\n" + 
				"<LI><A HREF=\"crawltest/cnn/\">CNN</A></LI>\n" + 
				"<LI><A HREF=\"crawltest/international/\">News in foreign \n" + 
				"languages</A></LI>\n" + 
				"</UL>\n" + 
				"\n" + 
				"<H3>Other XML data</H3>\n" + 
				"<UL>\n" + 
				"<LI><A HREF=\"crawltest/misc/weather.xml\">Weather data</A></LI>\n" + 
				"<LI><A HREF=\"crawltest/misc/eurofxref-daily.xml\">Current Euro exchange \n" + 
				"rate data</A></LI>\n" + 
				"<LI><A HREF=\"crawltest/misc/eurofxref-hist.xml\">Historical Euro exchange \n" + 
				"rate data</A></LI>\n" + 
				"</UL>\n" + 
				"\n" + 
				"<H3>Marie's XML data</H3>\n" + 
				"<UL>\n" + 
				"<LI><A HREF=\"crawltest/marie/\">More data</A></LI>\n" + 
				"<LI><A HREF=\"crawltest/marie/private\">Private</A></LI>\n" + 
				"</UL>\n" + 
				"\n" + 
				"\n" + 
				"</BODY></HTML>");
//		for (Element e :event.getRootNode().getAllElements() ) {
//			System.out.println(e.nodeName());
//		}
		System.out.println(eng.evaluateEvent(event)[0]);
	}

}
