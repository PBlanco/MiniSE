import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
// import org.apache.lucene.util.Version;
import org.apache.lucene.util.Version;

/** Index all text files under a directory, the directory is at data/txt/
 */

public class IndexFiles {
	
	private IndexFiles() {}
	
	public static CharArraySet makeStopwordSet(String filename){
		BufferedReader reader = null;
		CharArraySet stopwordSet = new CharArraySet(Version.LUCENE_44,0,false);
		try {
			File file = new File(filename);
			reader = new BufferedReader(new FileReader(file));
			String word;
			while ((word = reader.readLine()) != null){
				stopwordSet.add(word);
			}
		} catch (IOException e){
			e.printStackTrace();
		} finally{
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return stopwordSet;
	}
	
	public static ArrayList<String> removeStopWordsAndStem(String docpath, CharArraySet stopwords) throws IOException {
		
		//convert doc to string
		String docString = "";
		try{
			docString = new Scanner( new File(docpath)).useDelimiter("\\A").next();
			//System.out.println(docString);
		}  catch (IOException e){
			e.printStackTrace();
			return null;
		}
		
	    //initialize token stream with document
	    @SuppressWarnings("deprecation")
		TokenStream tokenStream = new StandardTokenizer(Version.LUCENE_44, new StringReader(docString));
	    tokenStream = new StopFilter(tokenStream, stopwords);
	    tokenStream = new PorterStemFilter(tokenStream);

	    
	    //StringBuilder sb = new StringBuilder(); //FOR TESTING
	    
	    //arralist to keep tokenized words
	    ArrayList<String> tokensArray = new ArrayList<String>();
	    
	    //use this to access tokens within stream
	    CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
	    //Brings stream back to starting token
	    tokenStream.reset();
	    while (tokenStream.incrementToken()) {
	        String term = charTermAttribute.toString();
	        tokensArray.add(term);
	        //sb.append(term + " ");//FOR TESTING
	    }
	    tokenStream.end();
	    tokenStream.close();
	    //System.out.println(sb.toString());//FOR TESTING
	    return tokensArray;
	}
	
	
	public static HashMap<String, Integer> createDocumentTokenMapping(ArrayList<String> docTokensAL){
		HashMap<String, Integer> tokenFrequencyMap = new HashMap<String, Integer>(); 
		for(String token : docTokensAL){
			int freq = tokenFrequencyMap.containsKey(token) ? tokenFrequencyMap.get(token) : 0;
			tokenFrequencyMap.put(token, freq + 1);
		}
		return tokenFrequencyMap;
	}
	
	/** Index all text files under a directory. */
	public static void buildIndex(String indexPath, String docsPath, CharArraySet stopwords, MangoDB database) {
		// Check whether docsPath is valid
		if (docsPath == null || docsPath.isEmpty()) {
			System.err.println("Document directory cannot be null");
			System.exit(1);
		}

		// Check whether the directory is readable
		final File docDir = new File(docsPath);
		if (!docDir.exists() || !docDir.canRead()) {
			System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}
		
		//============ My code ===================	    
		Date start = new Date();
		
		System.out.println("Indexing to directory '" + indexPath + "'...");
		
		indexDocs(indexPath, docsPath, stopwords, docDir , database);
		
		Date end = new Date();
		System.out.println(end.getTime() - start.getTime() + " total milliseconds"); 
	}

	/**
	 * Indexes the given file using the given writer, or if a directory is given,
	 * recurses over files and directories found under the given directory.
	 */
	static void indexDocs(String indexPath, String docsPath, CharArraySet stopwords, File file, MangoDB database) {
		if (file.canRead()) {
			if (file.isDirectory()) {
				String[] files = file.list();
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						indexDocs(indexPath, docsPath, stopwords, new File(file, files[i]), database);
					}
				}
			} else {
		
				try {
					//remove stopwords, stemm, and tokenize
					ArrayList<String> docTokenArrayList;
					
					String path = file.getName();
					if (docsPath != file.getName()){
						path = docsPath + file.getName();
					}

					docTokenArrayList =removeStopWordsAndStem(path, stopwords);

					//Get doc name
					//create term->frequency hashmap
					HashMap<String, Integer> freqs = createDocumentTokenMapping(docTokenArrayList);
					//add to databse
					database.setTokenFrequenciesForDocument(file.getName(), freqs);
				} catch (IOException e) {
					System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
}
