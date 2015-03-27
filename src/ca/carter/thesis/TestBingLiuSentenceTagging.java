package ca.carter.thesis;

import java.util.List;

import ca.carter.thesis.model.ProductFeatureOpinion;
import ca.carter.thesis.model.Sentence;
import ca.carter.thesis.model.TokenWithContext;
import ca.carter.thesis.model.phrasetree.PartOfSentimentStructure;

public class TestBingLiuSentenceTagging {
	private static final Integer defaultNumberOfLinesFromFile = null; //null for entire file; practical minimum is 10, so as to have enough total words that the classifiers can do internal cross-validation to build confidence estimates
	
	private static final String[] defaultFilesToProcess = 
		{
			//ProcessBingLiuReviews.defaultRootdir + "/bingliudata/Apex AD2600 Progressive-scan DVD player.txt",
			//ProcessBingLiuReviews.defaultRootdir + "/bingliudata/Canon G3.txt",
			//ProcessBingLiuReviews.defaultRootdir + "/bingliudata/Creative Labs Nomad Jukebox Zen Xtra 40GB.txt",
			ProcessReviews.defaultRootdir + "/bingliudata/Nikon coolpix 4300.txt",
			//ProcessBingLiuReviews.defaultRootdir + "/bingliudata/Nokia 6610.txt",
		};

	public static void main(String[] args)
	{
		try {
			List<Sentence> sentences = ProcessReviews.getSentences(defaultFilesToProcess[0], defaultNumberOfLinesFromFile, null, null, (defaultNumberOfLinesFromFile == null ? null : 2));

			int totalFound = 0;
			int totalNotFound = 0;

			//loop through the sentences and see how well I've been able to extract the sentiments
			for (Sentence sentence : sentences)
			{
				int numFound = 0;
				int numNotFound = 0;
				StringBuilder sbFound = new StringBuilder();
				StringBuilder sbNotFound = new StringBuilder();
				
				if (sentence.getFeatureOpinions() != null)
				{

					//see if we matched the feature tokens correctly
					for (ProductFeatureOpinion opinion : sentence.getFeatureOpinions())
					{
						//System.out.print("  " + opinion + ": _ ");
						for (String opinionToken :  opinion.getFeature().replaceAll("&", " & ").split(" "))
						{
							boolean found = false;
							for (TokenWithContext nextSentenceToken : sentence.getTokens())
							{
								if ((nextSentenceToken.getToken().equalsIgnoreCase(opinionToken) || nextSentenceToken.getLemma().equalsIgnoreCase(opinionToken)) && nextSentenceToken.getPartOfSentimentStructure() == PartOfSentimentStructure.FEATURE)
								{
									found = true;
									break;
								}
							}
							if (found)
							{
								sbFound.append(opinionToken).append(" ");
								numFound++;
								totalFound++;
							}
							else
							{
								sbNotFound.append(opinionToken).append(" ");
								numNotFound++;
								totalNotFound++;
							}
						}
					}
					
					//print out tagged sentence
					if (numNotFound > 0)
					{
						System.out.println(toStringWithTaggedFeatures(sentence));
	
						System.out.println("    Found     " + numFound + ":" + sbFound.toString());
						System.out.println("    Not found " + numNotFound + ":" + sbNotFound.toString());
						
						System.out.print("\n");
					}
				}
//				else
//					System.out.println("(Objective sentence)");
			}
			
			System.out.println("\nRough overall estimate (which doesn't include tagged adjectives): " + totalFound + " found / " + totalNotFound + " not found ");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String toStringWithTaggedFeatures(Sentence sentence)
	{
		StringBuilder sb = new StringBuilder();
		for (TokenWithContext token : sentence.getTokens())
		{
			boolean isFeature = (token.getPartOfSentimentStructure() == PartOfSentimentStructure.FEATURE);
			if (isFeature)
				sb.append("_");
			sb.append(token.getToken());
			if (isFeature)
				sb.append("_");
			sb.append(" ");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
}
