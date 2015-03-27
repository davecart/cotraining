package ca.carter.thesis.model.phrasetree;

public class TokenLeaf extends AbstractPhraseTreePart {
	private String token;

	public static final String capitalizedWordIndicator = "c";

	public TokenLeaf(String string, PartOfSpeech pos) throws java.lang.IllegalArgumentException
	{
		super();
		this.token = string;
		this.pos = pos;
	}
	
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
	
	@Override
	public String value()
	{
		return token;
	}
	
	@Override
	public boolean isToken()
	{
		return true;
	}

	@Override
	public String toString() {
		return pos + "(" + token + ")";
	}	
	
	
}
