package ca.carter.thesis.model.phrasetree;

public abstract class AbstractPhraseTreePart {
	protected PartOfSpeech pos;

	public PartOfSpeech getPos() {
		return pos;
	}
	public void setPos(PartOfSpeech pos) {
		this.pos = pos;
	}

	//for speed, we avoid doing class instance comparisons, and instead implement a simple fixed boolean return.
	public boolean isToken()
	{
		return false;
	}

	public String value()
	{
		return pos.toString();
	}
	
	//a convenience method so that we can avoid some class casting in PhraseTree.toString();
	protected String toString(int indent)
	{
		return null;
	}
	
}
