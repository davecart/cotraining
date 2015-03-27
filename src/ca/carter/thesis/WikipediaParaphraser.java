package ca.carter.thesis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WikipediaParaphraser {

	//caching requests to be polite to the wikipedia folks
	private static final Map<String, List<String>> cache = Collections.synchronizedMap( new HashMap<String, List<String>>() );
	private static final boolean debug = false;
	
	public static List<String> getParaphrases(String string, boolean printDebug)
	{
		try
		{
			List<String> cachedCopy = cache.get(string);
			if (cachedCopy != null)
			{
				if (debug)
					System.out.println("Cache hit pos.");
				return cachedCopy;
			}
			else if (cache.containsKey(string))
			{
				if (debug)
					System.out.println("Cache hit neg.");
				return null;
			}
			else
			{
				if (debug)
					System.out.println("Cache miss.");
			}
			
			//String jsonFromWikipedia = 
			//		"{\"query\":{\"normalized\":[{\"from\":\"picture quality\",\"to\":\"Picture quality\"}],\"pages\":{\"38253269\":{\"pageid\":38253269,\"ns\":0,\"title\":\"Picture quality\",\"revisions\":[{\"contentformat\":\"text/x-wiki\",\"contentmodel\":\"wikitext\",\"*\":\"#redirect [[image quality]]\"}]}}}}";
					//"{\"query\":{\"normalized\":[{\"from\":\"scroll button\",\"to\":\"Scroll button\"}],\"pages\":{\"-1\":{\"ns\":0,\"title\":\"Scroll button\",\"missing\":\"\"}}}}"
			
			String jsonFromWikipedia = getTextFromURL("http://en.wikipedia.org/w/api.php?format=json&action=query&titles=" + URLEncoder.encode(string, "UTF-8")  + "&prop=revisions&rvprop=content");
			
			//http://en.wikipedia.org/w/api.php?format=json&action=query&titles=picture%20quality&prop=revisions&rvprop=content
	
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(jsonFromWikipedia);
			
			try
			{
				JsonNode nameNode = rootNode.get("query").get("pages").elements().next().get("revisions").get(0).get("*");
				String nodeTitle = nameNode.asText().toLowerCase();
				
				if (nodeTitle.startsWith("#redirect"))
				{
					List<String> results = new ArrayList<String>();
					if (debug)
						System.out.println(nodeTitle);
					String trimmedNodeTitle = nodeTitle.substring(nodeTitle.indexOf("[[") + 2, nodeTitle.indexOf("]]")).replace('_', ' ').trim();
					if (trimmedNodeTitle.endsWith("(disambiguation)"))
						trimmedNodeTitle = trimmedNodeTitle.substring(0, trimmedNodeTitle.indexOf("(disambiguation)")).trim();
						
					String[] splitBySection = trimmedNodeTitle.split("#");
					for (String nextSection : splitBySection)
					{
						if (!nextSection.equalsIgnoreCase(string))
							results.add(nextSection);
					}
					if (!results.isEmpty())
					{
						if (printDebug)
							System.out.println("Possible paraphrase: " + string + " ==> " + serializeList(results));

						cache.put(string, results);
						return results;
					}
					
				}

			}
			catch (Exception e)
			{
				//do nothing; just a failure
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		cache.put(string, null);
		return null;
	}
	
	//almost verbatim from http://stackoverflow.com/questions/1485708/how-do-i-do-a-http-get-in-java
	private static String getTextFromURL(String urlToRead) {
	      URL url;
	      HttpURLConnection conn;
	      BufferedReader rd;
	      String line;
	      String result = "";
	      try {
	         url = new URL(urlToRead);
	         conn = (HttpURLConnection) url.openConnection();
	         conn.setRequestProperty("User-Agent", "CarterThesis/1.0 (Macintosh; U; Intel Mac OS X 10.9; en-CA; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
	         conn.setRequestMethod("GET");
	         rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	         while ((line = rd.readLine()) != null) {
	            result += line;
	         }
	         rd.close();
	      } catch (Exception e) {
	         e.printStackTrace();
	      }
	      return result;
	   }
	
	public static String serializeList(List<String> list)
	{
		if (list == null)
			return null;
		
		StringBuilder sb = new StringBuilder();
		
		boolean first = true;
		for (String nextParaphrase : list)
		{
			if (!first)
				sb.append(", ");
			first = false;
			sb.append(nextParaphrase);
		}
		
		return sb.toString();
	}
	
	public static void main(String[] args)
	{
		String[] testPhrases = {
				"picture quality",
				"set up",
				"rechargable battery", // ==> Rechargeable_battery
				"auto focus", // ==> Autofocus
				"picture quality", // ==> image quality
				"movie", // ==> film
				"spot metering", // ==> metering mode#spot metering
				"dvd player", //should have none, but wikipedia will tend to correct the capitalization
				"video format",
				"lens cap", // ==> lens cover
				"lense", // ==> lens
				"photo", // ==> photograph
				"white balance", // ==> color balance
				"uploading"
		};
		
		for (String nextTestPhrase : testPhrases)
		{
			System.out.print(nextTestPhrase);
			System.out.print(" -> ");
			List<String> paraphrases = getParaphrases(nextTestPhrase, false);
			if (paraphrases != null)
			{	
				System.out.print(serializeList(paraphrases));
			}
			System.out.print("\n");
			
		}
	}
}
