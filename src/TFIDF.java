import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TFIDF {
  public static String[] pieces(String original) {
    return original.split("\\s+");
  }
  public static int termFrequency(String term, String source) {
    String[] pieces = pieces(source);
    int freq = 0;
    for (String piece : pieces)
      if (piece.equals(source))
        freq++;
    return freq;
  }
  // Doc should be stemmed and tokenized
  // Essentially should be same data as stored in Mango
  public static int termFrequency(String term, HashMap<String, Integer> doc) {
    return doc.containsKey(term) ? doc.get(term) : 0;
  }
  public static int maxTF(String str) {
    String[] pieces = pieces(str);
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
  
  //TODO: Abstract to make this faster
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

  public static double idf(String term, MangoDB database) {
    double N = (double)database.documentCount();
    double n = (double)database.numberOfDocumentsWithTerm(term);
    double log = Math.log((N / n));
    return log;
  }
  
  public static double at(String term, HashMap<String, Integer> doc, MangoDB collection) {
    double tf = augmentedTF(term, doc);
    // https://piazza.com/class/hz0gtmi8y6v6eo?cid=193
    double idf = idf(term, collection);
    return tf * idf;
  }

  public static double smartIDF(String term, MangoDB database, HashMap<String,Integer> invertedIndex) {
	    double n = invertedIndex.containsKey(term) ? invertedIndex.get(term).doubleValue() : 0;
	    if (n == 0)
	    	return 0;
	    return Math.log10((double)database.documentCount()) - (double)Math.log10(n);
  }

  public static ArrayList<Double> computeNorm(ArrayList<Double> list){
	//get query norm weight
	  double norm = 0;
	  for (double weight : list){
		  norm += Math.pow(weight, 2);
	  }
	  norm = (double)Math.sqrt(norm);
	  
	  ArrayList<Double> normalizedList = new ArrayList<Double>();
	  for (double weight : list){ 
		  normalizedList.add(weight/norm);
	  }
	  
	  return normalizedList;
  }
  

  
  public static double computeatcatc(HashMap<String, Integer> query, String docName, MangoDB db, HashMap<String,Integer>invertedIndex){
	  /*
	   * first triplet atn gives the term weighting of the document vector
	   * second triplet atn gives the weighting in the query vector
	   * a -> tf component of the weighting
	   * t -> df component of the weighting
	   * c -> form of normalization used
	   */
	  
	  HashMap<String, Integer> document = db.tokenFrequenciesForDocument(docName);
	  double sum = 0.0;
	    
	  /*
	   * Loop through all terms in query
	   * 	if term in document:
	   * 		compute augmented tf for query
	   * 		computer t (idf) for query
	   */
	  
	  ArrayList<Double> queryWeights = new ArrayList<Double>();
	  ArrayList<Double> documentWeights = new ArrayList<Double>();
	  
	  HashMap<String, Integer> allTerms = new HashMap<String, Integer>(query);
	  allTerms.putAll(document);
	  
	  for (String term : allTerms.keySet()) {
		  //calculate query tf*idf
		  double idf = smartIDF(term, db, invertedIndex);
		  double qATN;
		  if (query.get(term) == null) {
			  qATN = 0.0 * idf;
			  queryWeights.add(qATN);
		  }
		  else {
			  double qtf = augmentedTF(term, query);
			  qATN = qtf * idf;
			  queryWeights.add(qATN);
		  }
		  if (document.get(term) == null){	
			  documentWeights.add((0.0 * idf));
		  } else {
			  double dtf = augmentedTF(term, document);
			  double dATN = dtf * idf;
			  documentWeights.add(dATN);

		  }
	  }
	  
	  //compute normalized weighted vectors
	  ArrayList<Double> normalizedQueryWeights = computeNorm(queryWeights);
	  ArrayList<Double> normalizedDocumentWeights = computeNorm(documentWeights);
	  
	  //take dot products of Query and Document Vector
	  for (int i = 0; i < normalizedQueryWeights.size(); i++)
		  sum += (normalizedQueryWeights.get(i).doubleValue() * normalizedDocumentWeights.get(i).doubleValue());
	  return sum;
  }

  
  // ================== NEW STUFF ===========================
  
  public static HashMap<String,Double> normalize(HashMap<String,Double> document){
		//get query norm weight
		  double norm = 0;
		  
		  Set<String> terms = document.keySet();
		  
		  for (String term : terms){
			  norm += Math.pow(document.get(term), 2);
		  }
		  norm = (double)Math.sqrt(norm);
		  
		  for (String term : terms){ 
			  double weight = document.get(term);
			  document.put(term, (weight/norm));
		  }
		  
		  return document;
	  }
  
  public static HashMap<String, Integer> invertIndex(MangoDB index){
	//Create inverted index
//	  System.out.println("Creating inverted index");
	  HashMap<String, Integer> invertedIndex = new HashMap<String, Integer>();
	  for(Object docName : index.documents()){
		  HashMap<String, Integer>document = index.get(String.valueOf(docName));
		  for(String term : document.keySet()){
			  if(invertedIndex.containsKey(term) ){
				  invertedIndex.put(term, (invertedIndex.get(term) +1)); 
			  } else {
				  invertedIndex.put(term, 1);
			  }
		  }
	  }
	  return invertedIndex;
  }
  public static WeightedIndex computeatcWeights(MangoDB docIndex){
	//Create inverted index
//	  System.out.println("Creating inverted index");
	  HashMap<String, Integer> invertedIndex = invertIndex(docIndex);
	  
//	  System.out.println("Computing weights");
	  //created weighted hashmap with double value
	  WeightedIndex atcDocIndex = new WeightedIndex();

	  //iterate through all docs
	  for(Object docname : docIndex.documents()){
		  //get doc and create new weighted doc with double
		  HashMap<String, Integer> document = docIndex.get((String) docname);
		  HashMap<String, Double> weightedDocument = new HashMap<String, Double>();
		  
		  //iterate through all terms to calculate weights
		  for(String term : document.keySet()){
			  double idf = smartIDF(term, docIndex, invertedIndex);
			  double dtf = augmentedTF(term, document);
			  weightedDocument.put(term, dtf * idf);
		  }
		  
		  //add weight to the atc doc index
		  ;
		  atcDocIndex.put((String)docname, normalize(weightedDocument));
	  }
//	  System.out.println("Done computing weights");

	  return atcDocIndex;
  }
  
  public static WeightedIndex computeQueryAtcWeights(MangoDB queryIndex, MangoDB docIndex){
	  HashMap<String, Integer> invertedIndex = invertIndex(docIndex);
	  
	  WeightedIndex atcQueryIndex = new WeightedIndex();
	  //iterate through all queries
	  for(Object name : queryIndex.documents()){
		  //get query and create new weighted query with double
		  HashMap<String, Integer> query = queryIndex.get((String) name);
		  HashMap<String, Double> weightedQuery = new HashMap<String, Double>();
		  
		  //iterate through all terms to calculate weights
		  for(String term : query.keySet()){
			  double idf = smartIDF(term, docIndex, invertedIndex);
			  double qtf = augmentedTF(term, query);
			  weightedQuery.put(term, qtf * idf);
		  }
		  
		  //add weight to the atc query index
		  atcQueryIndex.put((String)name, normalize(weightedQuery));
	  }
//	  System.out.println("Done computing weights");

	  return atcQueryIndex;  
  }
  
}

class WeightedIndex extends HashMap<String, HashMap<String, Double>>{}

class Roccio {

	//Creates list of documents sorted by atc.atc scores between a query, and all documents
	public static ArrayList<ReturnDoc> computeDocumentRanks(HashMap<String, Double> queryWM, WeightedIndex documentsWIndex){
		
		ArrayList<ReturnDoc> queryResults = new ArrayList<ReturnDoc>();
		for (String docname : documentsWIndex.keySet()){
			HashMap<String, Double> dWeights = documentsWIndex.get(docname);
			double score = DotProduct.dotProduct(queryWM, dWeights);	
			ReturnDoc doc = new ReturnDoc(docname.substring(0, docname.length() - 4), score);
			queryResults.add(doc);
		}
		
		Collections.sort(queryResults, new CustomComparator());
		return queryResults;
	}
	
	
	public static void computeRocchio(MangoDB queryIndex, MangoDB docIndex, Map<Integer, HashSet<String>>queryAnswers, int numRelevantDocs, int a, int b, int k){
		WeightedIndex queryATCIndex = TFIDF.computeQueryAtcWeights(queryIndex, docIndex);
		WeightedIndex docATCIndex = TFIDF.computeatcWeights(docIndex);
		
		double totalMAP = 0;
		double totalRoccioMAP = 0;
		for(String queryName : queryATCIndex.keySet()){
			HashMap<String, Double> query = queryATCIndex.get(queryName);
			
			//Get query key for answers
			Integer answersKey = Integer.parseInt(queryName.toString());
			//calculate MAP
			ArrayList<ReturnDoc> queryResults = computeDocumentRanks(query, docATCIndex);
			double map = EvaluateQueries.meanAverageprecision(queryAnswers.get(answersKey), queryResults);
//			System.out.println("Query "+query.toString()+": "+ EvaluateQueries.printDocs(queryResults, 7));
//			System.out.printf("atc.atc MAP for query "+query.toString() + " is: %1$.2f\n", map);
			totalMAP += map;
			
			//Create weighted index for top x docs & vocabulary using top x documents
			WeightedIndex topDocsIndex = new WeightedIndex();
			Set<String> vocabulary = new HashSet<String>();
			
			vocabulary.addAll(query.keySet());//add query to vocabulary
			
			int i = 0;
			for(ReturnDoc doc : queryResults){
				String docname = doc.getName() + ".txt";
				topDocsIndex.put(docname, docATCIndex.get(docname));
				vocabulary.addAll(docATCIndex.get(docname).keySet()); // create vocabulary
		
				i++;
				if (i == numRelevantDocs) //break if you've reached the set document amount
					break;
			}
			
			//compute the roccio terms 
			HashMap<String, Double> roccioTermWs = new HashMap<String, Double>();
			for(String term : vocabulary){
				double qatc = query.containsKey(term) ? query.get(term) : 0;
				double sumRel = 0;
				for(String name : topDocsIndex.keySet()){
					HashMap<String, Double> document = topDocsIndex.get(name);
					sumRel += document.containsKey(term) ? document.get(term) : 0;
				}
				roccioTermWs.put(term, a*qatc + (b/numRelevantDocs)*sumRel);
			}
			
			
			//Convert Hashmap to Arraylist to sort
			ArrayList<ReturnDoc> termScoreList = new ArrayList<ReturnDoc>();
			for(String term : roccioTermWs.keySet()){
				termScoreList.add(new ReturnDoc(term, roccioTermWs.get(term)));
			}
			//sort list
			Collections.sort(termScoreList, new CustomComparator());
			
			//update query weights
			HashMap<String, Double> roccioWeightedQuery = new HashMap<String, Double>();
			for(String term : query.keySet()){
				roccioWeightedQuery.put(term, roccioTermWs.get(term));
			}
			
			//Choose K highest weighted terms (not in query) to add to query
			int numTerms = k;
			for (ReturnDoc termObj : termScoreList){
				if(numTerms == 0) //break since we've added all the terms we need to the query
					break;			
				if(!roccioWeightedQuery.containsKey(termObj.getName())){
					roccioWeightedQuery.put(termObj.getName(), termObj.getScore());
					numTerms--;
				}
			}
			
			//Compute inner-product similarity of final Rocchio-weighted query with each weighted document in the collection
			//, and return the top documents
			
			ArrayList<ReturnDoc> roccioQueryResults = computeDocumentRanks(roccioWeightedQuery, docATCIndex);
			System.out.println("Query "+queryName.toString()+": "+ EvaluateQueries.printDocs(roccioQueryResults, 100));
			System.out.printf("Roccio MAP for query "+queryName.toString() + " is: %1$.2f\n", map);
			
			totalRoccioMAP += EvaluateQueries.meanAverageprecision(queryAnswers.get(answersKey), roccioQueryResults);
			
		}
		
		double map = totalMAP/queryIndex.documents().length;
		System.out.println("MAP for atc.atc is : " +  String.valueOf(map));
		
		double roccioMAP = totalRoccioMAP/queryIndex.documents().length;
		System.out.println("MAP for Rocchio is : " +  String.valueOf(roccioMAP));
		
		//create set of all words
		
		
		
		
		
	}
	
}
