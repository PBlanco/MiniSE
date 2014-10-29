import java.util.HashMap;
import java.util.Set;


public class MangoDB {
	private HashMap<String, HashMap<String, Integer>> mango;
	
	private double totalDocsLength=0;
	
	//getter for average document length
	public double getAvdl(){
		return (double) totalDocsLength/ documents().length; 
	}
	
	public MangoDB() {
		mango = new HashMap<String, HashMap<String, Integer>>();
	}
	
	public Integer frequencyForToken(String token) {
		int freq = 0;
		Set<String> documents = mango.keySet();
		for (String s : documents) {
			HashMap<String, Integer> tokens = mango.get(s);
			Integer count = tokens.get(token);
			freq += count;
		}
		return freq;
	}
	
	// Uses strings to uniquely represent documents.
	// In this case, string will be filename of document.
	public String[] documents() {
		Set<String> keySet = mango.keySet();
		String[] keyArray = (String[])keySet.toArray();
		return keyArray;
	}
	
	public Integer frequencyForTokenInDocument(String docName, String token) {
		HashMap<String, Integer> document = mango.get(docName);
		if (document == null)
			return null;
		return document.get(token);
	}
	
	public HashMap<String, Integer> tokenFrequenciesForDocument(String docName) {
		return mango.get(docName);
	}
	
	public boolean setTokenFrequenciesForDocument(String docName, HashMap<String, Integer> freqs, double docLength) {
		this.totalDocsLength += docLength;
		Object r = mango.put(docName, freqs);
		if (r == null)
			return false;
		return true;
	}
	
	
	public boolean setFrequencyForTokenInDocument(String docName, String token, Integer freq) {
		HashMap<String, Integer> document = mango.get(docName);
		if (document == null)
			return false;
		Object r = document.put(token, freq);
		if (r == null)
			return false;
		return true;
	}
	
	//Gets document length (dl) for passed in docname
	public double getDocLengthForDocument(String docName){
		HashMap<String, Integer> document = mango.get(docName);
		if (document == null)
			return 0;
		
		Set<String> terms = document.keySet();
		Integer docLength = 0;
		for (String s : terms) {
			docLength += document.get(s);
		}
		return docLength.doubleValue(); 
	}
	
}
