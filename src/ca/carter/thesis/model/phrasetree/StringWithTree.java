package ca.carter.thesis.model.phrasetree;

public class StringWithTree {
	private String string;
	private PhraseTree phraseTree;

	public StringWithTree(String string) {
		super();
		this.string = string;
		if (string != null)
			phraseTree = new PhraseTree(string);
	}
	public String getString() {
		return string;
	}
	public void setString(String string) {
		this.string = string;
	}
	public PhraseTree getPhraseTree() {
		return phraseTree;
	}
	public void setPhraseTree(PhraseTree phraseTree) {
		this.phraseTree = phraseTree;
	}
	@Override
	public String toString() {
		return string;
	}

	
}
