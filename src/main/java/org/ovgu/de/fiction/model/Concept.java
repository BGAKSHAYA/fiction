package org.ovgu.de.fiction.model;

import java.util.List;
import java.util.Map;

public class Concept {

	List<Word> words;
	int numOfSentencesPerBook;
	Map<String, Integer> characterMap;
	boolean protaganist;

	public List<Word> getWords() {
		return words;
	}

	public void setWords(List<Word> words) {
		this.words = words;
	}

	public Map<String, Integer> getCharacterMap() {
		return characterMap;
	}

	public void setCharacterMap(Map<String, Integer> characterMap) {
		this.characterMap = characterMap;
	}

	public int getNumOfSentencesPerBook() {
		return numOfSentencesPerBook;
	}

	public void setNumOfSentencesPerBook(int numOfSentencesPerBook) {
		this.numOfSentencesPerBook = numOfSentencesPerBook;
	}
	
	Map<String, List<Integer>> detailedCharacterMap;

	public boolean isProtaganist() {
		return protaganist;
	}

	public void setProtaganist(boolean protaganist) {
		this.protaganist = protaganist;
	}

	public Map<String, List<Integer>> getDetailedCharacterMap() {
		return detailedCharacterMap;
	}

	public void setDetailedCharacterMap(Map<String, List<Integer>> detailedCharacterMap) {
		this.detailedCharacterMap = detailedCharacterMap;
	}

}
