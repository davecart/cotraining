package ca.carter.thesis.ml;

public enum FeatureDistance {
	SELF	( 0),
	BEFORE	(-1),	//also, above
	AFTER	( 1),
	
	//for neighbours only
	PLUSONE		(1),
	PLUSTWO 	(2),
	PLUSTHREE 	(3),
	
	//for neighbours and parental lineage
	MINUSONE	(-1),
	MINUSTWO	(-2),
	MINUSTHREE	(-3),
	
	//for parental lineage only
	MINUSFOUR	(-4), 
	MINUSFIVE	(-5),
	MINUSSIX	(-6),
	MINUSSEVEN	(-7),
	MINUSEIGHT	(-8),
	MINUSNINE	(-9),
	MINUSTEN	(-10),
	MINUSMORE	(-11);

	private final int numericInterpretation;
	
	FeatureDistance(int numericInterpretation)
	{
		this.numericInterpretation = numericInterpretation;
	}
	public int getNumericInterpretation()
	{
		return this.numericInterpretation;
	}
	public boolean canBeGeneralized()
	{
		switch(this)
		{
		case MINUSONE:
		case MINUSTWO:
		case MINUSTHREE:
		case MINUSFOUR:
		case MINUSFIVE:
		case MINUSSIX:
		case MINUSMORE:
		case PLUSONE:
		case PLUSTWO:
		case PLUSTHREE:
			return true;
		default:
			return false;
		}
	}
	
	public FeatureDistance getGeneralCase()
	{
		if (this.numericInterpretation > 0)
			return AFTER;
		else if (this.numericInterpretation < 0)
			return BEFORE;
		else
			return SELF;
	}
	
	public static FeatureDistance byDistance(int distance)
	{
		//hard coded for performance; if more needed, change to an immutable map + lookup
		switch(distance)
		{
		case 0:
			return SELF;
		case 1:
			return PLUSONE;
		case 2:
			return PLUSTWO;
		case 3:
			return PLUSTHREE;
		case -1:
			return MINUSONE;
		case -2:
			return MINUSTWO;
		case -3:
			return MINUSTHREE;
		case -4:
			return MINUSFOUR;
		case -5:
			return MINUSFIVE;
		case -6:
			return MINUSSIX;
		case -7:
			return MINUSSEVEN;
		case -8:
			return MINUSEIGHT;
		case -9:
			return MINUSNINE;
		case -10:
			return MINUSTEN;
		default:
			if (distance < -10)
				return MINUSMORE;
			else
				return null;
		}
	}
}
