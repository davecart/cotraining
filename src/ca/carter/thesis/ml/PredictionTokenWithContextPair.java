package ca.carter.thesis.ml;

import ca.carter.thesis.model.TokenWithContext;

public class PredictionTokenWithContextPair {
	private TokenWithContext tokenWithContext;
	private Prediction prediction;
	
	
	
	public PredictionTokenWithContextPair(TokenWithContext tokenWithContext,
			Prediction prediction) {
		super();
		this.tokenWithContext = tokenWithContext;
		this.prediction = prediction;
	}
	public TokenWithContext getTokenWithContext() {
		return tokenWithContext;
	}
	public void setTokenWithContext(TokenWithContext tokenWithContext) {
		this.tokenWithContext = tokenWithContext;
	}
	public Prediction getPrediction() {
		return prediction;
	}
	public void setPrediction(Prediction prediction) {
		this.prediction = prediction;
	}
	
	
}
