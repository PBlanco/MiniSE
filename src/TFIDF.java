import java.util.ArrayList;
import java.util.Arrays;
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
      if (piece.equals(term))
        freq++;
    return freq;
  }
  // Doc should be stemmed and tokenized
  // Essentially should be same data as stored in Mango
  public static int termFrequency(String term, HashMap<String, Integer> doc) {
    return doc.get(term);
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
  
  public static int binaryTF(String term, String doc) {
    String[] pieces = pieces(doc);
    for (String piece : pieces)
      if (piece.equals(term))
        return 1;
    return 0;
  }
  public static int binaryTF(String term, HashMap<String, Integer> doc) {
    Integer freq = doc.get(term);
    if (freq == null)
      return 0;
    else
      return 1;
  }
  
  public static double idf(String term, MangoDB database) {
    double N = (double)database.documentCount();
    double n = (double)database.numberOfDocumentsWithTerm(term);
    double log = Math.log10((N / n));
    return log;
  }
  

  
  public static double atn(String term, HashMap<String, Integer> doc, MangoDB collection) {
    double tf = augmentedTF(term, doc);
    // https://piazza.com/class/hz0gtmi8y6v6eo?cid=193
    double idf = idf(term, collection);
    return tf * idf;
  }
  public static double atn(String term, String source, MangoDB collection) {
    double tf = augmentedTF(term, source);
    double idf = idf(term, collection);
    return tf * idf;
  }
  public static double ann(String term, HashMap<String, Integer> doc) {
    return augmentedTF(term, doc);
  }
  public static double ann(String term, String source) {
    return augmentedTF(term, source);
  }
  public static double bpn(String term, HashMap<String, Integer> doc, MangoDB collection) {
    double tf = augmentedTF(term, doc);
    double N = (double)collection.documentCount();
    double n = (double)collection.numberOfDocumentsWithTerm(term);
    double pidf = Math.max(0, Math.log((N - n) / n));
    return tf * pidf;
  }
  public static double bpn(String term, String source, MangoDB collection) {
    double tf = augmentedTF(term, source);
    double N = (double)collection.documentCount();
    double n = (double)collection.numberOfDocumentsWithTerm(term);
    double pidf = Math.max(0, Math.log((N - n) / n));
    return tf * pidf;
  }
  
  // https://piazza.com/class/hz0gtmi8y6v6eo?cid=253
  public static double[] normalizeWeights(Object[] weights) {
    double denomInner = 0;
    for (Object num : weights)
      denomInner += Math.pow(((Double)num).doubleValue(), 2);
    double denom = Math.sqrt(denomInner);
    double normFactor = 1.0 / denom;
    double[] mapped = new double[weights.length];
    for (int i = 0; i < weights.length; i++)
      mapped[i] = (((Double)weights[i]).doubleValue()) * normFactor;
    return mapped;
  }
  
  // ============ Compute Query Values =================
  public static HashMap<String, Double> computeAnn(HashMap<String, Integer> query){
	  HashMap<String, Double> annMap = new HashMap<String, Double>(); 
	  for (String token : query.keySet())
		  annMap.put(token, ann(token, query));
	  return annMap;
  }
  public static double computeatcatc(HashMap<String, Integer> query, String docName, MangoDB db){
	  HashMap<String, Integer> document = db.tokenFrequenciesForDocument(docName);
	  
	  double sum = 0.0;
	  
	  double maxTfq = maxTF(query);
	  double maxTfd = maxTF(document);
	  
	  ArrayList<Double> qWeights = new ArrayList<Double>();
	  ArrayList<Double> dWeights = new ArrayList<Double>();
	  
	  for (String term : query.keySet()){
		  if (document.get(term) != null){
//			  double tfq = query.get(term);
//			  double tfd = document.get(term);
//			  
//			  double aq = 0.5 + 0.5*(tfq/maxTfq);
//			  double ad = 0.5 + 0.5*(tfd/maxTfd);
			  
			  double aq = augmentedTF(term, query);
			  double ad = augmentedTF(term, document);
			  
			  double idf = idf(term, db);
			  
			  qWeights.add(aq * idf);
			  dWeights.add(ad * idf);
		  }
	  }
	  Object[] qWeightsArray = qWeights.toArray();
	  double[] normalizedQ = normalizeWeights(qWeightsArray);
	  Object[] dWeightsArray = dWeights.toArray();
	  double[] normalizedD = normalizeWeights(dWeightsArray);
	  
	  for (int i = 0; i < normalizedQ.length; i++) {
		  double prod = normalizedQ[i] * normalizedD[i];
		  sum += prod;
	  }
	  return sum;
  }
  
  //============= Compute Document Values =============
//run through doc terms and if the oken is in queyr compute bpn
  public static double computeAnnBpn(HashMap<String, Double> queryAnnMap, String docName, MangoDB db){
	  HashMap<String, Integer> document = db.tokenFrequenciesForDocument(docName);
	  double N = db.documents().length;
	  double sum = 0.0;
	  
	  for (String term : queryAnnMap.keySet()){
		  if(document.get(term) != null){
			  double n = (double)db.numberOfDocumentsWithTerm(term); 
			  double tf = augmentedTF(term, document);
			  double pidf = Math.max(0, Math.log((N - n) / n));
			  double b = termFrequency(term, document) > 0 ? 1 : 0;
			  double bpn = tf * b *pidf;
			  double tq = queryAnnMap.get(term);
			  sum += tq * bpn;
		  }
	  }
	  return sum;
  }
  
  public static double computeAtnatn(HashMap<String, Integer> query, String docName, MangoDB db){
	  HashMap<String, Integer> document = db.tokenFrequenciesForDocument(docName);
	  
	  double sum = 0.0;
	  
	  double maxTfq = maxTF(query);
	  double maxTfd = maxTF(document);

	  for (String term : query.keySet()){
		  if (document.get(term) != null){
			  double tfq = query.get(term);
			  double tfd = document.get(term);
			  
			  double aq = 0.5 + 0.5*(tfq/maxTfq);
			  double ad = 0.5 + 0.5*(tfd/maxTfd);
			  
			  double idf = idf(term, db);
			  
			  sum += (aq * idf) * (ad * idf);
			  
		  }
	  }
	  return sum;
  }
  
}

