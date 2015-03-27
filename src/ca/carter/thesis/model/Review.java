package ca.carter.thesis.model;

import java.util.List;

public class Review {
	String title;
	List<String> sentences;
	
	public Review(String title) {
		super();
		this.title = title;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public List<String> getSentences() {
		return sentences;
	}
	public void setSentences(List<String> sentences) {
		this.sentences = sentences;
	}
	
	
}
