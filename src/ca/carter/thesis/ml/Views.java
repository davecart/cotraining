package ca.carter.thesis.ml;

public enum Views {
	LEXICAL,
	SYNTACTIC,
	BAGOFWORDS;
	
	public static Integer getNumberForView(Views view)
	{
		switch (view)
		{
		case LEXICAL:
			return 0;
		case SYNTACTIC:
			return 1;
		case BAGOFWORDS:
			return 2;
		default:
			return null;
		}
	}

	public static Views getViewForNumber(int number)
	{
		switch (number)
		{
		case 0:
			return LEXICAL;
		case 1:
			return SYNTACTIC;
		case 2:
			return BAGOFWORDS;
		default:
			return null;
		}
	}
}
