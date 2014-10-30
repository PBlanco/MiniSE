import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;



import java.util.Set;




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
		
		
		System.out.println(evaluate(cacmIndexDir, cacmDocsDir, cacmQueryFile,
				cacmAnswerFile, cacmNumResults, stopwords));
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

	private static double precision(HashSet<String> answers,
			List<String> results) {
		double matches = 0;
		for (String result : results) {
			if (answers.contains(result))
				matches++;
		}
		return matches / results.size();
	}
	
	private static HashMap<String, List<TFIDFResult>> atnatn(MangoDB docIndex, MangoDB queryIndex) {
		HashMap<String, List<TFIDFResult>> results = new HashMap<String, List<TFIDFResult>>();
		for (String query : queryIndex.keys()) {
			ArrayList<TFIDFResult> scores = new ArrayList<TFIDFResult>();
			int count = 0;
			for (String doc : docIndex.keys()) {
				count++;
				Set<String> queryTokens = queryIndex.get(query).keySet();
				Set<String> docTokens = docIndex.get(doc).keySet();
				Set<String> commonTokens = new HashSet<String>(docTokens);
				commonTokens.addAll(queryTokens);
				double score = 0;
				for (String token : commonTokens) {
					double queryATN = TFIDF.atn(token, query, docIndex);
					double docATN = TFIDF.atn(token, doc, docIndex);
					double product = queryATN * docATN;
					score += product;
				}
				TFIDFResult res = new TFIDFResult(doc, score);
				scores.add(res);
				System.out.println("" + count);
			}
			Collections.sort(scores);
			results.put(query, scores.subList(0, 100));
		}
		return results;
	}
	
	private static HashMap<String, List<TFIDFResult>> annbpn(MangoDB docIndex, MangoDB queryIndex) {
		HashMap<String, List<TFIDFResult>> results = new HashMap<String, List<TFIDFResult>>();
		for (String query : queryIndex.keys()) {
			ArrayList<TFIDFResult> scores = new ArrayList<TFIDFResult>();
			for (String doc : docIndex.keys()) {
				Set<String> queryTokens = queryIndex.get(query).keySet();
				Set<String> docTokens = docIndex.get(doc).keySet();
				Set<String> commonTokens = new HashSet<String>(docTokens);
				commonTokens.addAll(queryTokens);
				double score = 0;
				for (String token : commonTokens) {
					double queryATN = TFIDF.ann(token, query);
					double docATN = TFIDF.bpn(token, doc, docIndex);
					double product = queryATN * docATN;
					score += product;
				}
				TFIDFResult res = new TFIDFResult(doc, score);
				scores.add(res);
			}
			Collections.sort(scores);
			results.put(query, scores.subList(0, 100));
		}
		return results;
	}
	
	private static HashMap<String, List<TFIDFResult>> atcatc(MangoDB docIndex, MangoDB queryIndex) {
		HashMap<String, List<TFIDFResult>> results = new HashMap<String, List<TFIDFResult>>();
		for (String query : queryIndex.keys()) {
			ArrayList<TFIDFResult> scores = new ArrayList<TFIDFResult>();
			for (String doc : docIndex.keys()) {
				Set<String> queryTokens = queryIndex.get(query).keySet();
				Set<String> docTokens = docIndex.get(doc).keySet();
				Set<String> commonTokens = new HashSet<String>(docTokens);
				commonTokens.addAll(queryTokens);
				double score = 0;
				ArrayList<Double> queries = new ArrayList<Double>(commonTokens.size());
				ArrayList<Double> docs = new ArrayList<Double>(commonTokens.size());
				for (String token : commonTokens) {
					double queryATN = TFIDF.ann(token, query);
					queries.add(queryATN);
					double docATN = TFIDF.bpn(token, doc, docIndex);
					docs.add(docATN);
				}
				double[] normQueries = TFIDF.normalizeWeights((Double[]) queries.toArray());
				double[] normDocs = TFIDF.normalizeWeights((Double []) docs.toArray());
				for (int i = 0; i < normQueries.length; i++)
					score += (normQueries[i] * normDocs[i]);
				TFIDFResult res = new TFIDFResult(doc, score);
				scores.add(res);
			}
			Collections.sort(scores);
			results.put(query, scores.subList(0, 100));
		}
		return results;
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
		
		// Calculate TF-IDF for each set of queries
		HashMap<String, List<TFIDFResult>> atnatn = atnatn(docIndex, queryIndex);
		HashMap<String, List<TFIDFResult>> annbpn = annbpn(docIndex, queryIndex);
		HashMap<String, List<TFIDFResult>> atcatc = atcatc(docIndex, queryIndex);
		
		return 0;
	}
	
}
