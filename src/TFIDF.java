import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public class TFIDF {
	public static int termFrequency(String term, String source) {
		String[] pieces = source.split("\\s+");
		int freq = 0;
		for (String piece : pieces)
			if (piece.equals(source))
				freq++;
		return freq;
	}
	// Doc should be stemmed and tokenized
	// Essentially should be same data as stored in Mango
	public static int termFrequency(String term, HashMap<String, Integer> doc) {
		return doc.get(term);
	}
	public static int maxTF(String str) {
		String[] pieces = str.split("\\s+");
		Set<String> uniquePieces = new HashSet<String>(Arrays.asList(pieces));
		int freq = 0;
		for (String piece : uniquePieces) {
			int tf = termFrequency(piece, str);
			if (tf > freq)
				freq = tf;
		}
		return freq;
	}
	// Doc should be stemmed and tokenized
		// Essentially should be same data as stored in Mango
	public static int maxTF(HashMap<String, Integer> doc) {
		int biggestTF = 0;
		Set<String> tokens = doc.keySet();
		for (String tok : tokens) {
			int tf = doc.get(tok);
			if (tf > biggestTF)
				biggestTF = tf;
		}
		return biggestTF;
	}
	
	public static double augmentedTF(String term, String doc) {
		double tf = (double)termFrequency(term, doc);
		double maxTF = (double)maxTF(doc);
		return 0.5 + 0.5 * (tf / maxTF);
	}
	public static double augmentedTF(String term, HashMap<String, Integer> doc) {
		double tf = (double)termFrequency(term, doc);
		double maxTF = (double)maxTF(doc);
		return 0.5 + 0.5 * (tf / maxTF);
	}
}
