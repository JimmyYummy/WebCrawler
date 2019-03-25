package edu.upenn.cis.cis455.xpathengine;

import java.util.ArrayDeque;
import java.util.Deque;

import edu.upenn.cis.cis455.model.OccurrenceEvent;

public class XPathEngineImpl implements XPathEngine {
	private PathStep[] heads;
	private boolean[] isValidPaths;

	@Override
	public void setXPaths(String[] expressions) {
		// TODO Auto-generated method stub
		int len =  expressions.length;
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
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * return the head of the XPath if the expression is valid, else return null
	 */
	private PathStep genStepList(String exp) {
		if (exp == null) return null;
		if (! exp.startsWith("/")) return null;
		exp = exp.substring(1);
		if (exp.length() == 0) return null;
		
		Deque<Character> stack = new ArrayDeque<>();
		for (int i = 0; i < exp.length(); i++) {
			
		}
		
		return null;
	}
}
