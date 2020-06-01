package org.ovgu.de.fiction.feature.extraction;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
	public Concept generateWordAttributes(Path path, String locale) {

		FeatureExtractorUtility feu = new FeatureExtractorUtility();
		Concept cncpt = new Concept();
		LOG.info(path.toString());
		Annotation document = new Annotation(FRFileOperationUtils.readFile(path.toString()));
		
		String charStopWord = FRConstants.CHARACTER_STOPWORD_REGEX;
		if(locale.equals(FRConstants.DE)) {
			StanfordPipeline.getGermanPipeline().annotate(document);
			charStopWord = FRConstants.CHARACTER_STOPWORD_REGEX_DE;
		}
		else {
			StanfordPipeline.getPipeline(null).annotate(document);			
		}
		
		List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
		List<Word> tokenList = new ArrayList<>();
		Map<String, Integer> charMap = new HashMap<>(); // a new object per new
														 // book // Book
		StringBuffer charName = new StringBuffer();
		int numOfSyllables = 0;
		int numOfSentences =0;
		double narratorCount = 0;
		Set<String> familyTreeSet = new HashSet<String>();
		Map<String, List<Integer>> characterMap = new HashMap<>();
		
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
				
				//String originalInLowerCase = original.toLowerCase();
				if(original.matches(FRConstants.FIRST_PERSON_REGEX)) {
					narratorCount++;
				}
				
				/*
				 * logic 2: check if ner is "P", then further check next 2 element in sentence , ex.
				 * Tom Cruise, Mr. Tom Cruise if yes, then concatenate all two or three tokens i.e.
				 * "Mr" +"Tom" + "Cruise" into a single token the single concatenated token is added
				 * to a Map , where key is number of times "Mr. Tom Cruise" appears
				 */
				if (ner.equals(FRConstants.NER_CHARACTER) && !original.matches(charStopWord)) {
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
			
			if(!sentence.toString().isEmpty()) {
				calculateFamilyLength(sentence.toString().toLowerCase(), familyTreeSet, locale);
			}

//			if (!sentence.toString().isEmpty()) {
//				Annotation corefAnnotation = new Annotation(sentence.toString());
//				corefPipeline.annotate(corefAnnotation);
//				processCorefChains(corefAnnotation, characterMap);
//			}
		}
		
		LOG.info(familyTreeSet);
		LOG.info("narratorCount: " + narratorCount + " tokenList.size(): " + tokenList.size() + " familyTreeSet.size(): " + familyTreeSet.size() + " narrator ratio: " + (narratorCount/tokenList.size()));
		boolean found = executeCharacterScriptAndFindMainCharater(sentences, narratorCount, tokenList.size(), familyTreeSet.size());
		
		//findMainCharacter(characterMap, narratorCount,tokenList.size());
		
		cncpt.setWords(tokenList);
		cncpt.setCharacterMap(feu.getUniqueCharacterMap(charMap));
		cncpt.setNumOfSentencesPerBook(numOfSentences);
		cncpt.setProtaganist(found);
		StanfordPipeline.resetPipeline();
		return cncpt;
	}

	private void calculateFamilyLength(String sentence, Set<String> familyTreeSet, String locale) {
		
		String father = FRConstants.FATHER;
		String mother = FRConstants.MOTHER;
		String sister = FRConstants.SISTER;
		String brother = FRConstants.BROTHER;
		String son = FRConstants.SON;
		String daughter = FRConstants.DAUGHTER;
		String uncle = FRConstants.UNCLE;
		String aunty = FRConstants.AUNTY;
		String man = FRConstants.MAN;
		String husband = FRConstants.HUSBAND;
		String wife = FRConstants.WIFE;
		String child = FRConstants.CHILD;
		String gurdian = FRConstants.GUARDIAN;
		String boy = FRConstants.BOY;
		String girl = FRConstants.GIRL;
		
		if(locale.equals(FRConstants.DE)) {
			father = FRConstants.FATHER_DE;
			mother = FRConstants.MOTHER_DE;
			sister = FRConstants.SISTER_DE;
			brother = FRConstants.BROTHER_DE;
			son = FRConstants.SON_DE;
			daughter = FRConstants.DAUGHTER_DE;
			uncle = FRConstants.UNCLE_DE;
			aunty = FRConstants.AUNTY_DE;
			man = FRConstants.MAN_DE;
			husband = FRConstants.HUSBAND_DE;
			wife = FRConstants.WIFE_DE;
			child = FRConstants.CHILD_DE;
			gurdian = FRConstants.GUARDIAN_DE;
			boy = FRConstants.BOY_DE;
			girl = FRConstants.GIRL_DE;			
		}
		
		if(sentence.contains(father)) {
			familyTreeSet.add(father);
		}
		if(sentence.contains(mother)) {
			familyTreeSet.add(mother);
		}
		if(sentence.contains(sister)) {
			familyTreeSet.add(sister);
		}
		if(sentence.contains(brother)) {
			familyTreeSet.add(brother);			
		}
		if(sentence.contains(son)) {
			familyTreeSet.add(son);	
		}
		if(sentence.contains(daughter)) {
			familyTreeSet.add(daughter);	
		}
		if(sentence.contains(uncle)) {
			familyTreeSet.add(uncle);
		}
		if(sentence.contains(aunty)) {
			familyTreeSet.add(aunty);	
		}
		if(sentence.contains(man)) {
			familyTreeSet.add(man);	
		}
		if(sentence.contains(husband)) {
			familyTreeSet.add(husband);	
		}
		if(sentence.contains(wife)) {
			familyTreeSet.add(wife);	
		}
		if(sentence.contains(child)) {
			familyTreeSet.add(child);	
		}
		if(sentence.contains(gurdian)) {
			familyTreeSet.add(gurdian);	
		}
		if(sentence.contains(boy)) {
			familyTreeSet.add(boy);	
		}
		if(sentence.contains(girl)) {
			familyTreeSet.add(girl);		
		}
	}

	//Execute the python script to find the most two common characters
	//and there appearance count in the book
	private boolean executeCharacterScriptAndFindMainCharater(List<CoreMap> sentences, double narratorCount, int noOfTokens, int familyTreeLength) {
		boolean found = FeatureExtractorUtility.getCharacters(sentences);
		
		double narratorRatio = (narratorCount/noOfTokens);
		if(found || narratorRatio > 0.04 || familyTreeLength >= 6) {
			return true;
		}
		return false;
		
	}

	//Find main character from the character map and first person words count using coref annotation
	//Not using because of the performance issue
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
							createCharacterMap(character.toString().trim(), characterMap, mention);
						}						
					}
				}
				// increase the count based on clusterid
				else if (mention.mentionType.equals(MentionType.PRONOMINAL)) {
				}
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
