package ca.carter.thesis.model;

import java.util.List;

import ca.carter.thesis.ml.ModelType;
import ca.carter.thesis.model.phrasetree.PartOfSentimentStructure;
import ca.carter.thesis.model.phrasetree.PartOfSpeech;

public class TokenWithContext implements Cloneable {
	private String token;
	private String lemma;
	private PartOfSpeech pos;
	private List<TokenWithContext> previousTokens;
	private List<TokenWithContext> nextTokens;
	private List<PartOfSpeech> parentage; //the clause hierarchy above this token
	private boolean isNamedEntity;
	private boolean isCoreferenceHead;
	private String flatResolvedCoreference;
	private String attribute;					//if adjective, what attribute the adjective describes, according to WordNet
	private int positionInSentence;
	
	//for inside/outside tagging; i.e., named entity, feature, sentiment, null
	private PartOfSentimentStructure partOfSentimentStructure;
	
	//dependency graph features
	private String semanticSpecificRole;
	private String semanticGeneralRole;
	private TokenWithContext semanticIncomingEdge;
	private boolean semanticOutgoingEdgesIncludeNegation;
	private List<SemanticallyTaggedTokenWithContext> semanticallyTaggedTokensWithContext;
	
	private ProductFeatureOpinion opinion;
	
	private ModelType predictedModel = null;
	private Double predictedClass = null;
	
	public TokenWithContext(int positionInSentence, String token, String lemma, PartOfSpeech pos,
			List<TokenWithContext> previousTokens,
			List<TokenWithContext> nextTokens, List<PartOfSpeech> parentage, boolean isNamedEntity) {
		super();
		this.positionInSentence = positionInSentence;
		this.token = token;
		this.lemma = lemma;
		this.pos = pos;
		this.previousTokens = previousTokens;
		this.nextTokens = nextTokens;
		this.parentage = parentage;
		this.isNamedEntity = isNamedEntity;
		
	}
	

	public int getPositionInSentence() {
		return positionInSentence;
	}
	public void setPositionInSentence(int positionInSentence) {
		this.positionInSentence = positionInSentence;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getLemma() {
		return lemma;
	}
	public void setLemma(String lemma) {
		this.lemma = lemma;
	}
	public PartOfSpeech getPos() {
		return pos;
	}
	public void setPos(PartOfSpeech pos) {
		this.pos = pos;
	}
	public PartOfSentimentStructure getPartOfSentimentStructure() {
		return partOfSentimentStructure;
	}
	public void setPartOfSentimentStructure(
			PartOfSentimentStructure partOfSentimentStructure) {
		this.partOfSentimentStructure = partOfSentimentStructure;
	}
	public List<TokenWithContext> getPreviousTokens() {
		return previousTokens;
	}
	public void setPreviousTokens(List<TokenWithContext> previousTokens) {
		this.previousTokens = previousTokens;
	}
	public List<TokenWithContext> getNextTokens() {
		return nextTokens;
	}
	public void setNextTokens(List<TokenWithContext> nextTokens) {
		this.nextTokens = nextTokens;
	}
	public boolean isAdjective()
	{
		if (pos == null)
			return false;
					
		if (pos == PartOfSpeech.JJ || pos == PartOfSpeech.JJR || pos == PartOfSpeech.JJS)
			return true;
		
		return false;
	}
	public List<PartOfSpeech> getLocalParentage() {
		//returns only the portion of the parentage up to the next S or SBAR (whichever is higher); so
		//[S, VP, NP, SBAR, S, VP, PP, NP, SBAR, S, VP, SBAR, S, VP, PP, NP] becomes
		//                                             [SBAR, S, VP, PP, NP] instead
		
		if (parentage == null)
			return null;
		
		for (int index = parentage.size() - 1; index >= 0; index--)
		{
			if (parentage.get(index) == PartOfSpeech.S)
			{
				//lookbehind
				if (index > 0 && parentage.get(index - 1) == PartOfSpeech.SBAR)
					index--;

				return parentage.subList(index, parentage.size());
			}
		}
			
		return parentage;
	}
	public List<PartOfSpeech> getParentage() {
		//returns all the clauses of which this is a part; could look like [S, NP] in a simple case, or
		//[S, VP, NP, SBAR, S, VP, PP, NP, SBAR, S, VP, SBAR, S, VP, PP, NP] in an ugly case
		return parentage;
	}
	public void setParentage(List<PartOfSpeech> parentage) {
		this.parentage = parentage;
	}
	public boolean isCoreferenceHead() {
		return isCoreferenceHead;
	}
	public void setCoreferenceHead(boolean isCoreferenceHead) {
		this.isCoreferenceHead = isCoreferenceHead;
	}
	public String getFlatResolvedCoreference() {
		return flatResolvedCoreference;
	}
	public void setFlatResolvedCoreference(String flatResolvedCoreference) {
		this.flatResolvedCoreference = flatResolvedCoreference;
	}
	public PartOfSpeech getImmediateParent() {
		if (parentage == null || parentage.isEmpty())
			return null;
		else
			return parentage.get(0);
	}
	public TokenWithContext getPreviousToken() {
		if (previousTokens == null || previousTokens.isEmpty())
			return null;
		else
			return previousTokens.get(previousTokens.size() - 1);
	}
	public TokenWithContext getNextToken() {
		if (nextTokens == null || nextTokens.isEmpty())
			return null;
		else
			return nextTokens.get(0);
	}
	
	
	//not part of classifier feature set
	public ModelType getPredictedModel() {
		return predictedModel;
	}
	public void setPredictedModel(ModelType predictedModel) {
		this.predictedModel = predictedModel;
	}
	public Double getPredictedClass() {
		return predictedClass;
	}
	public void setPredictedClass(Double predictedClass) {
		this.predictedClass = predictedClass;
	}


	public List<SemanticallyTaggedTokenWithContext> getSemanticallyTaggedTokensWithContext() {
		return semanticallyTaggedTokensWithContext;
	}
	public void setSemanticallyTaggedTokensWithContext(
			List<SemanticallyTaggedTokenWithContext> semanticallyTaggedTokensWithContext) {
		this.semanticallyTaggedTokensWithContext = semanticallyTaggedTokensWithContext;
	}
	public String getSemanticSpecificRole() {
		return semanticSpecificRole;
	}
	public void setSemanticSpecificRole(String semanticSpecificRole) {
		this.semanticSpecificRole = semanticSpecificRole;
	}
	public String getSemanticGeneralRole() {
		return semanticGeneralRole;
	}
	public void setSemanticGeneralRole(String semanticGeneralRole) {
		this.semanticGeneralRole = semanticGeneralRole;
	}
	public TokenWithContext getSemanticIncomingEdge() {
		return semanticIncomingEdge;
	}
	public void setSemanticIncomingEdge(TokenWithContext semanticIncomingEdge) {
		this.semanticIncomingEdge = semanticIncomingEdge;
	}
	public boolean isSemanticOutgoingEdgesIncludeNegation() {
		return semanticOutgoingEdgesIncludeNegation;
	}
	public void setSemanticOutgoingEdgesIncludeNegation(
			boolean semanticOutgoingEdgesIncludeNegation) {
		this.semanticOutgoingEdgesIncludeNegation = semanticOutgoingEdgesIncludeNegation;
	}
	public boolean isNamedEntity() {
		return this.isNamedEntity;
	}
	public void setNamedEntity(boolean isNamedEntity) {
		this.isNamedEntity = isNamedEntity;
	}
	public String getAttribute() {
		return attribute;
	}
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}


	//not part of classifier features
	public ProductFeatureOpinion getOpinion() {
		return opinion;
	}
	public void setOpinion(ProductFeatureOpinion opinion) {
		this.opinion = opinion;
	}
	
	
	private String flattenTokenList(List<TokenWithContext> list)
	{
		if (list == null)
			return "[<null list>]";
		
		StringBuilder sb = new StringBuilder();
		
		boolean first = true;
		sb.append("[");
		for (TokenWithContext nextToken : list)
		{
			if (first)
				first = false;
			else
				sb.append(",");
				
			if (nextToken == null)
				sb.append("null");
			else
				sb.append(nextToken.getToken());
		}
		sb.append("]");

		return sb.toString();
	}
	
	
	public String getFormattedTokenContext()
	{
		StringBuilder sb = new StringBuilder();
		
		if (this.getPreviousTokens() != null)
		{
			for (TokenWithContext toPrint : this.getPreviousTokens())
			{
				if (toPrint != null)
					sb.append(toPrint.getToken()).append(" ");
			}
		}
		
		
		sb.append("_").append(this.getToken()).append("_ ");
		
		if (this.getPreviousTokens() != null)
		{
			for (TokenWithContext toPrint : this.getNextTokens())
			{
				if (toPrint != null)
					sb.append(toPrint.getToken()).append(" ");
			}
		}
			
		

		return sb.toString();
		
	}
	
	
	


	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		TokenWithContext clone = (TokenWithContext) super.clone();

		clone.semanticallyTaggedTokensWithContext = null;

		clone.previousTokens = null;
	
		clone.nextTokens = null;
			
		clone.parentage = null;

		clone.semanticIncomingEdge = null;
			
		return clone;
	}

	
	@Override
	public String toString() {
		
		return "TokenWithContext [token=" + token + ", lemma=" + lemma
				+ ", pos=" + pos + ", previousTokens=" + flattenTokenList(previousTokens)
				+ ", nextTokens=" + flattenTokenList(nextTokens) + ", parentage=" + parentage
				+ ", localParentage=" + getLocalParentage()
				+ ", isNamedEntity=" + isNamedEntity
				+ ", isCoreferenceHead=" + isCoreferenceHead
				+ ", flatResolvedCoreference=" + flatResolvedCoreference
				+ ", partOfSentimentStructure=" + partOfSentimentStructure
				+ ", semanticSpecificRole=" + semanticSpecificRole
				+ ", semanticGeneralRole=" + semanticGeneralRole
				+ ", semanticIncomingEdge=" + (semanticIncomingEdge == null ? "null" : semanticIncomingEdge.getToken() + "-" + semanticIncomingEdge.getPos() + "-" + semanticIncomingEdge.getSemanticSpecificRole() )
 				+ ", semanticOutgoingEdgesIncludeNegation=" + semanticOutgoingEdgesIncludeNegation
 				+ ", semanticallyTaggedTokensWithContext=" + semanticallyTaggedTokensWithContext
 				+ ", opinion=" + opinion
 				+ ", attribute=" + attribute
				+ "]";
	}
	
	

	

}
