package edu.upenn.cis.cis455.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.handlers.LoginHandler;

public class RobotResolver {

	private static Logger logger = LogManager.getLogger(RobotResolver.class);

	private int delay;
	private Collection<String> generalAllows;
	private Collection<String> generalDisallows;
	private Collection<String> specificAllows;
	private Collection<String> specificDisallows;
	private Collection<String> visitedPaths;
	private boolean websiteOK;
	private long lastVisit;

	public RobotResolver(String urlStr) {
		logger.info("Creating new robots.txt resolver on url:" + urlStr);
		generalAllows = new HashSet<>();
		generalDisallows = new HashSet<>();
		specificAllows = new HashSet<>();
		specificDisallows = new HashSet<>();
		visitedPaths = new HashSet<>();
		try {
			logger.info("polling the robots.txt on url: " + urlStr);
			URL url = new URL(urlStr + "/robots.txt");
			InputStream inputStream = null;
			inputStream = getInputStream(url);

			// set the website being not working is code is not 404 or 200
			if (inputStream == null) {
				logger.info("Error while parsing the robots.txt, return.");
				return;
			}

			logger.debug("Parsing the robots.txt");
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
			String inputLine;
			StringBuilder response = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
				response.append("\n");
			}
			in.close();
			// print result
			String txt = response.toString();
			logger.debug("Received the robots.txt: \n" + txt);
			fillInPattern(txt, generalAllows, generalDisallows, "*");
			fillInPattern(txt, specificAllows, specificDisallows, "cis455crawler");
			websiteOK = true;
			lastVisit = Instant.now().getEpochSecond();
			System.err.println(generalAllows);
			System.err.println(generalDisallows);
			logger.info("created robot resolver: " + urlStr);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private InputStream getInputStream(URL url) throws IOException {
		InputStream inputStream = null;
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		int statusCode = conn.getResponseCode();
		if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
			logger.info("robots.txt not found, return");
			websiteOK = true;
			return null;
		} else if (statusCode == 301 || statusCode == 302) {
			logger.info("Redirecting the robots.txt url");
			return getInputStream(new URL(conn.getHeaderField("Location")));
		} else if (statusCode == HttpURLConnection.HTTP_OK) { // success
			inputStream = conn.getInputStream();
		} else {
			System.err.println(statusCode);
		}

		return inputStream;
	}

	private void fillInPattern(String txt, Collection<String> allows, Collection<String> disallows, String name) {
		String[] strs = txt.split("User-agent:");
		for (int i = 0; i < strs.length; i++) {
			strs[i] = strs[i].trim();
		}
		for (String sec : strs) {
			if (sec.startsWith(name + "\n") || sec.startsWith(name + "\r\n")) {
				String[] lines = sec.split("(\r\n|\n)");
				for (String line : lines) {
					if (!line.contains(":")) {
						continue;
					}
					String[] pair = line.split(":");
					String key = pair[0].trim().toLowerCase();
					String val = pair[1].trim();
					if (key.equals("delay")) {
						delay = Integer.parseInt(val);
					} else if (key.equals("allow")) {
						String path = Paths.get(val).normalize().toString();
						if (path.endsWith("*"))
							path = path.substring(0, path.length() - 1);
						allows.add(path);
					} else if (key.equals("disallow")) {
						String path = Paths.get(val).normalize().toString();
						if (path.endsWith("*"))
							path = path.substring(0, path.length() - 1);
						disallows.add(path);
					}
				}
			}
		}

	}

	public boolean isWebsiteOK() {
		return websiteOK;
	}

	public synchronized boolean shouldDefer() {
		if (!websiteOK)
			return true;
		return Instant.now().getEpochSecond() - lastVisit <= delay;
	}

	public boolean isOKtoParse(String filePath) {
		if (!websiteOK)
			return false;
		String path = Paths.get(filePath).normalize().toString();
		if (!visitedPaths.add(path))
			return false;
		int speRank = isAllowed(specificAllows, specificDisallows, path);
		if (speRank == 1)
			return true;
		else if (speRank == -1)
			return false;
		return isAllowed(generalAllows, generalDisallows, path) >= 0;
	}

	// allowed 1 / disallowed -1 / unspecified 0
	private static int isAllowed(Collection<String> allows, Collection<String> disallows, String fpath) {
		int allowRank = matchPath(allows, fpath);
		int disallowRank = matchPath(disallows, fpath);
		if (allowRank > disallowRank)
			return 1;
		else if (allowRank < disallowRank)
			return -1;
		else if (allowRank == -1)
			return 0;
		return 1;
	}

	private static int matchPath(Collection<String> pathSet, String fpath) {
		int rslt = -1;
		for (String path : pathSet) {
			int curRslt = checkPathMatch(path, fpath);
			if (curRslt > rslt)
				rslt = curRslt;
		}
		return rslt;
	}

	private static int checkPathMatch(String path1, String path2) {
		if (checkPathMatch(0, 0, path1, path2)) {
			return path1.length();
		}
		return -1;
	}

	private static boolean checkPathMatch(int idx1, int idx2, String path1, String path2) {
		if (idx1 == path1.length())
			return true;
		if (idx2 == path2.length())
			return (idx1 == path1.length() - 1 && path1.charAt(idx1) == '$');
		if (path1.charAt(idx1) == '*') {
			return checkPathMatch(idx1 + 1, idx2, path1, path2) || checkPathMatch(idx1, idx2 + 1, path1, path2);
		}
		if (path1.charAt(idx1) != path2.charAt(idx2))
			return false;
		return checkPathMatch(idx1 + 1, idx2 + 1, path1, path2);
	}

	public static void main(String[] args) {
		org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.DEBUG);
		RobotResolver r = new RobotResolver("https://google.com");
	}
}
