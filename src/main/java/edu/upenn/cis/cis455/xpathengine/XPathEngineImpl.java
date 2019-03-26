package edu.upenn.cis.cis455.xpathengine;

import java.util.ArrayDeque;
import java.util.Deque;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import edu.upenn.cis.cis455.model.OccurrenceEvent;

public class XPathEngineImpl implements XPathEngine {
	private static PathStep invalidIndicator = new PathStep(null);
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
			PathStep head = genStepList(expressions[i]);
			if (head != null) {
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
		int childSize = docNode.childNodeSize();
		for (int i = 0; i < childSize; i++) {
			Element child = docNode.child(i);
			if (child.nodeName().equals(pathStep.getNodeName())) {
				String childText = child.ownText();
				if (checkTests(childText, pathStep) && checkMatch(docNode, pathStep.next))
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
		if (!exp.startsWith("/"))
			return invalidIndicator;
		exp = exp.substring(1);
		if (exp.length() == 0)
			return null;

		Deque<Character> stack = new ArrayDeque<>();
		char[] arr = exp.toCharArray();
		int[] ptr = new int[0];
		PathStep head = generateNode(arr, ptr, stack);
		if (!stack.isEmpty())
			return invalidIndicator;
		PathStep next = genStepList(exp.substring(ptr[0]));
		if (next == invalidIndicator)
			return next;
		head.next = next;
		return head;
	}

	private PathStep generateNode(char[] arr, int[] ptr, Deque<Character> stack) {
		if (!stack.isEmpty())
			return invalidIndicator;
		PathStep step = null;
		while (ptr[0] < arr.length) {
			if (step == null && (ptr[0] == '/' || ptr[0] == '[')) {
				step = new PathStep(new String(arr, 0, ptr[0]));
			}
			if (ptr[0] == '/') {
				break;
			} else if (ptr[0] == '[') {
				if (step == null)
					return invalidIndicator;
				stack.offerFirst('[');
				ptr[0]++;
				try {
					generateTest(arr, ptr, stack, step);
				} catch (IllegalArgumentException e) {
					return invalidIndicator;
				}
			}
			ptr[0]++;
		}
		if (step == null || !stack.isEmpty())
			return invalidIndicator;
		return step;
	}

	private void generateTest(char[] arr, int[] ptr, Deque<Character> stack, PathStep step) {
		// function: contains -> check if the inner is text, if has a valid str
		// text -> check if has a valid str
		// others -> return exception
		// status code (binary) 00 -> undefined x1: text 1x: contains, thus 10 -> 11 is
		// legal, but 01 -> 11 is not
		int status = 0;
		String textStr = null;
		int funcNameStart = ptr[0];
		while (ptr[0] < arr.length) {
			if (arr[ptr[0]] == '"') {
				if ((status & 0b01) == 1) {
					if (textStr != null)
						throw new IllegalArgumentException();
					ptr[0]++;
					textStr = getTextStr(arr, ptr);
					if ((status & 0b10) == 1) {
						if (!stack.isEmpty() && stack.peekFirst() == ',') {
							stack.pollFirst();
						} else {
							throw new IllegalArgumentException();
						}
					}
				} else {
					throw new IllegalArgumentException();
				}
			} else if (arr[ptr[0]] == ']') {
				if (stack.peekFirst() == '[') {
					stack.pollFirst();
					// post-processing
					if ((status & 0b01) == 0 || textStr == null)
						throw new IllegalArgumentException();
					// add the function to the test list
					if ((status & 0b10) == 1) {
						step.getContains().add(textStr);
					} else {
						step.getTexts().add(textStr);
					}
				} else {
					throw new IllegalArgumentException();
				}
			} else if (arr[ptr[0]] == '(') {
				if ((status & 0b01) == 1) {
					throw new IllegalArgumentException();
				}
				if ((status & 0b10) == 1) {
					// check text
					if ("text".equals(new String(arr, funcNameStart, ptr[0]).replaceAll(" ", ""))) {
						status = status | 0b01;
					} else {
						throw new IllegalArgumentException();
					}
				} else {
					if ("text".equals(new String(arr, funcNameStart, ptr[0]).replaceAll(" ", ""))) {
						status = status | 0b01;
					} else if ("contains".equals(new String(arr, funcNameStart, ptr[0]).replaceAll(" ", ""))) {
						status = status | 0b10;
					} else {
						throw new IllegalArgumentException();
					}
					funcNameStart = ptr[0] + 1;
				}
				stack.offerFirst('(');
			} else if (arr[ptr[0]] == ')') {
				if (stack.peekFirst() == '(') {
					stack.pollFirst();
				} else {
					throw new IllegalArgumentException();
				}
			} else if (arr[ptr[0]] == ',') {
				if (stack.peekFirst() != '(' || status != 0b11) {
					throw new IllegalArgumentException();
				} else {
					stack.offerFirst(',');
				}
			} else if ((status & 0b01) == 1 && arr[ptr[0]] != ' ') {
				throw new IllegalArgumentException();
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
				return new String(arr, start, ptr[0]);
			} else {
				ptr[0]++;
			}
		}
		throw new IllegalArgumentException();
	}

}
