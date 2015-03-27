package ca.carter.thesis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import libsvm.svm_parameter;
import ca.carter.thesis.evaluation.ResultsSummary;
import ca.carter.thesis.ml.ClassWeighting;
import ca.carter.thesis.ml.ModelType;
import ca.carter.thesis.ml.Prediction;
import ca.carter.thesis.ml.PredictionTokenWithContextPair;
import ca.carter.thesis.ml.SVMTokenModel;
import ca.carter.thesis.ml.SVMTokenModelSentiment;
import ca.carter.thesis.model.AspectMatchPolicy;
import ca.carter.thesis.model.ProductFeatureOpinion;
import ca.carter.thesis.model.ReconciledFeatureOpinion;
import ca.carter.thesis.model.Review;
import ca.carter.thesis.model.SemanticallyTaggedTokenWithContext;
import ca.carter.thesis.model.Sentence;
import ca.carter.thesis.model.SimpleSentence;
import ca.carter.thesis.model.Task;
import ca.carter.thesis.model.TokenWithContext;
import ca.carter.thesis.model.phrasetree.PartOfSentimentStructure;
import ca.carter.thesis.model.phrasetree.PartOfSpeech;
import edu.stanford.nlp.trees.Tree;

public class ProcessReviews {
	
	private static final double defaultPercentageSeed = 0.8;
	private static final double defaultPercentageIncremental = 0.0;

	private static final int defaultMinNumAdded = 1;									//make sure that at least n items are added every iteration
	private static final boolean addAtLeastOneFromEachClass = false;
	private static final int defaultMaxToAddPerIteration = 2500;				//6000	//while cotraining, add the tokens with the top "n" confidence estimates 
	private static final double defaultMinConfidenceThresholdNegClass = 0.75;	//0.55	//make sure the models are at least this confident of their top predictions when adding cases while cotraining
	private static final double defaultMinConfidenceThresholdPosClass = 0.75;	//0.55	//make sure the models are at least this confident of their top predictions when adding cases while cotraining
	private static Integer maxNumberOfIterations = null;
	public static final boolean removeGenericAspects = true;							//default to true; whether to remove product-level incorrect annotations like "dvd player" from the reviews, in order to make sure that the system is really learning only product aspects
	
	public static String defaultRootdir = "/Users/" + System.getProperty("user.name") + "/Dropbox/Thesis data";
	//public static String defaultRootdir = "/Users/" + System.getProperty("user.name") + "/Documents/Thesis data";

	private static final Integer defaultNumberOfLinesFromFile = null; //null for entire file; practical minimum is 10, so as to have enough total words that the classifiers can do internal cross-validation to build confidence estimates
	
	private static Task task = Task.SEMEVALTASK4PART1;
	private static AspectMatchPolicy aspectMatchPolicy = AspectMatchPolicy.EXACT;	//for multi-word apsects, whether we count a partial match (e.g., if we tag "salad" but not "Nicoise" in "I like the Nicoise salad there")
	//eg t15/cn/t9 PARTIAL is 0.934 & 0.748 & 0.831 & 0.737
	
	private static final boolean useLaTeXStyleOutput = false;
	

	private static final String[] defaultFilesToProcess = 
		{
			"/bingliudata/Apex AD2600 Progressive-scan DVD player.txt",
			"/bingliudata/Canon G3.txt",
			"/bingliudata/Creative Labs Nomad Jukebox Zen Xtra 40GB.txt",
			"/bingliudata/Nikon coolpix 4300.txt",
			"/bingliudata/Nokia 6610.txt",
			
			"/Semeval-2014-task4/Laptop_Train_v2.xml",
			"/Semeval-2014-task4/Restaurants_Train_v2.xml",
			"/Semeval-2014-task4/ABSA_Gold_TestData/Laptops_Test_Gold.xml",
			"/Semeval-2014-task4/ABSA_Gold_TestData/Restaurants_Test_Gold.xml",
			"/Semeval-2014-task4/laptops-trial.xml",
			"/Semeval-2014-task4/restaurants-trial.xml",

			"/Semeval-2014-task4/Laptop_Train_v2-halfone.xml",
			"/Semeval-2014-task4/Laptop_Train_v2-halftwo.xml",
			"/Semeval-2014-task4/Restaurants_Train_v2-halfone.xml",
			"/Semeval-2014-task4/Restaurants_Train_v2-halftwo.xml",

			"/Semeval-2014-task4/ABSA_TestData_PhaseA/Laptops_Test_Data_PhaseA.xml",
			"/Semeval-2014-task4/ABSA_TestData_PhaseA/Restaurants_Test_Data_PhaseA.xml",
			"/Semeval-2014-task4/ABSA_TestData_PhaseB/Laptops_Test_Data_phaseB.xml",
			"/Semeval-2014-task4/ABSA_TestData_PhaseB/Restaurants_Test_Data_phaseB.xml",

		};
	
	private static final String[] genericNamesOfItems =
		{
			"dvd player",
			"camera",
			"player",
			"camera",
			"phone",
			
			"laptop",
			"restaurant",
			"laptop",
			"restaurant",
			"laptop",
			"restaurant",

			"laptop",
			"laptop",
			"restaurant",
			"restaurant",
			
			"laptop",
			"restaurant",
			"laptop",
			"restaurant",


		};

	private static final String[] brandNamesOfItems =
		{
			"apex",
			"canon",
			"creative",
			"nikon",
			"nokia",
			
			null,
			null,
			null,
			null,
			null,
			null,

			null,
			null,
			null,
			null,

			null,
			null,
			null,
			null,
		};
	
	private static int[] seedFilesToUse = 
		{
//			1,
//			2,
//			3,
//			4,
//			5,
		
		//6, //laptops all
		//7, //restaurants all
		
		12, //laptops first half
		//14, //restaurants first half
		//13, //laptops second half
		//15 //restaurants second half
		

		};
	private static int[] cotrainingFilesToUse = 
		{
//		1,
//		2,
//		3,
//		4,
//		5
//		11
		13, //laptops second half
		//15 //restaurants second half
		//12, //laptops first half
		//14, //restaurants first half

		};
	private static int[] testFilesToUse = 
		{
//		1,
//		2,
//		3,
//		4,
//		5,
		8, //laptops gold standard
		//9, //restaurants gold standard
		
		

		};
	private static int[] allFilesToUse = 
		{
//			1,  //0.5934 accuracy on sentences, March 31
//			2,  //0.7311 accuracy on sentences, March 31
//			3,  //0.7184 accuracy on sentences, March 31
//			4,  //0.6473 accuracy on sentences, March 31
//			5,   //0.7477 accuracy on sentences, March 31
//			6,
//			7,
//		8,
//		9,
//		10,
//		11,
		
		};
	
	private static boolean useMixedFiles = false;   //true to just gather a bunch of files and then chop them into percentages; false to experiment with domain adaptation, i.e., training model on four products and testing on the fifth
	private static boolean doCrossValidation = false;
	private static int crossValidationFolds = (doCrossValidation && useMixedFiles ? 5 : 1);

	//this is used for SVM parameter tuning; set to null to avoid needlessly writing out large files of numeric features
	private static final String modelFileOutput = null; //"/Users/" + System.getProperty("user.name") + "/Dropbox/Thesis work/libsvm-3.17/tools/outputforparameterestimation5-" + Math.round(defaultPercentageSeed * 100) + "percent-";
	
	private static final int numThreads = Math.max(Runtime.getRuntime().availableProcessors() / 2, 2); //use a reasonable number of cores for the sentence parsing
	//private static final int numThreads = 3;

	public static void main(String[] args)
	{
		double percentageSeed = defaultPercentageSeed;
		double percentageIncremental = defaultPercentageIncremental;
		int minNumAdded = defaultMinNumAdded;						
		int maxToAddPerIteration = defaultMaxToAddPerIteration;			
		double minConfidenceThresholdPosClass = defaultMinConfidenceThresholdPosClass;	
		double minConfidenceThresholdNegClass = defaultMinConfidenceThresholdNegClass;	


		try
		{
			if (args != null && args.length > 0)
			{
				if (args.length < 4 || args.length > 9 )
				{
					System.err.println("The arguments are task { BINGLIU | SEMEVALTASK4PART1 | SEMEVALTASK4PART2}, seed file(s), cotraining file(s) (or null), gold standard file(s), optional max number to add each cotraining iteration, optional aspect matching policy {PARTIAL | EXACT}, confidence threshold for classification, maximum number of cotraining iterations (or null) (useful for one-shot cotraining, for example), optional root directory of the data files.");
					System.err.println("e.g. java -Xmx8000m -jar <thisjarfile> BINGLIU 6 null 8");
					System.err.println("e.g. java -Xmx6000m -jar <thisjarfile> SEMEVALTASK4PART1 6 10 8");
					System.err.println("e.g. java -Xmx6000m -jar <thisjarfile> SEMEVALTASK4PART2 6 10 8 2500");
					System.err.println("e.g. java -Xmx6000m -jar <thisjarfile> SEMEVALTASK4PART1 6 10 8 2500 ");
					System.err.println("e.g. java -Xmx6000m -jar <thisjarfile> SEMEVALTASK4PART1 6 10 8 2500 PARTIAL 0.75");
					System.err.println("e.g. java -Xmx6000m -jar <thisjarfile> SEMEVALTASK4PART1 6 10 8 2500 PARTIAL 0.75 4");
					System.err.println("e.g. java -Xmx6000m -jar <thisjarfile> SEMEVALTASK4PART1 6 10 8 2500 PARTIAL 0.75 null");
					System.err.println("e.g. java -Xmx6000m -jar <thisjarfile> SEMEVALTASK4PART1 6 10 8 2500 PARTIAL 0.75 null /my/path/to/the/bingliuandsemeval/files");
					System.err.println("");
					System.err.println("Note that this process needs at least approximately 5GB of RAM to run (so set the -Xmx parameter to at least 5000m; 8000m is preferred for the BINGLIU task, which uses twice as many classification models).");
					System.err.println("");
					System.err.println("If only a training file is specified (with cotraininging and test files specifically listed as null), five-fold cross validation will be performed.");
					System.err.println("e.g. java -Xmx8000m -jar <thisjarfile> BINGLIU 1 null null");
					System.err.println("");
					System.err.println("Numbers that can be used for seed/cotraining/gold standard files are as follows:");
					
					int i = 1;
					for (String defaultFile : defaultFilesToProcess)
					{
						System.err.println("    " + i++ + " " + defaultFile);
					}
					
					System.exit(-1);
				}
				else
				{
					task = Task.valueOf(args[0].toUpperCase());
					seedFilesToUse = commandLineNumberArray(args[1]);
					cotrainingFilesToUse = commandLineNumberArray(args[2]);
					testFilesToUse = commandLineNumberArray(args[3]);
					
					if (testFilesToUse == null || testFilesToUse.length == 0)
					{
						doCrossValidation = true;
						useMixedFiles = true;
						allFilesToUse = seedFilesToUse;
						crossValidationFolds = 5;
					}
					
					if (args.length >= 5)
						maxToAddPerIteration = Integer.valueOf(args[4]);

					if (args.length >= 6)
						aspectMatchPolicy = AspectMatchPolicy.valueOf(args[5].toUpperCase());

					if (args.length >= 7)
					{
						double arg = Double.valueOf(args[6]);
						if (arg < 0 || arg > 1)
						{
							System.err.println("The confidence threshold must be between 0 and 1.");
							System.exit(-1);
						}
						minConfidenceThresholdPosClass = arg;
						minConfidenceThresholdNegClass = arg;
					}
					
					if (args.length >= 8)
					{
						if (args[7] == null || args[7].equals("") || args[7].equalsIgnoreCase("null"))
						{
							//do nothing
						}
						else
							maxNumberOfIterations = Integer.valueOf(args[7]);
					}

					if (args.length >= 9)
						defaultRootdir = args[8];
				}
			}
			
			Integer numberOfLinesFromFile = defaultNumberOfLinesFromFile;
			
			ClassWeighting classWeighting = ClassWeighting.EQUAL;
			
			Double c1 = null;
			Double gamma1 = null;
			Double eps1 = null;
			Double c2 = null;
			Double gamma2 = null;
			Double eps2 = null;
			

		    Date date = new Date();
			System.out.println("Starting with " + numThreads + " threads at " + date.toString());
			System.out.println("Percentage seed is " + percentageSeed);
			System.out.println("Task is " + task);
			if (doCrossValidation)
				System.out.println("Will do cross validation");
			System.out.println("Policy is " + aspectMatchPolicy);
			if (defaultNumberOfLinesFromFile != null)
				System.out.println("WARNING: Only using first " + defaultNumberOfLinesFromFile + " lines from the files.");
			if (cotrainingFilesToUse != null && cotrainingFilesToUse.length > 0)
			{
				if (maxNumberOfIterations != null)
					System.out.println("Will terminate cotraining after " + maxNumberOfIterations + " iterations.");
				else
					System.out.println("No limit to the number of cotraining iterations.");
			}
			
			System.out.print("Kernel is ");
			switch (SVMTokenModel.kernel)
			{
			case svm_parameter.RBF:
				System.out.print("RBF\n");
				break;
			case svm_parameter.LINEAR:
				System.out.print("linear\n");
				break;
			default:
				System.out.print("other: " + SVMTokenModel.kernel + "\n");

			}
			System.out.println("Thresholds are: " + minConfidenceThresholdPosClass + " pos/" + minConfidenceThresholdNegClass + " neg");
			if (useMixedFiles)
				System.out.println("Mixing all files, and they are " + arrayToString(allFilesToUse));
			else
			{
				System.out.println("Training files are " + arrayToString(seedFilesToUse));
				System.out.println("Cotraining files are " + arrayToString(cotrainingFilesToUse));
				System.out.println("Test files are " + arrayToString(testFilesToUse));
				
			}
			
			if (SVMTokenModel.useOnlyOneView)
			{
				System.out.println("Only considering a single view : " + SVMTokenModel.useOnlyOneViewWhichView);
			}
			
			
			final List<List<Sentence>> trainingFoldsSentences = new ArrayList<List<Sentence>>();
			final List<List<Sentence>> cotrainingFoldsSentences = new ArrayList<List<Sentence>>();
			final List<List<Sentence>> testFoldsSentences = new ArrayList<List<Sentence>>();
			
			if (useMixedFiles)
			{	
				List<Sentence> allSentences = new ArrayList<Sentence>();
	
				//read in all the sentences
				for (int fileIndexToUse : allFilesToUse)
				{
					allSentences.addAll(getSentences(defaultRootdir + defaultFilesToProcess[fileIndexToUse - 1], numberOfLinesFromFile, genericNamesOfItems[fileIndexToUse - 1], brandNamesOfItems[fileIndexToUse - 1], null));
				}
				
				final int sentencesSize = allSentences.size();
				final int seedOffset = (int) Math.round(sentencesSize * percentageSeed);
				final int incrementalOffset = (int) Math.round(sentencesSize * (percentageSeed + percentageIncremental));
	
				if (doCrossValidation)
				{
					for (int i = 0; i < crossValidationFolds; i++)
					{
						int foldLowerBound = i * sentencesSize / crossValidationFolds;
						int foldUpperBound = (i + 1) * sentencesSize / crossValidationFolds;
						
						final List<Sentence> trainingSentences = new ArrayList<Sentence>();
						final List<Sentence> testSentences = new ArrayList<Sentence>();
						
						//test sentences are 1 "foldth" of the data, and the training data is the rest
						testSentences.addAll(allSentences.subList(foldLowerBound, foldUpperBound));
						trainingSentences.addAll(allSentences.subList(0, foldLowerBound));
						trainingSentences.addAll(allSentences.subList(foldUpperBound, sentencesSize));

						//System.out.println("Test bound " + i + " is from " + foldLowerBound + " to " + foldUpperBound);
						//System.out.println("Training size is " + trainingSentences.size() + " and test size is " + testSentences.size() + " and sentences size is " + sentencesSize);

						trainingFoldsSentences.add(trainingSentences);
						testFoldsSentences.add(testSentences);
						
					}
				}
				else
				{
					final List<Sentence> trainingSentences = allSentences.subList(0, seedOffset);
					final List<Sentence> cotrainingSentences = allSentences.subList(seedOffset, incrementalOffset);
					final List<Sentence> testSentences = allSentences.subList(incrementalOffset, sentencesSize);
					trainingFoldsSentences.add(trainingSentences);
					cotrainingFoldsSentences.add(cotrainingSentences);
					testFoldsSentences.add(testSentences);
				}
			}
			else
			{
				final List<Sentence> trainingSentences = new ArrayList<Sentence>();
				final List<Sentence> cotrainingSentences = new ArrayList<Sentence>();
				final List<Sentence> testSentences = new ArrayList<Sentence>();

				for (int fileIndexToUse : seedFilesToUse)
				{
					trainingSentences.addAll(getSentences(defaultRootdir + defaultFilesToProcess[fileIndexToUse - 1], numberOfLinesFromFile, genericNamesOfItems[fileIndexToUse - 1], brandNamesOfItems[fileIndexToUse - 1], null));
				}
				for (int fileIndexToUse : cotrainingFilesToUse)
				{
					cotrainingSentences.addAll(getSentences(defaultRootdir + defaultFilesToProcess[fileIndexToUse - 1], numberOfLinesFromFile, genericNamesOfItems[fileIndexToUse - 1], brandNamesOfItems[fileIndexToUse - 1], null));
				}
				for (int fileIndexToUse : testFilesToUse)
				{
					testSentences.addAll(getSentences(defaultRootdir + defaultFilesToProcess[fileIndexToUse - 1], numberOfLinesFromFile, genericNamesOfItems[fileIndexToUse - 1], brandNamesOfItems[fileIndexToUse - 1], null));
				}
				
				trainingFoldsSentences.add(trainingSentences);
				cotrainingFoldsSentences.add(cotrainingSentences);
				testFoldsSentences.add(testSentences);

			}
			
			System.out.println("Starting training with " +
					 trainingFoldsSentences.get(0).size() + " training sentences, " +
					 (cotrainingFoldsSentences.isEmpty() ? 0 : cotrainingFoldsSentences.get(0).size()) + " cotraining sentences, and " +
					 testFoldsSentences.get(0).size() + " test sentences."
				);
			
			
			List<List<ResultsSummary>> individualClassifierResults = new ArrayList<List<ResultsSummary>>();
			List<ResultsSummary> sentenceResults = new ArrayList<ResultsSummary>();
			
			for (int foldNum = 0; foldNum < crossValidationFolds; foldNum++)
			{
				System.out.println("\n##### FOLD " + foldNum + " #####\n");

				final List<TokenWithContext> seedTokens = new ArrayList<TokenWithContext>();
				
				for (Sentence trainingSentence : trainingFoldsSentences.get(foldNum))
				{
					seedTokens.addAll( trainingSentence.getTokens() );
					List<TokenWithContext> tokensForFeatureOpinions = trainingSentence.peekTokensInFeatureOpinions();
					if (tokensForFeatureOpinions != null)
						seedTokens.addAll( tokensForFeatureOpinions );
				}
				
				List<SVMTokenModel> models = new ArrayList<SVMTokenModel>();
				
				Thread modelCreatorSentiment = null;
				Thread modelCreatorFeature = null;
				if (task != Task.SEMEVALTASK4PART1)
				{
					modelCreatorSentiment = new SeedModelCreatorThread(models, modelFileOutput, "sentiment.txt", ModelType.SENTIMENT, task, seedTokens, classWeighting, c1, gamma1, eps1);
					modelCreatorSentiment.start();
				}
	
				if (task != Task.SEMEVALTASK4PART2)
				{
					modelCreatorFeature = new SeedModelCreatorThread(models, modelFileOutput, "productfeature.txt", ModelType.FEATURE, task, seedTokens, classWeighting, c2, gamma2, eps2);
					modelCreatorFeature.start();
				}
	
				if (modelCreatorFeature != null)
					modelCreatorFeature.join();
				if (modelCreatorSentiment != null)
					modelCreatorSentiment.join();
				
				final int modelsSize = models.size();
				
				System.out.println("Done initial training with " + seedTokens.size() + " tokens.");
				
				if (percentageIncremental == 0 && cotrainingFoldsSentences.isEmpty())
				{
					System.out.println("Skipping cotraining.");
				}
				else
				{
					System.out.println("Starting cotraining.");
		
					int addedLastIteration = minNumAdded; //priming the while loop below
					int numIterations = 0;
					
					List<TokenWithContext> cotrainingTokens = new ArrayList<TokenWithContext>();
					//loop through sentences and collect tokens
					for (Sentence cotrainingSentence : cotrainingFoldsSentences.get(foldNum) )
					{
						for (TokenWithContext nextToken : cotrainingSentence.getTokens())
						{
							cotrainingTokens.add(nextToken);
						}
					}
					
					System.out.println("Starting with " + cotrainingTokens.size() + " cotraining tokens.");
					System.out.println("Force add one from each class is " + addAtLeastOneFromEachClass);
					
					//skip cotraining if only writing out tuning files
					if (modelFileOutput == null)
					{
						while (addedLastIteration >= minNumAdded && (maxNumberOfIterations == null || numIterations < maxNumberOfIterations))
						{
							System.out.print("Iteration " + numIterations + ": ");
							
							numIterations++;
										
							//TODO: option here to keep all highest predictions together and tag them with appropriate model; or pick top N predictions for every classifier
							List<ConcurrentSkipListMap<Double, TokenWithContext>> passablePredictions = new ArrayList<ConcurrentSkipListMap<Double, TokenWithContext>>();
							Map<ModelType, PredictionTokenWithContextPair> topPosPredictions = new HashMap<ModelType, PredictionTokenWithContextPair>();
							Map<ModelType, PredictionTokenWithContextPair> topNegPredictions = new HashMap<ModelType, PredictionTokenWithContextPair>();
	
							List<TokenPredictorThread> predictionThreads = new ArrayList<TokenPredictorThread>();
							for (int i = 0; i < models.size(); i++)
							{
								passablePredictions.add(new ConcurrentSkipListMap<Double, TokenWithContext>());
								predictionThreads.add(null);
							}
							List<TokenWithContext> nonPassablePredictions = new ArrayList<TokenWithContext>();
							
							int tokenNumber = 0;
							for (TokenWithContext nextCotrainingToken : cotrainingTokens)
							{
								if (tokenNumber++ % 100 == 0)
									System.out.print((tokenNumber - 1) + " ");
								
								//classify the token with all models, and see if any have enough confidence to add it to the tentative list of tokens to be added to the training set next iteration
								//List<Prediction> predictions = new ArrayList<Prediction>();
								Prediction topPrediction = null;
								ConcurrentSkipListMap<Double, TokenWithContext> modelSpecificListOfPassablePredictions = null;
								ModelType topConfidenceModelForToken = null;
								
								//multi-threaded predictions; this probably won't save any time on fast (workstation-class) machines, but the overhead is relatively small
								for (int i = 0; i < modelsSize; i++)
								{
									TokenPredictorThread newPredictionThread = new TokenPredictorThread(models.get(i));
									predictionThreads.set(i, newPredictionThread);
									newPredictionThread.setNextCotrainingToken(nextCotrainingToken);
									newPredictionThread.start();
								}
								for (TokenPredictorThread predictionThread : predictionThreads)
								{
									predictionThread.join();
								}
								
								//this loop pulls out the model prediction for this token that has the highest confidence, with a bias towards positive predictions (i.e., a lower-confidence positive prediction trumps a higher-confidence negative prediction, subject to minimum confidence thresholds)
								for (int i = 0; i < modelsSize; i++)
								{	
		//							Prediction prediction = models.get(i).predict(nextCotrainingToken);  //single-threaded version
									Prediction prediction = predictionThreads.get(i).getPrediction();
									if (prediction == null)
										System.err.println("Null prediction for " + nextCotrainingToken);
									else
									{
										//weighting in favour of positive classes
										if (topPrediction == null || 
												(prediction.getClassNumber() == 0 && topPrediction.getClassNumber() == 0 && prediction.getProbability() > topPrediction.getProbability()) ||
												(prediction.getClassNumber() != 0 && topPrediction.getClassNumber() == 0 && prediction.getProbability() > minConfidenceThresholdPosClass) ||
												(prediction.getClassNumber() != 0 && topPrediction.getClassNumber() != 0 && prediction.getProbability() > topPrediction.getProbability())
												)
										{
											topPrediction = prediction;
											topConfidenceModelForToken = models.get(i).getModelType();
											modelSpecificListOfPassablePredictions = passablePredictions.get(i);
										}
									}
								}
								
								//TODO: could probably speed this up by just checking lowest confidence value in the existing list
								//topPrediction means, for this token, the model that had the highest confidence
								if (topPrediction != null &&
										 ( ( topPrediction.getClassNumber() == 0 && topPrediction.getProbability() > minConfidenceThresholdNegClass )
										|| ( topPrediction.getClassNumber() != 0 && topPrediction.getProbability() > minConfidenceThresholdPosClass ) ) )
								{
									//add to confidence list
									nextCotrainingToken.setPredictedClass((double) topPrediction.getClassNumber());
									modelSpecificListOfPassablePredictions.put(topPrediction.getProbability(), nextCotrainingToken);
									if (modelSpecificListOfPassablePredictions.size() > maxToAddPerIteration)
									{
										TokenWithContext toRemove = modelSpecificListOfPassablePredictions.remove(modelSpecificListOfPassablePredictions.firstKey());
										toRemove.setPredictedClass(null);
										nonPassablePredictions.add(toRemove);
									}
									
									//also add to top pos/neg prediction if appropriate
									if (topPrediction.getClassNumber() == 0 && (topNegPredictions.get(topConfidenceModelForToken) == null || topPrediction.getProbability() > topNegPredictions.get(topConfidenceModelForToken).getPrediction().getProbability()))
									{
										topNegPredictions.put(topConfidenceModelForToken, new PredictionTokenWithContextPair(nextCotrainingToken, topPrediction));
									}
									else if (topPrediction.getClassNumber() != 0 && (topPosPredictions.get(topConfidenceModelForToken) == null || topPrediction.getProbability() > topPosPredictions.get(topConfidenceModelForToken).getPrediction().getProbability()))
									{
										topPosPredictions.put(topConfidenceModelForToken, new PredictionTokenWithContextPair(nextCotrainingToken, topPrediction));
									}
								}
								else
									nonPassablePredictions.add(nextCotrainingToken);
			
							}
							
							//retrain the models with the newest high-confidence tokens
							//System.out.print(". Adding cases. ");
							String addedByEachModelLastIteration = "";
							addedLastIteration = 0;
							for (int i = 0; i < models.size(); i++)
							{
								int posClassAddedLastModelIteration = 0;
								int negClassAddedLastModelIteration = 0;
								
								//need to use most confident examples of positive cases for its model and as negative example for all others
								SVMTokenModel currentModel = models.get(i);
								ConcurrentSkipListMap<Double, TokenWithContext> modelSpecificListOfPassablePredictions = passablePredictions.get(i);
	
								//first pass to make sure we have at least one positive and one negative prediction
								if (addAtLeastOneFromEachClass)
								{
									boolean hasPos = false;
									boolean hasNeg = false;
									for (TokenWithContext nextPredictedToken : modelSpecificListOfPassablePredictions.values())
									{
										if (nextPredictedToken.getPredictedClass() == 0)
											hasNeg = true;
										else
											hasPos = true;
										
										if (hasPos && hasNeg)
											break;
									}
									
									if (hasPos && hasNeg)
									{
										//do nothing
									}
									else
									{
										PredictionTokenWithContextPair supplementalTokenPrediction = (hasPos ? topNegPredictions.get(currentModel.getModelType()) : topPosPredictions.get(currentModel.getModelType()));
										if (supplementalTokenPrediction == null)
										{
											//do nothing; we did not have a single pos/neg outlier to include
											//System.err.println("Null token prediction.");
										}
										else
										{
											modelSpecificListOfPassablePredictions.remove(modelSpecificListOfPassablePredictions.firstKey());
											supplementalTokenPrediction.getTokenWithContext().setPredictedClass((double) supplementalTokenPrediction.getPrediction().getClassNumber());
											modelSpecificListOfPassablePredictions.put(supplementalTokenPrediction.getPrediction().getProbability(), supplementalTokenPrediction.getTokenWithContext());
										}
									}
								}
								
								for (TokenWithContext nextPredictedToken : modelSpecificListOfPassablePredictions.values())
								{
									addedLastIteration++;
									if (nextPredictedToken.getPredictedClass() == 0)
										negClassAddedLastModelIteration++;
									else
										posClassAddedLastModelIteration++;
									nextPredictedToken.setPredictedModel(currentModel.getModelType());
									
									
									//TODO: as of Feb 26th, THIS MAY BE VERY BROKEN
									//Update: seems maybe OK? By the time we get here, if the top prediction is class 0, means that all classifiers have classified it class zero;
									//so could go back to the simpler case where, for any prediction, it becomes a zero case for all other classifiers too
									if (nextPredictedToken.getPredictedClass() == 0)
									{
										//if negative class, we can't make any assumptions about whether it's positive or negative for the other classes
										//TODO: OTOH, this means that it never really really gets tried again, ever
										currentModel.addTokenForNextTraining(nextPredictedToken);
									}
									else
									{
										//if positive class, we can add it to all models (negative class cases for everyone else, positive for this one model)
										for (SVMTokenModel modelToWhichToAddNewToken : models)
										{
											modelToWhichToAddNewToken.addTokenForNextTraining(nextPredictedToken);
										}
									}
			
								}
	
								addedByEachModelLastIteration += (i == 0 ? "" : "/") + posClassAddedLastModelIteration + " pos " + negClassAddedLastModelIteration + " neg " + currentModel.getName();
							}
							System.out.print("Added " + addedLastIteration + " cases (" + addedByEachModelLastIteration + "); retraining.\n");
							
							//multi-threaded for performance
							if (addedLastIteration > 0)
							{
								List<Thread> retrainingThreads = new ArrayList<Thread>();
								for (SVMTokenModel modelToRetrain : models)
								{
									Thread nextThread = new RetrainingThread(modelToRetrain);
									retrainingThreads.add(nextThread);
									nextThread.start();
								}
								for (Thread nextThread : retrainingThreads)
								{
									nextThread.join();
								}
	
								//do next iteration minus the tokens we added to the models
								cotrainingTokens = nonPassablePredictions;
								
							}
							
						}
						
						int cotrainingTokensSize = cotrainingTokens.size();
						if (cotrainingTokensSize > 0)
							System.out.println(cotrainingTokensSize + " cotraining tokens were left over.");
						
						//TODO: what about cases where all models give very low votes; use them as negative cases for all models?
						
						System.out.println("Done cotraining.");
					}
				}
				
				
				if (modelFileOutput == null)
				{
					individualClassifierResults.add(
							testModels(models, testFoldsSentences.get(foldNum))
							);
					System.out.println("\n\n");
					System.out.println("################################################################################################");
					System.out.println("\n\n");
					
					sentenceResults.add(
							testSentences(models, testFoldsSentences.get(foldNum), minConfidenceThresholdPosClass)
							);
				}
			
			}			
		
			if (crossValidationFolds > 1)
			{
				
				//average the results if we're cross-validating

				System.out.println("\n### SUMMARY OF RESULTS #######################################\n");
				
				if (!sentenceResults.isEmpty())
				{
					double sumAccuracy = 0; 
					double sumPrecision = 0;
					double sumRecall = 0;
					double sumF1 = 0;
					System.out.println("Cross validation results of sentences over " + crossValidationFolds + " folds:");

					for (ResultsSummary nextFoldResults : sentenceResults)
					{
						sumAccuracy += nextFoldResults.getAccuracy();
						sumPrecision += nextFoldResults.getPrecision();
						sumRecall += nextFoldResults.getRecall();
						sumF1 += nextFoldResults.getF1();
					}
					
					System.out.println("Overall precision: " + (1.0 * sumPrecision / crossValidationFolds));
					System.out.println("Overall recall:    " + (1.0 * sumRecall / crossValidationFolds));
					System.out.println("Overall F1:        " + (1.0 * sumF1 / crossValidationFolds));
					System.out.println("Overall accuracy:  " + (1.0 * sumAccuracy / crossValidationFolds));
					System.out.println((1.0 * sumPrecision / crossValidationFolds) + "\t" + (1.0 * sumRecall / crossValidationFolds) + "\t" + (1.0 * sumF1 / crossValidationFolds) + "\t" + (1.0 * sumAccuracy / crossValidationFolds));
					System.out.println(ResultsSummary.toThreePlaces(1.0 * sumPrecision / crossValidationFolds) + " & " + ResultsSummary.toThreePlaces(1.0 * sumRecall / crossValidationFolds) + " & " + ResultsSummary.toThreePlaces(1.0 * sumF1 / crossValidationFolds) + " & " + ResultsSummary.toThreePlaces(1.0 * sumAccuracy / crossValidationFolds));
					System.out.println("\n");

				}
				
				if (!individualClassifierResults.isEmpty())
				{
					ModelType[] models = { ModelType.FEATURE, ModelType.SENTIMENT };
					
					for (ModelType modelType : models)
					{
						double sumAccuracy = 0; 
						double sumPrecision = 0;
						double sumRecall = 0;
						double sumF1 = 0;
						
						System.out.println("Cross validation results of " + modelType.toString() +  " classifier over " + crossValidationFolds + " folds:");
	
						for (List<ResultsSummary> nextFoldResults : individualClassifierResults)
						{
							for (ResultsSummary perClassifierResult : nextFoldResults)
							{
								if (perClassifierResult.getModelType() == modelType)
								{
									sumAccuracy += perClassifierResult.getAccuracy();
									sumPrecision += perClassifierResult.getPrecision();
									sumRecall += perClassifierResult.getRecall();
									sumF1 += perClassifierResult.getF1();
								}
							}
						}
						
						System.out.println("Overall precision: " + (1.0 * sumPrecision / crossValidationFolds));
						System.out.println("Overall recall:    " + (1.0 * sumRecall / crossValidationFolds));
						System.out.println("Overall F1:        " + (1.0 * sumF1 / crossValidationFolds));
						System.out.println("Overall accuracy:  " + (1.0 * sumAccuracy / crossValidationFolds));
						System.out.println((1.0 * sumPrecision / crossValidationFolds) + "\t" + (1.0 * sumRecall / crossValidationFolds) + "\t" + (1.0 * sumF1 / crossValidationFolds));
						System.out.println("\n");
					}
				}
				
			}
			
			System.out.println("\n------\nDone.");
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	

	private static Double doublify(String arg)
	{
		return doublify(arg, null);
	}
	
	private static Double doublify(String arg, Double defaultValue)
	{
		if (arg == null || "".equals(arg) || "null".equals(arg))
			return defaultValue;
		
		try {
			return Double.valueOf(arg);
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}
	
	protected static List<Sentence> getSentences(String nextFile, Integer numberOfLinesFromFile, String genericName, String brandName, Integer numParsingThreads) throws InterruptedException, IOException
	{
		List<Sentence> sentences = new ArrayList<Sentence>();
	
		int numActualThreads = (numParsingThreads == null ? numThreads : numParsingThreads);
		int sentencesAttempted = 0;
		int sentencesCompleted = 0;
		int titleCount = 0;
		
		System.out.println("********** Processing " + nextFile.substring(nextFile.lastIndexOf('/')) + "**********");
		
		File f = new File(nextFile);
		Queue<SimpleSentence> lines = null;
		
		if (nextFile.toLowerCase().endsWith("xml"))
			lines = ReviewFileReaderXML.readReviewFile(f, numberOfLinesFromFile); //last arg means "take first n sentences", null for all sentences in file)
		else
			lines = ReviewFileReaderFlat.readReviewFile(f, numberOfLinesFromFile); //last arg means "take first n sentences", null for all sentences in file)
		
		if (lines == null || lines.isEmpty())
			System.out.println("******** Empty file: " + nextFile + ". Probably means that the path to the data files was incorrect. ********");
		
		sentencesAttempted += lines.size();
		
		SentenceProcessorThread[] threads = new SentenceProcessorThread[numActualThreads];
		for (int i = 0; i < numActualThreads; i++)
		{
			threads[i] = new SentenceProcessorThread(lines, sentences, genericName, brandName);
			threads[i].start();
		}

		for (int i = 0; i < numActualThreads; i++)
		{
			threads[i].join();
			sentencesCompleted += threads[i].getSentencesProcessed();
			titleCount += threads[i].getTitleLineIgnored();
			threads[i] = null;
		}
			
		System.out.println("Parsed " + sentencesCompleted + " out of " + sentencesAttempted + " (having ignored " + titleCount + " title lines).");

		System.gc();
		
		return sentences;
	}
	
	//TODO: this can be threaded
	private static List<ResultsSummary> testModels(List<SVMTokenModel> models, List<Sentence> testSentences)
	{
		List<ResultsSummary> resultsSummary = new ArrayList<ResultsSummary>();
		
		//evalute ML tasks
		for (SVMTokenModel model : models)
		{
			List<PredictionTokenWithContextPair> falsePositives = new ArrayList<PredictionTokenWithContextPair>();
			
			System.out.println("Starting " + model.getName() + " predictions on test set of " + testSentences.size() + " sentences.");
			
			int truePositive = 0;
			int trueNegative = 0;
			int falsePositive = 0;
			int falseNegative = 0;
			int numTested = 0;
			
			for (Sentence sentence : testSentences)
			{
				for (TokenWithContext nextToken : sentence.getTokens())
				{
					numTested++;
					
					Prediction prediction = model.predict(nextToken);
					
					//TODO: will have to change this when I go to any non-binary classifier
					if (prediction.getClassNumber() == 0)
					{
						if (model.getClassForToken(nextToken) == 0)
							trueNegative++;
						else
							falseNegative++;
					}
					else
					{
						if (model.getClassForToken(nextToken) == prediction.getClassNumber())
							truePositive++;
						else
						{
							falsePositive++;
							falsePositives.add(new PredictionTokenWithContextPair( nextToken, prediction ) );
						}
						
					}
				}
			}

			System.out.println("Done predictions on test set for " + model.getName() + ". Tested " + numTested + " tokens.");

			ResultsSummary results = new ResultsSummary(numTested, truePositive, trueNegative, falsePositive, falseNegative, model.getModelType());
			resultsSummary.add(results);
			results.printOutResults();
			
			System.out.println("False positives:");
			for (PredictionTokenWithContextPair nextFalsePositive : falsePositives)
			{
				System.out.println("    " + nextFalsePositive.getTokenWithContext().getFormattedTokenContext() + " (" + nextFalsePositive.getPrediction().getProbability() + " confidence)");
			}
		}

		return resultsSummary;
	}
	

	
	private static ResultsSummary testSentences(List<SVMTokenModel> models, List<Sentence> testSentences, Double minConfidenceThresholdPosClass) //, Double minConfidenceThresholdNegClass)
	{
		//we shall deem a feature correct if it is in the same synset in Wordnet (i.e., price and cost)
		
		int numTested = testSentences.size();
		
		int sentenceFP = 0;
		double sentenceTP = 0;
		double sentenceFN = 0;
		int sentenceTN = 0;
		
		
		//evaluate whole sentences for easier comparison
		for (Sentence sentence : testSentences)
		{
			StringBuilder lineOut = new StringBuilder();
			
			
			List<String> taggedFeatures = new ArrayList<String>();
			List<String> taggedSentiments = new ArrayList<String>();
			
			//for re-combining inside/outside-classified bigrams, trigrams, etc.
			ModelType currentlyTagging = null;
			String taggingInProgress = null;
			
			//run the prediciton models on each token in the sentence, and collect the tagged ngrams (as the classifiers are inside/outside classifiers)
			for (TokenWithContext nextToken : sentence.getTokens())
			{
				Prediction bestPrediction = null;
				ModelType bestPredictionModelType = null;
				for (SVMTokenModel model : models)
				{
					Prediction nextPrediction = model.predict(nextToken);
					if (nextPrediction.getClassNumber() != 0 && 
							//nextPrediction.getProbability() > minConfidenceThresholdPosClass && 
							(bestPrediction == null || nextPrediction.getProbability() > bestPrediction.getProbability()))
					{
						bestPrediction = nextPrediction;
						bestPredictionModelType = model.getModelType();
					}
				}

				//System.out.println(nextToken.getToken() + " " + currentlyTagging + " " + taggingInProgress + " " + bestPrediction + " " + bestPredictionModelType);
				if (task == Task.SEMEVALTASK4PART2 && nextToken.getPartOfSentimentStructure() == PartOfSentimentStructure.FEATURE)
				{
					//hack the evaluation a bit to assume that the features are known ahead of time as in SemEval-2014 Task 4 Subtask 2
					bestPrediction = new Prediction(1, 1.0, null);
					bestPredictionModelType = ModelType.FEATURE;
				}
				
				if (bestPrediction != null)
				{
					//TODO: need to do voting among models here, as before
					nextToken.setPredictedModel(bestPredictionModelType);
					nextToken.setPredictedClass((double) bestPrediction.getClassNumber());
					
					if (currentlyTagging == null)
					{
						//start tagging
						currentlyTagging = bestPredictionModelType;
						taggingInProgress = nextToken.getToken();
					}
					else if (bestPredictionModelType == currentlyTagging)
					{
						//keep tagging
						taggingInProgress += " " + nextToken.getToken();
					}
					else
					{
						//finish last tagging and start anew
						//finish tagging
						switch (bestPredictionModelType)
						{
						case FEATURE:
							taggedFeatures.add(taggingInProgress);
							break;
						case SENTIMENT:
							taggedSentiments.add(taggingInProgress);
							break;
						}
						currentlyTagging = bestPredictionModelType;
						taggingInProgress = nextToken.getToken();
					}
					

				}
				else if (currentlyTagging != null)
				{
					//finish tagging
					switch (currentlyTagging)
					{
					case FEATURE:
						taggedFeatures.add(taggingInProgress);
						break;
					case SENTIMENT:
						taggedSentiments.add(taggingInProgress);
						break;
					}
					currentlyTagging = null;
					taggingInProgress = null;
				}
					
				
			}
			
			//print out the sentence and record whether the aspects and sentiment words were tagged correctly
			for (TokenWithContext token : sentence.getTokens())
			{
				ModelType nominalModel = null;
				for (SVMTokenModel model : models)
				{
					if (model.getClassForToken(token) != 0)
					{
						nominalModel = model.getModelType();
						break;
					}
				}

				String suffix = "";
				if (nominalModel != null && token.getPredictedModel() != null)
				{
					if (nominalModel == token.getPredictedModel())
					{
						//true positive
						if (useLaTeXStyleOutput)
						{
							lineOut.append("\\textcolor[rgb]{0,0.7,0}{");
							suffix = "}\\textsubscript{" + (token.getPredictedModel() == ModelType.FEATURE ? "aspect" : "sentiment") + "}";
						}
						else
						{
							lineOut.append("[ correct" + token.getPredictedModel() + " ");
							suffix = " ]";
						}
					}
					else
					{
						//false positive
						if (useLaTeXStyleOutput)
						{
							lineOut.append("\\textcolor[rgb]{0.8,0,0}{");
							suffix = "}\\textsubscript{wrong class - " + (token.getPredictedModel() == ModelType.FEATURE ? "aspect" : "sentiment") + "}";
						}
						else
						{
							lineOut.append("[ wrongclass" + token.getPredictedModel() + " " );
							suffix = " ]";
						}
					}
				}
				else if (nominalModel == null && token.getPredictedModel() == ModelType.FEATURE && task == Task.SEMEVALTASK4PART2 )
				{
					//feature that we did not need to predict for this task; e.g., in SemEval sentiment classification task, we can still list the features anyway
					if (useLaTeXStyleOutput)
					{
						lineOut.append("\\textbf{");
						suffix = "}\\textsubscript{aspect}";
					}
					else
					{
						lineOut.append("[ " + ModelType.FEATURE + " " );
						suffix = " ]";
					}
				}
				else if (nominalModel == null && token.getPredictedModel() != null )
				{
					//false positive
					if (useLaTeXStyleOutput)
					{
						lineOut.append("\\textcolor[rgb]{0.8,0,0}{");
						suffix = "}\\textsubscript{false positive - " + (token.getPredictedModel() == ModelType.FEATURE ? "aspect" : "sentiment") + "}";
					}
					else
					{
						lineOut.append("[ falsepositive" + token.getPredictedModel() + " " );
						suffix = " ]";
					}
				}
				else if (nominalModel != null && token.getPredictedModel() == null )
				{
					//false negative
					if (useLaTeXStyleOutput)
					{
						lineOut.append("\\textcolor[rgb]{0.8,0,0}{");
						suffix = "}\\textsubscript{false negative - " + (nominalModel == ModelType.FEATURE ? "aspect" : "sentiment") + "}";
					}
					else
					{
						lineOut.append("[ falsenegative" + nominalModel + " " );
						suffix = " ]";
					}
				}
				else
				{
					//true negative
				}
				
				lineOut.append(token.getToken());
				lineOut.append(suffix);
				lineOut.append(" ");
			}
			
			/*
			if ((taggedFeatures != null && !taggedFeatures.isEmpty()) || (taggedSentiments != null && !taggedSentiments.isEmpty()) )
			{
				System.out.print("\n     --> Features: ");
				if (taggedFeatures != null)
					for (String taggedFeature : taggedFeatures) { System.out.print(taggedFeature + " "); }
				System.out.print("\n     -->Sentiment words: ");
				if (taggedSentiments != null)
					for (String taggedSentiment : taggedSentiments) { System.out.print(taggedSentiment + " "); }
			}
			*/
			
			//output the correct feature opinions as tagged in the original data
			
			if (sentence.getFeatureOpinions() != null)
			{
				if (useLaTeXStyleOutput)
					lineOut.append(" & ");
				
				boolean firstFeature = true;
				for (ProductFeatureOpinion opinion : sentence.getFeatureOpinions())
				{
					
					if (useLaTeXStyleOutput)
					{
						if (!firstFeature)
							lineOut.append("\\newline ");
						lineOut.append(opinion.getFeature());

						if (opinion.getSentimentValue() > 0)
							lineOut.append(" (pos)");
						else if (opinion.getSentimentValue() < 0)
							lineOut.append(" (neg)");
					}
					else
					{
						lineOut.append("[");
						lineOut.append(opinion.getFeature());
						if (opinion.getSentimentValue() > 0)
							lineOut.append("+");
						lineOut.append(opinion.getSentimentValue());
						lineOut.append("]");
					}
					
					firstFeature = false;
				}
			}
			else
			{
				if (useLaTeXStyleOutput)
					lineOut.append("& (none)");
				else
					lineOut.append("[no opinions]");
			}
		
			String metric = "?";
			if ((taggedFeatures == null || taggedFeatures.isEmpty()) && (sentence.getFeatureOpinions() == null || sentence.getFeatureOpinions().isEmpty()))
			{
				//no features in sentence, none found
				if (useLaTeXStyleOutput)
					metric = "100\\%";
				else
					metric = "Y";
				sentenceTN++;
			}
			else if ((taggedFeatures == null || taggedFeatures.isEmpty()) && (sentence.getFeatureOpinions() != null && !sentence.getFeatureOpinions().isEmpty()))
			{
				//features in sentence, none found (false negative)
				if (useLaTeXStyleOutput)
					metric = "0\\%";
				else
					metric = "N";
				sentenceFN++;
			}
			else if ((taggedFeatures != null && !taggedFeatures.isEmpty()) && (sentence.getFeatureOpinions() == null || sentence.getFeatureOpinions().isEmpty()))
			{
				//no features in sentence, some found (false positive)

				if (task == Task.SEMEVALTASK4PART1)
				{
					sentenceFP++;
					if (useLaTeXStyleOutput)
						metric = "0\\% (FP)";
					else
						metric = "N-FP";
				}
				else
				{
					//inspect features and see if any of them have valid sentiments attached; if not, there's no opinion.
					boolean falsePositive = false;
					for (TokenWithContext token : sentence.getTokens())
					{
						if (isTokenPredictedAs(token, ModelType.FEATURE))
						{
							TokenWithContext matchingSemanticToken = getRelatedPredictedSemanticToken(token);
							if (matchingSemanticToken != null)
							{
								falsePositive = true;
								break;
							}
						}
					}
					
					if (falsePositive)
						sentenceFP++;
					else
						sentenceTN++;
					
					if (useLaTeXStyleOutput)
						metric = (falsePositive ? "0\\% (FP)" : "100\\%");
					else
						metric = (falsePositive ? "N-FP" : "Y-FPOK");
				}
			}
			else if (task == Task.SEMEVALTASK4PART1)
			{
				//some features in sentence, some found; need to reconcile them
				
				int opinionsReconciled = 0;
				int missingMentions = 0;
				int falsePositives = 0;
				
				//create list of all tagged features, then pop off the ones that match correctly; leftovers are false positives
				List<List<TokenWithContext>> allTaggedFeatures = getTaggedFeaturesForSentence(aspectMatchPolicy, sentence);
				List<List<TokenWithContext>> allAccountedForTaggedFeatures = new ArrayList<List<TokenWithContext>>();

				
				for (ProductFeatureOpinion opinion : sentence.getFeatureOpinions())
				{
					boolean foundMention = false;
			
					for (int i = 0; i < allTaggedFeatures.size(); i++)
					{
						List<TokenWithContext> featureTokens = allTaggedFeatures.get(i);
							
						if (matchTokenLists(aspectMatchPolicy, opinion.getFeature(), featureTokens ))
						{
							foundMention = true;
							allTaggedFeatures.remove(i);
							allAccountedForTaggedFeatures.add(featureTokens);
							i--;
						}
					}
					
					if (!foundMention)
					{
						for (int i = 0; i < allAccountedForTaggedFeatures.size(); i++)
						{
							List<TokenWithContext> featureTokens = allAccountedForTaggedFeatures.get(i);

							if (matchTokenLists(aspectMatchPolicy, opinion.getFeature(), featureTokens ))
							{
								foundMention = true;
							}
						}
						
					}
					
					if (foundMention)
						opinionsReconciled++;
					else
						missingMentions++;
				}
				
				//if (!allTaggedFeatures.isEmpty())
				//	System.out.print("Leftover feature tokens: ");
				List<TokenWithContext> lastExtraFeatureToken = null;
				for (List<TokenWithContext> nextFeatureToken : allTaggedFeatures)
				{
				//	System.out.print(nextFeatureToken.getToken() + " ");
					if (lastExtraFeatureToken == null || lastExtraFeatureToken.get(lastExtraFeatureToken.size() - 1).getPositionInSentence() != nextFeatureToken.get(0).getPositionInSentence() - 1)
						falsePositives++;
					lastExtraFeatureToken = nextFeatureToken;
				}
				//if (!allTaggedFeatures.isEmpty())
				//	System.out.print(" in sentence " + sentence.getSentence() + "\n");

				double denominator = opinionsReconciled + falsePositives + missingMentions + 0.0d;
				if (useLaTeXStyleOutput)
					metric = Math.round(100 * opinionsReconciled / denominator) + "\\%";
				else
					metric = "Y" + Math.round(100 * opinionsReconciled / denominator) + "%";
				sentenceTP += opinionsReconciled / denominator;
				sentenceFN += missingMentions / denominator;
				sentenceFP += falsePositives / denominator;
				
				//System.out.print("TP" + ResultsSummary.toThreePlaces(opinionsReconciled / denominator) + "FP" + ResultsSummary.toThreePlaces(missingMentions / denominator) + "FN" + ResultsSummary.toThreePlaces(falsePositives / denominator));
				//System.out.print("TP" + (opinionsReconciled / denominator)  + "FN" + (missingMentions / denominator) + "FP" + (falsePositives / denominator) + " ");
			}
			else
			{
				//some features in sentence, some found; need to reconcile them

				List<ReconciledFeatureOpinion> reconciledOpinions = getReconciledFeatureOpinions(sentence);
				
				if (reconciledOpinions.size() == sentence.getFeatureOpinions().size())
				{
					//Never  mind...since these are reconciled opinions, they're fine. TODO: incorrect; we need to still check each and every one; eg exampe where we have one false negative and one false positive in the same sentence
					if (useLaTeXStyleOutput)
						metric = "100\\%";
					else
						metric = "Y100%";
					sentenceTP++;
				}
				else if (!reconciledOpinions.isEmpty())
				{
					//Never mind...since these are reconciled opinions, they're fine. TODO: incorrect; we need to still check each and every one
					if (useLaTeXStyleOutput)
						metric = Math.round(100.0 * reconciledOpinions.size() / sentence.getFeatureOpinions().size()) + "\\%";
					else
						metric = "Y" + Math.round(100.0 * reconciledOpinions.size() / sentence.getFeatureOpinions().size()) + "%";

					double partialScore = 1.0 * reconciledOpinions.size() / sentence.getFeatureOpinions().size();
					sentenceTP += partialScore;
					sentenceFN += (1 - partialScore);
				}
				else
				{
					if (useLaTeXStyleOutput)
						metric = "0\\%";
					else
						metric = "N0%";
					sentenceFN++;
				}
			}
			
			if (useLaTeXStyleOutput)
				System.out.println(padRight(metric, 6) + " & " + lineOut.toString() + "\\\\");
			else
				System.out.println(padRight(metric, 6) + " " + lineOut.toString());
				
		}

		ResultsSummary results = new ResultsSummary(numTested, sentenceTP, sentenceTN, sentenceFP, sentenceFN, null);
		results.printOutResults();
		return results;

	}
	
	
	private static List<List<TokenWithContext>> getTaggedFeaturesForSentence(AspectMatchPolicy policy, Sentence sentence)
	{
		List<List<TokenWithContext>> toRet = new ArrayList<List<TokenWithContext>>();
		
		//System.out.print("Feature tokens: ");
		int sentenceLength = sentence.getTokens().size();
		for (int i = 0; i < sentenceLength; i++) // TokenWithContext token : sentence.getTokens())
		{
			TokenWithContext token = sentence.getTokens().get(i);
			
			if (isTokenPredictedAs(token, ModelType.FEATURE))
			{
				List<TokenWithContext> nextAspect = new ArrayList<TokenWithContext>();
				
				nextAspect.add(token);

				if (policy == AspectMatchPolicy.PARTIAL || (policy == AspectMatchPolicy.EXACT && i == sentenceLength - 1))
				{
					//add solo token; falls through to after this "if"
				}
				else if (policy == AspectMatchPolicy.EXACT)
				{
					//add n-gram feature
					int extraTokensAdded = 0;
					for (int j = 1; j < sentenceLength - i; j++)
					{
						TokenWithContext possibleNextToken = sentence.getTokens().get(i + j);
						//System.out.println("Considering " + possibleNextToken.getToken());
						if (isTokenPredictedAs(possibleNextToken, ModelType.FEATURE))
						{
							extraTokensAdded++;
							nextAspect.add(possibleNextToken);
						}
						else
							break;
					}
					i += extraTokensAdded;
				}

				/*System.out.print("Adding aspect ");
				for (TokenWithContext nextToken : nextAspect)
				{
					System.out.print(nextToken.getToken() + " ");
				}
				System.out.print("\n");*/
				toRet.add(nextAspect);
				nextAspect = null;
			}
		}
		
		if (!toRet.isEmpty())
			return toRet;
		else
			return null;
	}
	
	private static boolean matchTokenLists(AspectMatchPolicy policy, String flatList, List<TokenWithContext> tokenList)
	{
		if ((flatList == null || "".equals(flatList)) && (tokenList == null || tokenList.isEmpty()))
			return true;
		if (tokenList == null)
			return false;
		if (tokenList.isEmpty())
			return false;
			
		if (policy == AspectMatchPolicy.PARTIAL)
		{
			if (tokenList.size() > 1)
				System.err.println("With aspect matching policy PARTIAL, the aspect-tagged tokens passed in to matchTokenLists should be unigrams");
			
			TokenWithContext featureToken = tokenList.get(0);
			if (flatList.contains(featureToken.getToken()) || 
					(featureToken.getFlatResolvedCoreference() != null && flatList.contains(getLastWord(featureToken.getFlatResolvedCoreference())))
					)
				return true;
		}
		else if (policy == AspectMatchPolicy.EXACT)
		{
			String[] flatListTokens = flatList.split(" ");
			if (flatListTokens.length != tokenList.size())
				return false;
			
			int i = 0;
			for (String nextFlatListToken : flatListTokens )
			{
				TokenWithContext toCompareTo = tokenList.get(i++);
				if (! (nextFlatListToken.equalsIgnoreCase(toCompareTo.getToken()) || nextFlatListToken.equalsIgnoreCase(toCompareTo.getLemma())))
				{
					return false;
				}
			}
			
			return true;
		}
		
		return false;

	}
	
	private static String getLastWord(String string)
	{
		if (string == null)
			return null;
		
		String[] tokens = string.split(" ");
		
		if (tokens.length == 0)
			return null;
		else
			return tokens[tokens.length - 1];
	}
			
	private static List<ReconciledFeatureOpinion> getReconciledFeatureOpinions(Sentence sentence)
	{
		List<ReconciledFeatureOpinion> reconciledOpinions = new ArrayList<ReconciledFeatureOpinion>();
		for (ProductFeatureOpinion opinion : sentence.getFeatureOpinions())
		{
			ReconciledFeatureOpinion reconciledOpinion = new ReconciledFeatureOpinion(opinion);
			boolean foundOneValid = false;
			
			for (TokenWithContext token : sentence.getTokens())
			{
				if (isTokenPredictedAs(token, ModelType.FEATURE) && tokenAppearsInOpinion(token, opinion))
				{
					reconciledOpinion.getTokensWithFeature().add(token);

					//find the sentiment-bearing word among the close-ish semantic dependencies of the token
					TokenWithContext semToken = getRelatedPredictedSemanticToken(token);

					if (semToken != null)
					{					
						reconciledOpinion.addSentimentToken(semToken);
						foundOneValid = true;
					}
				}
			}
			
			if (foundOneValid)
			{
				//merge
				if (reconciledOpinion.isCorrect())
				{
					reconciledOpinions.add(reconciledOpinion);
				}
				else
				{
					System.out.println("Failed isCorrect() for " + reconciledOpinion);
				}
			}
			
			//TODO check to see if we were correct; might need to also add extra handling of nearby negation
		}

		return reconciledOpinions;
	}

	private static TokenWithContext getRelatedPredictedSemanticToken(TokenWithContext token)
	{
		boolean debug = (token.getToken().equals("gprs"));

		return getRelatedPredictedSemanticToken(token, 0, debug);
	}

	private static TokenWithContext getRelatedPredictedSemanticToken(TokenWithContext token, int currentDepth, boolean debug)
	{
		//TODO: Seems to grab wrong adjective in 
		//Failed isCorrect() for ReconciledFeatureOpinion [tokensWithFeature=[food], tokensWithSentiment=[dreadful], opinion=ProductFeatureOpinion [feature=food, lemmatizedFeature=food, sentimentValue=2, no extra details], sentiment=[NEG]]
		//Y50%   [ correctSENTIMENT Great ] [ FEATURE food ] but the [ FEATURE service ] was [ correctSENTIMENT dreadful ] ! [food+2][service-2]
		//...OK; Stanford parser error
		
		//Failed isCorrect() for ReconciledFeatureOpinion [tokensWithFeature=[staff], tokensWithSentiment=[friendly], opinion=ProductFeatureOpinion [feature=staff, lemmatizedFeature=staff, sentimentValue=-2, no extra details], sentiment=[POS]]
		//Y50%   Our [ FEATURE waiter ] was [ correctSENTIMENT friendly ] and it is a [ falsenegativeSENTIMENT shame ] that [ FEATURE he ] didnt have a [ correctSENTIMENT supportive ] [ FEATURE staff ] to [ correctSENTIMENT work ] with . [waiter+2][staff-2]
		//misspelled "didnt" might be botching negation 
	
		if (debug)
			System.out.println("Analysing " + token.getToken() + " at depth " + currentDepth);
		
		TokenWithContext incomingEdge = token.getSemanticIncomingEdge();
		if (incomingEdge != null)
		{
			if (debug)
				System.out.println("Considering " + incomingEdge.getToken() + " at incoming edge");

			if (isTokenPredictedAs(incomingEdge, ModelType.SENTIMENT))
			{
				if (debug)
					System.out.println("Found " + incomingEdge.getToken() + " at incoming edge");
				return incomingEdge;
			}
			
			if (currentDepth <= 3)
			{
				TokenWithContext incomingEdgeRelation = getRelatedPredictedSemanticToken(incomingEdge, currentDepth + 1, debug);
				if (incomingEdgeRelation != null)
				{
					if (debug)
						System.out.println("Found " + incomingEdgeRelation.getToken() + "  at incoming edge relation");
					return incomingEdgeRelation;
				}
			}
		}
				
		if (token.getSemanticallyTaggedTokensWithContext() != null )
		{
			for (SemanticallyTaggedTokenWithContext semToken : token.getSemanticallyTaggedTokensWithContext())
			{
				if (debug)
					System.out.println("Considering outgoing " + semToken.getTokenWithContext().getToken());
				if (isTokenPredictedAs(semToken.getTokenWithContext(), ModelType.SENTIMENT))
				{
					if (debug)
						System.out.println("Found " + semToken.getTokenWithContext().getToken() + " at outgoing");

					return semToken.getTokenWithContext();
				}
			}
		}
		
		if (currentDepth >= 3)
			return null;
		
		if (token.getSemanticallyTaggedTokensWithContext() != null )
		{
			for (SemanticallyTaggedTokenWithContext semToken : token.getSemanticallyTaggedTokensWithContext())
			{
				if (debug)
					System.out.println("Considering " + semToken.getTokenWithContext().getToken() + " at outgoing relation");
				TokenWithContext outgoingEdgeRelation = getRelatedPredictedSemanticToken(semToken.getTokenWithContext(), currentDepth + 1, debug);
				if (outgoingEdgeRelation != null)
				{
					if (debug)
						System.out.println("Found " + outgoingEdgeRelation.getToken() + " at outgoing relation");

					return outgoingEdgeRelation;
				}
			}
		}
		
		
		/*
			//fall through
			for (SemanticallyTaggedTokenWithContext semToken : token.getSemanticallyTaggedTokensWithContext())
			{
				if (semToken.getSemanticRole().equals("conj") && isTokenPredictedAs(semToken.getTokenWithContext(), ModelType.FEATURE))
				{
					//TODO: handle negation explicitly here
					if (semToken.getTokenWithContext().getSemanticallyTaggedTokensWithContext() != null)
					{
						for (SemanticallyTaggedTokenWithContext conjunctionSemToken : semToken.getTokenWithContext().getSemanticallyTaggedTokensWithContext())
						{
							if (conjunctionSemToken.getSemanticRole().equals("amod") && isTokenPredictedAs(conjunctionSemToken.getTokenWithContext(), ModelType.SENTIMENT))
							{
								//System.out.println("Found valid conj feature");
								return conjunctionSemToken.getTokenWithContext();
							}
						}
					}
				}
			}
			
			//TODO: also look for conjunction here; if another product feature is conjuncted and has a valid sentiment, use that
			//		do this as a second loop after this one fails
			//TODO: probably have to branch out further to figure out where more sentiments are
		}
		
		if (incomingEdge != null && incomingEdge.getSemanticallyTaggedTokensWithContext() != null)
		{
			if (incomingEdge.getSemanticIncomingEdge() != null &&  isTokenPredictedAs(incomingEdge.getSemanticIncomingEdge(), ModelType.SENTIMENT))
			{
				//two hops away...for sentences like "voice[-2]##when talking the voice is not very clear . " voice -> talking -> clear
				return incomingEdge.getSemanticIncomingEdge();
			}

			for (SemanticallyTaggedTokenWithContext semToken : incomingEdge.getSemanticallyTaggedTokensWithContext())
			{
				if (isTokenPredictedAs(semToken.getTokenWithContext(), ModelType.SENTIMENT))
				{
					return semToken.getTokenWithContext();
				}
			}
			for (SemanticallyTaggedTokenWithContext semToken : incomingEdge.getSemanticallyTaggedTokensWithContext())
			{
				if (semToken.getTokenWithContext().getSemanticallyTaggedTokensWithContext() != null)
				{
					for (SemanticallyTaggedTokenWithContext deeperSemToken : incomingEdge.getSemanticallyTaggedTokensWithContext())
					{
						//allow hops three away, for sentences like "the design is sleek and the color screen has good resolution" (screen -> has -> resolution -> good)
						if (isTokenPredictedAs(deeperSemToken.getTokenWithContext(), ModelType.SENTIMENT))
						{
							return deeperSemToken.getTokenWithContext();
						}
					}

				}
			}

		}
		*/
		return null;
	}
	
	public static String padRight(String s, int n) {
	     return String.format("%1$-" + n + "s", s);  
	}
	
	private static boolean isTokenPredictedAs(TokenWithContext token, ModelType modelType)
	{
		if (token == null)
			return false;
		if (token.getPredictedModel() == null)
			return false;
		if (token.getPredictedModel() == modelType && token.getPredictedClass() != 0)
			return true;
		
		return false;
		
	}
	
	private static boolean tokenAppearsInOpinion(TokenWithContext token, ProductFeatureOpinion opinion)
	{
		if (token == null || opinion == null)
			return false;
		
		if (opinion.getFeature().contains(token.getToken()))
			return true;
		
		if (opinion.getLemmatizedFeature().contains(token.getLemma()))
			return true;
		
		if (token.getAttribute() != null &&(opinion.getFeature().contains(token.getAttribute()) || opinion.getLemmatizedFeature().contains(token.getAttribute())))
			return true;
		
		return false;
	}
	
	private static String arrayToString(int[] array)
	{
		if (array == null)
			return "null";
		if (array.length == 0)
			return "empty";
		
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		boolean first = true;
		for (Integer nextInt : array)
		{
			if (first)
				first = false;
			else
				sb.append(", ");
			
			sb.append(nextInt);
			
		}
		sb.append("]");
		return sb.toString();
	}
	
	private static int[] commandLineNumberArray(String arg)
	{
		if (arg == null || arg.equals("") || arg.equalsIgnoreCase("null"))
		{
			return new int[0];	
		}
		
		String[] argParts = arg.split(",");
		
		int[] returnValue = new int[argParts.length];
		
		
		for (int i = 0; i < argParts.length; i++)
		{
			try
			{
				returnValue[i] = Integer.valueOf(argParts[i]);
			}
			catch (Exception e)
			{
				System.err.println("Invalid input " + arg + "; must be either a number, the word 'null' (without the quotation marks), or a comma-separated list of numbers containing no spaces");
				return null;
			}
		}
		
		return returnValue;
	}
	
}





//parse command line options, if present
/*if (args != null && args.length > 0)
{
	int argNumber = 0;
	
	percentageSeed = Double.valueOf(args[argNumber++]);
	percentageIncremental = Double.valueOf(args[argNumber++]);
	minNumAdded = Integer.valueOf(args[argNumber++]);						
	maxToAddPerIteration = Integer.valueOf(args[argNumber++]);			
	minConfidenceThresholdPosClass = doublify(args[argNumber++], defaultMinConfidenceThresholdPosClass);	
	minConfidenceThresholdNegClass = doublify(args[argNumber++], defaultMinConfidenceThresholdNegClass);	

	classWeighting = ClassWeighting.valueOf(args[argNumber++]);

	c1 = doublify(args[argNumber++]);
	gamma1 = doublify(args[argNumber++]);
	eps1 = doublify(args[argNumber++]);
	c2 = doublify(args[argNumber++]);
	gamma2 = doublify(args[argNumber++]);
	eps2 = doublify(args[argNumber++]);
	
	
	rootdir = args[argNumber++];
	
	String numLinesArg = args[argNumber++];
	if (numLinesArg == null || numLinesArg.isEmpty() || numLinesArg.equalsIgnoreCase("null"))
		numberOfLinesFromFile = null;
	else
		numberOfLinesFromFile = Integer.valueOf(numLinesArg);
	
	List<String> argFilesToProcess = new ArrayList<String>();
	for (int i = argNumber; i < args.length; i++)
	{
		argFilesToProcess.add(args[i]);
	}
}
*/




