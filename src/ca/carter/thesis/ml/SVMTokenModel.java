package ca.carter.thesis.ml;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import ca.carter.thesis.languagemodels.StopWords;
import ca.carter.thesis.model.*;
import ca.carter.thesis.model.phrasetree.PartOfSentimentStructure;
import ca.carter.thesis.model.phrasetree.PartOfSpeech;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_print_interface;
import libsvm.svm_problem;

/******
 * 
 * @author davecarter
 *
 * Co-training enabled model of a vector space split into two views
 * This is used as the basis for both the sentiment/opinion word classifier and the product aspect classifier
 * 
 * The two views are lexical (including part of speech, lemma, etc.) and syntactic, respectively
 *  
 */

public abstract class SVMTokenModel {

	protected static final boolean useViews = true;
	public static final boolean useOnlyOneView = false;
	public static final Views useOnlyOneViewWhichView = Views.BAGOFWORDS;
	
	private final int numberOfViews = (useViews && !useOnlyOneView ? 2 : 1);
	private svm_problem[] problemViews;
	private svm_parameter[] parametersViews;	
	private svm_model[] svmModelViews = new svm_model[numberOfViews];;
	private Task task;
	
	private final ClassWeighting classWeighting;
	public static final int kernel = svm_parameter.RBF;
		
	protected final Double specifiedC;
	protected final Double specifiedGamma;
	protected final Double specifiedEpsilon;
	
	List<TokenWithContext> tokens;
	
	FeatureRepository featureRepository = new FeatureRepository();
		
	private static final boolean omitStopWords = false;
	
	private static final boolean debugFeatureNames = false;
	
	public SVMTokenModel(Task task, List<TokenWithContext> tokens, Writer[] writer, ClassWeighting classWeighting, Double c, Double gamma, Double epsilon) {
		
		this.task = task;
		this.tokens = tokens;
		this.classWeighting = (classWeighting == null ? ClassWeighting.EQUAL : classWeighting);
		
		this.specifiedC = c;
		this.specifiedGamma = gamma;
		this.specifiedEpsilon = epsilon;
		
		if (tokens == null)
		{
			System.err.println("No tokens present. Can not create training model.");
			return;
		}
		
		//writer != null ----- this means that, as we're building the model, we should also record it to a text file for, say, parameter estimation using an external tool

		//build master features list
		for (TokenWithContext nextTrainingToken : tokens)
		{
			buildFeaturesForToken(nextTrainingToken);
		}
		
		

		retrain(writer);
	}	
	
	public void retrain(Writer[] writer)
	{
		boolean outputToFile = (writer != null);
		
		svm.svm_set_print_string_function(svm_print_null);
		
		List<Vector<Double>> xClassesViews = new ArrayList<Vector<Double>>();
		List<Vector<svm_node[]>> yFeaturesViews = new ArrayList<Vector<svm_node[]>>();
		
		for (int viewNum = 0 ; viewNum < numberOfViews; viewNum++)
		{
			xClassesViews.add(new Vector<Double>());
			yFeaturesViews.add(new Vector<svm_node[]>());
		}
				
		int max_index[] = {0 , 0}; //todo: this is views-based; backfill single-view case
		
		int numInPositiveClass = 0;
		int numInNegativeClass = 0;
		
		for (TokenWithContext nextTrainingToken : tokens)
		{
			if (omitStopWords && StopWords.isStopWord(nextTrainingToken.getToken()))
				continue;
			
			try
			{
				//if we added this token in the cotraining phase, it will have a predicted class that we should use instead of the ground truth class, which would be unknown in a semi-supervised case
				double classNumber;
				
				if (nextTrainingToken.getPredictedClass() != null)
				{
					classNumber = (nextTrainingToken.getPredictedModel() == this.getModelType() ? nextTrainingToken.getPredictedClass() : 0.0);
				}
				else
					classNumber = getClassForToken(nextTrainingToken);
				
				//debugging code only
				//if (classNumber != 0)
				//	System.out.println("Positive token for " + this.getModelType() + " " + getFormattedTokenContext(nextTrainingToken) + "\n" + nextTrainingToken.toString() );
					
				if (classNumber != 0)
					numInPositiveClass++;
				else
					numInNegativeClass++;
				
				if (outputToFile)
				{
					for (Writer nextWriter : writer)
					{
						nextWriter.write((int) classNumber + " ");
					}
				}
				
				for (int viewNum = 0 ; viewNum < numberOfViews; viewNum++)
				{
					Map<Integer, Double> featureMap = getFeaturesForToken(nextTrainingToken, viewNum);
					//System.out.println(nextTrainingToken.getToken() + " " + featureMap);	
					
					if (featureMap == null)
						continue;
					
					//build into SVM model
					int m = featureMap.size();
					svm_node[] x = new svm_node[m];
					int j = 0;
					
					for (Entry<Integer, Double> nextFeature : featureMap.entrySet())
					{
						x[j] = new svm_node();
						x[j].index = nextFeature.getKey();
						x[j].value = nextFeature.getValue();
		
						j++;
	
						if (outputToFile)
							writer[viewNum].write(nextFeature.getKey() + ":" + nextFeature.getValue() + " ");
					}
					
					if (outputToFile)
						writer[viewNum].write("\n");
					
					if(m>0) max_index[viewNum] = Math.max(max_index[viewNum], x[m-1].index);
	
					xClassesViews.get(viewNum).addElement(classNumber);
					yFeaturesViews.get(viewNum).addElement(x);
				}
			}
			catch (IOException e)
			{
				
			}
		}
		
		try {
			if (outputToFile)
			{
				for (Writer nextWriter : writer)
				{
					nextWriter.close();
				}
				System.out.println("Done writing " + this.getName() + " model files. Will not build classifier model.");
				return;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//System.out.println( xClasses );
		//System.out.println( yFeatures );

		problemViews = new svm_problem[numberOfViews];
		
		for (int viewNum = 0; viewNum < numberOfViews; viewNum++)
		{
			//System.out.println("View " + viewNum);
			
			problemViews[viewNum] = new svm_problem();
			
			problemViews[viewNum].l = xClassesViews.get(viewNum).size();
			problemViews[viewNum].x = new svm_node[problemViews[viewNum].l][];
			for(int i=0;i<problemViews[viewNum].l;i++)
				problemViews[viewNum].x[i] = yFeaturesViews.get(viewNum).elementAt(i);
			problemViews[viewNum].y = new double[problemViews[viewNum].l];
			for(int i=0;i<problemViews[viewNum].l;i++)
				problemViews[viewNum].y[i] = xClassesViews.get(viewNum).elementAt(i);
		}
		
		parametersViews = new svm_parameter[numberOfViews];
		
		for (int viewNum = 0; viewNum < numberOfViews; viewNum++)
		{
			svm_parameter parameters =  new svm_parameter();
			
			/* the old default values
			parameters.svm_type = svm_parameter.C_SVC;
			parameters.kernel_type = svm_parameter.RBF;
			parameters.degree = 3;
			//parameters.gamma = 0;	// 1/num_features
			parameters.gamma = 1 / getTotalNumberOfAvailableFeatures();
			parameters.coef0 = 0;
			parameters.nu = 0.5;
			parameters.cache_size = 100;
			parameters.C = 1;
			parameters.eps = epsilon; //1e-3;
			parameters.p = 0.1;
			parameters.shrinking = 1;
			parameters.probability = 1;
			parameters.nr_weight = 0;
			parameters.weight_label = new int[0];
			parameters.weight = new double[0];
			if(parameters.gamma == 0 && max_index > 0)
				parameters.gamma = 1.0/max_index;
			*/
	
			parameters.probability = 1;
			parameters.cache_size = 250;
			
			if (specifiedEpsilon != null)
				parameters.eps = specifiedEpsilon;
			else
				parameters.eps = getEpsilon();
	
			parameters.kernel_type = kernel;
			
			//determined classififier-by-classifier by grid search on initial 20% training data, averaged over the five data sets
			//parameters.gamma = 1 / (numInPositiveClass + numInNegativeClass); //getGamma(); ...since number of data points changes, so should gamma; and experiment to do parameter estimation found that ideal gamma is usually around 1 / number of data points 
			parameters.C = getC(viewNum);
			parameters.gamma = getGamma(viewNum);
	
			//weight the C of the classes to accommodate the imbalance; re. Asa Ben-Hur and Jason Weston "A User's Guide to Support Vector Machines", section 7
			if (classWeighting != ClassWeighting.EQUAL)
			{
				parameters.weight_label = new int[2];
				parameters.weight = new double[2];
				parameters.weight_label[0] = 0; //negative class
				parameters.weight_label[1] = 1; //positive class
	
				switch (classWeighting)
				{
					case OVERSIZENEG:
						parameters.weight[0] = getC(viewNum) * numInPositiveClass / numInNegativeClass;
						parameters.weight[1] = getC(viewNum);
						break;
					case UNDERSIZENEG:
						parameters.weight[0] = getC(viewNum) / (numInPositiveClass / numInNegativeClass);
						parameters.weight[1] = getC(viewNum);
						break;
					case OVERSIZEPOS:
						parameters.weight[0] = getC(viewNum);
						parameters.weight[1] = getC(viewNum) * (numInPositiveClass / numInNegativeClass);
						break;
					case UNDERSIZEPOS:
						parameters.weight[0] = getC(viewNum);
						parameters.weight[1] = getC(viewNum) / (numInPositiveClass / numInNegativeClass);
						break;
					default:
				}
			}
		
			parametersViews[viewNum] = parameters;

			String error_msg = svm.svm_check_parameter(problemViews[viewNum],parametersViews[viewNum]);

			/*
			 * default gamma given no tuning information
			parameters.C = 1;
			if(parameters.gamma == 0 && max_index > 0)
				parameters.gamma = 1.0/max_index;
			*/


			/*
			System.out.println("svm_type " + parameters.svm_type);
			System.out.println("kernel_type " + parameters.kernel_type);  //0 is linear (special case of RBF), 2 is RBF
			System.out.println("degree " + parameters.degree);
			System.out.println("gamma " + parameters.gamma);
			System.out.println("coef0 " + parameters.coef0);
			System.out.println("nu " + parameters.nu);
			System.out.println("C " + parameters.C);
			System.out.println("eps " + parameters.eps);
			System.out.println("p " + parameters.p);
			System.out.println("shrinking " + parameters.shrinking);
			System.out.println("probability " + parameters.probability);
			System.out.println("nr_weight " + parameters.nr_weight);
			System.out.println("weight_label " + parameters.weight_label);
			System.out.println("weight " + parameters.weight);
			*/
			
			if(error_msg != null)
			{
				System.err.print("ERROR: "+error_msg+"\n");
				//System.exit(1);
			}
		 
			svmModelViews[viewNum] = svm.svm_train(problemViews[viewNum],parametersViews[viewNum]);
		}
		
	}
	
	
	abstract public double getC(int viewNum);
	abstract public double getGamma(int viewNum);
	abstract public double getEpsilon();

	abstract public ModelType getModelType();
	
	public void addTokenForNextTraining(TokenWithContext token)
	{
		buildFeaturesForToken(token);
		tokens.add(token);
	}
	
	abstract public Double getClassForToken(TokenWithContext token);
	//abstract protected Double getExpectedClassNumber(TokenWithContext token);
	
	abstract public String getName();
	
	private void buildFeaturesForToken(TokenWithContext token)
	{
		featureRepository.getNumberInList(token.getToken(), true);
	}

	//get the features for the token bean in a sparse map
	private Map<Integer, Double> getFeaturesForToken(TokenWithContext token, int viewNumber)
	{
		//by reflection, go get all get____ for Strings and is_____ for booleans
		//then process lists, etc. manually

		//using TreeMap because it means it will be sorted as it is built; needed for flat file output
		Map<Integer, Double> localMap = new TreeMap<Integer, Double>();

		// simplest, token only version, is :
		//	int tokenNumber = featureRepository.getNumberInList(token.getToken(), true);
		//  localMap.put(tokenNumber, 1.0);
		
		for (Method nextMethod : TokenWithContext.class.getDeclaredMethods())
		{
			try {
				String value = null;
				int featureNumber;
				
				//TODO xxx need to arrange this by view number, splitting lexical and syntactic
				
				if (nextMethod.getName().startsWith("get") && validForView(nextMethod.getName(), viewNumber) )
				{
					if (nextMethod.getName().startsWith("getPredicted") || nextMethod.getName().startsWith("getFormatted"))
					{
						//do nothing; this are not real features
					}	
					else if (nextMethod.getName().equals("getOpinion"))
					{
						//TODO: do nothing, since it is something we are trying to predict
					}
					else if (nextMethod.getName().equals("getPartOfSentimentStructure"))
					{
						//TODO: should do nothing, since these are something we're trying to predict
					}
					else if (nextMethod.getName().equals("getPositionInSentence"))
					{
						
						// TODO: consider putting this in/taking this out; could be valuable or could be a total red herring
						/*double positionValue = (1.0 * (double) token.getPositionInSentence() / 100);
						featureNumber = featureRepository.getNumberInList("sentencepos", true);
						localMap.put(featureNumber, positionValue);*/
						
					}
					else if (nextMethod.getReturnType() == String.class)
					{
						value = (String) nextMethod.invoke(token, null);
						featureNumber = featureRepository.getNumberInList(nextMethod.getName() + "=" + value, true);
						if (debugFeatureNames)
							System.out.println(nextMethod.getName() + " -> " + value);
						localMap.put(featureNumber, 1.0);
					}
					else if (nextMethod.getReturnType() == TokenWithContext.class)
					{
						TokenWithContext localValue = (TokenWithContext) nextMethod.invoke(token, null);
						addFeaturesForNearbyTokenWithContext(localMap, localValue, nextMethod.getName(), null, viewNumber);
					}
					else if (nextMethod.getReturnType() == PartOfSpeech.class)
					{
						PartOfSpeech localValue = (PartOfSpeech) nextMethod.invoke(token, null);
						if (localValue != null)
							value = localValue.toString();
						featureNumber = featureRepository.getNumberInList(nextMethod.getName() + "=" + value, true);
						if (debugFeatureNames)
							System.out.println(nextMethod.getName() + " -> " + value);
						localMap.put(featureNumber, 1.0);
					}
					else if (nextMethod.getReturnType() == PartOfSentimentStructure.class)
					{
						PartOfSentimentStructure localValue = (PartOfSentimentStructure) nextMethod.invoke(token, null);
						if (localValue != null)
							value = localValue.toString();
						featureNumber = featureRepository.getNumberInList(nextMethod.getName() + "=" + value, true);
						if (debugFeatureNames)
							System.out.println(nextMethod.getName() + " -> " + value);
						localMap.put(featureNumber, 1.0);
					}
					else if (nextMethod.getReturnType() == List.class)
					{
						final List<?> list = (List<?>) nextMethod.invoke(token, null);
						
						final boolean isListBefore = (nextMethod.getName().contains("Previous") || nextMethod.getName().contains("Parentage"));
						final boolean isListAfter = (nextMethod.getName().contains("Next"));
						
						//if (!isListBefore && !isListAfter)
						//	System.err.println("Not sure if the list for " + nextMethod.getName() + " is before or after the given token, so it'll be hard to figure out how to assign distance relationships.");
												
						if (list != null && ! list.isEmpty())
						{
							int relativePosition = 0;
							if (isListBefore)
								relativePosition = 0 - list.size();
							else if (isListAfter)
								relativePosition = 1;
							else
								relativePosition = 0;

							//necessary for something like the preceeding token list for the second token in a sentence
							Object firstNonNullInList = null;
							for (Object nextInList : list)
							{
								if (nextInList != null)
								{
									firstNonNullInList = nextInList;
									break;
								}
							}
												
							//if the list is useable, check the class type of the first item in the list
							if (firstNonNullInList == null)
							{
								//entire list is null, so there's nothing more interesting to report
								featureNumber = featureRepository.getNumberInList(nextMethod.getName() + "=allnull", true);
								localMap.put(featureNumber, 1.0);
								if (debugFeatureNames)
									System.out.println(nextMethod.getName() + " -> " + value);
							}
							else if (firstNonNullInList instanceof TokenWithContext)
							{
								for (Object nextItem : list)
								{
									final TokenWithContext nextTokenInList = (TokenWithContext) nextItem;
									FeatureDistance featureDistance = FeatureDistance.byDistance(relativePosition);
									if (featureDistance != FeatureDistance.MINUSMORE)
										addFeaturesForNearbyTokenWithContext(localMap, nextTokenInList, nextMethod.getName(), featureDistance, viewNumber);
									if (isListBefore || isListAfter)
										relativePosition++;
								}
							}
							else if (firstNonNullInList instanceof PartOfSpeech) //&& viewNumber == 0 //despite being POS, is only called from view 1; only in terms of syntactic structure
							{
								for (Object nextItem : list)
								{
									final PartOfSpeech nextPOS = (PartOfSpeech) nextItem;
									FeatureDistance featureDistance = FeatureDistance.byDistance(relativePosition);
									if (featureDistance != FeatureDistance.MINUSMORE)
									{
										featureNumber = featureRepository.getNumberInList(nextMethod.getName() + featureDistance + "=" + nextItem.toString(), true);
										localMap.put(featureNumber, 1.0);
										if (debugFeatureNames)
											System.out.println(nextMethod.getName() + " -> " + value);
									}
									if (isListBefore || isListAfter)
										relativePosition++;
								}
							}
							else if (firstNonNullInList instanceof SemanticallyTaggedTokenWithContext) //&& viewNumber == 1  //only called from view 1
							{
								for (Object nextItem : list)
								{
									//TODO: might be worthwhile to pull in more TokenWithContext features here?
									final SemanticallyTaggedTokenWithContext nextTokenInList = (SemanticallyTaggedTokenWithContext) nextItem;
									FeatureDistance featureDistance = FeatureDistance.byDistance(relativePosition);
									if (featureDistance != FeatureDistance.MINUSMORE)
									{
										featureNumber = featureRepository.getNumberInList(nextMethod.getName() + featureDistance + ".Role=" + nextTokenInList.getSemanticRole(), true);
										localMap.put(featureNumber, 1.0);
										if (debugFeatureNames)
											System.out.println(nextMethod.getName() + featureDistance + ".Role=" + nextTokenInList.getSemanticRole() + " -> " + value);
										featureNumber = featureRepository.getNumberInList(nextMethod.getName() + featureDistance + ".RoleAndToken=" + nextTokenInList.getSemanticRole() + nextTokenInList.getTokenWithContext().getLemma(), true);
										localMap.put(featureNumber, 1.0);
										if (debugFeatureNames)
											System.out.println(nextMethod.getName() + featureDistance + ".RoleAndToken=" + nextTokenInList.getSemanticRole() + nextTokenInList.getTokenWithContext().getLemma() + " -> " + value);
										featureNumber = featureRepository.getNumberInList(nextMethod.getName() + featureDistance + ".RoleAndPOS=" + nextTokenInList.getSemanticRole() + nextTokenInList.getTokenWithContext().getPos(), true);
										localMap.put(featureNumber, 1.0);
										if (debugFeatureNames)
											System.out.println(nextMethod.getName() + featureDistance + ".RoleAndPOS=" + nextTokenInList.getSemanticRole() + nextTokenInList.getTokenWithContext().getPos() + " -> " + value);

									}

									if (isListBefore || isListAfter)
										relativePosition++;
								}
							}
							else
							{
								System.err.println("Unhandled list type for " + nextMethod.getName() + " / " + firstNonNullInList.getClass().getName());
							}
						}
							

					}
					else
					{
						//should never get called; if so, we need to implement a new type in here
						System.err.println("Unhandled getter : " + nextMethod.getName() + " " + nextMethod.getReturnType().getName());
					}
				}
				else if (nextMethod.getReturnType() == boolean.class && nextMethod.getName().startsWith("is") && validForView(nextMethod.getName(), viewNumber))
				{
					value = ((Boolean) nextMethod.invoke(token, null)).toString();
					featureNumber = featureRepository.getNumberInList(nextMethod.getName() + "=" + value, true);
					localMap.put(featureNumber, 1.0);
					if (debugFeatureNames)
						System.out.println(nextMethod.getName() + " -> " + value);
				}

			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		
		
		
		return localMap;
	}
	
	private boolean validForView(String getterName, int viewNum)
	{
		if (!useViews)
			return true;
		
		final String[] validLexicalMethods = {"getToken",
				"getLemma",
				"getPos",
				"getPreviousTokens",
				"getNextTokens",
				"isAdjective",
				"getAttribute",	//WordNet attribute property; i.e., fast -> speed	
				//"isSemanticOutgoingEdgesIncludeNegation"
		};
		final String[] validSyntacticMethods = {"getPositionInSentence",
				"getPartOfSentimentStructure",
				"getLocalParentage",
				"getParentage",
				"isCoreferenceHead",
				"getFlatResolvedCoreference",
				"getImmediateParent",
				"getPreviousToken",
				"getNextToken",
				"getSemanticallyTaggedTokensWithContext",
				"getSemanticSpecificRole",
				"getSemanticGeneralRole",
				"getSemanticIncomingEdge",
				"isSemanticOutgoingEdgesIncludeNegation",
				"isNamedEntity"
		};
		final String[] validBagOfWordsMethods = {"getToken"};
		
		//TODO: lots of String comparisons here against a fixed list; there should be a way to speed this up
		
		if (useOnlyOneView)
		{
			if (useOnlyOneViewWhichView == Views.LEXICAL) //lexical
			{
				for (String nextMethod : validLexicalMethods)
				{
					if (nextMethod.equals(getterName))
						return true;
				}
			}
			else if (useOnlyOneViewWhichView == Views.SYNTACTIC) //syntactic
			{
				for (String nextMethod : validSyntacticMethods)
				{
					if (nextMethod.equals(getterName))
						return true;
				}
			}
			else if (useOnlyOneViewWhichView == Views.BAGOFWORDS)
			{
				for (String nextMethod : validBagOfWordsMethods)
				{
					if (nextMethod.equals(getterName))
						return true;
				}
			}
		}	
		else
		{
			if (Views.getViewForNumber(viewNum) == Views.LEXICAL) //lexical
			{
				for (String nextMethod : validLexicalMethods)
				{
					if (nextMethod.equals(getterName))
						return true;
				}
			}
			else if (Views.getViewForNumber(viewNum) == Views.SYNTACTIC) //syntactic
			{
				for (String nextMethod : validSyntacticMethods)
				{
					if (nextMethod.equals(getterName))
						return true;
				}
				
			}
		}

		return false;
		
	}
	
	//adapted from LibSVM sample code
	@SuppressWarnings("unused")
	public Prediction predict(TokenWithContext token)
	{	
		if (omitStopWords && StopWords.isStopWord(token.getToken()))
			return new Prediction(0, -1, null);	//estimate probability at less than zero, so as to not include stop words in subsequent cotraining models

		Prediction bestPredictionSoFar = null;
		
		for (int viewNum = 0; viewNum < numberOfViews; viewNum++)
		{
			
			Map<Integer, Double> features = getFeaturesForToken(token, viewNum);
		
			int nr_class=svm.svm_get_nr_class(svmModelViews[viewNum]);
			double[] prob_estimates = null;
			int[] labels = null;

			labels=new int[nr_class];
			svm.svm_get_labels(svmModelViews[viewNum],labels);
			prob_estimates = new double[nr_class];

			svm_node[] x = new svm_node[features.size()];
			int i = 0;
			for (Entry<Integer, Double> nextFeature : features.entrySet())
			{
				x[i] = new svm_node();
				x[i].index = nextFeature.getKey();
				x[i].value = nextFeature.getValue();
	
				i++;
			}

			double v = svm.svm_predict_probability(svmModelViews[viewNum],x,prob_estimates);
			Map<Integer, Double> classProbabilities = new HashMap<Integer, Double>();
			for(int j=0;j<nr_class;j++)
			{
				//the classes are not in any particular order in the SVM model, so it would be correct to use j without consulting the class labels in the SVM model
				classProbabilities.put(labels[j], prob_estimates[j]); 
			}
			
			if (bestPredictionSoFar == null || (classProbabilities.get((int) v ) > bestPredictionSoFar.getProbability())  )
				bestPredictionSoFar = new Prediction((int) v, classProbabilities.get((int) v), classProbabilities);
		}
		
		return bestPredictionSoFar;
	}
	
	public void addFeaturesForNearbyTokenWithContext(Map<Integer, Double> featureMap, TokenWithContext tokenWithContext, String tokenWithContextGetterName, FeatureDistance featureDistance, int view)
	{
		int featureNumber;
	
		final String featureNamePrefix;
		
		if (featureDistance == null)
			featureNamePrefix = tokenWithContextGetterName;
		else
		{
			//handle cases where having a word, say, before, is interesting, but the fact that it's one or two or three words before is too specific
			
			featureNamePrefix = tokenWithContextGetterName + featureDistance.toString();
			
			if (featureDistance.canBeGeneralized())
			{
				addFeaturesForNearbyTokenWithContext(featureMap, tokenWithContext, tokenWithContextGetterName, featureDistance.getGeneralCase(), view);
			}
		}
		
		if (tokenWithContext != null)
		{
			//in a TokenWithContext case, we want several features: token, lemma, POS, part of sentiment, etc.
			
			if (!useViews || view == 0) //lexical
			{
				if (debugFeatureNames)
					System.out.println(featureNamePrefix + ".getToken");
				featureNumber = featureRepository.getNumberInList(featureNamePrefix + ".getToken" + "=" + tokenWithContext.getToken(), true);
				featureMap.put(featureNumber, 1.0);
				if (debugFeatureNames)
					System.out.println(featureNamePrefix + ".getLemma");
				featureNumber = featureRepository.getNumberInList(featureNamePrefix + ".getLemma" + "=" + tokenWithContext.getLemma(), true);
				featureMap.put(featureNumber, 1.0);
				if (debugFeatureNames)
					System.out.println(featureNamePrefix + ".getPos");
				featureNumber = featureRepository.getNumberInList(featureNamePrefix + ".getPos" + "=" + tokenWithContext.getPos(), true);
				featureMap.put(featureNumber, 1.0);
				
			}
			if (!useViews || view == 1) //syntactic
			{
				//featureNumber = featureRepository.getNumberInList(featureNamePrefix + ".getPartOfSentimentStructure" + "=" + tokenWithContext.getPartOfSentimentStructure(), true);
				//featureMap.put(featureNumber, 1.0);
				if (debugFeatureNames)
					System.out.println(featureNamePrefix + ".isCoreferenceHead");
				featureNumber = featureRepository.getNumberInList(featureNamePrefix + ".isCoreferenceHead" + "=" + tokenWithContext.isCoreferenceHead(), true);
				featureMap.put(featureNumber, 1.0);
				if (debugFeatureNames)
					System.out.println(featureNamePrefix + ".getFlatResolvedCoreference");
				featureNumber = featureRepository.getNumberInList(featureNamePrefix + ".getFlatResolvedCoreference" + "=" + tokenWithContext.getFlatResolvedCoreference(), true);
				featureMap.put(featureNumber, 1.0);
				if (debugFeatureNames)
					System.out.println(featureNamePrefix + ".isNamedEntity");
				featureNumber = featureRepository.getNumberInList(featureNamePrefix + ".isNamedEntity" + "=" + tokenWithContext.isNamedEntity(), true);
				featureMap.put(featureNumber, 1.0);
				if (debugFeatureNames)
					System.out.println(featureNamePrefix + ".getSemanticGeneralRole");
				featureNumber = featureRepository.getNumberInList(featureNamePrefix + ".getSemanticGeneralRole" + "=" + tokenWithContext.getSemanticGeneralRole(), true);
				featureMap.put(featureNumber, 1.0);
				if (debugFeatureNames)
					System.out.println(featureNamePrefix + ".getSemanticSpecificRole");
				featureNumber = featureRepository.getNumberInList(featureNamePrefix + ".getSemanticSpecificRole" + "=" + tokenWithContext.getSemanticSpecificRole(), true);
				featureMap.put(featureNumber, 1.0);
				if (debugFeatureNames)
					System.out.println(featureNamePrefix + ".isSemanticOutgoingEdgesIncludeNegation");
				featureNumber = featureRepository.getNumberInList(featureNamePrefix + ".isSemanticOutgoingEdgesIncludeNegation" + "=" + tokenWithContext.isSemanticOutgoingEdgesIncludeNegation(), true);
				featureMap.put(featureNumber, 1.0);
				//POS feature; could be argued that this does not belong in this view; on the other hand, it's taken from the parse tree, making it syntactic, so I'll leave it in for now.
				if (debugFeatureNames)
					System.out.println(featureNamePrefix + ".getImmediateParent");
				featureNumber = featureRepository.getNumberInList(featureNamePrefix + ".getImmediateParent" + "=" + tokenWithContext.getImmediateParent(), true);
				featureMap.put(featureNumber, 1.0);
				
			}
			//TODO: could branch out one more here...get semantic roles two away
		}	
	}
	private static svm_print_interface svm_print_null = new svm_print_interface()
	{
		public void print(String s) {}
	};
	
	
	public static void main(String[] args)
	{
		System.out.println("Testing SVM model creation.");
		
		//TODO this works very poorly with skewed classes; i.e., removing a bunch of named entity cases makes things work poorly *unless* we revert to predicting binary without probabilities; so weird
		
		//Also, LibSVM estimates probabilities using internal 5-fold cross validation, so we need enough data in the test set to allow that to work
		
		/*TokenWithContext[] trainingTokens = 
		{
				//http://stackoverflow.com/questions/5988574/why-does-svm-predict-and-svm-predict-probability-give-different-results-in-java
				
				new TokenWithContext("this", null, null, null, null, null, false),
				new TokenWithContext("is", null, null, null, null, null, false),
				new TokenWithContext("a", null, null, null, null, null, false),
				new TokenWithContext("Sentential", null, null, null, null, null, true),
				new TokenWithContext("sentence", null, null, null, null, null, false),
				new TokenWithContext("with", null, null, null, null, null, false),
				new TokenWithContext("Apple", null, null, null, null, null, true),
				new TokenWithContext("iPad", null, null, null, null, null, true),
				new TokenWithContext("features", null, null, null, null, null, false),
				new TokenWithContext("plus", null, null, null, null, null, false),
				new TokenWithContext("some", null, null, null, null, null, false),
				new TokenWithContext("extra", null, null, null, null, null, false),
				new TokenWithContext("Lucky", null, null, null, null, null, true),
				new TokenWithContext("Brand", null, null, null, null, null, true),
				new TokenWithContext("words", null, null, null, null, null, false),
				new TokenWithContext("thrown", null, null, null, null, null, false),
				new TokenWithContext("in", null, null, null, null, null, false),
				new TokenWithContext("for", null, null, null, null, null, false),
				new TokenWithContext("good", null, null, null, null, null, false),
				new TokenWithContext("measure", null, null, null, null, null, false),
				
				new TokenWithContext("this", null, null, null, null, null, false),
				new TokenWithContext("is", null, null, null, null, null, false),
				new TokenWithContext("a", null, null, null, null, null, false),
				new TokenWithContext("Sentential", null, null, null, null, null, true),
				new TokenWithContext("sentence", null, null, null, null, null, false),
				new TokenWithContext("with", null, null, null, null, null, false),
				new TokenWithContext("Apple", null, null, null, null, null, true),
				new TokenWithContext("iPad", null, null, null, null, null, true),
				new TokenWithContext("features", null, null, null, null, null, false),
				new TokenWithContext("plus", null, null, null, null, null, false),
				new TokenWithContext("some", null, null, null, null, null, false),
				new TokenWithContext("extra", null, null, null, null, null, false),
				new TokenWithContext("Lucky", null, null, null, null, null, true),
				new TokenWithContext("Brand", null, null, null, null, null, true),
				new TokenWithContext("words", null, null, null, null, null, false),
				new TokenWithContext("thrown", null, null, null, null, null, false),
				new TokenWithContext("in", null, null, null, null, null, false),
				new TokenWithContext("for", null, null, null, null, null, false),
				new TokenWithContext("good", null, null, null, null, null, false),
				
				new TokenWithContext("good", "good", PartOfSpeech.ADJP, null, null, null, true),
		};
		
		SVMTokenModel model = new SVMTokenModelSentiment(Arrays.asList(trainingTokens), null, ClassWeighting.EQUAL, null, null, null);

		*/

		String[] sentences = {
				"feature[+2], ##the car 's features are wonderful .",
				"feature[+2], ##the car has a wonderful set of features .",
				"feature[+2], ##the camera has a wonderful set of features .",
				"lens[+2], ##the camera has a great lens .",
				"grip[+1], ##the camera has a fine grip .",
				"grip[-1], ##i didn't like the grip on the camera .",
				
		};
		
		List<TokenWithContext> trainingTokens = new ArrayList<TokenWithContext>();
		for (String nextSentence : sentences) {
			Sentence sentence = new Sentence(new SimpleSentence(nextSentence, true), Sentence.getDefaultPipeline(), ProductFeatureOpinion.getDefaultPipeline(), null, null, false);
			trainingTokens.addAll(sentence.getTokens());
		}
		
		List<SVMTokenModel> models = new ArrayList<SVMTokenModel>();
		models.add(new SVMTokenModelSentiment(Task.BINGLIU, trainingTokens, null, ClassWeighting.EQUAL, null, null, null));
		models.add(new SVMTokenModelFeature(Task.BINGLIU, trainingTokens, null, ClassWeighting.EQUAL, null, null, null));

		Sentence testSentence = new Sentence(new SimpleSentence("shmork[+2], ##the camera has a decent shmork .", true), Sentence.getDefaultPipeline(), ProductFeatureOpinion.getDefaultPipeline(), null, null, false);
		//Sentence testSentence = new Sentence("shmork[+2], ##i did n't grok the camera 's features .", Sentence.getDefaultPipeline(), ProductFeatureOpinion.getDefaultPipeline(), null, null, false);

		for (SVMTokenModel model : models)
		{
			System.out.println("Model is " + model.getName());
			for (TokenWithContext nextToken : trainingTokens)
			{
				Prediction prediction = model.predict(nextToken);
				System.out.println((prediction.getClassNumber() == model.getClassForToken(nextToken) ? "Correct " : "Incorrect ") + nextToken.getToken() + " " + prediction);
				//System.out.println(nextToken.getToken() + " " + prediction);
			}
/*			for (TokenWithContext nextToken : testSentence.getTokens())
			{
				Prediction prediction = model.predict(nextToken);
				System.out.println((prediction.getClassNumber() == model.getClassForToken(nextToken) ? "Correct " : "Incorrect ") + nextToken.getToken() + " " + prediction);
				//System.out.println(nextToken.getToken() + " " + prediction);
			}
*/			System.out.println("-----");
		}

		
		for (TokenWithContext nextToken : testSentence.getTokens())
		{

			
			Prediction topPrediction = null;
			ModelType topPredictionModel = null;
			Double nominalClassForTopPrediction = null;
			for (SVMTokenModel model : models)
			{
				Map<Integer, Double> featuresLexical = model.getFeaturesForToken(nextToken, 0);
				Map<Integer, Double> featuresSyntactic = model.getFeaturesForToken(nextToken, 1);
				System.out.println("lexical features: " + featuresLexical.size() + " / syntactic featuers: " + featuresSyntactic.size());
				
				Prediction prediction = model.predict(nextToken);
				if (prediction.getClassNumber() != 0 && (topPrediction == null || prediction.getProbability() > topPrediction.getProbability()))
				{
					topPrediction = prediction;
					topPredictionModel = model.getModelType();
					nominalClassForTopPrediction = model.getClassForToken(nextToken);
				}
				
			}

			if (topPrediction != null)
				System.out.print("[ ");
			System.out.print(nextToken.getToken());
			if (topPrediction != null)
				System.out.print(" (" + topPredictionModel.toString() + " " + (topPrediction.getClassNumber() == nominalClassForTopPrediction ? "Correct" : "Incorrect") + ") ]");
			System.out.print(" ");
		}

	}


	

	
}
