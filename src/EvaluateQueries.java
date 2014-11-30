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
		System.out.println("CACM MAP: " + String.valueOf(cacmMAP));

		
		System.out.println("\n");
		
		double medMAP = evaluate(medIndexDir, medDocsDir, medQueryFile,
				medAnswerFile, medNumResults, stopwords);
		System.out.println("MED MAP: "+ String.valueOf(medMAP)+"\n");;
		System.out.println("CACM MAP: " + String.valueOf(cacmMAP));
		
	}

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
	
	private static double bm25(MangoDB queryIndex, MangoDB docIndex, Map<Integer, HashSet<String>>queryAnswers, int numResults){
		double totalMAP = 0;
		for(Object key : queryIndex.documents()){
			HashMap<String, Integer> query = queryIndex.tokenFrequenciesForDocument(key.toString());
			//search for query
			ArrayList<ReturnDoc> queryResults = new ArrayList<ReturnDoc>();

			//Create list of documents
			for(Object docName : docIndex.documents()){
				//calculate bm25
				Double bm25Score = BM25Similarity.computeBM25Similarity(query, docName.toString(), docIndex);
				String fullDocName = docName.toString();
				ReturnDoc doc = new ReturnDoc(fullDocName.substring(0, fullDocName.length() - 4), bm25Score);
				queryResults.add(doc);
			}
			//sort list of docs by score greatest first
			Collections.sort(queryResults, new CustomComparator());	

			//Get query key for answers
			Integer answersKey = Integer.parseInt(key.toString());
			//calculate MAP
			double map = meanAverageprecision(queryAnswers.get(answersKey), queryResults);
			System.out.println("Query "+key.toString()+": "+ printDocs(queryResults, numResults));
			System.out.printf("BM25 MAP for query "+key.toString() + " is: %1$.2f\n", map);
			totalMAP += map;
		}
		return totalMAP/queryIndex.documents().length;
	}
	
	private static double atnatn(MangoDB queryIndex, MangoDB docIndex, Map<Integer, HashSet<String>>queryAnswers, int numResults){
		double totalMAP = 0;
		
		//loop through queries
		for(Object key : queryIndex.documents()){
			HashMap<String, Integer> query = queryIndex.tokenFrequenciesForDocument(key.toString());
			
			//create list to store score results
			ArrayList<ReturnDoc> queryResults = new ArrayList<ReturnDoc>();

			//loop through documents
			for(Object docName : docIndex.documents()){		
				Double atnatnScore = TFIDF.computeAtnatn(query, docName.toString(), docIndex);
				String fullDocName = docName.toString();
				ReturnDoc doc = new ReturnDoc(fullDocName.substring(0, fullDocName.length() - 4), atnatnScore);
				queryResults.add(doc);
			}
			//sort list of docs by score greatest first
			Collections.sort(queryResults, new CustomComparator());	

			//Get query key for answers
			Integer answersKey = Integer.parseInt(key.toString());
			//calculate MAP
			double map = meanAverageprecision(queryAnswers.get(answersKey), queryResults);
			System.out.println("Query "+key.toString()+": "+ printDocs(queryResults, numResults));
			System.out.printf("atn.atn MAP for query "+key.toString() + " is: %1$.2f\n", map);
			totalMAP += map;
		}
		return totalMAP/queryIndex.documents().length;
	}
	
	private static double atcatc(MangoDB queryIndex, MangoDB docIndex, Map<Integer, HashSet<String>>queryAnswers, int numResults){
		double totalMAP = 0;
		for(Object key : queryIndex.documents()){
			
			HashMap<String, Integer> query = queryIndex.tokenFrequenciesForDocument(key.toString());
			//search for query
			ArrayList<ReturnDoc> queryResults = new ArrayList<ReturnDoc>();
				
			for(Object docName : docIndex.documents()){		
				Double atcatcScore = TFIDF.computeatcatc(query, docName.toString(), docIndex);
				String fullDocName = docName.toString();
				ReturnDoc doc = new ReturnDoc(fullDocName.substring(0, fullDocName.length() - 4), atcatcScore);
				queryResults.add(doc);
			}
			//sort list of docs by score greatest first
			Collections.sort(queryResults, new CustomComparator());	
			
			//Get query key for answers
			Integer answersKey = Integer.parseInt(key.toString());
			//calculate MAP
			double map = meanAverageprecision(queryAnswers.get(answersKey), queryResults);
			System.out.println("Query "+key.toString()+": "+ printDocs(queryResults, numResults));
			System.out.printf("atc.atc MAP for query "+key.toString() + " is: %1$.2f\n", map);
			totalMAP += map;
		}
		return totalMAP/queryIndex.documents().length;
	}
	
	private static double annbpn(MangoDB queryIndex, MangoDB docIndex, Map<Integer, HashSet<String>>queryAnswers, int numResults){
		double totalMAP = 0;
		for(Object key : queryIndex.documents()){
			
			HashMap<String, Integer> query = queryIndex.tokenFrequenciesForDocument(key.toString());
			//search for query
			ArrayList<ReturnDoc> queryResults = new ArrayList<ReturnDoc>();
			
			//Create list of documents
			
			//compute ann
			HashMap<String, Double> queryAnnMap = TFIDF.computeAnn(query);
		
			
			for(Object docName : docIndex.documents()){		
				
				Double annBpnScore = TFIDF.computeAnnBpn(queryAnnMap, docName.toString(), docIndex);
				String fullDocName = docName.toString();
				ReturnDoc doc = new ReturnDoc(fullDocName.substring(0, fullDocName.length() - 4), annBpnScore);
				queryResults.add(doc);
			}
			//sort list of docs by score greatest first
			Collections.sort(queryResults, new CustomComparator());	
			
			//Get query key for answers
			Integer answersKey = Integer.parseInt(key.toString());
			//calculate MAP
			double map = meanAverageprecision(queryAnswers.get(answersKey), queryResults);
			System.out.println("Query "+key.toString()+": "+ printDocs(queryResults, numResults));
			System.out.printf("ann.bpn MAP for query "+key.toString() + " is: %1$.2f\n", map);
			totalMAP += map;
		}
		return totalMAP/queryIndex.documents().length;
	}
	
	
	private static double meanAverageprecision(HashSet<String> answers, ArrayList<ReturnDoc> results) {
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
	
	private static String printDocs(ArrayList<ReturnDoc> docList, int number){
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
	
	private static double evaluate(String indexDir, String docsDir,
			String queryFile, String answerFile, int numResults,
			CharArraySet stopwords) {

		// Build Index
		MangoDB docIndex = new MangoDB();
		IndexFiles.buildIndex(indexDir, docsDir, stopwords, docIndex);


		// load queries and answer
		Map<Integer, String> queries = loadQueries(queryFile);
		Map<Integer, HashSet<String>> queryAnswers = loadAnswers(answerFile);
		MangoDB queryIndex = new MangoDB();
		IndexFiles.buildQueryIndex(queries, stopwords, queryIndex);
		
		//============ Uncomment the one you want to run =====================
		//return annbpn(queryIndex, docIndex, queryAnswers, numResults);		
		//return atnatn(queryIndex, docIndex, queryAnswers, numResults);
		return atcatc(queryIndex, docIndex, queryAnswers, numResults);
		//return bm25(queryIndex, docIndex, queryAnswers, numResults);
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
        
    public ReturnDoc(String n, Double bm25Score){
        this.name = n;
        this.score = bm25Score;
    }
     
    public String getName() {
        return name;
    }

    public double getScore() {
        return score;
    }
}


