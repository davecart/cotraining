package ca.carter.thesis.evaluation;

import ca.carter.thesis.ml.ModelType;

public class ResultsSummary {
	int numTested;
	int falsePositives;
	double truePositives; //is double for cases where we have a partial result (i.e., multiple aspect-sentiment pairs in a sentence)
	double falseNegatives;
	int trueNegatives;
	ModelType modelType;
	
	public ResultsSummary(int numTested, double truePositives, int trueNegatives, int falsePositives,
			 double falseNegatives, ModelType modelType ) {
		super();
		this.numTested = numTested;
		this.falsePositives = falsePositives;
		this.truePositives = truePositives;
		this.falseNegatives = falseNegatives;
		this.trueNegatives = trueNegatives;
		this.modelType = modelType;
	}


	public ModelType getModelType() {
		return modelType;
	}


	public void setModelType(ModelType modelType) {
		this.modelType = modelType;
	}


	public int getNumTested() {
		return numTested;
	}


	public void setNumTested(int numTested) {
		this.numTested = numTested;
	}


	public int getFalsePositives() {
		return falsePositives;
	}


	public void setFalsePositives(int falsePositives) {
		this.falsePositives = falsePositives;
	}


	public double getTruePositives() {
		return truePositives;
	}


	public void setTruePositives(double truePositives) {
		this.truePositives = truePositives;
	}


	public double getFalseNegatives() {
		return falseNegatives;
	}


	public void setFalseNegatives(double falseNegatives) {
		this.falseNegatives = falseNegatives;
	}


	public int getTrueNegatives() {
		return trueNegatives;
	}


	public void setTrueNegatives(int trueNegatives) {
		this.trueNegatives = trueNegatives;
	}
	
	public void printOutResults()
	{
		System.out.println("True positives: "  + truePositives  + " (" + (100 * truePositives  / numTested) + "%)");
		System.out.println("True negatives: "  + trueNegatives  + " (" + (100 * trueNegatives  / numTested) + "%)");
		System.out.println("False positives: " + falsePositives + " (" + (100 * falsePositives / numTested) + "%)");
		System.out.println("False negatives: " + falseNegatives + " (" + (100 * falseNegatives / numTested) + "%)");
	
		
		System.out.println("Precision: " + getPrecision());
		System.out.println("Recall/sensitivity: " + getRecall());
		System.out.println("Accuracy: " + getAccuracy());
		System.out.println("Specificity: " + getSpecificity());
		System.out.println("F1 = " + getF1());
		System.out.println("Total tested: " + numTested);
		
		System.out.println(toThreePlaces(getPrecision()) + " & " + toThreePlaces(getRecall())  + " & " + toThreePlaces(getF1())  + " & " + toThreePlaces(getAccuracy()));
		
	}
	
	public static double toThreePlaces(double num)
	{
		return Math.round(num * 1000) / 1000.0;
	}
	
	public double getPrecision() {
		if (truePositives == 0)
			return 0.0;
		
		return 1.0 * truePositives / (truePositives + falsePositives);
	}
	public double getRecall() {
		if (truePositives == 0)
			return 0.0;

		return 1.0 * truePositives / (truePositives + falseNegatives);

	}
	public double getAccuracy() {
		return 1.0 * (truePositives + trueNegatives) / (numTested);

	}
	public double getSpecificity() {
		if (trueNegatives == 0)
			return 0.0;
		
		return 1.0 * trueNegatives / (trueNegatives + falsePositives);

	}
	public double getF1() {
		if (truePositives == 0)
			return 0.0;

		return (2.0 * getPrecision() * getRecall() / (getPrecision() + getRecall()) );
	}

}
