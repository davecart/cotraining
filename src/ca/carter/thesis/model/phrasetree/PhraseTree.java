package ca.carter.thesis.model.phrasetree;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import ca.carter.thesis.languagemodels.DefaultTokenizer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.AnnotatedTree;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class PhraseTree extends AbstractPhraseTreePart {
	private List<AbstractPhraseTreePart> leaves;
	private List<List<PartOfSpeech>> flatLeaves;
	
    private static final LexicalizedParser lexParser  = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
	
    public PhraseTree()
    {
    	super();
    }
    
    public PhraseTree(String string)
    {
    	StanfordCoreNLP pipeline = ca.carter.thesis.model.Sentence.getDefaultPipeline();
		Annotation document = new Annotation(string);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		CoreMap sentence = sentences.get(0);
		Tree lexTree = sentence.get(TreeAnnotation.class);

    	this.pos = PartOfSpeech.valueOf(lexTree.firstChild().value());
    	this.leaves = organizeTree(lexTree.firstChild(), true);
    	this.flatLeaves = flatTree(lexTree.firstChild(), null, null, true);    	

		Tree sentiTree = sentence.get(SentimentCoreAnnotations.AnnotatedTree.class);
		sentiTree.pennPrint();
		
		//RNNCoreAnnotations.
		
		System.out.println("Lexi tree is :  " + lexTree);
		System.out.println("Senti tree is : " + sentiTree);
		

    }
   
    public PhraseTree(Tree lexTree, String pos)
    {
    	this.leaves = organizeTree(lexTree, true);

    	this.flatLeaves = flatTree(lexTree.firstChild(), null, null, true);

    	try
    	{
    		this.pos = PartOfSpeech.fromString(pos);
    	}
    	catch (java.lang.IllegalArgumentException e)
    	{
    		System.err.println("No part of speech for " + pos);
    	}
    }

    
	/*protected Tree getParseTreeForPhrase(String phrase)
	{
		StringTokenizer st = DefaultTokenizer.getDefaultTokenizer(phrase);
		int numTokens = st.countTokens();
		String[] tokens = new String[numTokens];
		for (int i = 0; i < numTokens; i++)
		{
			tokens[i] = st.nextToken();
		}
		
        List<CoreLabel> rawWords = Sentence.toCoreLabelList(tokens);
        Tree parseTree = lexParser.apply(rawWords);

        return parseTree;
	}*/
		
	private List<AbstractPhraseTreePart> organizeTree(Tree tree, boolean topLevel)
	{
		List<AbstractPhraseTreePart> leaves = new ArrayList<AbstractPhraseTreePart>();
		
		for (Tree child : tree.getChildrenAsList())
		{
			if (child.isPreTerminal())
			{
				PartOfSpeech pos = null;
				String childValue = child.value();
				try
				{
					pos = PartOfSpeech.fromString(childValue);
				}
				catch (java.lang.IllegalArgumentException e)
				{
					System.err.println("Could not get part of speech. Value of child is " + child.value() + " and its first child is " + child.firstChild().value());
					//System.err.println(tree.getChildrenAsList());
					e.printStackTrace();	
					throw(e); //only throw for debugging purposes; spits out full sentence if we do
				}
				leaves.add(new TokenLeaf(child.firstChild().value(), pos));

			}
			else
			{
				leaves.add(new PhraseTree(child, child.value()));
			}			
		}
		
		return leaves;
	}
	
	protected List<List<PartOfSpeech>> flatTree(Tree tree, List<List<PartOfSpeech>> list, Stack<PartOfSpeech> depthStack, boolean topLevel)
	{
		if (topLevel)
		{
			list = new ArrayList<List<PartOfSpeech>>();
			depthStack = new Stack<PartOfSpeech>();
		}
		
		for (Tree child : tree.getChildrenAsList())
		{
			if (child.isPreTerminal() || child.isLeaf())
			{
				list.add((Stack<PartOfSpeech>) depthStack.clone());
			}
			else
			{
				PartOfSpeech pos = null;
				String childValue = child.value();

				try
				{
					pos = PartOfSpeech.fromString(childValue);
				}
				catch (java.lang.IllegalArgumentException e)
				{
					//not all that important
					if (child.firstChild() != null)
					{
						System.err.println("Could not get part of speech. Value of child is " + childValue + " and its first child is " + (child.firstChild() == null ? "null" : child.firstChild().value()));
						throw(e); //only throw for debugging purposes; spits out full sentence if we do
					}
				}

				
				depthStack.push(pos);
				flatTree(child, list, depthStack, false);
				depthStack.pop();

				//leaves.add(new PhraseTree(child, child.value()));
			}			
		}
		
		if (topLevel)
			return list;
		else
			return null;
	}
    	
	public List<AbstractPhraseTreePart> getLeaves() {
		return leaves;
	}

	public void setLeaves(List<AbstractPhraseTreePart> leaves) {
		this.leaves = leaves;
	}

	@Override
	public String toString()
	{
		return toString(0);
	}

	public List<List<PartOfSpeech>> getFlatLeaves() {
		return flatLeaves;
	}

	public void setFlatLeaves(List<List<PartOfSpeech>> flatLeaves) {
		this.flatLeaves = flatLeaves;
	}

	@Override
	protected String toString(int indent)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(new String(new char[indent]).replace('\0', ' '));
		sb.append(pos).append("(\n");
		for (AbstractPhraseTreePart nextLeaf : leaves)
		{
			if (nextLeaf.isToken())
				sb.append(new String(new char[indent + 2]).replace('\0', ' ')).append(nextLeaf.toString()).append("\n");
			else
				sb.append(nextLeaf.toString(indent + 2)).append("\n");
		}
		sb.append(new String(new char[indent]).replace('\0', ' '));
		sb.append(")");
		return sb.toString();
	}
	
	
	public static void main(String[] args)
	{
		System.out.println("Starting");

		/*
		PhraseTree noPt = new PhraseTree();
        String[] sent = { "This", "is", "an", "easy", "sentence", "-", "in", "theory", ",", "so", "it", "goes", "eh", "?" };
        List<CoreLabel> rawWords = Sentence.toCoreLabelList(sent);
        Tree parse = PhraseTree.lexParser.apply(rawWords);
        parse.pennPrint();
        System.out.println();
        */

		
		PhraseTree[] phraseTrees =
			{
				//new PhraseTree("We intend to raise this violation of the Security Council resolution, if it goes forward, in the U.N.,"),
				//new PhraseTree("UN Security Council"),
				//new PhraseTree("From the loan that we took up, EUR 5 bln from EU and EUR 1 bln from the World Bank are intended for the Finance Ministry. Not any EUR intended for the Finance Ministry and not any EUR from the money intended for BNR can go to salaries or bonuses. This money is for Romania,"),
				//new PhraseTree("The line was really rather long."),
				//new PhraseTree("the voice quality is very good , and it gets great reception ( that is , in places where you get t-mobile coverage , which is not that good ; see below ) ."),
				//new PhraseTree("This is an easy phrase to parse - in theory , or so it goes , eh ?"),

				//testing negation
				//new PhraseTree("remote control are only so-so ; it doesn't show the complete filenames of mp3s with really long names ."),

				//testing negation
				new PhraseTree("the voice quality is poor, but not its reception"),
			};

		for (PhraseTree pt : phraseTrees)
		{
			System.out.println("toString: " + pt.toString());
			System.out.println("getFlatLeaves: " + pt.getFlatLeaves());
		}
		
		System.out.println("Done");

	}
	
	
	/*
	//System.out.println("-----------");
	System.out.print(subTree.pennString());
	System.out.println(
	subTree.isPhrasal() + " / " +
	subTree.isPrePreTerminal() + " / " +
	subTree.isPreTerminal() + " / " +
	//subTree.label() + " / " + "\n" +
	//subTree.labels() + " / " + "\n" +
	//subTree.value() + " / " +  "\n" + //actual word or part of speech, depending on node
	//subTree.getChildrenAsList() +
	""
	);
	*/
	
	/*
    public void demoAPI(LexicalizedParser lp) {
        // This option shows parsing a list of correctly tokenized words
        String[] sent = { "This", "is", "an", "easy", "sentence", "." };
        List<CoreLabel> rawWords = Sentence.toCoreLabelList(sent);
        Tree parse = lp.apply(rawWords);
        parse.pennPrint();
        System.out.println();


        // This option shows loading and using an explicit tokenizer
        String sent2 = "This is another sentence.";
        TokenizerFactory<CoreLabel> tokenizerFactory = 
          PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
        List<CoreLabel> rawWords2 = 
          tokenizerFactory.getTokenizer(new StringReader(sent2)).tokenize();
        parse = lp.apply(rawWords2);

        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
        GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
        List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
        System.out.println(tdl);
        System.out.println();

        TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
        tp.printTree(parse);
      }
      */
}
