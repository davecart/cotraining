package ca.carter.thesis.ml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import ca.carter.thesis.ProcessReviews;
import ca.carter.thesis.model.Sentiment;
import ca.carter.thesis.model.Task;
import ca.carter.thesis.model.TokenWithContext;
import ca.carter.thesis.model.phrasetree.PartOfSpeech;

public class SVMTokenModelSentiment extends SVMTokenModel {

	//as of March 16th, working OK with 24.251, 0.0011218, 1.0E-4
	
	private static final double svmC = 24.251; //41.600; //742.4; //73.51;
	private static final double svmGamma = 0.0011218; //0.0013672; //0.000048828125;	//3.0517578125e-05 , 0.0001220703125, 3.0517578125e-05, 3.0517578125e-05, 3.0517578125e-05
 	private static final double svmEpsilon = 1.0E-4;

 	//tuned for single view
// 	private static final double[] svmCForViews = {512, 512};
// 	private static final double[] svmGammaForViews = {0.0001220703, 0.0001220703};
 	
 	//hand tuned for two views
 	private static final double[] svmCForViews = {147.998047, 2.87743798}; // {147.998047, 2.87743798} <--bestyet-- {147.998047, 2.87743798} <--slightlyworse-- {73.9990235, 5.75487596} <--better-- {73.9990235, 5.75487596} <--notmuchchange-- {59.1992188, 7.67316795} <--merelyrollingbackgammato-- {59.1992188, 7.67316795} <-?- {47.359375, 10.2308906} <--improvement-- {37.8875, 13.6411875} <--neglibibleimprovementinPneglibibledecreaseinR-- {30.31, 18.1825} <--improves-- {24.251,24,251}
 	//private static final double[] svmGammaForViews = {0.0011218 * 3, 0.0068469238 * 15}; // {0.0011218 * 2 , 0.0068469238} <--bestyet-- {0.0011218 / 2 , 0.0068469238} <--slightlyworse-- {0.0011218, 0.0034234619} <--better-- {0.0011218, 0.0027387695} <--notmuchchange-- {0.0011218, 0.0027387695} <--merelyrollingbackgammato-- {0.00084135, 0.0027387695} <-?- {0.0011218, 0.0021910156} <--improvement-- {0.0011218, 0.0010516875} (latter should have been 0.00175) <--neglibibleimprovementinPneglibibledecreaseinR--  {0.0011218, 0.00140225} <--improves-- {0.0011218, 0.0011218}
 	private static final double[] svmGammaForViews = {0.0011218 * 3, 0.0068469238 * 15}; // {0.0011218 * 2 , 0.0068469238} <--bestyet-- {0.0011218 / 2 , 0.0068469238} <--slightlyworse-- {0.0011218, 0.0034234619} <--better-- {0.0011218, 0.0027387695} <--notmuchchange-- {0.0011218, 0.0027387695} <--merelyrollingbackgammato-- {0.00084135, 0.0027387695} <-?- {0.0011218, 0.0021910156} <--improvement-- {0.0011218, 0.0010516875} (latter should have been 0.00175) <--neglibibleimprovementinPneglibibledecreaseinR--  {0.0011218, 0.00140225} <--improves-- {0.0011218, 0.0011218}

 	//auto-tuned with 80%
// 	private static final double[] svmCForViews = {6208.3750564266, 10.5560632862};
// 	private static final double[] svmGammaForViews = {0.0001610727, 0.0078125};

 	//auto-tuned with 20%
// 	private static final double[] svmCForViews = {97.0058602567, 10.5560632862};
// 	private static final double[] svmGammaForViews = {0.0011217757, 0.0136023526};

 	
	private static final List<String> posWords = new ArrayList<String>();
	private static final List<String> negWords = new ArrayList<String>();
	
	private static final String positiveWordsFile = "/bingliulexicon/positive-words.txt";
	private static final String negativeWordsFile = "/bingliulexicon/negative-words.txt";
	
	private static boolean startedUp = false;

	
	public SVMTokenModelSentiment(Task task, List<TokenWithContext> tokens, Writer[] fileToOutput, ClassWeighting classWeighting, Double c, Double gamma, Double epsilon) {
		super(task, tokens, fileToOutput, classWeighting, c, gamma, epsilon);
	}

	private void loadBootstrapping()
	{
		synchronized(this)
		{
			if (startedUp)
				return;
			startedUp = true;
		}
		
		try
		{
			loadSentimentWordList(new File(ProcessReviews.defaultRootdir + positiveWordsFile), posWords);
			loadSentimentWordList(new File(ProcessReviews.defaultRootdir + negativeWordsFile), negWords);
			
			if (posWords.isEmpty() || negWords.isEmpty())
			{
				System.err.println("############ Could not bootstrap sentiment-bearing words ##############");
				System.exit(-1);
			}
			
			//System.out.print("Bootstrapping sentiment-bearing words with list of " + posWords.size() + " positive words and " + negWords.size() + " negative words.");
		}
		catch (IOException e)
		{
			System.err.println("Could not open file containing sentiment words for bootstrapping.");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private void loadSentimentWordList(File file, List<String> list) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.isEmpty() || line.charAt(0) == ';')
			{
				//do nothing
			}
			else
			{
				list.add(line);
			}
		}
		br.close();
	}
	
	//TODO: switch away from multi-class 
	
	
	//here, unusually, we bootstrap the negative and positive words
	//TODO: need to test negation features
	
	@Override
	public Double getClassForToken(TokenWithContext token)
	{	
		if (!startedUp)
			loadBootstrapping();
		
		String cleanedToken = token.getToken().toLowerCase();
		String cleanedLemma = token.getLemma().toLowerCase();
		
		if (negWords.contains(cleanedToken) || negWords.contains(cleanedLemma))
			return -1.0;		
		else if (posWords.contains(cleanedToken) || posWords.contains(cleanedLemma))
			return 1.0;
		else
			return 0.0;
	}
	
	public static Double lookupClassForToken(TokenWithContext token)
	{
		if (!startedUp)
			System.err.println("Cannot look up class for sentiment token. Have not loaded lexicons.");
		
		String cleanedToken = token.getToken().toLowerCase();
		String cleanedLemma = token.getLemma().toLowerCase();
		
		if (negWords.contains(cleanedToken) || negWords.contains(cleanedLemma))
			return -1.0;		
		else if (posWords.contains(cleanedToken) || posWords.contains(cleanedLemma))
			return 1.0;
		else
			return 0.0;

	}

	public static Sentiment decodeClassNumber(Double classNumber)
	{
		if (classNumber == null)
			return null;
	
		switch((int) Math.round(classNumber)  )
		{
		case -1:
			return Sentiment.NEG;
		case 0:
			return Sentiment.OBJ;
		case 1:
			return Sentiment.POS;
		default:
			return null;
		}
	}
	
	@Override
	public String getName() {
		return "sentiment word";
	}
	
	/*
	public static void main(String[] args)
	{
		SVMTokenModelSentiment model = new SVMTokenModelSentiment(null);
		
		System.out.println("Negative words: " + model.negWords.size());
		System.out.println("Positive words: " + model.posWords.size());
		
		Sentence testSentence = new Sentence("It has a great big screen but a terrible little tiny shutter release.", Sentence.getDefaultPipeline(), ProductFeatureOpinion.getDefaultPipeline());

		for (TokenWithContext nextToken : testSentence.getTokens())
		{
			System.out.println(nextToken.getToken() + " " + model.getClassForToken(nextToken) );
		}
		
	}
	*/
	
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
		return ModelType.SENTIMENT;
	}

	
	public static void main(String[] args)
	{
//		public SVMTokenModelSentiment(Task task, List<TokenWithContext> tokens, Writer[] fileToOutput, ClassWeighting classWeighting, Double c, Double gamma, Double epsilon) {

		SVMTokenModelSentiment model = new SVMTokenModelSentiment(Task.BINGLIU, null, null, null, null, null, null);
		
		System.out.println(model.getClassForToken(new TokenWithContext(1, "horrible", null, PartOfSpeech.JJ, null, null, null, false)));
		System.out.println(model.getClassForToken(new TokenWithContext(1, "great", null, PartOfSpeech.JJ, null, null, null, false)));
		System.out.println(model.getClassForToken(new TokenWithContext(1, "uneventful", null, PartOfSpeech.JJ, null, null, null, false)));
		System.out.println(model.getClassForToken(new TokenWithContext(1, "outstanding", null, PartOfSpeech.JJ, null, null, null, false)));
		System.out.println(model.getClassForToken(new TokenWithContext(1, "OUTSTANDING", null, PartOfSpeech.JJ, null, null, null, false)));

		
	}
	
}
