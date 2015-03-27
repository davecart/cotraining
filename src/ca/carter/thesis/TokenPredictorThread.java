package ca.carter.thesis;

import ca.carter.thesis.ml.Prediction;
import ca.carter.thesis.ml.SVMTokenModel;
import ca.carter.thesis.model.TokenWithContext;

public class TokenPredictorThread extends Thread {

	private SVMTokenModel model;
	private TokenWithContext nextCotrainingToken;
	private Prediction prediction;
	
	
	
	public TokenPredictorThread(SVMTokenModel model
			) {
		super();
		this.model = model;
	}



	public TokenWithContext getNextCotrainingToken() {
		return nextCotrainingToken;
	}



	public void setNextCotrainingToken(TokenWithContext nextCotrainingToken) {
		this.nextCotrainingToken = nextCotrainingToken;
	}



	public Prediction getPrediction() {
		return prediction;
	}



	public void setPrediction(Prediction prediction) {
		this.prediction = prediction;
	}



	public void run() {
		prediction = model.predict(nextCotrainingToken);
		if (prediction == null)
			System.err.println("Null prediction for " + nextCotrainingToken);
	}
}
