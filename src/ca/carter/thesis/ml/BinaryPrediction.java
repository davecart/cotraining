package ca.carter.thesis.ml;

import java.util.Map;

public class BinaryPrediction {
	private double probability;
	private boolean classNumber;
	private Map<Integer, Double> classProbabilities;
	
	
	public BinaryPrediction(boolean classNumber, double probability,
			Map<Integer, Double> classProbabilities) {
		super();
		this.classNumber = classNumber;
		this.probability = probability;
		this.classProbabilities = classProbabilities;
	}
	public BinaryPrediction(boolean classNumber, double probability) {
		super();
		this.probability = probability;
		this.classNumber = classNumber;
	}
	public double getProbability() {
		return probability;
	}
	public void setProbability(double probability) {
		this.probability = probability;
	}
	public boolean getClassNumber() {
		return classNumber;
	}
	public void setClassNumber(boolean classNumber) {
		this.classNumber = classNumber;
	}
	public Map<Integer, Double> getClassProbabilities() {
		return classProbabilities;
	}
	public void setClassProbabilities(Map<Integer, Double> classProbabilities) {
		this.classProbabilities = classProbabilities;
	}
	@Override
	public String toString() {
		return classNumber + ", probability " + probability + " " + classProbabilities;
	}
	
	
	
}
