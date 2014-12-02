import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
  
<<<<<<< HEAD
  public static double smartIDF(String term, MangoDB database, HashMap<String,Integer> invertedIndex) {
	    double N = (double)database.documentCount();
	    double n = invertedIndex.containsKey(term) ? invertedIndex.get(term).doubleValue() : 0;
	    double log = 0;
	    if(n != 0){
	    	log = Math.log((N / n));
	    }
	    return log;
=======
  
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
	  
>>>>>>> 7b42be4ac327b70417c63a6d68d14fa34642fc8c
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
<<<<<<< HEAD
=======
		  //calculate query tf*idf
>>>>>>> 7b42be4ac327b70417c63a6d68d14fa34642fc8c
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
<<<<<<< HEAD
=======
		  
		  //if not in document set to 5
>>>>>>> 7b42be4ac327b70417c63a6d68d14fa34642fc8c
		  if (document.get(term) == null){	
			  documentWeights.add((0.0 * idf));
		  } else {
			  double dtf = augmentedTF(term, document);
			  double dATN = dtf * idf;
			  documentWeights.add(dATN);
<<<<<<< HEAD
		  }
	  }

	  double[] normalizedQueryWeights = normalizeWeights(queryWeights);
	  double[] normalizedDocumentWeights = normalizeWeights(documentWeights);
	  for (int i = 0; i < normalizedQueryWeights.length; i++)
		  sum += (normalizedQueryWeights[i] * normalizedDocumentWeights[i]);
	  
	  return sum;
  }
  
  //============= Compute Document Values =============
//run through doc terms and if the oken is in queyr compute bpn
  public static double computeAnnBpn(HashMap<String, Double> queryAnnMap, String docName, MangoDB db, HashMap<String, Integer> invertedIndex){
	  HashMap<String, Integer> document = db.tokenFrequenciesForDocument(docName);
	  double N = db.documents().length;
	  double sum = 0.0;
	  
	  for (String term : queryAnnMap.keySet()){
		  if(document.get(term) != null){
			  double n = invertedIndex.containsKey(term) ? invertedIndex.get(term).doubleValue() : 0;
			  double tf = augmentedTF(term, document);
			  double pidf = Math.max(0, Math.log((N - n) / n));
			  double b = termFrequency(term, document) > 0 ? 1 : 0;
			  double bpn = tf * b *pidf;
			  double tq = queryAnnMap.get(term);
			  sum += tq * bpn;
		  }
	  }
=======
//			  sum += qATN * dATN;
		  }
	  }
	  
	  //compute normalized weighted vectors
	  ArrayList<Double> normalizedQueryWeights = computeNorm(queryWeights);
	  ArrayList<Double> normalizedDocumentWeights = computeNorm(documentWeights);
	  
	  //take dot products of Query and Document Vector
	  for (int i = 0; i < normalizedQueryWeights.size(); i++)
		  sum += (normalizedQueryWeights.get(i).doubleValue() * normalizedDocumentWeights.get(i).doubleValue());
	  
>>>>>>> 7b42be4ac327b70417c63a6d68d14fa34642fc8c
	  return sum;
  }

<<<<<<< HEAD
	  for (String term : query.keySet()){
		  if (document.get(term) != null){
			  sum += at(term, query, db) * at(term, document, db);
		  }
	  }
	  return sum;
  }
  
=======
>>>>>>> 7b42be4ac327b70417c63a6d68d14fa34642fc8c
}

