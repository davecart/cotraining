package ca.carter.thesis.ml;

import java.io.Writer;
import java.util.List;

import ca.carter.thesis.model.Task;
import ca.carter.thesis.model.TokenWithContext;
import ca.carter.thesis.model.phrasetree.PartOfSentimentStructure;


public class SVMTokenModelFeature extends SVMTokenModel {

	//up to March 16th, was using 8 and 0.0078125 with reasonably good results
	//tried log average of 2.639 and 0.00592 and had terrible results
	//tried numeric average of 4.1v and 0.00664
	
	private static final double svmC = 8; //64; //128; //2; //33.612;  //102.8; //55.72; // 2.63;
	private static final double svmGamma = 0.0078125; //0.0078125; //6.103515625e-05; //0.0013810679; //0.00012207; //0.0025771639; //0.0048828; //0.0016601563;	//0.0001220703125, 0.0001220703125, 0.0078125, 0.0001220703125, 0.0001220703125
 	private static final double svmEpsilon = 1.0E-3;
	
 	//for single view
// 	private static final double[] svmCForViews = {13.9288090127, 0.1649384888};
// 	private static final double[] svmGammaForViews = {0.0078125, 0.2871745887};
 	
 	//hand-tuned for views
 	private static final double[] svmCForViews = {1.265625, 39.0625}; // {1.265625, 39.0625} <--bestyet-- {1.265625, 39.0625} <--slightlyworse?-- {2.53125, 19.53125} <--bestyet-- {2.53125, 19.53125} <--disimprovement-- {3.375, 15.625} <--merelyrollingbackto-- {3.375, 15.625} <-?- {4.5, 12.5} <--noticeableimprovement-- {6, 10} <--noticeableimprovement--{10,6} <--disimproves-- {8,8}
 	//private static final double[] svmGammaForViews = {0.0076293945 * 4.5, 0.0078125 * 3}; // {0.0076293945, 0.0078125 * 2} <--bestyet-- {..., ... / 2} <--slightlyworse?-- {0.015258789, 0.0078125} <--bestyet-- {0.0091552732, 0.0078125} <--disimprovement-- {0.0091552732, 0.0078125} <--merelyrollingbackto-- {0.0091552732, 0.0078125} <-?- {0.012207031, 0.0078125} <--noticeableimprovement-- {0.009765625, 0.0078125} <--noticeableimprovement-- {0.0078125, 0.009765625} <--disimproves-- {0.0078125,0.0078125}
 	private static final double[] svmGammaForViews = {0.0076293945 * 4.5, 0.0078125 * 3}; // {0.0076293945, 0.0078125 * 2} <--bestyet-- {..., ... / 2} <--slightlyworse?-- {0.015258789, 0.0078125} <--bestyet-- {0.0091552732, 0.0078125} <--disimprovement-- {0.0091552732, 0.0078125} <--merelyrollingbackto-- {0.0091552732, 0.0078125} <-?- {0.012207031, 0.0078125} <--noticeableimprovement-- {0.009765625, 0.0078125} <--noticeableimprovement-- {0.0078125, 0.009765625} <--disimproves-- {0.0078125,0.0078125}

 	//auto-tuned with 80%
// 	private static final double[] svmCForViews = {13.929, 2};
// 	private static final double[] svmGammaForViews = {0.0059207678, 0.0717936472};

 	//auto-tuned with 20%
// 	private static final double[] svmCForViews = {13.9288090127, 0.1649384888}; 
// 	private static final double[] svmGammaForViews = {0.0078125, 0.2871745887};

 	
	public SVMTokenModelFeature(Task task, List<TokenWithContext> tokens, Writer[] fileToOutput, ClassWeighting classWeighting, Double c, Double gamma, Double epsilon) {
		super(task, tokens, fileToOutput, classWeighting, c, gamma, epsilon);
	}

	@Override
	public Double getClassForToken(TokenWithContext token)
	{	
		if (token.getPartOfSentimentStructure() == PartOfSentimentStructure.FEATURE)
			return 1.0;
		else
			return 0.0;
	}

	@Override
	public String getName() {
		return "product feature";
	}

	@Override
	public double getC(int viewNum) {
		if (this.specifiedC != null)
			return this.specifiedC;
		else
		{
			if (useViews)
				return svmCForViews[viewNum];
			else
				return svmC;
		}
	}

	@Override
	public double getGamma(int viewNum) {
		if (this.specifiedGamma != null)
			return this.specifiedGamma;
		else
		{
			if (useViews)
				return svmGammaForViews[viewNum];
			else
				return svmGamma;
		}
	}
	
	@Override
	public double getEpsilon() {
		if (this.specifiedEpsilon != null)
			return specifiedEpsilon;
		else
			return svmEpsilon;
	}

	@Override
	public ModelType getModelType()
	{
		return ModelType.FEATURE;
	}
	

}
