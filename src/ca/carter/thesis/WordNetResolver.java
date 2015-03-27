package ca.carter.thesis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.Pointer;
import net.didion.jwnl.data.PointerType;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.Word;
import net.didion.jwnl.dictionary.Dictionary;

public class WordNetResolver {

	private static final String jwnlRoot ="/Users/" + System.getProperty("user.name") + "/Dropbox/Thesis work/workspace/process-reviews/jwnl14_file_properties.xml";

	private static boolean initialized = false;
	private static Dictionary dict;
	
	static 
	{
		
		try {
			JWNL.initialize(new FileInputStream(new File(jwnlRoot)));
			
			dict = Dictionary.getInstance();
			
			initialized = true;

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JWNLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
		
	}
	
	public static synchronized String getAttributeForAdjective(String adj)
	{
		try
		{
			if (!initialized)
			{
				while (!initialized)
				{
					Thread.sleep(1000);
				}
			}
			IndexWord indexWord = null;
			try
			{
				indexWord = dict.lookupIndexWord(POS.ADJECTIVE, adj);
			}
			catch (java.util.NoSuchElementException e)
			{
				System.out.println("Could not find adjective " + adj);
				e.printStackTrace();
			}
	
				
			if (indexWord == null)
				return null;
			
			Synset firstSense = indexWord.getSense(1); //numbering starts at 1, not 0
	
			if (firstSense == null)
				return null;
			
			Pointer[] attributePointers = firstSense.getPointers(PointerType.ATTRIBUTE); //should be length 0 or 1 in most cases
	
			if (attributePointers == null || attributePointers.length == 0)
			{
				if (indexWord.getSenseCount() == 1)
				{
					//check for stuff like "low-cost", "low-priced" in synset when it is a fairly well-defined word
					for (Word word: firstSense.getWords())
					{
						if (word.getLemma().startsWith("low-") || word.getLemma().startsWith("high-"))
						{
							String tentativeValue = word.getLemma().substring(word.getLemma().indexOf("-") + 1);
							
							if (tentativeValue.endsWith("d"))
							{
								//if it's a past participle we want to return "price", not "priced"
								IndexWord coreVerb = dict.lookupIndexWord(POS.VERB, tentativeValue);
								if (coreVerb != null)
								{
									Synset firstVerbSense = coreVerb.getSense(1); //numbering starts at 1, not 0
									if (firstVerbSense != null)
									{
										String tentativeReplacementVerb = firstVerbSense.getWords()[0].getLemma();
										if (tentativeReplacementVerb.substring(0, 2).equalsIgnoreCase(tentativeValue.substring(0, 2)))
											return tentativeReplacementVerb;
									}
								}

							}

							return tentativeValue;
						}
					}
					
				}

				//fall through
				return null;
			}
			
			Synset attributeSynset = attributePointers[0].getTargetSynset();
			
			if (attributeSynset == null || attributeSynset.getWordsSize() == 0)
				return null;
			
			return attributeSynset.getWords()[0].getLemma();
		}
		catch (Exception e)
		{
			System.out.println("Could not look up " + adj);
			e.printStackTrace();
			return null;
		}
		
	}
	
	
	public static boolean isFeatureNearlySynonymous(String word1, String word2)
	{
		try
		{
			IndexWord indexWord = dict.lookupIndexWord(POS.NOUN, word1);
			
			if (indexWord == null)
				return false;	//TODO: check the reverse?
			
			for (Synset nextSense : indexWord.getSenses())
			{
				for (Word nextWord : nextSense.getWords())
				{
					if (nextWord.getLemma().equalsIgnoreCase(word2))
						return true;
				}
			}
			
			return false;
		}
		catch (Exception e)
		{
			System.out.println("Could not look up " + word1 + " and " + word2);
			e.printStackTrace();
			return false;
		}

	}
	
	
	public static void main(String[] args)
	{
		try {
			String[] testWords = {
					
					"small",  //size
					"large",
					"loud",
					"bright",
					"wide",
					"full",
					"empty",
					"light", 	//weight
					"easy",  	//ease
					"big",    	//size
					"compact", 	//size
					"useful",	//none
					"affordable", //price    **doesn't have attribute, but other words in synset are "low-cost" and "low-priced"
					"pricey",
					"heavy",	//weight
					"beautiful",	//beauty
					"cold",
					"razor-sharp",
					"wicked",
					"fast",
					
			};
			
			for (String testWord : testWords)
			{
				System.out.println(testWord + " --> " + WordNetResolver.getAttributeForAdjective(testWord));
			}
			
			System.out.println(WordNetResolver.isFeatureNearlySynonymous("cost", "price"));
			System.out.println(WordNetResolver.isFeatureNearlySynonymous("price", "cost"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}


/*
 * Examples that might be correctable with a better heuristic:
* Found temperature for adjective hot while considering heat
* Found volume for adjective loud while considering sound
* Found comfort for adjective comfortable while considering earbud
Found beauty for adjective ugly while considering style
Found difficulty for adjective difficult while considering software
Found comfort for adjective comfortable while considering earbud
Found attractiveness for adjective attractive while considering design
Found comfort for adjective comfortable while considering earpiece
Found clarity for adjective clear while considering sound quality


*/
