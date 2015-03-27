package ca.carter.thesis;

import java.util.List;

import ca.carter.thesis.model.ProductFeatureOpinion;
import ca.carter.thesis.model.Sentence;

public class TestBingLiuReviewFeatureExtraction {

	public static final String defaultRootdir = "/Users/" + System.getProperty("user.name") + "/Dropbox/Thesis data";
	public static String rootdir;

	private static final Integer defaultNumberOfLinesFromFile = null; //null for entire file; practical minimum is 10, so as to have enough total words that the classifiers can do internal cross-validation to build confidence estimates
	
	private static final String[] filesToProcess = 
		{
			//defaultRootdir + "/bingliudata/Apex AD2600 Progressive-scan DVD player.txt",
			//defaultRootdir + "/bingliudata/Canon G3.txt",
			//defaultRootdir + "/bingliudata/Creative Labs Nomad Jukebox Zen Xtra 40GB.txt",
			defaultRootdir + "/bingliudata/Nikon coolpix 4300.txt",
			//defaultRootdir + "/bingliudata/Nokia 6610.txt",
		};
	

	public static void main(String[] args)
	{
		try
		{
	
			List<Sentence> sentences = ProcessReviews.getSentences(filesToProcess[0], defaultNumberOfLinesFromFile, null, null, null);
	
			for (Sentence sentence : sentences)
			{
				//System.out.print(sentence.getSentence());
				System.out.print(TestBingLiuSentenceTagging.toStringWithTaggedFeatures(sentence));
				
				
				if (sentence.getFeatureOpinions() != null)
				{
					for (ProductFeatureOpinion opinion : sentence.getFeatureOpinions())
					{
						System.out.print("[");
						System.out.print(opinion.getFeature());
						if (opinion.getSentimentValue() > 0)
							System.out.print("+");
						System.out.print(opinion.getSentimentValue());
						System.out.print("]");
					}
				}
				else
				{
					System.out.print("[no opinions were tagged]");
				}
				
				System.out.print("\n");
			}
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
