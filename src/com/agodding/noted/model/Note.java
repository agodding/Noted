package com.agodding.noted.model;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Note {

	private long id = -1;
	private String title;
	private String body;
	private List<String> tags;
	private String password;
	private long timestamp = -1;

	public Note(long id, String title, String body, List<String> tags) {
		this.id = id;
		this.title = title;
		this.body = body;
		this.tags = tags;
		timestamp = System.currentTimeMillis();
	}

	public Note() {
		tags = new ArrayList<String>();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	
	public List<String> addTag(String aTag) {
		tags.add(aTag);
		return tags;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public String getFormattedModDate() {
		Date d = new Date(timestamp);
		DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
		return df.format(d);
	}

}
