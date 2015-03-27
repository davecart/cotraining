package ca.carter.thesis.model;

import java.util.List;

public class SimpleSentence {
	private String sentence;
	private List<ProductFeatureOpinion> opinions;
	private boolean needsOpinionParsing;
	
	public SimpleSentence(String sentence, boolean needsOpinionParsing) {
		super();
		this.sentence = sentence;
		this.opinions = null;
		this.needsOpinionParsing = needsOpinionParsing;
	}
	public String getSentence() {
		return sentence;
	}
	public void setSentence(String sentence) {
		this.sentence = sentence;
	}
	public List<ProductFeatureOpinion> getOpinions() {
		return opinions;
	}
	public void setOpinions(List<ProductFeatureOpinion> opinions) {
		this.opinions = opinions;
	}
	public boolean isNeedsOpinionParsing() {
		return needsOpinionParsing;
	}
	
	
}
