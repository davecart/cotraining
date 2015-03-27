package ca.carter.thesis.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import ca.carter.thesis.ProcessReviews;
import ca.carter.thesis.TestBingLiuSentenceTagging;
import ca.carter.thesis.WordNetResolver;
import ca.carter.thesis.model.phrasetree.PartOfSentimentStructure;
import ca.carter.thesis.model.phrasetree.PartOfSpeech;
import ca.carter.thesis.model.phrasetree.PhraseTree;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class Sentence {
	private String sentence;
	private List<ProductFeatureOpinion> featureOpinions;
	private List<TokenWithContext> tokens;
	private List<TokenWithContext> tokensInFeatureOpinions;
	private Tree tree;

	
	private static final int windowAroundToken = 3;

	private static boolean debugSentence = false;
	
	protected Sentence(String rawSentence)
	{
		this(new SimpleSentence(rawSentence, true), getDefaultPipeline(), ProductFeatureOpinion.getDefaultPipeline(), null, null, false);
	}

	public static StanfordCoreNLP getDefaultPipeline()
	{
		//System.out.println("Getting default pipeline.");
		
		Properties props = new Properties();
		//props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, truecase, parse, dcoref");
		return new StanfordCoreNLP(props);
	}
	
	//generic name refers to what the product is ("phone", "dvd player"), while the brand name is just that ("nokia", "canon")
	public Sentence(SimpleSentence rawSentence, StanfordCoreNLP pipeline, StanfordCoreNLP pipelineForFeatureOpinions, String genericName, String brandName, boolean isOnlyTaggedFeature)
	{
		//parse the sentence into a List of TokenWithContext
		if (isOnlyTaggedFeature)
		{
			this.sentence = rawSentence.getSentence();
			this.featureOpinions = null;
		}
		else
		{
			if (rawSentence.isNeedsOpinionParsing())
			{
				String[] featuresVsText = rawSentence.getSentence().split("##");
		
				if (featuresVsText.length > 1 && !featuresVsText[0].isEmpty())
				{
					featureOpinions = new ArrayList<ProductFeatureOpinion>();
					for (String nextFeature : featuresVsText[0].split(","))
					{
						if (!"".equals(nextFeature) && !" ".equals(nextFeature))
						{
							ProductFeatureOpinion featureOpinion = new ProductFeatureOpinion(nextFeature, pipelineForFeatureOpinions);
							
							if (!ProcessReviews.removeGenericAspects ||
									(
									(genericName == null || !featureOpinion.getFeature().equalsIgnoreCase(genericName)) && 
									(brandName == null || !featureOpinion.getFeature().equalsIgnoreCase(brandName)))
									)
							{
								featureOpinions.add(featureOpinion);
			
								Sentence featureOpinionAsSentence = new Sentence(new SimpleSentence(featureOpinion.getFeature(), false), pipeline, null, null, null, true);
								if (tokensInFeatureOpinions == null)
									tokensInFeatureOpinions = new ArrayList<TokenWithContext>();
								for (TokenWithContext nextToken : featureOpinionAsSentence.getTokens())
								{
									//System.out.println("xxx 97 " + nextToken.getToken());
									nextToken.setPartOfSentimentStructure(PartOfSentimentStructure.FEATURE);
								}
								tokensInFeatureOpinions.addAll(featureOpinionAsSentence.getTokens());
							}
						}
					}
					
					if (featureOpinions.isEmpty())
						featureOpinions = null;
					
					this.sentence = featuresVsText[1];
					
				}
				else if (featuresVsText.length > 1 && featuresVsText[0].isEmpty())
				{
					featureOpinions = null;
					this.sentence = featuresVsText[1];
				}
				else
				{
					featureOpinions = null;
					this.sentence = featuresVsText[0];
				}
			}
			else
			{
				this.sentence = rawSentence.getSentence();
				this.featureOpinions = rawSentence.getOpinions();
			}
			
			if (debugSentence)
				System.out.println("Sentence to be processed is " + this.sentence);
		}
		
		//perform a Stanford NLP pipeline to parse the sentence, etc.
		tokens = new ArrayList<TokenWithContext>();
		
		Annotation document = new Annotation(cleanRawSentence(this.sentence));
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		//using these both as a List<> and a Queue<>
		LinkedList<TokenWithContext> frontPartOfSentence = new LinkedList<TokenWithContext>();
		LinkedList<TokenWithContext> backPartOfSentence = new LinkedList<TokenWithContext>();

		//System.out.println("Number of sentences: " + sentences.size());
		
//		int offsetForPreviousSentences = 0;
		int tokenNumber = 0;
		
		for(CoreMap sentence: sentences) {
			List<TokenWithContext> subSentenceTokens = new ArrayList<TokenWithContext>();
			
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				String word = token.get(TextAnnotation.class);
				String pos = token.get(PartOfSpeechAnnotation.class);
				String ne = token.get(NamedEntityTagAnnotation.class);      
				String lemma = token.get(LemmaAnnotation.class);

				PartOfSpeech cleanedPOS = null;
				try {
					cleanedPOS = PartOfSpeech.fromString(pos);
				} catch (Exception e) {
					System.out.println("Unhandled POS: " + pos);
				}

				boolean isNamedEntity = ne.startsWith("ORG") || ne.startsWith("PERS") || ne.startsWith("LOC");
				//if (isNamedEntity)  //can be ORGANIZATION, NUMBER, DATE, DURATION, PERCENT, ORDINAL, MONEY, PERSON, LOCATION, etc.
				//	System.out.println(word + " is a named entity " + ne);

				//TODO: would be nice to do something with the other tagged entities; so as to remove them from consideration by the classifiers
				
				//fill in basic tokens
				TokenWithContext nextToken = new TokenWithContext(tokenNumber++, word, lemma, cleanedPOS, null, null, null, isNamedEntity);
				
				if (cleanedPOS == PartOfSpeech.JJ || cleanedPOS == PartOfSpeech.JJR || cleanedPOS == PartOfSpeech.JJS || 
						cleanedPOS == PartOfSpeech.RB  || cleanedPOS == PartOfSpeech.RBR || cleanedPOS == PartOfSpeech.RBS)
				{
					String attributeOf =  WordNetResolver.getAttributeForAdjective(word);
					if (attributeOf != null)
						nextToken.setAttribute(attributeOf);
				}
				
				subSentenceTokens.add(nextToken);
				backPartOfSentence.add(nextToken);
				
			}

			if (!isOnlyTaggedFeature)
			{
				// this is the parse tree of the current sentence
				this.tree = sentence.get(TreeAnnotation.class);
				PhraseTree phraseTree = new PhraseTree(tree, "S");
				
				//apply semantic lineage to tokens
				for (int i = 0; i < subSentenceTokens.size(); i++)
				{
					subSentenceTokens.get(i).setParentage(phraseTree.getFlatLeaves().get(i));
				}
				
				//try going through token-by-token (#3) and see what the incoming/outgoing edges look like
				
				// this is the Stanford dependency graph of the current sentence
				SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);

				if (debugSentence)
				{
					System.out.println("1\n" + dependencies.toString());
					//System.out.println("2\n" + dependencies.toList());
					System.out.println("3\n" + dependencies.vertexListSorted());
					//System.out.println("4\n" + dependencies.edgeListSorted());
				}
				
				for (IndexedWord nextSemanticToken : dependencies.vertexListSorted())
				{
					TokenWithContext tokenToUpdate = subSentenceTokens.get(nextSemanticToken.index() - 1);
					
					//deal with incoming i.e.,   the-DT: incoming: quality-NN -> the-DT det
					if (debugSentence)
						System.out.println(nextSemanticToken + " " + nextSemanticToken.index());
					List<SemanticGraphEdge> incomingEdges = dependencies.incomingEdgeList(nextSemanticToken);
					if (incomingEdges.isEmpty())
					{
						tokenToUpdate.setSemanticSpecificRole("root");
						tokenToUpdate.setSemanticGeneralRole("root");
					}
					else
					{
						//there should be exactly one incoming edge
						SemanticGraphEdge edge = incomingEdges.get(0);
						if (debugSentence)
							System.out.println("  incoming: " + edge.getSource() + " -> " + edge.getTarget() + " " + edge.getRelation() + " " + edge.getRelation().getParent()); // + " " + edge.getRelation().getShortName() + " " + edge.getRelation().getLongName() + " " + edge.getRelation().getSpecific());
						tokenToUpdate.setSemanticSpecificRole(edge.getRelation().getShortName());
						if (edge.getRelation().getParent() == null)
							tokenToUpdate.setSemanticGeneralRole(edge.getRelation().getShortName());
						else
							tokenToUpdate.setSemanticGeneralRole(edge.getRelation().getParent().getShortName());
						tokenToUpdate.setSemanticIncomingEdge(subSentenceTokens.get(edge.getSource().index() - 1));
					}
					
					//deal with outgoing i.e., good-JJ:   outgoing: good-JJ -> quality-NN nsubj,   outgoing: good-JJ -> is-VBZ cop,   outgoing: good-JJ -> very-RB advmod,   outgoing: good-JJ -> gets-VBZ conj_and
					List<SemanticGraphEdge> outgoingEdges = dependencies.outgoingEdgeList(nextSemanticToken);
					if (outgoingEdges != null && ! outgoingEdges.isEmpty())
					{
						List<SemanticallyTaggedTokenWithContext> outgoingTokens = new ArrayList<SemanticallyTaggedTokenWithContext>();
						for (SemanticGraphEdge edge : outgoingEdges)
						{
							if (debugSentence)
								System.out.println("  outgoing: " + edge.getSource() + " -> " + edge.getTarget() + " " + edge.getRelation() + " " + edge.getRelation().getShortName());
							
							outgoingTokens.add(new SemanticallyTaggedTokenWithContext(edge.getRelation().getShortName(), subSentenceTokens.get(edge.getTarget().index() - 1) ));
							
							//TODO: the first form of this "if" is preferable, but current version (3.3.1) of parser classifies all sorts of non-negative adverbial modifiers as negation class
							//if (edge.getRelation().isAncestor(EnglishGrammaticalRelations.NEGATION_MODIFIER) ) 
							String edgeTarget = edge.getTarget().toString();
							if ( edge.getRelation().getShortName().equals("neg") || edgeTarget.equalsIgnoreCase("nor-cc")
									|| edgeTarget.equalsIgnoreCase("could-MD") || edgeTarget.equalsIgnoreCase("should-MD") || edgeTarget.equalsIgnoreCase("would-MD") ) 
							{
								if (debugSentence)
									System.out.println("*****Found negation***** " + edge.getRelation().getClass() + " " + edge.getRelation().getParent()
											+ " " + edge.getRelation().getSpecific()
											+ " " + edge.getRelation().isAncestor(EnglishGrammaticalRelations.NEGATION_MODIFIER) 
									);
								tokenToUpdate.setSemanticOutgoingEdgesIncludeNegation(true);
							}
							tokenToUpdate.setSemanticallyTaggedTokensWithContext(outgoingTokens);
						}
					}
					
					//NOTE: case 1: it appears that recursively going up the "incoming" chain (maybe by only 2 levels) gets us the sentiment
					//quality nsubj -> good (+ good has outgoing "very" advmod)
					
					//case 2: more roundabout
					//reception dobj -> gets conj_and -> good XXXX (incorrect; it gets "great" reception)
					//great amod -> reception (OK!)
					//reception --> (outgoing) great amod
					
					//case 3: more roundabout still
					//coverage -> outgoing good (rcmod) -> outgoing not (neg)
					
					//offsetForPreviousSentences += sentence.get(TokensAnnotation.class).size();
				}
			}
			
			//System.out.println(dependencies.getChildList());
/*
			  public List<SemanticGraphEdge> getIncomingEdgesSorted(IndexedWord vertex) {
				    List<SemanticGraphEdge> edges = incomingEdgeList(vertex);
				    Collections.sort(edges);
				    return edges;
				  }

				  public List<SemanticGraphEdge> getOutEdgesSorted(IndexedWord vertex) {
				    List<SemanticGraphEdge> edges = outgoingEdgeList(vertex);
				    Collections.sort(edges);
				    return edges;
				  }
				  */

//			sentence.get(SentimentCoreAnnotations.AnnotatedTree.class) ;
			
			tokens.addAll(subSentenceTokens);
		}
		
		

		// This is the coreference link graph
		// Each chain stores a set of mentions that link to each other, along with a method for getting the most representative mention
		// Both sentence and token offsets start at 1!
		Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
		for (CorefChain nextChain : graph.values())
		{
			// Not obviously useful unless there is more than one clause in a chain, other than flagging NPs
			if (nextChain.getMentionsInTextualOrder().size() > 1)
			{
				//System.out.println("Size: " + nextChain.getMentionsInTextualOrder().size());
				//System.out.println(nextChain);
				
				for (CorefMention corefMention : nextChain.getMentionsInTextualOrder())
				{
					TokenWithContext tokenToTag = tokens.get(corefMention.headIndex - 1); //note that coreferences start at 1, while tokens start at 0
					
					String fullMentionSpan = nextChain.getRepresentativeMention().mentionSpan;
					int tokensInFullMention = fullMentionSpan.split(" ").length;
					
					//once in a while, the parser puts a huge chunk of the sentence in the mention, and we start tagging a bunch of weird coreferences
					//eg in "Straight-forward, no surprises, very decent Japanese food.", we end up tagging straight-forward and suprises as features based on the coreference
					//It seems unlikely that real corefernces should be more than 5-grams; and if they are, perhaps it's forgiveable to not link it back to the reference anyway
					if (tokensInFullMention <= 5)
					{
						tokenToTag.setFlatResolvedCoreference(fullMentionSpan);
						
						if (nextChain.getRepresentativeMention().equals(corefMention))
							tokenToTag.setCoreferenceHead(true);
					}
					/*
					System.out.println(corefMention.corefClusterID);
					System.out.println(corefMention.headIndex); //**** this is the word to tag
					System.out.println(corefMention.endIndex);
					System.out.println(corefMention.mentionID);
					System.out.println(corefMention.mentionSpan);
					System.out.println(corefMention.mentionType);
					System.out.println(corefMention.startIndex);
					System.out.println(corefMention.number);
					System.out.println(corefMention.position);
					System.out.println(corefMention.animacy);
					*/
				}
			}
		}
				
		//update tokens with three-word window on either side
		LinkedList<TokenWithContext> windowBehind = new LinkedList<TokenWithContext>();
		for (int i = 0; i < windowAroundToken; i++)
		{	
			frontPartOfSentence.add(null);
			backPartOfSentence.add(null);

			/*TokenWithContext nextWindowToken = backPartOfSentence.poll();
			System.out.println(nextWindowToken);
			windowBehind.add(nextWindowToken);*/

			windowBehind.add(backPartOfSentence.poll());
		}

		for (TokenWithContext nextToken : tokens)
		{
			windowBehind.pop();
			windowBehind.add(backPartOfSentence.pop());

			nextToken.setPreviousTokens( localLimitedCloneOfNeighbours(frontPartOfSentence));
			nextToken.setNextTokens( localLimitedCloneOfNeighbours(windowBehind));			
			
			frontPartOfSentence.add(nextToken);
			frontPartOfSentence.pop();
		}

		//try to tag the words in the sentence that refer to product features 
		//FOUR CASES:
		//  1. mentioned explicitly (easy)
		//  2. found in the coreference information (i.e., feature "voice quality" may appear as "it" with coreference "*the* voice quality")
		//  3. may be an unlisted feature, in which case maybe coreference is the only way to go
		//  4. may appear hyphenated (i.e., tagged feature is "auto focus" and appears in sentence as "auto-focus")
		
		//TODO: case 3 not yet handled
		
		//TODO: tag sentiment-bearing words?
		
		if (featureOpinions != null)
		{
			final int tokensSize = tokens.size();
			
			for (ProductFeatureOpinion nextFeature : featureOpinions)
			{	
				boolean found = false;

				String[] tokensInFeature = nextFeature.getFeature().split(" ");
				String[] lemmasInFeature = nextFeature.getLemmatizedFeature().split(" ");
				//System.out.println(flattenStringArray(tokensInFeature) + " / " + flattenStringArray(lemmasInFeature));
				int tokensInFeatureLength = tokensInFeature.length;
				int lemmasInFeatureLength = lemmasInFeature.length;

				while (!found)
				{
	
					boolean canUseHyphenatedFeature = (tokensInFeatureLength == 2);
					
					List<String> hyphenatedPermutations = null; 
					if (canUseHyphenatedFeature)
					{
						hyphenatedPermutations = new ArrayList<String>();
						hyphenatedPermutations.add(tokensInFeature[0] + "-" + tokensInFeature[1]);
						hyphenatedPermutations.add(tokensInFeature[0] + "-" + lemmasInFeature[1]);
						hyphenatedPermutations.add(lemmasInFeature[0] + "-" + tokensInFeature[1]);
						hyphenatedPermutations.add(lemmasInFeature[0] + "-" + lemmasInFeature[1]);
					}
						
					for (int i = 0; i < tokensSize; i++)
					{
						if (tokensInFeatureLength == 1)
						{
							if (compareTokenAndFeature(tokens.get(i), nextFeature))
							{
								//System.out.println("xxx 417 " + tokens.get(i).getToken());
								tokens.get(i).setPartOfSentimentStructure(PartOfSentimentStructure.FEATURE);
								found = true;
							}
						}
						if (canUseHyphenatedFeature && (hyphenatedPermutations.contains(tokens.get(i).getToken()) || hyphenatedPermutations.contains(tokens.get(i).getLemma()) )  ) 
						{
							//takes case of CASE 4
							//System.out.println("xxx 425 " + tokens.get(i).getToken());
							tokens.get(i).setPartOfSentimentStructure(PartOfSentimentStructure.FEATURE);
							found = true;
						}
						else
						{						
							int numMatchingSequentialTokens = 0;
							for (int j = 0; j < lemmasInFeatureLength; j++)
							{
								if (i + j >= tokensSize) 
									break;

								if (lemmasInFeatureLength > tokensInFeatureLength)
								{
									
									//System.out.println(tokens.get(i + j).getLemma() + " vs " + lemmasInFeature[j]);
									if (//tokens.get(i + j).getToken().equalsIgnoreCase(tokensInFeature[j]) ||
										//	tokens.get(i + j).getLemma().equalsIgnoreCase(tokensInFeature[j]) || 
										//	tokens.get(i + j).getToken().equalsIgnoreCase(lemmasInFeature[j]) ||
											tokens.get(i + j).getLemma().equalsIgnoreCase(lemmasInFeature[j])
											)	
										numMatchingSequentialTokens++;
									else
										break;
								}
								else
								{
									//System.out.println(tokens.get(i + j).getToken() + "/" + tokens.get(i + j).getLemma() + " vs " + tokensInFeature[j] + "/" + lemmasInFeature[j]);
									if (tokens.get(i + j).getToken().equalsIgnoreCase(tokensInFeature[j]) ||
											tokens.get(i + j).getLemma().equalsIgnoreCase(tokensInFeature[j]) || 
											tokens.get(i + j).getToken().equalsIgnoreCase(lemmasInFeature[j]) ||
											tokens.get(i + j).getLemma().equalsIgnoreCase(lemmasInFeature[j])
											)	
										numMatchingSequentialTokens++;
									else
										break;
								}
							}
			
							//System.out.println("  " + numMatchingSequentialTokens + " vs " + tokensInFeatureLength);
							//taking care of case 1
							if (numMatchingSequentialTokens == lemmasInFeatureLength)
							{
								//we have a match; tag 'em all
								for (int j = 0; j < lemmasInFeatureLength; j++)
								{
									//System.out.println("xxx 471 " + tokens.get(i+j).getToken());
									tokens.get(i+j).setPartOfSentimentStructure(PartOfSentimentStructure.FEATURE);
									tokens.get(i+j).setOpinion(nextFeature);
								}
								i = i + lemmasInFeatureLength;
								found = true;
							}
							//taking care of case 2
							else if (tokens.get(i).getFlatResolvedCoreference() != null && tokens.get(i).getFlatResolvedCoreference().contains(nextFeature.getFeature()))
							{
								//System.out.println("xxx 481 " + tokens.get(i).getToken());
								tokens.get(i).setPartOfSentimentStructure(PartOfSentimentStructure.FEATURE);
								tokens.get(i).setOpinion(nextFeature);
								found = true;
							}
						}
					}
					
					if (!found && tokensInFeatureLength > 1)
					{
						String poppedToken = tokensInFeature[0];
						String poppedLemma = lemmasInFeature[0];
						
						//there are cases where "quality of the pictures" tagged as -> "picture quality", "sprint customer service" -> "customer service", "4300" -> "nikon 4300", "easy to use" -> "ease of use", "images taken indoor" -> "indoor image"
						//where we should at least try to tag the last smaller ngram	
						//so, chop the first word off the aspect and carry on
						//not idea for cases like "at&t customer service" where lemmatized version is longer than tokenized version
						//System.out.println("Shrinking " + flattenStringArray(tokensInFeature) + "; size is " + tokensInFeatureLength);
						//System.out.println("Shrinking " + flattenStringArray(lemmasInFeature) + "; size is " + lemmasInFeatureLength);
						tokensInFeature = Arrays.copyOfRange(tokensInFeature, 1, tokensInFeatureLength);
						lemmasInFeature = Arrays.copyOfRange(lemmasInFeature, 1, lemmasInFeatureLength);
						//System.out.println(flattenStringArray(tokensInFeature) + " / " + flattenStringArray(lemmasInFeature));
						tokensInFeatureLength--;
						lemmasInFeatureLength--;
						
						//if the first (or subsequent) word in the feature appears exactly once in the sentence, let's tag it.
						int tokenAppeared = 0;
						TokenWithContext singleton = null;
						for (TokenWithContext nextToken  : tokens)
						{
							if (nextToken.getToken().equalsIgnoreCase(poppedToken) || nextToken.getLemma().equalsIgnoreCase(poppedLemma))
							{
								tokenAppeared++;
								singleton = nextToken;
							}
						}
						if (tokenAppeared == 1)
						{
							singleton.setPartOfSentimentStructure(PartOfSentimentStructure.FEATURE);
						}
						
						//TODO: if the chopped off word appears exactly once in the sentence, tag it anyway (as in "_auto_ , _manual_ , and the very helpful '' scene '' _mode_ ")
					}
					else if (!found)
					{
						break; //end the while loop
					}
						
					
				}
				
				
				if (!found)
				{
					//System.out.println("Falling through for " + nextFeature.getFeature() + ". Checking adjectives.");
					
					for (TokenWithContext token : tokens)
					{
						if (token.getPos() == PartOfSpeech.JJ && token.getAttribute() != null && token.getAttribute().equalsIgnoreCase( nextFeature.getFeature() ))
						{
							//String attributeOf =  WordNetResolver.getAttributeForAdjective(token.getToken());
							//
							//if (attributeOf != null)
							//{
							//	//System.out.println("Found " + attributeOf + " for adjective " + token.getToken() + " while considering " + nextFeature.getFeature());
							//	
							//	if (attributeOf.equalsIgnoreCase( nextFeature.getFeature() ))
							//	{
									//System.out.println("Tagging " + token.getToken() + " for feature " + nextFeature.getFeature());
									token.setPartOfSentimentStructure(PartOfSentimentStructure.FEATURE);
									break;
							//	}
							//}
						}
					}
				}
				
				//Overall: 246 found / 29 not found 


			}
		}

	}

	//clones the list of neighbours and strips the neighbours' neighbour information so that we don't get recursive lists, which make serialization difficult
	private LinkedList<TokenWithContext> localLimitedCloneOfNeighbours(LinkedList<TokenWithContext> list)
	{
		LinkedList<TokenWithContext> newList = new LinkedList<TokenWithContext>();
		
		for (TokenWithContext nextToken : list)
		{
			try
			{
				if (nextToken == null)
					newList.add(null);
				else
				{
					TokenWithContext newToken = (TokenWithContext) nextToken.clone();
					newToken.setNextTokens(null);
					newToken.setPreviousTokens(null);
					newList.add(newToken);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		return newList;
	}
	
	private boolean compareTokenAndFeature(TokenWithContext token, ProductFeatureOpinion feature)
	{
		if (compareTextAndFeature(token.getToken(), feature))
			return true;
		if (compareTextAndFeature(token.getLemma(), feature))
			return true;
		
		if (token.getToken().contains("-"))
		{
			if (compareTextAndFeature(token.getToken().replaceAll("[-]", ""), feature))
				return true;
			if (compareTextAndFeature(token.getLemma().replaceAll("[-]", ""), feature))
				return true;
		}
		
		return false;
	}
	
	private boolean compareTextAndFeature(String tokenText, ProductFeatureOpinion feature)
	{
		if (tokenText.equalsIgnoreCase(feature.getFeature()))
			return true;
		if (tokenText.equalsIgnoreCase(feature.getLemmatizedFeature()))
			return true;
		return false;
	}

	
	private String flattenStringArray(String[] array)
	{
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		for (String nextElement : array)
		{
			if (first)
				first = false;
			else
				sb.append(" ");
			sb.append(nextElement);
		}
		return sb.toString();
	}
	
	public String getSentence() {
		return sentence;
	}
	public void setSentence(String sentence) {
		this.sentence = sentence;
	}
	public List<ProductFeatureOpinion> getFeatureOpinions() {
		return featureOpinions;
	}
	/*public void setFeatureOpinions(List<ProductFeatureOpinion> featureOpinions) {
		this.featureOpinions = featureOpinions;
	}*/
	public List<TokenWithContext> getTokens() {
		return tokens;
	}
	public void setTokens(List<TokenWithContext> tokens) {
		this.tokens = tokens;
	}
	public List<TokenWithContext> peekTokensInFeatureOpinions() {
		return tokensInFeatureOpinions;
	}
	public void setTokensInFeatureOpinions(List<TokenWithContext> tokensInFeatureOpinions) {
		this.tokensInFeatureOpinions = tokensInFeatureOpinions;
	}
	public Tree getTree() {
		return tree;
	}
	public void setTree(Tree tree) {
		this.tree = tree;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Sentence [sentence=").append(sentence).append("\n");
		sb.append("  featureOpinions=").append(featureOpinions).append("\n");
		sb.append("  tokens=[\n");
		for (TokenWithContext token : tokens)
		{
			sb.append("    ").append(token).append("\n");
		}
		sb.append("  ]]");
		
		return sb.toString();
	}
	private String cleanRawSentence(String sentence)
	{
		//remove initial numerals used in bullets
		while (sentence.substring(0, 1).matches("[0-9.-]"))
			sentence = sentence.substring(1);
		
		return sentence.trim();
	}
	
	public static void main(String[] args)
	{
		System.out.println("Starting single sentences.");
		
		debugSentence = true;
		
		String[] rawSentences = {
				//"t-mobile service[+2][u]##all my questions or problems have been quickly and satisfactorily resolved . " ,
				
				
				
				//note: coreference resolution decides "it" is the voice quality, which is incorrect
				//"voice quality[+3],reception[+2]##the voice quality is very good , and it gets great reception ( that is , in places where you get t-mobile coverage , which is not that good ; see below ) .",
				//"gprs[-1],t-zone[+2]##the gprs connection is sometimes slow , and writing instant messages with the included aol instant messenger software is a pain , but the other t-zones applications are quite useful ." 	,
				//"##no more annoying series of beeps on AT&T.",
				//"##the p/n button switches your dvd players video output signal between pal and ntsc .", //good test case for NP-TMP phrasal structure
				//"finish[+2]##update : the finish is more \" mirror \" than silver -- and i like it !", //good test case for quotation marks and double hyphen 
				
				//tests hash
				//"player[-2][p]##they play just about everything , but # 2 and # 3 died very shortly after getting them .",
				
				//tests java.lang.IllegalArgumentException: No enum const class ca.carter.thesis.model.phrasetree.PartOfSpeech.XS
				//"play[-2], dvd[-2]##- not playing some dvds and then finally after less than 60 days , it just would not recognize anything i pop in it .",
				
				//tests java.lang.IllegalArgumentException: No enum const class ca.carter.thesis.model.phrasetree.PartOfSpeech.XS
				//"picture[-2], player[-3][p]##it is more than 90 days and it does not show the picture no matter what i do .",

				//tests java.lang.IndexOutOfBoundsException: Index: 9, Size: 9 errors (seems to be multiple sentences on one line)
				//"format[+2][u],progressive scan[+2]##this dvd player plays raw mpeg2 and mpeg1 videos , jpeg , wma , cdr / cdrw / dvdr / dvdrw , and of course plays dvd movies from uk. i love the aiff , progressive scan feature .",
				//"player[+3], format[+2]##i am very pleased with the apex ad2600.it plays just about anything you put in the drawer .",
				//"##having said that , this is what you do . \" add to cart \" ! ",
				//"freeze[-2], player[-2]##1 ) frame freezes and the family yells , \" dad ! somethings wrong with the dvd player ! come quick ! \"",
				//"dvd[-3], read[-2]##unfortunately , the player would not read any of my region one dvds. even though they were not damaged in any way .",

				//tests for empty extra features in list
				//"feature[+2], ##the camera has a wonderful set of features .",

				//tests for opening curly quote, latex style: java.lang.IllegalArgumentException: No enum const class ca.carter.thesis.model.phrasetree.PartOfSpeech.``
				//"##for `` cool factor '' buffs it looks and feels like a small plastic brick brick and does n't have any wow factor .",
				
				//tests for }{ in feature list
				//"option[+1}, control[+1]##canon have packed a lot in here and the options and controls are easy to use and logically laid out .",

				//tests for named entity, similar to previous
				//"option[+1}, control[+1]##nikon have packed a lot in here and the options and controls are easy to use and logically laid out .",
				
				//tests for lack of number in brackets
				//"player[+], price[+3]##the creative labs zen xtra has all the features the i-pod has and if you get if from amazon your only going to pay $ 300 for this great player .",

				//java.lang.ArrayIndexOutOfBoundsException: 1
				//"look##this thing , while looking pretty cool , is not as sexy as the ipod .",
				
				//"feature##one highly beneficial feature of this phone ( at least to me ) is that it can be used anywhere in the world except a few countries that do not use gsm , a few in asia .",

				//"size[+2][u], look{+1]##first let me say that it is much smaller than it looks on the web and it also looks better .",

				//"auto focus[+2], look{+1]##the auto-focus is good .",
				
				//"lcd[-1]##the only things i have found that i havent liked is that the _lcd_ is hard to read in daylight but everyone elses is too .",
				
				//"nikon 4300[+3]##the 4300 is a very durable , compact package , and i find nikon to be a brand that i can trust . ",

				// trying to be able to rebuild multi-word features
				
				/*
				"picture quality[+1]##qualities of the picture",

				"picture quality[+1]##quality of pictures",

				"picture quality[+1]##picture quality",

				"picture quality[+1]##picture quality .",
				*/

				//should be able to pull out "plan[s]" and "customer service"
				//"sprint plan[-2],sprint customer service[-3]##after years with that carrier 's expensive plans and horrible customer service , portability seemed heaven-sent ." ,
				
				//case where dailing gets lemmatized to dial in the sentence but not in the feature itself
				//"voice dialing[-2]##unfortunately , the 6610 does not offer voice dialing like my previous phone , but the other features it packs outweighs this shortcoming . ",

				//tests when the term is hyphenated in the sentence
				//"auto focus[+2],scene mode[+2]##the auto-focus performs well , but i love having the 12 optional scene modes - they are dummy-proof , and correspond to many situations in which i would actually seek to use the camera . ",

				//"scene mode[+2]##the \" scene \" mode works well for the remainder of shots that are not going to be in a \" regular \" setting . ",
				//"txt file[+2]##i particularly like the \" txt \" file which records all the control information for all the pictures you take.",
				
				//"optics" has some problems
				//"design[+2],construction[+2],optic[+2]##the design and construction are excellent -- as is the legendary quality of the nikon optics .",
				
				//"4mp" has the space in the sentence
				//"4mp[+2],optical zoom[+2]##4 mp gives you room for the future gaining experience ; cost offsets over time from wanting more in a camera , 3x optical and the fact that it carries nikon 's quality reputation behind it make the whole package prove itself worthwile at the price .",
				
				//"Pierre thought it was a little heavy.",
				
				//testing sentiment matching
				//"screen[+2],sound[+2]##great screen and great sound .",
				
				//testing ear-piece/earpiece tagging
				//"earpiece[+2]##it is just a tad small to hold to your ear with your shoulder , but that is solved with the very comfortable handsfree ear-piece which is included .",
				
				
				//SIMPLE SENTENCES LIKE THIS AREN'T RECONCILED...
				
				//"earpiece[+3]##the included earpiece is very comfortable and easy to use . ",
						
				//testing sentiment
				//"weight[+2],signal[+2]##it is very light weight and has a good signal strength .",
				
				//testing sentiment negation
				//"voice[-2]##when talking the voice is not very clear . ",
				
				//"gprs[-1],t-zone[+2]##the gprs connection is sometimes slow , and writing instant messages with the included aol instant messenger software is a pain , but the other t-zones applications are quite .",
				//Failed isCorrect() for ReconciledFeatureOpinion [tokensWithFeature=[gprs], tokensWithSentiment=[useful], opinion=ProductFeatureOpinion [feature=gprs, lemmatizedFeature=gpr, sentimentValue=-1, no extra details], sentiment=[POS]]
				//N0%    the [ correctFEATURE gprs ] [ falsepositiveFEATURE connection ] is sometimes [ falsenegativeSENTIMENT slow ] , and writing instant messages with the included aol instant messenger software is a [ falsenegativeSENTIMENT pain ] , but the other [ falsenegativeFEATURE t-zones ] applications are quite [ correctSENTIMENT useful ] . [gprs-1][t-zone+2]

				/* testing basic negation
				"bread[+2]##The bread is top notch . ",

				"bread[+2]##The bread is top notch as well . ",

				"bread[-2]##The bread is not top notch as well . ",

				"bread[-2]##The bread isn't not top notch as well . ",

				"bread[-2]##Nor is the bread top notch . ",
				*
				*/
				
				//testing matching of _design_ to _sleek_ and colour _screen_ to _good_
				//"design[+2],screen[+2]##the design is sleek and the color screen has good resolution . ",
				
				//should tag "it" correctly and note "heavy" as an adjective and a feature 
				//"the ipod was ok, though it was a little shiny",
				
				//testing to see that "feature" gets tagged properly
				//"feature[+2], ##the car 's features are wonderful .",
				//"feature[+2], ##the camera has a wonderful set of features .",

				//testing initial numeral
				//"price[+2]##10 great price for all the features . ",
				
				//testing filtering out both brand name and generic product descriptor
				//"camera[+3],nokia[+1]##the nokia camera is great .",
				
				//tests finding "auto" and such when separated by commas in a list
				//"auto mode[+2], manual mode[+2], scene mode[+2]##the possibilities with auto , manual , and the very helpful \" scene \" mode , which offers 11 optimized situational settings like portrait , landscape , beach / snow , sunset etc. , are endless . ",
				
				//tests single-word opinion features
				//"infrared[+2]##the infrared is a blessing if you have a previous nokia and want to transfer your old phone book to this phone , saved me hours of re-entering my numbers .", 

				//tests multi-word opinion features
				//"at&t customer service[-2]##after several years of torture in the hands of at&t customer service i am delighted to drop them , and look forward to august 2004 when i will convert our other 3 family-phones from at&t to t-mobile !",

				//"signal quality[+3]##i have had the phone for 1 week , the signal quality has been great in the detroit area ( suburbs ) and in my recent road trip between detroit and northern kentucky ( cincinnati ) i experienced perfect signal and reception along i-75 , far superior to at &#38; t 's which does not work along several long stretches on that same route .",

				//"waiter[-2]##Our waiter was horrible ; so rude and disinterested .",
				
				//"food[+3],service[-2]##Great food but the service was dreadful .",
				
				//"waiter[+2],staff[-2]##Our waiter was friendly and it is a shame that he didnt have a supportive staff to work with .",
				//"waiter[+2],staff[-2]##Our waiter was friendly and it is a shame that he didn't have a supportive staff to work with .",
				
				//"service[-1]##The service should have been better.",

				//"service[-1]##The service should have been fast.",
				
				//"desserts[+2],creme brulee[+2],apple tart[+2]##Montparnasse 's desserts -- especially the silken creme brulee and paper-thin apple tart -- are good enough on their own to make the restaurant worth the trip . ",
				
				"Japanese food[+2]##Straight-forward, no surprises, very decent Japanese food.",
		};

		StanfordCoreNLP sentencePipeline = getDefaultPipeline();
		
		for (String nextRawSentence : rawSentences)
		{
			Sentence sentence = new Sentence(new SimpleSentence(nextRawSentence, true), sentencePipeline, sentencePipeline, "camera", "nokia", false); //keeping in mind that product feature opinion pipeline is a subset of the regular sentence pipeline, so for a small number of cases we can be speedier by reusing the longer pipeline rather than (slowly) instantiating a new one
			System.out.println(sentence.toString());
			System.out.println(TestBingLiuSentenceTagging.toStringWithTaggedFeatures(sentence));
		}
		
		System.out.println("Done.");

	}

}
