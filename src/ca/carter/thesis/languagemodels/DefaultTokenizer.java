package ca.carter.thesis.languagemodels;

import java.util.StringTokenizer;

public class DefaultTokenizer {
	public static StringTokenizer getDefaultTokenizer(String text)
	{
		return new StringTokenizer(text," \t\n\r\f:,'\"");
	}

}
