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
				
		double finalValue = 0;
		for(String term : queryMap.keySet()){
			//check if the term is in the document, else we just skip it
			Integer fiInteger = document.get(term);
			double fi= 0;
			if(fiInteger != null){
				fi = fiInteger.doubleValue();
				//calculate idf
				double ni = (double)db.numberOfDocumentsWithTerm(term);
				double qfi = queryMap.get(term).doubleValue();
				
				//Follow equation from lecture 12
						
				double firstpart = Math.log( ((ri+0.5)/(R-ri+0.5)) / ((ni-ri+0.5)/(N-ni-R+ri+0.5)) );
				double secondpart = ((k1+1)*fi)/(K+fi);
				double thirdpart = ((k2+1)/qfi)/(k2+qfi);
				
				finalValue += firstpart*secondpart*thirdpart;
			}
			 
		}
		return finalValue;
	}
}

