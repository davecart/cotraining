package ca.carter.thesis.model;

public class SemanticallyTaggedTokenWithContext implements Cloneable {
	private String semanticRole;
	private TokenWithContext tokenWithContext;
	
	
	
	public SemanticallyTaggedTokenWithContext(String semanticRole,
			TokenWithContext tokenWithContext) {
		super();
		this.semanticRole = semanticRole;
		this.tokenWithContext = tokenWithContext;
	}
	public String getSemanticRole() {
		return semanticRole;
	}
	public void setSemanticRole(String semanticRole) {
		this.semanticRole = semanticRole;
	}
	public TokenWithContext getTokenWithContext() {
		return tokenWithContext;
	}
	public void setTokenWithContext(TokenWithContext tokenWithContext) {
		this.tokenWithContext = tokenWithContext;
	}
	
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		SemanticallyTaggedTokenWithContext clone = (SemanticallyTaggedTokenWithContext) super.clone();
		clone.tokenWithContext = (TokenWithContext) tokenWithContext.clone();
		return clone;
	}
	@Override
	public String toString() {
		return "SemanticallyTaggedTokenWithContext [semanticRole="
				+ semanticRole + ", tokenWithContext=" + tokenWithContext.getToken() + "]";
	}
	
	
}
