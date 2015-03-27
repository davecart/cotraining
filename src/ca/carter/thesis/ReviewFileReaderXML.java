package ca.carter.thesis;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import ca.carter.thesis.model.ProductFeatureOpinion;
import ca.carter.thesis.model.Sentence;
import ca.carter.thesis.model.SimpleSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

//mostly borrowed from http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/

public class ReviewFileReaderXML {

	static Queue<SimpleSentence> readReviewFile(File file, Integer limit) throws IOException
	{
		StanfordCoreNLP pipeline = Sentence.getDefaultPipeline();
		
		Queue<SimpleSentence> output = new LinkedList<SimpleSentence>();
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);

			//optional, but recommended
			//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			//System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

			NodeList nList = doc.getElementsByTagName("sentence");

			//System.out.println("----------------------------");

			final int listLength = nList.getLength();
			int sentenceNum = 0;
			
			for (int temp = 0; temp < Math.min(listLength, (limit == null ? listLength : limit)); temp++) {

				Node nNode = nList.item(temp);

				//System.out.println("\nCurrent Element :" + nNode.getNodeName());

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;

					String sentenceText = eElement.getElementsByTagName("text").item(0).getTextContent();
					
					SimpleSentence sentenceToReturn = new SimpleSentence(sentenceText, false);
					
					//System.out.println("ID : " + eElement.getAttribute("id"));
					
//					if (sentenceNum == 1521 || sentenceNum == 1522)
//						System.out.println("Sentence " + temp + ": " + sentenceText); //eElement.getElementsByTagName("text").item(0).getTextContent());
					sentenceNum++;
					
					if (eElement.getElementsByTagName("aspectTerms").getLength() == 0 )
					{
						//System.out.println("No aspects");
					}
					else
					{
						sentenceToReturn.setOpinions(new ArrayList<ProductFeatureOpinion>());
						
						
						NodeList aspectsNodeList =  ((Element) eElement.getElementsByTagName("aspectTerms").item(0) ).getElementsByTagName("aspectTerm");
						final int numberOfAspects = aspectsNodeList.getLength();
						
						//System.out.println(numberOfAspects + " aspects");
						
						for (int i = 0; i < numberOfAspects; i++)
						{
							Element aspectElement = (Element) aspectsNodeList.item(i);
	
							//System.out.println("     Aspect:   " + aspectElement.getAttribute("term"));
							//System.out.println("     Polarity: " + aspectElement.getAttribute("polarity"));
							//System.out.println("     From: " + aspectElement.getAttribute("from"));
							//System.out.println("     To: " + aspectElement.getAttribute("to"));
							
							String aspect = aspectElement.getAttribute("term");
							String polarity = aspectElement.getAttribute("polarity");
							int from = Integer.valueOf(aspectElement.getAttribute("from"));
							int to = Integer.valueOf(aspectElement.getAttribute("to"));
							
							if ("conflict".equals(polarity))
							{
								sentenceToReturn.getOpinions().add(new ProductFeatureOpinion(aspect, "positive", from, to , pipeline));
								sentenceToReturn.getOpinions().add(new ProductFeatureOpinion(aspect, "negative", from, to , pipeline));

							}
							else
							{
								sentenceToReturn.getOpinions().add(new ProductFeatureOpinion(aspect, polarity, from, to , pipeline));
							}
						}
						
					}
					
					output.add(sentenceToReturn);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return output;
	}
	
	public static void main(String[] args) {
		File xmlFile = new File("/Users/davecarter/Dropbox/Thesis data/Semeval-2014-task4/Restaurants_Train_v2.xml");

		
		try {
			Queue<SimpleSentence> sentences = readReviewFile(xmlFile, null);
			
			System.out.println("Parsed " + sentences.size() + " sentences.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
