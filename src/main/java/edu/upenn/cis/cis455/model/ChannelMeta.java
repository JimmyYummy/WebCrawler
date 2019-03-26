package edu.upenn.cis.cis455.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ChannelMeta implements Serializable{
	private int channelNo;
	private String channelName;
	private String channelCreater;
	private String channelXPath;
	private List<String> urls;
	
	public ChannelMeta(int channelNo, String channelName, String channelCreater, String channelXPath) {
		this.channelNo = channelNo;
		this.channelName = channelName;
		this.channelCreater = channelCreater;
		this.channelXPath = channelXPath;
		this.urls = new ArrayList<>();
	}
	
	/**
	 * @return the channelNo
	 */
	public int getChannelNo() {
		return channelNo;
	}
	/**
	 * @return the channelCreater
	 */
	public String getChannelCreater() {
		return channelCreater;
	}
	/**
	 * @return the chanelXPath
	 */
	public String getChannelXPath() {
		return channelXPath;
	}
	
	public List<String> getUrls() {
		return urls;
	}

	/**
	 * @return the channelName
	 */
	public String getChannelName() {
		return channelName;
	}

}
