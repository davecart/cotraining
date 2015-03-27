package ca.carter.thesis;

import ca.carter.thesis.ml.SVMTokenModel;

public class RetrainingThread extends Thread {

	SVMTokenModel model;
	
	
	public RetrainingThread(SVMTokenModel model) {
		super();
		this.model = model;
	}
	
	public void run() {
		model.retrain(null);
	}
}
