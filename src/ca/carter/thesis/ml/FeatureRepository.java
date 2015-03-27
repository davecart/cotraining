package ca.carter.thesis.ml;

import java.util.ArrayList;
import java.util.List;


public class FeatureRepository  {

//	protected List<String> masterClassList = new ArrayList<String>();
	protected List<String> masterTokenList = new ArrayList<String>();

	//feature list management: add to list if it doesn't exist, else increment the count
	protected int getNumberInList(String token, boolean addMissing)
	{
		//useful for both features and classes
		int featureNumber = masterTokenList.indexOf(token);
		if (addMissing && featureNumber < 0)
		{
			masterTokenList.add(token);
			featureNumber = masterTokenList.size() - 1;
		}
		
		return featureNumber;
	}
	
	public int getNumberOfFeatures()
	{
		return masterTokenList.size();
	}

	public String getNameOfFeature(int key)
	{
		return masterTokenList.get(key);
	}

	public FeatureRepository()
	{
	}
	
	//default features using raw (non-normalized) word counts
	//override this for fancier features
	//if we're building a new model, we want to add the missing features; otherwise, if we're merely predicting, we probably don't
	/*
	@Override
	public Map<Integer, Double> getFeaturesForTriple(Triple nextTriple, boolean addMissingFeatures, boolean isTraining) {

		//can remove named entities as a step towards coupled training
		//String toAnalyze = nextTriple.getPhrase().getString();
		String toAnalyze = NamedEntityModelImpl.removeThisModelsFeatures(nextTriple);
		
		if (toAnalyze == null)
			return null;
		
		StringTokenizer st = DefaultTokenizer.getDefaultTokenizer(toAnalyze);
		int numTokens = st.countTokens();
		
		Map<Integer, Double> tokenCounts = new HashMap<Integer, Double>(); 
		
		List<String> wordsWithCaps = null;
		boolean lastWordHadCap = false;
		
		for (int i = 0; i < numTokens; i++)
		{
			//tokenization notes:
			//	remove trailing punctuation (so that final words in sentences are folded with non-terminating words
			//	if a token contains punctuation (hyphen), record it both with and without
			//  if a token contains punctuation (period), record it both with and without for variations in hyphenated names and acronyms (UN versus U.N.)
			//	if a series of tokens has capital letters, record it together
			List<String> tokenVariations = new ArrayList<String>();
			
			String nextToken = stripTrailingPunctuation(st.nextToken());
			tokenVariations.add(nextToken);

			//hyphens
			if (nextToken.contains("-"))
			{
				for (String nextChunk : nextToken.split("-"))
				{
					tokenVariations.add(nextChunk);
				}
			}
			
			//periods and acronyms
			if (nextToken.contains("."))
				tokenVariations.add(nextToken.replaceAll("\\.", ""));
			
			//strings of capitalized words (which may be named entities)
			boolean isCapitalized = nextToken.matches(".*[A-Z].*");
			if (isCapitalized)
			{
				if (wordsWithCaps == null)
					wordsWithCaps = new ArrayList<String>();
				wordsWithCaps.add(nextToken);
				lastWordHadCap = true;
			}
			if ((!isCapitalized && lastWordHadCap && wordsWithCaps != null) || (isCapitalized && i == numTokens - 1))
			{
				int wordsWithCapsSize = wordsWithCaps.size();
				if (wordsWithCaps.size() > 1)
				{
					//do all combinations: so "UN Security Council" becomes "UN Security" + "Security Council" + "UN Security Council"
					for (int length = 2; length <= wordsWithCapsSize; length++ )
					{
						for (int firstWord = 0; firstWord <= wordsWithCapsSize - length; firstWord++)
						{
							//System.out.println("length is " + length + "; firstWord is " + firstWord );
							StringBuilder sb = new StringBuilder();
							for (int j = 0; j < length; j++)
							{
								if (j > 0)
									sb.append(" ");
								sb.append(wordsWithCaps.get(firstWord + j));
							}
							String permutation = sb.toString();
							tokenVariations.add(permutation);
							if (permutation.contains("-"))
								tokenVariations.add(permutation.replace('-', ' '));
							if (permutation.contains("."))
								tokenVariations.add(permutation.replaceAll("\\.", ""));
							if (permutation.contains(".") && permutation.contains("-"))
								tokenVariations.add(permutation.replace('-', ' ').replaceAll("\\.", ""));
						}
					}
				}
				lastWordHadCap = false;
				wordsWithCaps = null;
			}
			
			for (String nextTokenVariation : tokenVariations)
			{
				addFeature(nextTokenVariation, tokenCounts, addMissingFeatures);
			}
		}

		return tokenCounts;
	}
	*/
	
	/*
	protected void addFeature(String token, Map<Integer, Double> tokenCounts, boolean addMissingFeatures)
	{
		addFeature(token, tokenCounts, addMissingFeatures, 1.0);
	}
	
	protected void addFeature(String token, Map<Integer, Double> tokenCounts, boolean addMissingFeatures, double amountToAddToFeature)
	{
		//System.out.println(nextTokenVariation);
		
		int featureNumber = getNumberInList(masterTokenList, token, addMissingFeatures);
		
		if (addMissingFeatures || featureNumber > 0)  //first half is redundant but add speed; feature number can only be less than zero if addMissingFeatures is false
		{
			Double existingCount = tokenCounts.get(featureNumber);
			if (existingCount == null)
				tokenCounts.put(featureNumber, amountToAddToFeature);
			else
				tokenCounts.put(featureNumber, existingCount + amountToAddToFeature);
		}
		
	}
*/
	
	/*
	private String stripTrailingPunctuation(String token)
	{
		Matcher m = patEndsWithPunctuation.matcher(token);
		if (m.find())
			return token.substring(0, token.length() - 1);
		else
			return token;
	}
	*/
	
	/*
	@Override
	public Prediction getCertainPrediction(Triple triple) {
		return new Prediction(getClassNumber(triple), 1.0);
	}
	*/

	
}
