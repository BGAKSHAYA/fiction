package org.ovgu.de.fiction;

import java.io.IOException;
import java.util.List;

import org.ovgu.de.fiction.feature.extraction.ChunkDetailsGenerator;
import org.ovgu.de.fiction.feature.extraction.FeatureExtractorUtility;
import org.ovgu.de.fiction.model.BookDetails;
import org.ovgu.de.fiction.model.TopKResults;
import org.ovgu.de.fiction.preprocess.ContentExtractor;
import org.ovgu.de.fiction.search.FictionRetrievalSearch;
import org.ovgu.de.fiction.search.InterpretSearchResults;
import org.ovgu.de.fiction.utils.FRConstants;
import org.ovgu.de.fiction.utils.FRGeneralUtils;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Suhita, Sayantan
 */
@RestController
@EnableAutoConfiguration
@RequestMapping("/search")
@SpringBootApplication
@CrossOrigin(origins = "https://fictionui.herokuapp.com")
public class FictionRetrievalDriver extends SpringBootServletInitializer{

    @RequestMapping(method = RequestMethod.POST)
    String getTopKResults(@RequestParam String bookID) throws Exception {
		TopKResults topKResults = FictionRetrievalSearch.findRelevantBooks(bookID, FRGeneralUtils.getPropertyVal("file.feature") , 
				FRConstants.SIMI_PENALISE_BY_CHUNK_NUMS, FRConstants.SIMI_ROLLUP_BY_ADDTN, 
				FRConstants.SIMI_EXCLUDE_TTR_NUMCHARS,FRConstants.TOP_K_RESULTS,FRConstants.SIMILARITY_L2);	
		
		InterpretSearchResults interp = new InterpretSearchResults();
        return topKResults.getResults_topK().toString() + "-" +	interp.performStatiscalAnalysisUsingRegression(topKResults).toString();
    }
    
	@RequestMapping(method = RequestMethod.GET)
	String getTopKResultsGet(@RequestParam String bookID, @RequestParam String systemName) throws Exception {

		TopKResults topKResults = null;
		if (systemName.equals(FRConstants.BAG_OF_WORDS)) {
			topKResults = FictionRetrievalSearch.pickFromBOWModel(bookID, FRConstants.TOP_K_RESULTS);
		} else if (systemName.equals(FRConstants.RANDOM)) {
			topKResults = FictionRetrievalSearch.pickNRandom(FRConstants.TOP_K_RESULTS);
		} else {
			topKResults = FictionRetrievalSearch.findRelevantBooks(bookID,
					FRGeneralUtils.getPropertyVal("file.feature"), FRConstants.SIMI_PENALISE_BY_CHUNK_NUMS,
					FRConstants.SIMI_ROLLUP_BY_ADDTN, FRConstants.SIMI_EXCLUDE_TTR_NUMCHARS, FRConstants.TOP_K_RESULTS,
					FRConstants.SIMILARITY_L2);
		}

		InterpretSearchResults interp = new InterpretSearchResults();
		return topKResults.getResults_topK().toString() + "-"
				+ interp.performStatiscalAnalysisUsingRegression(topKResults).toString();
	}

	public static void main(String[] args) throws Exception {
        //SpringApplication.run(FictionRetrievalDriver.class, args);


		long start = System.currentTimeMillis();		/* 1> Extract content from Gutenberg corpus - one time */
		ContentExtractor.generateContentFromAllEpubs(FRConstants.EN);
		//For German books
		ContentExtractor.generateContentFromAllEpubs(FRConstants.DE);
		System.out.println("Time taken for generating content (min)-" + (System.currentTimeMillis() - start) / (1000 * 60));

		start = System.currentTimeMillis();
		/* 2> Generate features from the extracted content - one time */
		List<BookDetails> features = generateOtherFeatureForAll(FRConstants.EN);
		//For German books
		List<BookDetails> featuresDe = generateOtherFeatureForAll(FRConstants.DE);
		System.out.println("Time taken for feature extraction and chunk generation (min)-" + (System.currentTimeMillis() - start) / (1000 * 60));
		start = System.currentTimeMillis();

		/* 3> Write features to CSV - one time */
		FeatureExtractorUtility.writeFeaturesToCsv(features);
		//For German books
		FeatureExtractorUtility.writeFeaturesToCsv(featuresDe);
		start = System.currentTimeMillis();
		System.out.println("Time taken for writing to CSV (min)-" + (System.currentTimeMillis() - start) / (1000 * 60));
		
		
		//FeatureExtractorUtility.writeFeaturesToCsv(featuresDe);
		
		/* 4> Query */
		String qryBookNum = "pg1400DickensGreatExp"; //pg11CarolAlice,  pg1400DickensGreatExp,pg766DickensDavidCopfld
														 // pg2701HermanMobyDick,pg537DoyleTerrorTales
		// pg13720HermanVoyage1, pg2911Galsw2, pg1155Agatha2,pg2852DoyleHound, pg2097DoyleSignFour

		// read from csv features and prints ranked relevant books, run after CSV is written
		String FEATURE_CSV_FILE = FRGeneralUtils.getPropertyVal("file.feature");
		//Config 1: three possible setting similarity objective penalization: divide chunks by (1) OR (number_of_chunks) OR sqr_root(number_of_chunks)
		//Config 2: two possible settings for similarity roll up : add_chunks (default) OR multipl_chunks
		//Config 3: Include or exclude TTR and Numbr of Chars
		
		
		TopKResults topKResults = FictionRetrievalSearch.findRelevantBooks(qryBookNum, FEATURE_CSV_FILE, 
				FRConstants.SIMI_PENALISE_BY_CHUNK_NUMS, FRConstants.SIMI_ROLLUP_BY_ADDTN, 
				FRConstants.SIMI_EXCLUDE_TTR_NUMCHARS,FRConstants.TOP_K_RESULTS,FRConstants.SIMILARITY_L2);
		
		/* * 5> Perform some machine learning over the results
		 
		*/
		InterpretSearchResults interp = new InterpretSearchResults();
		// interp.performStatiscalAnalysis(topKResults);
		interp.performStatiscalAnalysisUsingRegression(topKResults);
		
		//findLuceneRelevantBooks(qryBookNum);
	}

	public static List<BookDetails> generateOtherFeatureForAll(String locale) throws IOException {
		ChunkDetailsGenerator chunkImpl = new ChunkDetailsGenerator();
		List<BookDetails> books = chunkImpl.getChunksFromAllFiles(locale);
		return books;
	}









}
