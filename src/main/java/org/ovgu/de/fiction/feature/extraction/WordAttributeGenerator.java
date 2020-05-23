package org.ovgu.de.fiction.feature.extraction;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ovgu.de.fiction.model.Concept;
import org.ovgu.de.fiction.model.Word;
import org.ovgu.de.fiction.utils.FRConstants;
import org.ovgu.de.fiction.utils.FRFileOperationUtils;
import org.ovgu.de.fiction.utils.FRGeneralUtils;
import org.ovgu.de.fiction.utils.StanfordPipeline;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.coref.data.Dictionaries.Gender;
import edu.stanford.nlp.coref.data.Dictionaries.MentionType;
import edu.stanford.nlp.ie.KBPRelationExtractor.NERTag;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * @author Suhita, Sayantan
 * @version - Changes for sentiment features
 */
public class WordAttributeGenerator {
	
	final static Logger LOG = Logger.getLogger(WordAttributeGenerator.class);

	private static final String NNP = "NNP";

	/**
	 * @param path
	 *            = path of whole book, not chunk
	 * @return = List of "Word" objects, each object has original token, POS tag, lemma, NER as
	 *         elements
	 * @author Suhita, Modified by Sayantan for # of characters
	 */
	public Concept generateWordAttributes(Path path) {

		FeatureExtractorUtility feu = new FeatureExtractorUtility();
		Concept cncpt = new Concept();
		Annotation document = new Annotation(FRFileOperationUtils.readFile(path.toString()));

		StanfordPipeline.getPipeline(null).annotate(document);
		List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
		List<Word> tokenList = new ArrayList<>();
		Map<String, Integer> charMap = new HashMap<>(); // a new object per new
														 // book // Book
		StringBuffer charName = new StringBuffer();
		int numOfSyllables = 0;
		int numOfSentences =0;
		double narratorCount = 0;
		Map<String, List<Integer>> characterMap = new HashMap<>();
		
		Properties properties = new Properties();
		properties.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref, sentiment");
		properties.put("ner.applyFineGrained", "false");	
		StanfordCoreNLP corefPipeline = new StanfordCoreNLP(properties);
		
		for (CoreMap sentence : sentences) {
			//LOG.info("sentence " + sentence);// this loop will iterate each of the sentences
			tokenList.add(new Word(FRConstants.S_TAG, FRConstants.S_TAG, null, null, 0));
			numOfSentences++;
			for (CoreLabel cl : sentence.get(CoreAnnotations.TokensAnnotation.class)) {// this
																						// loop
																						// iterates
																						// each
																						// token
																						// of
																						// a
																						// sentence

				String original = cl.get(CoreAnnotations.OriginalTextAnnotation.class);
				String pos = cl.get(CoreAnnotations.PartOfSpeechAnnotation.class);
				String ner = cl.get(CoreAnnotations.NamedEntityTagAnnotation.class);
				String lemma = cl.get(CoreAnnotations.LemmaAnnotation.class).toLowerCase();
				
				String originalInLowerCase = original.toLowerCase();
				if(originalInLowerCase.equals("i") || originalInLowerCase.equals("my") || originalInLowerCase.equals("mine") ||  originalInLowerCase.equals("me")) {
					narratorCount++;
				}
				/*
				 * logic 2: check if ner is "P", then further check next 2 element in sentence , ex.
				 * Tom Cruise, Mr. Tom Cruise if yes, then concatenate all two or three tokens i.e.
				 * "Mr" +"Tom" + "Cruise" into a single token the single concatenated token is added
				 * to a Map , where key is number of times "Mr. Tom Cruise" appears
				 */
				if (ner.equals(FRConstants.NER_CHARACTER) && !original.matches(FRConstants.CHARACTER_STOPWORD_REGEX)) {
					if (charName.length() == 0)
						charName.append(original.toLowerCase());
					else
						charName.append(FRConstants.SPACE).append(original.toLowerCase());
				} else if (!ner.equals(FRConstants.NER_CHARACTER) && charName.length() != 0) {

					// calculate for character
					numOfSyllables = FRGeneralUtils.countSyllables(charName.toString().toLowerCase());
					addToTokenList(tokenList, charName.toString(), NNP, FRConstants.NER_CHARACTER, charName.toString(), numOfSyllables);

					String charNameStr = charName.toString().replaceAll(FRConstants.REGEX_ALL_PUNCTUATION, " ")
							.replaceAll(FRConstants.REGEX_TRAILING_SPACE, "");
					int count = charMap.containsKey(charNameStr) ? charMap.get(charNameStr) : 0;
					charMap.put(charNameStr, count + 1);

					// add the next word after character
					addToTokenList(tokenList, original, pos, ner, lemma, FRGeneralUtils.countSyllables(original.toLowerCase()));
					// rest the string buffer
					charName = new StringBuffer();

				} else {
					addToTokenList(tokenList, original, pos, ner, lemma, FRGeneralUtils.countSyllables(original.toLowerCase()));
				}

			}

			//LOG.info("sentence" + sentence);
//			if (!sentence.toString().isEmpty()) {
//				Annotation corefAnnotation = new Annotation(sentence.toString());
//				corefPipeline.annotate(corefAnnotation);
//				// corefAnnotation = COREF_PIPELINE.process(sentence);
//				processCorefChains(corefAnnotation, characterMap);
//			}
		}
		
		boolean found = findMainCharacter(characterMap, narratorCount,tokenList.size());
		
		cncpt.setWords(tokenList);
		cncpt.setCharacterMap(feu.getUniqueCharacterMap(charMap));
		cncpt.setNumOfSentencesPerBook(numOfSentences);
		cncpt.setProtaganist(found);
		StanfordPipeline.resetPipeline();
		return cncpt;
	}

	//Find main character from the character map and narator count
	private boolean findMainCharacter(Map<String, List<Integer>> characterMap, double narratorCount, int noOfTokens) {
		Comparator<Entry<String, List<Integer>>> valueComparator = new Comparator<Entry<String, List<Integer>>>() {

			public int compare(Entry<String, List<Integer>> o1, Entry<String, List<Integer>> o2) {
				Integer a = o1.getValue().get(0);
				Integer b = o2.getValue().get(0);
				return b.compareTo(a);
			}
		};

		Set<Entry<String, List<Integer>>> entries = characterMap.entrySet();
		List<Entry<String, List<Integer>>> listOfEntries = new ArrayList<Entry<String, List<Integer>>>(entries);

		Collections.sort(listOfEntries, valueComparator);
		LinkedHashMap<String, List<Integer>> sortedByValue = new LinkedHashMap<String, List<Integer>>(
				listOfEntries.size());
		
		int one = 1;
		int two = 1;
		int count=1;
		for (Entry<String, List<Integer>> entry : listOfEntries) {
			sortedByValue.put(entry.getKey(), entry.getValue());
			LOG.info(entry.getKey());
			LOG.info(entry.getValue());
			if(count==1) {
				one = entry.getValue().get(0);
			}
			if(count==2) {
				two = entry.getValue().get(0);
			}
			count++;
		}
		
		LOG.info("i count ---- " + narratorCount);
		LOG.info("token count ---- " + noOfTokens);
		//Rule for protagonist - not yet confirmed
		int characterDifference = one - two;
		double narratorRatio = (narratorCount/noOfTokens);
		if(characterDifference > 50 || narratorRatio > 0.04) {
			return true;
		}
		return false;
	}

	public void addToTokenList(List<Word> tokenList, String original, String pos, String ner, String lemma, int numOfSyllbles) {
		if (lemma.matches("^'.*[a-zA-Z]$")) { // 's o'clock 'em
			StringBuffer sbf = new StringBuffer();
			Arrays.stream(lemma.split("'")).forEach(l -> sbf.append(l));
			tokenList.add(new Word(original, sbf.toString(), pos, ner, numOfSyllbles));
		} // mr. mrs.
		else if (lemma.matches("[a-zA-Z0-9].*[.].*") && ner.matches("(O|MISC)")) {
			tokenList.add(new Word(original, lemma.split("\\.")[0], pos, ner, numOfSyllbles));
		} else {
			tokenList.add(new Word(original, lemma, pos, ner, numOfSyllbles));
		}
	}
	
	private void processCorefChains(Annotation corefAnnotation, Map<String, List<Integer>> characterMap) {
		// Find characters and get coreference information
		Map<Integer, CorefChain> corefChains = corefAnnotation.get(CorefCoreAnnotations.CorefChainAnnotation.class);
		if (corefChains == null) {
			return;
		}
		List<CoreMap> sentences = corefAnnotation.get(CoreAnnotations.SentencesAnnotation.class);
		for (Map.Entry<Integer, CorefChain> entry : corefChains.entrySet()) {
			for (CorefChain.CorefMention mention : entry.getValue().getMentionsInTextualOrder()) {
				List<CoreLabel> tokens = sentences.get(mention.sentNum - 1).get(CoreAnnotations.TokensAnnotation.class);
				if (mention.mentionType.equals(MentionType.PROPER)) {
					List<CoreLabel> sublist = tokens.subList(mention.startIndex - 1, mention.endIndex - 1);
					if (sublist == null || sublist.isEmpty()) {
						continue;
					}
					// only name present, add it to charactermap
					String ner = null;
					if (sublist.size() == 1) {
						ner = sublist.get(0).get(CoreAnnotations.NamedEntityTagAnnotation.class);
						if (isValidNer(ner) && Character.isUpperCase(sublist.get(0).originalText().charAt(0))) {
							String name = sublist.get(0).originalText().toLowerCase();
							createCharacterMap(name, characterMap, mention);							
						}
					} else {
						// multiple mentions present, check for maximum length 3, Mr Joe, others are invalid identifications
						if(sublist.size()<=3) {
							StringBuffer character = new StringBuffer();
							for (CoreLabel token : sublist) {
								if (token.get(CoreAnnotations.NamedEntityTagAnnotation.class).equals(String.valueOf(NERTag.PERSON))) {
									character.append(token.originalText().toLowerCase() + " ");
								}
							}
							createCharacterMap(character.toString().stripTrailing(), characterMap, mention);
						}						
					}
				}
				// increase the count based on clusterid
//				else if (mention.mentionType.equals(MentionType.PRONOMINAL)) {
//				}
			}
		}
	}

	private void createCharacterMap(String name, Map<String, List<Integer>> characterMap, CorefMention mention) {
		if(name.isEmpty() || name.matches(FRConstants.CHARACTER_STOPWORD_REGEX)) {
			return;
		}
		if (characterMap.containsKey(name)) {
			List<Integer> attributeList = characterMap.get(name);
			// index 0 - count
			// index 1 - corefclusterid
			// index 2 - gender, if male=1, female=2, unknown=3
			int count = attributeList.get(0);
			count++;
			List<Integer> newAttributeList = new ArrayList<Integer>();
			newAttributeList.add(0, count);
			newAttributeList.add(1, mention.corefClusterID);
			if (attributeList.get(2) == 3 && !(mention.gender.equals(Gender.UNKNOWN)|| mention.gender.equals(Gender.NEUTRAL))) {
				if (mention.gender.equals(Gender.FEMALE)) {
					newAttributeList.add(2, 2);
				} else {
					newAttributeList.add(2, 1);
				}
			}
			else {
				newAttributeList.add(2, 3);
			}
			//LOG.info("name --- " + name);
			//LOG.info("attributeList --- " + newAttributeList);
			characterMap.put(name, newAttributeList);
		} else {
			addToCharacterMap(name, mention, characterMap);
		}
	}

	private void addToCharacterMap(String name, CorefMention mention, Map<String, List<Integer>> characterMap) {
		List<Integer> attributeList = new ArrayList<Integer>();
		attributeList.add(0, 1);
		attributeList.add(1, mention.corefClusterID);
		if (mention.gender.equals(Gender.UNKNOWN) || mention.gender.equals(Gender.NEUTRAL)) {
			attributeList.add(2, 3);
		} else if (mention.gender.equals(Gender.FEMALE)) {
			attributeList.add(2, 2);
		} else {
			attributeList.add(2, 1);
		}
		//LOG.info("name --- " + name);
		//LOG.info("attributeList --- " + attributeList);
		characterMap.put(name, attributeList);
	}

	private boolean isValidNer(String ner) {
		if (ner == null) {
			return false;
		}
		if (ner == String.valueOf(NERTag.PERSON) || ner.equals(FRConstants.O)) {
			return true;
		}
		return false;
	}


}
