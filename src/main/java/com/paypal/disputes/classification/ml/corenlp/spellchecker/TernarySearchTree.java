package com.paypal.disputes.classification.ml.corenlp.spellchecker;

import static com.paypal.common.utils.MLConstants.MAX_EDIT_DISTANCE;
import static com.paypal.common.utils.MLConstants.MAX_SUGGESTIONS;
import static com.paypal.common.utils.MLConstants.WORD_FREQ_MAP;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.control.Try;

/**
 * Ternary Search Tree for Edit distance usage.
 *
 */
public class TernarySearchTree {

	private static Logger logger = LoggerFactory.getLogger(TernarySearchTree.class);

	private TernaryNode root;
	private static TernarySearchTree instance = new TernarySearchTree();


	private TernarySearchTree(){
		buildTree();
	}

	private void buildTree() {

		long startTime = System.currentTimeMillis();

		long totalRows =Try.of(() -> Files.lines(Paths.get(this.getClass().getClassLoader().getResource(WORD_FREQ_MAP).getPath())).
				filter(keyValuePair -> !keyValuePair.isEmpty()).
				map(line -> {
					insert(line.split("\t")[0].trim(),line.split("\t")[1].trim());
					return 1;
				}).count()).get();

		logger.info("Added {} rows in {} ms",totalRows, (System.currentTimeMillis() - startTime));
	}

	public static TernarySearchTree getInstance() {
		return instance;
	}

	public void insert(String word, String frequency) {
		root = insert(root, word, Long.parseLong(frequency), 0);
	}

	private TernaryNode insert(TernaryNode ternaryNode, String word, Long frequency, int charIndex) {

		if (ternaryNode == null) {

			if (charIndex >= word.length())
				return ternaryNode;

			ternaryNode = new TernaryNode(word.charAt(charIndex));

			if (charIndex == word.length() - 1) {
				ternaryNode.setIsWordCharAndFrequency(frequency,true);
				return ternaryNode;
			}
		}

		if (word.charAt(charIndex) < ternaryNode.data) {
			ternaryNode.setLeft(insert(ternaryNode.getLeft(), word, frequency, charIndex));
		} else if (word.charAt(charIndex) > ternaryNode.data) {
			ternaryNode.setRight(insert(ternaryNode.getRight(), word, frequency, charIndex));
		} else {

			if (charIndex  < word.length() -1 )
				ternaryNode.setEqual(insert(ternaryNode.getEqual(), word, frequency, charIndex + 1));
			else
				ternaryNode.setIsWordCharAndFrequency(frequency,true);

		}
		return ternaryNode;
	}

	public boolean containsWord(String searchWord) {

		return searchWord(root,searchWord,0) != null;
	}

	private TernaryNode searchWord(TernaryNode root, String searchWord, int charIndex) {

		if(root == null)
			return root;

		if(root.isWordChar() && charIndex == searchWord.length() - 1 && searchWord.charAt(charIndex) == root.data)
			return root;

		return searchWord.charAt(charIndex) < root.data ? searchWord(root.getLeft(),searchWord,charIndex) :
				searchWord.charAt(charIndex) > root.data ? searchWord(root.getRight(),searchWord,charIndex) :
						searchWord(root.getEqual(),searchWord,charIndex+1);

	}

	/**
	 *
	 * Its basically a M * N matrix.
	 * M =row length = (word1.length+1)
	 * N =Col length = (word2.length+1)
	 *
	 * To save memory, we take only previous row foot print and prev col value.
	 *
	 * We assume first char of word1 and word2 to be empty.
	 *
	 * The first row is formed assuming first char of word1 is empty and similarly first column.
	 *
	 * Formula is matrix[rowIndex][colIndex] = minimum(matrix[rowIndex-1,colIndex]+1,matrix[rowIndex,colIndex-1]+1, matrix[rowIndex-1,colIndex-1] + (word1[rowIndex] == word2[colIndex] ? 0:1))
	 *
	 * If word1 = abc and word2 = xa
	 * Matrix would be
	 * <pre>
	 * -------------------
	 * |   | ''|  x | a  |
	 * -------------------
	 * | ''| 0 |  1 | 2  |
	 * -------------------
	 * | a | 1 |  1 | 1  |
	 * -------------------
	 * | b | 2 |  2 | 2  |
	 * -------------------
	 * | c | 3 |  3 | 3  |
	 * -------------------
	 * </pre>
	 *
	 * @param word1
	 * @param word2
	 * @return
	 */
	private int getLevenshteinDistance(String word1, String word2) {


		int[] prevRowDistances = new int[1 + word2.length() ];

		for (int colIndex = 0; colIndex < prevRowDistances.length; colIndex++)
			prevRowDistances[colIndex] = colIndex;

		for (int rowIndex = 1; rowIndex <= word1.length(); rowIndex++) {

			prevRowDistances[0] = rowIndex;

			int prevDiagnolValue = rowIndex - 1;

			for (int colIndex = 1; colIndex <= word2.length(); colIndex++) {

				int distance = Math.min(1 + Math.min(prevRowDistances[colIndex], prevRowDistances[colIndex - 1]),
						word1.charAt(rowIndex - 1) == word2.charAt(colIndex - 1) ? prevDiagnolValue : prevDiagnolValue + 1);

				prevDiagnolValue = prevRowDistances[colIndex];
				prevRowDistances[colIndex] = distance;
			}
		}

		return prevRowDistances[word2.length()];
	}

	public List<String> getSuggestions(String inCorrectWord) {

		List<NodeWrapper> suggestedWords = new ArrayList<>();

		traverseTree(root,inCorrectWord,"",MAX_EDIT_DISTANCE,suggestedWords);

		logger.info("Incorrect word is {}, suggested words are {} and size is {}", inCorrectWord,suggestedWords,suggestedWords.size());

		List<String> minEditDistanceWords = suggestedWords.stream().
				sorted(comparing(NodeWrapper::getEditDistance).thenComparing(comparing(NodeWrapper::getFrequency).reversed())).
				limit(MAX_SUGGESTIONS).
				map(NodeWrapper::getWord).
				collect(toList());

		if(minEditDistanceWords.isEmpty())
			minEditDistanceWords.add(inCorrectWord);

		return minEditDistanceWords;
	}

	private void traverseTree(TernaryNode root, String inCorrectWord, String suggestedWordPrefix, int maxEditDistance, List<NodeWrapper> suggestedWords) {

		if (root == null)
			return;

		//If  Suggested word is less than wrong word & if the distance is greater than maxEditDistance for smaller inCorrectWord &
		// Suggested word is greater than input word by maxEditDistance, we ignore those words.
		if (
				(suggestedWordPrefix.length() < inCorrectWord.length() &&
						getLevenshteinDistance( inCorrectWord.substring(0, suggestedWordPrefix.length() + 1),suggestedWordPrefix) > maxEditDistance
				) ||
				suggestedWordPrefix.length() > inCorrectWord.length() + maxEditDistance  )

			return;

		int distance = getLevenshteinDistance(inCorrectWord, suggestedWordPrefix + root.data);

		if(Math.abs(suggestedWordPrefix.length() - inCorrectWord.length()) <= maxEditDistance && distance > maxEditDistance	)
			return;


		// recursively traverse through the nodes for words
		traverseTree(root.getLeft(), inCorrectWord,suggestedWordPrefix,maxEditDistance,suggestedWords);

		if (root.isWordChar() && distance <= maxEditDistance)
			suggestedWords.add(new NodeWrapper(suggestedWordPrefix + root.data, distance, root.getFrequency()));

		traverseTree(root.getEqual(), inCorrectWord,suggestedWordPrefix + root.data,maxEditDistance,suggestedWords);
		traverseTree(root.getRight(), inCorrectWord,suggestedWordPrefix,maxEditDistance,suggestedWords);
	}

	public static final class NodeWrapper {

		private   String word ;
		private  int editDistance;
		private  Long frequency ;

		public NodeWrapper(String word, int editDistance, Long frequency) {
			this.word = word;
			this.editDistance = editDistance;
			this.frequency = frequency;
		}

		public String getWord() {
			return word;
		}

		public int getEditDistance() {
			return editDistance;
		}

		public Long getFrequency() {
			return frequency;
		}

		@Override
		public String toString() {
			return "NodeWrapper{" +
					"word='" + word + '\'' +
					", editDistance=" + editDistance +
					", frequency=" + frequency +
					'}';
		}
	}

	public static void main(String[] args) {
		TernarySearchTree tree = TernarySearchTree.getInstance();
		System.out.println("Tree is "+tree);
	}

}
