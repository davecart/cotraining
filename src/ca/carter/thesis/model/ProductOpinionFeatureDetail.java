package ca.carter.thesis.model;

public enum ProductOpinionFeatureDetail {

	UNLISTED, 				// ("u"),
	PRONOUN,  				//("p"),
	SUGGESTION, 			//("s"),
	COMPARISONCONTRAST, 	//("cc"),
	COMPARISONSAMEBRAND ;	//("cs");
	
	/*
	  [u] : feature not appeared in the sentence.
	  [p] : feature not appeared in the sentence. Pronoun resolution is needed.
	  [s] : suggestion or recommendation.
	  [cc]: comparison with a competing product from a different brand.
	  [cs]: comparison with a competing product from the same brand.
	  */
	
	//private final String abbrev;
	//private ProductOpinionFeatureDetail(String abbrev) {
    //    this.abbrev = abbrev;
    //}

	public static ProductOpinionFeatureDetail byValue(String abbrev)
	{
		if (abbrev == null | abbrev.isEmpty())
			return null;
		
		switch (abbrev.charAt(0))
		{
			case 'u':
				return UNLISTED;
			case 'p':
				return PRONOUN;
			case 's':
				return SUGGESTION;
			case 'c':
				switch (abbrev.charAt(1))
				{
				case 'c':
					return COMPARISONCONTRAST;
				case 's':
					return COMPARISONSAMEBRAND;
				default:
					return null;
				}
			default:
				return null;
		}
	}
	
}
