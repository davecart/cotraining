package ca.carter.thesis.model;

import java.util.ArrayList;
import java.util.List;

import ca.carter.thesis.ml.SVMTokenModelSentiment;

public class ReconciledFeatureOpinion {
	private List<TokenWithContext> tokensWithFeature = new ArrayList<TokenWithContext>();
	private List<TokenWithContext> tokensWithSentiment = new ArrayList<TokenWithContext>();
	private ProductFeatureOpinion opinion;
	private List<Sentiment> sentiment = new ArrayList<Sentiment>();
	
	
	public ReconciledFeatureOpinion(ProductFeatureOpinion opinion) {
		super();
		this.opinion = opinion;
	}
	public ProductFeatureOpinion getOpinion() {
		return opinion;
	}
	public void setOpinion(ProductFeatureOpinion opinion) {
		this.opinion = opinion;
	}
	public List<TokenWithContext> getTokensWithFeature() {
		return tokensWithFeature;
	}
	public void setTokensWithFeature(List<TokenWithContext> tokensWithFeature) {
		this.tokensWithFeature = tokensWithFeature;
	}
	public List<TokenWithContext> getTokensWithSentiment() {
		return tokensWithSentiment;
	}
	public void setTokensWithSentiment(List<TokenWithContext> tokensWithSentiment) {
		this.tokensWithSentiment = tokensWithSentiment;
	}
	public List<Sentiment> getSentiment() {
		return sentiment;
	}
	public void setSentiment(ArrayList<Sentiment> sentiment) {
		this.sentiment = sentiment;
	}
	
	public void addSentimentToken(TokenWithContext sentiToken)
	{
		tokensWithSentiment.add(sentiToken);
		
		//the classifier is very good at deciding whether a word is sentiment-bearing; it is not as good at deciding whether rarely-seen words are positive or negative; so, if it exists in our lexicon, trust that; otherwise, use the prediction; if word is not in lexicon, only use the prediction
		Sentiment lexicalizedSentiment = SVMTokenModelSentiment.decodeClassNumber(SVMTokenModelSentiment.lookupClassForToken(sentiToken));
		
		Sentiment predictedSentiment = null;
		if (lexicalizedSentiment != Sentiment.OBJ)
		{
			//if (predictedSentiment != lexicalizedSentiment)
			//	System.out.println("Correcting sentiment for '" + sentiToken.getToken() + "' from " + SVMTokenModelSentiment.decodeClassNumber(sentiToken.getPredictedClass()) + " (predicted) to " + lexicalizedSentiment + " (according to lexicon).");
			
			predictedSentiment = lexicalizedSentiment;
		}
		else
			predictedSentiment = SVMTokenModelSentiment.decodeClassNumber(sentiToken.getPredictedClass());
		
		
		//System.out.println("Predicted " + sentiToken.getPredictedClass());
		
		if (sentiToken.isSemanticOutgoingEdgesIncludeNegation() && predictedSentiment == Sentiment.POS)
			sentiment.add(Sentiment.NEG);
		else if (sentiToken.isSemanticOutgoingEdgesIncludeNegation() && predictedSentiment == Sentiment.NEG)
			sentiment.add(Sentiment.POS);
		else
			sentiment.add(predictedSentiment);

	}

	public boolean isComplete()
	{
		if (tokensWithFeature == null || tokensWithFeature.isEmpty())
			return false;
		if (tokensWithSentiment == null || tokensWithSentiment.isEmpty())
			return false;
		return true;
	}

	//use voting among sentiments to see how we did
	public boolean isCorrect()
	{
		//biggest gap among tokens must be no greater than one token (for simplicity of business logic)
		if (tokensWithFeature == null || tokensWithFeature.isEmpty())
			return false;
		if (tokensWithSentiment == null || tokensWithSentiment.isEmpty())
			return false;
		
		//check to see if we got the sentiment polarity correct
		int numPos = 0;
		int numNeg = 0;
		boolean sentimentIsCorrect = false;
		for (Sentiment nextSentiment : sentiment)
		{
			switch (nextSentiment)
			{
			case POS:
				numPos++;
				break;
			case NEG:
				numNeg++;
				break;
			}
		}
		if (numPos > numNeg && opinion.getSentiment() == Sentiment.POS)
			sentimentIsCorrect = true;
		else if (numNeg > numPos && opinion.getSentiment() == Sentiment.NEG)
			sentimentIsCorrect = true;
		else
			return false;
			
		//TODO: maybe assign score based on getting more tokens correct and correctly predicting the feature
		if (sentimentIsCorrect)
			return true;
		
		return false;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("ReconciledFeatureOpinion [tokensWithFeature=");
		if (tokensWithFeature == null)
			sb.append("null");
		else
		{
			boolean first = true;
			sb.append("[");
			for (TokenWithContext token : tokensWithFeature)
			{
				if (!first)
					sb.append(", ");
				first = false;
				sb.append(token.getToken());
			}
			sb.append("]");
		}
		sb.append(", tokensWithSentiment=");
		if (tokensWithSentiment == null)
			sb.append("null");
		else
		{
			boolean first = true;
			sb.append("[");
			for (TokenWithContext token : tokensWithSentiment)
			{
				if (!first)
					sb.append(", ");
				first = false;
				sb.append(token.getToken());
			}
			sb.append("]");
		}
		sb.append(", opinion=").append(opinion);

		sb.append(", sentiment=");
		if (sentiment == null)
			sb.append("null");
		else
		{
			boolean first = true;
			sb.append("[");
			for (Sentiment nextSentiment : sentiment)
			{
				if (!first)
					sb.append(", ");
				first = false;
				sb.append(nextSentiment);
			}
			sb.append("]");
		}
		sb.append("]");
				
		return sb.toString();
	}
	
	/*
	public static void main(String[] args)
	{
		Sentence sentence = new Sentence("screen[+2],sound[+2]##great screen and great sound .");

		ReconciledFeatureOpinion firstOpinion = 
	
	}
	*/
	
	
}
