package ca.carter.thesis.model.phrasetree;




public enum PartOfSpeech implements Cloneable {
	
	//from http://bulba.sdsu.edu/jeanette/thesis/PennTags.html
	
	//clauses
	S,
	SBAR,
	SBARQ,
	SINV,
	SQ,
	
	//phrases
	ADJP, // - Adjective Phrase.
	ADVP, // - Adverb Phrase.
	CONJP, // - Conjunction Phrase.
	FRAG, // - Fragment.
	INTJ, // - Interjection. Corresponds approximately to the part-of-speech tag UH.
	LST, // - List marker. Includes surrounding punctuation.
	NAC, // - Not a Constituent; used to show the scope of certain prenominal modifiers within an NP.
	NP, // - Noun Phrase.
	NPTMP, // - weird temporal noun phrase
	NX, // - Used within certain complex NPs to mark the head of the NP. Corresponds very roughly to N-bar level but used quite differently.
	PP, // - Prepositional Phrase.
	PRN, // - Parenthetical.
	PRT, // - Particle. Category for words that should be tagged RP.
	QP, // - Quantifier Phrase (i.e. complex measure/amount phrase); used within NP.
	RRC, // - Reduced Relative Clause.
	UCP, // - Unlike Coordinated Phrase.
	VP, // - Verb Phrase.
	WHADJP, // - Wh-adjective Phrase. Adjectival phrase containing a wh-adverb, as in how hot.
	WHAVP, // - Wh-adverb Phrase. Introduces a clause with an NP gap. May be null (containing the 0 complementizer) or lexical, containing a wh-adverb such as how or why.
	WHNP, // - Wh-noun Phrase. Introduces a clause with an NP gap. May be null (containing the 0 complementizer) or lexical, containing some wh-word, e.g. who, which book, whose daughter, none of which, or how many leopards.
	WHPP, // - Wh-prepositional Phrase. Prepositional phrase containing a wh-noun phrase (such as of which or by whose authority) that either introduces a PP gap or is contained by a WHNP.
	X, // - Unknown, uncertain, or unbracketable. X is often used for bracketing typos and in bracketing the...the-constructions.
	XS, // - Unknown sentence? Seems to apply to "more than" or "less than" type constructs, i.e., ...and then finally after less than 60 days ,...

	WHADVP, //extra
	PUNCTCOLON,
	PUNCTCOMMA,
	PUNCTENDOFSENTENCE,
	PUNCTCURRENCY,
	PUNCTQUOTATIONMARK,
	PUNCTHASH,
	LRB,	// (
	RRB,	// )
	
	//from http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html

	CC ,
	CD ,
	DT ,
	EX ,
	FW ,
	IN ,
	JJ ,
	JJR ,
	JJS ,
	LS ,
	MD ,
	NN ,
	NNS ,
	NNP ,
	NNPS ,
	PDT ,
	POS ,
	PRP ,
	PRP$ ,
	RB ,
	RBR ,
	RBS ,
	RP ,
	SYM ,
	TO ,
	UH ,
	VB ,
	VBD ,
	VBG ,
	VBN ,
	VBP ,
	VBZ ,
	WDT ,
	WP ,
	WP$ ,
	WRB;
	
	//alternatively, this could probably be parsed out of edu.stanford.nlp.parser.lexparser.EnglishTreebankParserParams.sisterSplitters()
	
	public static PartOfSpeech fromString (String string) throws java.lang.IllegalArgumentException
	{
		if (string.equals(":"))
			return PartOfSpeech.PUNCTCOLON;
		else if (string.equals(","))
			return PartOfSpeech.PUNCTCOMMA;
		else if (string.equals("."))
			return PartOfSpeech.PUNCTENDOFSENTENCE;
		else if (string.equals("$"))
			return PartOfSpeech.PUNCTCURRENCY;
		else if (string.equals("-LRB-"))
			return PartOfSpeech.LRB;
		else if (string.equals("-RRB-"))
			return PartOfSpeech.RRB;
		else if (string.equals("NP-TMP"))
			//return PartOfSpeech.NP;	//TODO: this is an interesting case, as in "the p/n button switches your dvd players video output signal between pal and ntsc ."
			return PartOfSpeech.NPTMP;
		else if (string.equals("''") || string.equals("\"") || string.equals("``"))
			return PartOfSpeech.PUNCTQUOTATIONMARK;
		else if (string.equals("#"))
			return PartOfSpeech.PUNCTHASH;
		else
			//may throw IllegalArgumentException
			return PartOfSpeech.valueOf(string);
	}
}
