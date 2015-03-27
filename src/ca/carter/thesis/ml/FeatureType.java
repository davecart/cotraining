package ca.carter.thesis.ml;

public enum FeatureType {
	
	/* The purpose of this class is to identify specific types of features for use in a general-purpose feature store.
	
    /* TokenWithContext
     * [token=first, lemma=first,
     * pos=RB,
     * previousTokens=[null,null,null], nextTokens=[let,me,say],
     * parentage=[ADVP], localParentage=[ADVP],
     * isNamedEntity=true, isCoreferenceHead=true,
     * flatResolvedCoreference=first,
     * partOfSentimentStructure=null, --> what we're trying to classify
     * semanticSpecificRole=advmod, semanticGeneralRole=mod,
     * semanticIncomingEdge=let-VB-root, //this is just a TokenWithContext
     * semanticOutgoingEdgesIncludeNegation=false, semanticallyTaggedTokensWithContext=null]
     */

	TOKEN,
	LEMMA,
	POS,
	TOKENNEIGHBOUR,
	LEMMANEIGHBOUR,
	POSNEIGHBOUR,
	PARENTAGE,
	LOCALPARENTAGE,
	BOOLEAN, //isNamedEntity, isCoreferenceHead, semanticOutgoingEdgesIncludeNegation
	
	RESOLVEDCOREFERENCE, //TODO: need to somehow incorporate the properties the resolved coreference
	
	SEMANTICSPECIFICROLE,
	SEMANTICGENERALROLE,
	
	SEMANTICINCOMINGEDGEROLE,
	SEMANTICINCOMINGEDGETOKEN,
	SEMANTICINCOMINGEDGELEMMA,
	SEMANTICINCOMINGEDGEPOS,
	
	SEMANTICOUTGOINGEDGEROLE,
	SEMANTICOUTGOINGEDGETOKEN,
	SEMANTICOUTGOINGEDGELEMMA,
	SEMANTICOUTGOINGEDGEPOS

}
