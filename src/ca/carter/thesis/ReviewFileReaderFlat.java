package ca.carter.thesis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import ca.carter.thesis.model.SimpleSentence;

public class ReviewFileReaderFlat {

	static Queue<SimpleSentence> readReviewFile(File file, Integer limit) throws IOException
	{
		Queue<SimpleSentence> output = new LinkedList<SimpleSentence>();
		
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		int lineNum = 0;
		while ((line = br.readLine()) != null) {
			if (line.isEmpty() || line.charAt(0) == '*')
			{
				//do nothing
			}
			else
			{
				output.add(new SimpleSentence(line, true));
				
				if (limit != null && lineNum++ >= limit)
					break;
			}
		}
		br.close();
		
		return output;
	}
	
}
