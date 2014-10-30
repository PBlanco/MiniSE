import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;





// import lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;

public class EvaluateQueries {
	
	
	
	public static void main(String[] args) {
		/*edited this so it's only one doc */
		String cacmDocsDir = "data/cacm/"; // directory containing CACM documents
		String medDocsDir = "data/med"; // directory containing MED documents
		
		String cacmIndexDir = "data/index/cacm"; // the directory where index is written into
		String medIndexDir = "data/index/med"; // the directory where index is written into
		
		String cacmQueryFile = "data/cacm_processed.query";    // CACM query file
		String cacmAnswerFile = "data/cacm_processed.rel";   // CACM relevance judgements file

		String medQueryFile = "data/med_processed.query";    // MED query file
		String medAnswerFile = "data/med_processed.rel";   // MED relevance judgements file
		
		int cacmNumResults = 10;
		int medNumResults = 10;

		
		//tokenizer 
		String stopwordFile = "stopwords/stopwords_indri.txt"; //Stop word file
		CharArraySet stopwords = IndexFiles.makeStopwordSet(stopwordFile);
		
		
		System.out.println("MAP for "+cacmDocsDir+": "+ evaluate(cacmIndexDir, cacmDocsDir, cacmQueryFile,
				cacmAnswerFile, cacmNumResults, stopwords));

		/*
		System.out.println("\n");
		
		System.out.println(evaluate(medIndexDir, medDocsDir, medQueryFile,
				medAnswerFile, medNumResults, stopwords));
		*/
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
	
	private static double evaluate(String indexDir, String docsDir,
			String queryFile, String answerFile, int numResults,
			CharArraySet stopwords) {

		// Build Index
		MangoDB docIndex = new MangoDB();
		IndexFiles.buildIndex(indexDir, docsDir, stopwords, docIndex);


		// load queries and answer
		Map<Integer, String> queries = loadQueries(queryFile);
		MangoDB queryIndex = new MangoDB();
		IndexFiles.buildQueryIndex(queries, stopwords, queryIndex);
		
		//MAP
		//get AP of the query and add to total val
		//divide by total queries
		
		double totalPrecision = 0;
		
		Object[] queryKeys = queryIndex.documents();
		Object[] docKeys = docIndex.documents();
		
		for(Object key : queryKeys){
			double queryPrecision= 0;
			HashMap<String, Integer> query = queryIndex.tokenFrequenciesForDocument(key.toString());
			for(Object docName : docKeys){
				queryPrecision += BM25Similarity.computeBM25Similarity(query, docName.toString(), docIndex);
			}
			double temp = queryPrecision/docIndex.documents().length;
			System.out.println(key.toString() + ": "+temp);
			totalPrecision += temp;
		}
		
//		Map<Integer, HashSet<String>> queryAnswers = loadAnswers(answerFile);
		
		return totalPrecision/queryIndex.documentCount();
	}
	
}
