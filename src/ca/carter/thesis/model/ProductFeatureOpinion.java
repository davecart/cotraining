package ca.carter.thesis.model;

import java.util.Properties;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class ProductFeatureOpinion {
	private String rawFeature;
	private String feature;
	private String lemmatizedFeature;
	private int sentimentValue;
	private ProductOpinionFeatureDetail detail;	//this is whether the feature is unlisted, etc.; values other than unlisted are probably not useful for my work
	//private List<String> paraphrases;	
	
	private Integer from = null;
	private Integer to = null;
	
	
	//for parsing features and listed polarities from Bing Liu data
	public ProductFeatureOpinion(String feature, StanfordCoreNLP pipeline) {
		super();

		this.rawFeature = feature;
		
		//t-mobile service[+2][u]
		
		String parts[] = feature.split("[\\[\\]{}]+");
		int partsLength = parts.length;

		this.feature = parts[0].trim();
		
		//this.paraphrases = WikipediaParaphraser.getParaphrases(this.feature, true);
		
		Annotation document = new Annotation(this.feature);
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		CoreMap sentenceFragment = document.get(SentencesAnnotation.class).get(0);

		
		
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (CoreLabel token: sentenceFragment.get(TokensAnnotation.class)) {
			String lemma = token.get(LemmaAnnotation.class);
			if (first)
				first = false;
			else
				sb.append(" ");
			sb.append(lemma);
		}
		this.lemmatizedFeature = sb.toString();
		
		if (parts.length == 1)
		{
			//this is triggered in a very small number of cases where a given feature is both good and bad in a sentence 
			//i.e.,				"look##this thing , while looking pretty cool , is not as sexy as the ipod .",
			sentimentValue = 0;
		}
		else
		{
			switch (parts[1].charAt(0))
			{
			case '+':
				if (parts[1].length() == 1)
					sentimentValue = 1;
				else
					sentimentValue = Integer.valueOf(parts[1].substring(1));
				break;
			case '-':
				sentimentValue = -1 * Integer.valueOf(parts[1].substring(1));
				break;
			default:
				sentimentValue = Integer.valueOf(parts[1].substring(0));
			}
		}
		
		if (partsLength > 2)
			detail = ProductOpinionFeatureDetail.byValue(parts[2]);
		if (partsLength > 3)
		{
			if ((parts[2].equalsIgnoreCase("p") && parts[3].equalsIgnoreCase("u")) || (parts[2].equalsIgnoreCase("u") && parts[3].equalsIgnoreCase("p")))
				//resolve redundancy in this case
				detail = ProductOpinionFeatureDetail.PRONOUN;
			else
				System.out.println("More than one product opinion feature detail, which is an unusual and/or conflicting occurrence: " + feature);
		}
		
	}
	
	//for feeding in aspects from XML where the polarity is already defined
	public ProductFeatureOpinion(String feature, String polarity, int from, int to, StanfordCoreNLP pipeline) {
		super();

		this.feature = feature;
		this.from = from;
		this.to = to;
		
		Annotation document = new Annotation(this.feature);
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		CoreMap sentenceFragment = document.get(SentencesAnnotation.class).get(0);

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (CoreLabel token: sentenceFragment.get(TokensAnnotation.class)) {
			String lemma = token.get(LemmaAnnotation.class);
			if (first)
				first = false;
			else
				sb.append(" ");
			sb.append(lemma);
		}
		this.lemmatizedFeature = sb.toString();
		
		if ("positive".equals(polarity))
			sentimentValue = 2;
		else if ("negative".equals(polarity))
			sentimentValue = -2;
		else if ("neutral".equals(polarity))
			sentimentValue = 0;
		else
		{
			System.out.println("Could not assign polarity " + polarity);
			sentimentValue = 0;
		}
	}
	public String getFeature() {
		return feature;
	}
	public void setFeature(String feature) {
		this.feature = feature;
	}
	public String getLemmatizedFeature() {
		return lemmatizedFeature;
	}
	public void setLemmatizedFeature(String lemmatizedFeature) {
		this.lemmatizedFeature = lemmatizedFeature;
	}
	public int getSentimentValue() {
		return sentimentValue;
	}
	public void setSentimentValue(int sentimentValue) {
		this.sentimentValue = sentimentValue;
	}
	public Sentiment getSentiment() {
		if (sentimentValue > 0)
			return Sentiment.POS;
		else if (sentimentValue < 0)
			return Sentiment.NEG;
		else
			return Sentiment.OBJ;
	}
	public ProductOpinionFeatureDetail getDetail() {
		return detail;
	}
	public void setDetail(ProductOpinionFeatureDetail detail) {
		this.detail = detail;
	}
	public String getRawFeature() {
		return rawFeature;
	}
	public static StanfordCoreNLP getDefaultPipeline()
	{
		//System.out.println("Getting default pipeline.");
		//TODO: stem it

		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");
		return new StanfordCoreNLP(props);
	}

	
	
	@Override
	public String toString() {
		return "ProductFeatureOpinion [feature=" + feature
				+ ", lemmatizedFeature=" + lemmatizedFeature + ", sentimentValue="
				+ sentimentValue + (detail != null ? ", detail=" + detail : ", no extra details") + "]";
	}
	public static void main(String[] args)
	{
		//String testOpinion = "t-mobile service[+2][u]";
		String testOpinion = "feature[+2}, ";
		//String testOpinion = "look";
		
		ProductFeatureOpinion test = new ProductFeatureOpinion(testOpinion, ProductFeatureOpinion.getDefaultPipeline());
		
		System.out.println(test.toString());
	}
}
