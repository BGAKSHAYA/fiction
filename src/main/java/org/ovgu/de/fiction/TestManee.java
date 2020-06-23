package org.ovgu.de.fiction;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.maltparser.core.helper.HashMap;
import org.maltparser.core.helper.HashSet;

public class TestManee {

	public static void main(String[] args) {
		
		SortedMap<Double, String> sorted_results = new TreeMap<Double, String>(Collections.reverseOrder());
		String querybook = "pg105";
		//Read the csv generated from TF-IDF
		Map<String, double[]> valueMap = readCsv();
		
		//Calculate cosine similarity 
		for(String book : valueMap.keySet()) {
			Double cosineSimilarity = calculateSimilarity(valueMap.get(querybook), valueMap.get(book));
			sorted_results.put(cosineSimilarity, book);
		}
		
		System.out.println(sorted_results.values());
		System.out.println(sorted_results.keySet());
		
		List<String> bookList = new ArrayList<String>(sorted_results.values());
		List<String> randomBookList = pickNRandom(bookList, 10);
		System.out.println(randomBookList);
		
	}
	
	public static Map<String, double[]> readCsv() {
		String csvFile = "C:\\Users\\49152\\Documents\\SIMFIC\\Features.csv";
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        Map<String, double[]> valueMap = new HashMap<String, double[]>();

        try {
			br = new BufferedReader(new FileReader(csvFile));
			br.readLine();
			while ((line = br.readLine()) != null) {
				
	            String[] valueArray = line.split(cvsSplitBy);
	            double[] doubleValueArray = new double[50];
	            for(int i=1; i < valueArray.length;i++) {
	            	doubleValueArray[i-1] = Double.valueOf(valueArray[i]);
	            }
	            valueMap.put(valueArray[0], doubleValueArray);
	        }
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return valueMap;
	}
	
	public static double calculateSimilarity(double[] docVector1, double[] docVector2) {
		
		double dotProduct = 0.0;
		double magnitude1 = 0.0;
		double magnitude2 = 0.0;
		double cosineSimilarity = 0.0;
		

		for (int i = 0; i < docVector1.length; i++) // docVector1 and docVector2 must be of same length
		{
			dotProduct += docVector1[i] * docVector2[i]; // a.b
			magnitude1 += Math.pow(docVector1[i], 2); // (a^2)
			magnitude2 += Math.pow(docVector2[i], 2); // (b^2)
		}

		magnitude1 = Math.sqrt(magnitude1);// sqrt(a^2)
		magnitude2 = Math.sqrt(magnitude2);// sqrt(b^2)

		if (magnitude1 != 0.0 | magnitude2 != 0.0) {
			cosineSimilarity = dotProduct / (magnitude1 * magnitude2);
		}
		return cosineSimilarity;
	}

	public static List<String> pickNRandom(List<String> bookList, int n) {
		List<String> copy = new LinkedList<String>(bookList);
	    Collections.shuffle(copy);
	    return copy.subList(0, n);
	}
}
