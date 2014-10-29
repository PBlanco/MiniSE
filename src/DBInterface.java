import java.util.HashMap;


public interface DBInterface {
	public Integer frequencyForToken(String token);
	
	// Uses strings to uniquely represent documents.
	// In this case, string will be filename of document.
	public String[] documents();
	public Integer frequencyForTokenInDocument(String docName, String token);
	public HashMap<String, Integer> tokenFrequenciesForDocument(String docName);
	public void setTokenFrequenciesForDocument(String docName, HashMap<String, Integer> freqs);
	public void setFrequencyForTokenInDocument(String docName, String token, Integer freq);
}
