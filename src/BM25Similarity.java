
public class BM25Similarity {
	/*Implement the BM25 similarity measure, 
	 * assuming that no relevance information is available. 
	 * Set parameters b=0.75, k1=1.2, k2=100. 
	 * Run on the two collections you indexed in part 2, 
	 * retrieving 100 documents, and reporting 
	 * the MAP evaluation figures for your two retrievals.
	 */
	
	private double k1 = 1.2; // determines how the tf component of the term weight changes as fi increases. (if 0, then tf component is ignored.) Typical value for TREC is 1.2; so fi is very non-linear (similartotheuseoflogfintermwtsofthevectorspacemodel)--- after3or4 occurrences of a term, additional occurrences will have little impact.
	private double k2 = 100; // has a similar role for the query term weights. Typical values (see slide) make the equation less sensitive to k2 than k1 because query term frequencies are much lower and less variable than doc term frequencies.
	private double b = 0.75; // regulates the impact of length normalization. (0 means none; 1 is full normalization.)

	private double calculateK(double dl, double avdl){ 
		return k1*((1-b)+b*(dl/avdl));
	}
	
	public double computeBM25Similarity(query, document){
		double ri=0; // is the # of relevant documents containing term i (set to 0 if no relevancy info is known)
		double R=0; //is the number of relevant documents for this query – (set to 0 if no relevancy info is known)

//		double K = calculateK(k1,b,dl,avdl); //is more complicated. Its role is basically to normalize the tf component by document length.

		
		double N; //is the total # of docs in the collection

		
		
		double finalValue;
		for(String term in query){
			double ni; // is the # of docs containing term 
			double fi; //is the frequency of term i in the doc under consideration
			double qfi; // is the frequency of term i in the query

			double numFrac1 = (r+0.5)/(R-ri+0.5);
			double denFrac1 = (ni-ri+0.5)/(N-ni-R+ri+0.5);
			
			double numFrac2 = ((k1+1)*fi)/(K+fi);
			double denFrac2 = (K+fi);
			
			double numFrac3 = (k2+1)*qfi;
			double denFrac3 = k2+qfi;
			
			double val = (numFrac1/denFrac1)+(numFrac2/denFrac2)+(numFrac3/denFrac3);
			finalValue += Math.log(val);
		}
		return finalValue;
	
	}
	

