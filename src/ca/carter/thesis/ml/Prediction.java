package ca.carter.thesis.ml;

import java.text.DecimalFormat;
import java.util.Map;

public class Prediction {
	private double probability;
	private int classNumber;
	private Map<Integer, Double> classProbabilities;
	
	
	public Prediction(int classNumber, double probability,
			Map<Integer, Double> classProbabilities) {
		super();
		this.classNumber = classNumber;
		this.probability = probability;
		this.classProbabilities = classProbabilities;
	}
	public Prediction(int classNumber, double probability) {
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
	public int getClassNumber() {
		return classNumber;
	}
	public void setClassNumber(int classNumber) {
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
		return classNumber + ", probability " + new DecimalFormat("#.###").format(probability) + " " + classProbabilities;
	}
	
	
	
}
