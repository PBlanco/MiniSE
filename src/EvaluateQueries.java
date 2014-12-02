import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import java.util.Set;


// import lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;

public class EvaluateQueries {
	
	public static void main(String[] args) {
		String cacmDocsDir = "data/cacm/"; // directory containing CACM documents
		String medDocsDir = "data/med"; // directory containing MED documents
		
		String cacmIndexDir = "data/index/cacm"; // the directory where index is written into
		String medIndexDir = "data/index/med"; // the directory where index is written into
		
		String cacmQueryFile = "data/cacm_processed.query";    // CACM query file
		String cacmAnswerFile = "data/cacm_processed.rel";   // CACM relevance judgements file

		String medQueryFile = "data/med_processed.query";    // MED query file
		String medAnswerFile = "data/med_processed.rel";   // MED relevance judgements file
		
		int cacmNumResults = 100;
		int medNumResults = 100;

		//tokenizer 
		String stopwordFile = "stopwords/stopwords_indri.txt"; //Stop word file
		CharArraySet stopwords = IndexFiles.makeStopwordSet(stopwordFile);
		
		double cacmMAP = evaluate(cacmIndexDir, cacmDocsDir, cacmQueryFile,
				cacmAnswerFile, cacmNumResults, stopwords);
//		System.out.println("CACM MAP: " + String.valueOf(cacmMAP));

		System.out.println("\n");
		
		double medMAP = evaluate(medIndexDir, medDocsDir, medQueryFile,
				medAnswerFile, medNumResults, stopwords);
//		System.out.println("MED MAP: "+ String.valueOf(medMAP)+"\n");;
//		System.out.println("CACM MAP: " + String.valueOf(cacmMAP));
	}

	/*================== Load Methods ==================*/
	private static Map<Integer, String> loadQueries(String filename) {
		HashMap<Integer, String> queryIdMap = new HashMap<Integer, String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(
					new File(filename)));
		} catch (FileNotFoundException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}

		String line;
		try {
			while ((line = in.readLine()) != null) {
				int pos = line.indexOf(',');
				queryIdMap.put(Integer.parseInt(line.substring(0, pos)), line
						.substring(pos + 1));
			}
		} catch(IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch(IOException e) {
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
		}
		return queryIdMap;
	}

	private static Map<Integer, HashSet<String>> loadAnswers(String filename) {
		HashMap<Integer, HashSet<String>> queryAnswerMap = new HashMap<Integer, HashSet<String>>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(
					new File(filename)));

			String line;
			while ((line = in.readLine()) != null) {
				String[] parts = line.split(" ");
				HashSet<String> answers = new HashSet<String>();
				for (int i = 1; i < parts.length; i++) {
					answers.add(parts[i]);
				}
				queryAnswerMap.put(Integer.parseInt(parts[0]), answers);
			}
		} catch(IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch(IOException e) {
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
		}
		return queryAnswerMap;
	}
	
	
	/* ================= Calculations =================*/
	private static double evaluate(String indexDir, String docsDir, String queryFile, String answerFile, int numResults, CharArraySet stopwords) {

		// Build Index
		MangoDB docIndex = new MangoDB();
		IndexFiles.buildIndex(indexDir, docsDir, stopwords, docIndex);


		// load queries and answer
		Map<Integer, String> queries = loadQueries(queryFile);
		Map<Integer, HashSet<String>> queryAnswers = loadAnswers(answerFile);
		MangoDB queryIndex = new MangoDB();
		IndexFiles.buildQueryIndex(queries, stopwords, queryIndex);
			

		//computeRocchio(queries, documents, answers,|rel|, a, b, K)
		//(A) 
		Roccio.computeRocchio(queryIndex, docIndex, queryAnswers, 7, 4, 8, 5);
		//(B) Roccio.computeRocchio(queryIndex, docIndex, queryAnswers, 7, 4, 16, 10);
		
		//WeightedIndex x =  TFIDF.computeatcWeights(docIndex);
		//return atcatc(queryIndex, docIndex, queryAnswers, numResults, x);
		return 0;
	}
	
	public static double meanAverageprecision(HashSet<String> answers, ArrayList<ReturnDoc> results) {
		double avp = 0;
		double matches = 0;
		double docs = 0;
		for (ReturnDoc result : results) {
			docs++;
			if (answers.contains(result.getName())){
				matches++;
				avp+= matches/docs;
			}
			if(docs == 100)
				break;
		}

		return avp/answers.size();
	}

	private static double atcatc(MangoDB queryIndex, MangoDB docIndex, Map<Integer, HashSet<String>>queryAnswers, int numResults, WeightedIndex docWeights){
		//Create inverted index
		System.out.print("Creating inverted index");
		HashMap<String, Integer> invertedIndex = new HashMap<String, Integer>();
		for(Object docName : docIndex.documents()){
			System.out.print(".");
			HashMap<String, Integer>document = docIndex.get(String.valueOf(docName));
			for(String term : document.keySet()){
				if(invertedIndex.containsKey(term) ){
					invertedIndex.put(term, (invertedIndex.get(term) +1)); 
				} else {
					invertedIndex.put(term, 1);
				}
			}
		}
		System.out.print("\n");
		System.out.print("Done!!\n");
		
		double totalMAP = 0;
		
		for(Object key : queryIndex.documents()){
			//get query 
			HashMap<String, Integer> query = queryIndex.tokenFrequenciesForDocument(key.toString());
			
			//Store doc scores in this array
			ArrayList<ReturnDoc> queryResults = new ArrayList<ReturnDoc>();
			
			//iterate through documents to create atc scores
			for(Object docName : docIndex.documents()){		
				Double atcatcScore = TFIDF.computeatcatc(query, docName.toString(), docIndex, invertedIndex);
				String fullDocName = docName.toString();
				ReturnDoc doc = new ReturnDoc(fullDocName.substring(0, fullDocName.length() - 4), atcatcScore, docWeights.get(fullDocName));
				queryResults.add(doc);
			}
			
			//sort list of docs by score greatest first
			Collections.sort(queryResults, new CustomComparator());	
			
			// For Clustering
			ArrayList<ReturnDoc> top30 = new ArrayList<ReturnDoc>();
			for (int i = 0; i < 30; i++)
				top30.add(queryResults.get(i));
			ArrayList<ReturnDoc> clequalizedTop30 = CompleteClustering.clequalizeDocs(top30, 10);
			for (int i = 0; i < 30; i++)
				queryResults.set(i, clequalizedTop30.get(i));

			
			//Get query key for answers
			Integer answersKey = Integer.parseInt(key.toString());
			//calculate MAP
			double map = meanAverageprecision(queryAnswers.get(answersKey), queryResults);
			System.out.println("Query "+key.toString()+": "+ printDocs(queryResults, numResults));
			System.out.printf("20-Clustered atc.atc MAP for query "+key.toString() + " is: %1$.2f\n", map);
			totalMAP += map;
		}
		return totalMAP/queryIndex.documents().length;
	}

	/* =================== Helpers =================== */
	public static String printDocs(ArrayList<ReturnDoc> docList, int number){
		int i = 0;
		String ret = "[";
		for (ReturnDoc doc : docList){
			i++;
			ret+=(doc.getName()+", ");
			if (i == number)
				break;
		}
		ret += "]";
		return ret;
	}
	
}

class CustomComparator implements Comparator<ReturnDoc> {
    @Override
    public int compare(ReturnDoc o1, ReturnDoc o2) {
    	double a = o1.getScore();
    	double b = o2.getScore();
    	int cmp = b > a ? 1 : b < a ? -1 : 0;
        return  cmp;
    }
}

//Doc returned from BM25
class ReturnDoc{
    private String name;
    private double score;
    private HashMap<String, Double> atc;
        
    public ReturnDoc(String n, Double bm25Score){
        this.name = n;
        this.score = bm25Score;
        atc = new HashMap<String, Double>();
    }
    public ReturnDoc(String n, Double score, HashMap<String, Double> weights) {
    	name = n;
    	this.score = score;
    	atc = weights;
    }
     
    public String getName() {
        return name;
    }

    public double getScore() {
        return score;
    }
    
    public void setScore(double newScore) {
    	this.score = newScore;
    }
    
    public HashMap<String, Double> weights() {
    	return atc;
    }
}

class DotProduct {
	public static double dotProduct(HashMap<String, Double> v1, HashMap<String, Double> v2) {
		Set<String> keyset1 = v1.keySet();
		Set<String> keyset2 = v2.keySet();
		Set<String> commonKeyset = new HashSet<String>();
		commonKeyset.addAll(keyset1);
		commonKeyset.retainAll(keyset2);
		double sum = 0.0;
		for (String key : commonKeyset)
			sum += (v1.get(key) * v2.get(key));
		return sum;
	}
	public static double dotProduct(ReturnDoc d1, ReturnDoc d2) {
		return dotProduct(d1.weights(), d2.weights());
	}
}

class ClusterDistance {
	private Cluster d1;
	private Cluster d2;
	private double distance;
	private int clusterID;
	
	public ClusterDistance(Cluster doc1, Cluster doc2, double dist, int cID) {
		d1 = doc1;
		d2 = doc2;
		distance = dist;
		clusterID = cID;
	}
	
	public Cluster[] clusters() {
		Cluster[] docs = {d1, d2};
		return docs;
	}
	
	public Cluster lCluster() {
		return d1;
	}
	
	public Cluster rCluster() {
		return d2;
	}
	
	public double distance() {
		return distance;
	}
	
	public int clusterID() {
		return clusterID;
	}
}

class Cluster {
	private ArrayList<ReturnDoc> docs;
	
	public Cluster() {
		docs = new ArrayList<ReturnDoc>();
	}
	public Cluster(ReturnDoc doc) {
		docs = new ArrayList<ReturnDoc>();
		docs.add(doc);
	}
	
	public void addDoc(ReturnDoc d) {
		docs.add(d);
	}
	
	private void addAllDocs(ArrayList<ReturnDoc> docs) {
		for (ReturnDoc doc : docs)
			this.addDoc(doc);
	}
	public void mergeWith(Cluster otherCluster) {
		this.addAllDocs(otherCluster.docs());
	}
	
	public ArrayList<ReturnDoc> docs() {
		return docs;
	}
	
	public int size() {
		return docs.size();
	}
	
	public ReturnDoc get(int index) {
		return docs.get(index);
	}
}

class ClusterDistanceComparator implements Comparator<ClusterDistance> {
    @Override
    public int compare(ClusterDistance o1, ClusterDistance o2) {
    	double a = o1.distance();
    	double b = o2.distance();
    	// First distance is largest
    	int cmp = b < a ? 1 : b > a ? -1 : 0;
        return  cmp;
    }
}

class CompleteClustering {
	private static double distance(Cluster c1, Cluster c2) {
		ArrayList<Double> dotProducts = new ArrayList<Double>(c1.size() * c2.size());
		for (ReturnDoc d1 : c1.docs())
			for (ReturnDoc d2 : c2.docs())
				dotProducts.add(DotProduct.dotProduct(d1, d2));
		return Collections.max(dotProducts);
	}
	private static ArrayList<Cluster> cluster(ArrayList<Cluster> clusters, int k) {
		if (clusters.size() == k)
			return clusters;
		
		ArrayList<ClusterDistance> distances = new ArrayList<ClusterDistance>();
		for (int i = 0; i < clusters.size(); i++) {
			for (int j = 0; j < i; j++) {
				Cluster primaryCluster = clusters.get(i);
				Cluster otherCluster = clusters.get(j);
				double clusterDistance = distance(primaryCluster, otherCluster);
				distances.add(new ClusterDistance(primaryCluster, otherCluster, clusterDistance, j));
			}
		}
		Collections.sort(distances, new ClusterDistanceComparator());
		ClusterDistance greatestDistance = distances.get(0);
		Cluster primaryCluster = greatestDistance.lCluster();
		Cluster otherCluster = greatestDistance.rCluster();
		
		primaryCluster.mergeWith(otherCluster);
		clusters.remove(greatestDistance.clusterID());
		return cluster(clusters, k);
	}
	private static void equalizeCluster(Cluster c) {
		double maxScore = 0.0;
		for (ReturnDoc d : c.docs())
			if (d.getScore() > maxScore)
				maxScore = d.getScore();
		
		for (ReturnDoc d : c.docs())
			d.setScore(maxScore);
	}
	public static ArrayList<ReturnDoc> clequalizeDocs(ArrayList<ReturnDoc> docs, int k) {
		ArrayList<Cluster> clusters = new ArrayList<Cluster>();
		for (ReturnDoc doc : docs)
			clusters.add(new Cluster(doc));
		ArrayList<Cluster> clusteredClusters = cluster(clusters, k);
		for (Cluster c : clusteredClusters)
			equalizeCluster(c);
		
		ArrayList<ReturnDoc> clequalizedDocs = new ArrayList<ReturnDoc>();
		for (Cluster c : clusteredClusters)
			clequalizedDocs.addAll(c.docs());
		return clequalizedDocs;
	}
}
