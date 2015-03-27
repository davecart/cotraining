package ca.carter.thesis;

import java.util.List;
import java.util.Queue;

import ca.carter.thesis.model.ProductFeatureOpinion;
import ca.carter.thesis.model.Sentence;
import ca.carter.thesis.model.SimpleSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class SentenceProcessorThread extends Thread
{
	private Queue<SimpleSentence> rawSentences;
	private List<Sentence> sentences;
	private String genericName;
	private String brandName;
	private int titleLineIgnored = 0;
	private int sentencesProcessed = 0;
	
	protected static Integer lock = 1;
	
	public SentenceProcessorThread(Queue<SimpleSentence> rawSentences, List<Sentence> sentences, String genericName, String brandName)
	{
		this.sentences = sentences;
		this.rawSentences = rawSentences;
		this.genericName = genericName;
		this.brandName = brandName;
	}
	
	public int getTitleLineIgnored() {
		return titleLineIgnored;
	}

	public int getSentencesProcessed() {
		return sentencesProcessed;
	}

	public void run() {
		
		try {
			StanfordCoreNLP pipeline = Sentence.getDefaultPipeline();
			StanfordCoreNLP featurePipeline = ProductFeatureOpinion.getDefaultPipeline();

			SimpleSentence nextLine = null;

			if (rawSentences == null || rawSentences.isEmpty())
			{
				System.err.println("Sentence list was null/empty.");
				return;
			}
			
			synchronized(lock) {
				nextLine = rawSentences.poll();
			}
			
			while (nextLine != null) {
				if (nextLine.isNeedsOpinionParsing() == true && nextLine.getSentence().startsWith("[t]"))
				{
					titleLineIgnored++;
					//TODO: this would be useful at some point; but for now, skipping titles
				}
				else
				{
					try
					{
						Sentence sentence = null;
						if (nextLine.isNeedsOpinionParsing() == true)
							sentence = new Sentence(nextLine, pipeline, featurePipeline, genericName, brandName, false);
						else
							sentence = new Sentence(nextLine, pipeline, featurePipeline, genericName, brandName, false);
							
						sentencesProcessed++;
						synchronized(sentences)
						{
							sentences.add(sentence);
						}
					}
					catch (Exception e)
					{
						System.err.println("Had trouble parsing " + nextLine);
						e.printStackTrace();
					}
				}
				synchronized(lock) {
					nextLine = rawSentences.poll();
				}

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Processed " + sentencesProcessed);
	}

}
