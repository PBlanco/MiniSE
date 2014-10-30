import java.util.HashMap;


public class BM25Similarity {
	/*Implement the BM25 similarity measure, 
	 * assuming that no relevance information is available. 
	 * Set parameters b=0.75, k1=1.2, k2=100. 
	 * Run on the two collections you indexed in part 2, 
	 * retrieving 100 documents, and reporting 
	 * the MAP evaluation figures for your two retrievals.
	 */
	
	private static double k1 = 1.2; // determines how the tf component of the term weight changes as fi increases. (if 0, then tf component is ignored.) Typical value for TREC is 1.2; so fi is very non-linear (similartotheuseoflogfintermwtsofthevectorspacemodel)--- after3or4 occurrences of a term, additional occurrences will have little impact.
	private static double k2 = 100; // has a similar role for the query term weights. Typical values (see slide) make the equation less sensitive to k2 than k1 because query term frequencies are much lower and less variable than doc term frequencies.
	private static double b = 0.75; // regulates the impact of length normalization. (0 means none; 1 is full normalization.)

	
	private static double calculateK(double dl, double avdl){ 
		return k1*((1-b)+b*(dl/avdl));
	}
	
	public static double computeBM25Similarity(HashMap<String,Integer> queryMap, String docName, MangoDB db){
		double ri=0; // is the # of relevant docs containing term i (set to 0 if no relevancy info is known)
		double R=0; //is the number of relevant docs for this query – (set to 0 if no relevancy info is known)

		double dl = db.getDocLengthForDocument(docName) ; //get doc length
		double avdl = db.getAvdl(); //get average doc length
		double K = calculateK(dl,avdl); //is more complicated. Its role is basically to normalize the tf component by document length.

		
		HashMap<String, Integer> document = db.tokenFrequenciesForDocument(docName);

		double N = db.documents().length; //is the total # of docs in the collection
		
		//Make a basic inverted index.... term->#docs ins
		HashMap<String, Integer> qToDFreqMap =new HashMap<String, Integer>();
		Object[] queryTerms = queryMap.keySet().toArray();
		for (Object doc : db.documents()){
			HashMap<String, Integer> docFreqMap = db.tokenFrequenciesForDocument(doc.toString());
			for(Object term : queryTerms){
				Integer termfInD = docFreqMap.get(term.toString());
				if(termfInD != null){
					int freq = qToDFreqMap.containsKey(term.toString()) ? qToDFreqMap.get(term.toString()) : 0;
					qToDFreqMap.put(term.toString(), freq + 1);
				}
			}
		}
		
		double finalValue = 0;
		for(String term : queryMap.keySet()){
			// get the # of docs containing term 			
			double ni=0; 
			Integer tmp = qToDFreqMap.get(term);
			if(tmp != null){
				ni = tmp.doubleValue();
			}
			
			//get the frequency of term i in the doc under consideration
			double fi = (document.get(term) != null)? document.get(term).doubleValue(): 0; 
			// get the frequency of term i in the query
			double qfi = (queryMap.get(term) != null)? queryMap.get(term).doubleValue(): 0; 

			
			double numFrac1 = (ri+0.5)/(R-ri+0.5);
			double denFrac1 = (ni-ri+0.5)/(N-ni-R+ri+0.5);
			
			double numFrac2 = ((k1+1)*fi)/(K+fi);
			double denFrac2 = (K+fi);
			
			double numFrac3 = (k2+1)*qfi;
			double denFrac3 = k2+qfi;
			
			double val = Math.log(numFrac1/denFrac1) +(numFrac2/denFrac2)+(numFrac3/denFrac3);
			finalValue += val;
		}
		return finalValue;
	}
}

