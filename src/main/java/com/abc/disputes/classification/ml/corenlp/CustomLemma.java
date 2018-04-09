package com.abc.disputes.classification.ml.corenlp;

import opennlp.tools.lemmatizer.Lemmatizer;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * Shamelessly copied from DictionaryLemmatizer to change the method sign to public and for
 * custom file format.
 */
public class CustomLemma implements Lemmatizer {

    /**
     * The hashmap containing the dictionary.
     */
    private final Map<List<String>, List<String>> dictMap = new HashMap<>();

    /**
     * Construct a hashmap from the input tab separated dictionary.
     *
     * The input file should have, for each line, word\tabpostag\tablemma.
     * Alternatively, if multiple lemmas are possible for each word,postag pair,
     * then the format should be word\tab\postag\tablemma01#lemma02#lemma03
     *
     * @param dictionary
     *          the input dictionary via inputstream
     */
    public CustomLemma(final InputStream dictionary) throws IOException {
        init(dictionary);
    }

    public CustomLemma(File dictionaryFile) throws IOException {
        try (InputStream in = new FileInputStream(dictionaryFile)) {
            init(in);
        }
    }

    public CustomLemma(Path dictionaryFile) throws IOException {
        this(dictionaryFile.toFile());
    }

    private void init(InputStream dictionary) throws IOException {
        final BufferedReader breader = new BufferedReader(
                new InputStreamReader(dictionary));
        String line;
        while ((line = breader.readLine()) != null) {
            final String[] elems = line.split("\t");
            final String[] lemmas = elems[2].split("#");
            this.dictMap.put(Arrays.asList(elems[0], elems[1]), Arrays.asList(lemmas));
        }
    }
    /**
     * Get the Map containing the dictionary.
     *
     * @return dictMap the Map
     */
    public Map<List<String>, List<String>> getDictMap() {
        return this.dictMap;
    }

    /**
     * Get the dictionary keys (word and postag).
     *
     * @param word
     *          the surface form word
     * @param postag
     *          the assigned postag
     * @return returns the dictionary keys
     */
    private List<String> getDictKeys(final String word, final String postag) {
        final List<String> keys = new ArrayList<>();
        keys.addAll(Arrays.asList(word.toLowerCase(), postag));
        return keys;
    }


    public String[] lemmatize(final String[] tokens, final String[] postags) {
        List<String> lemmas = new ArrayList<>();
        for (int i = 0; i < tokens.length; i++) {
            lemmas.add(this.lemmatize(tokens[i], postags[i]));
        }
        return lemmas.toArray(new String[lemmas.size()]);
    }

    public List<List<String>> lemmatize(final List<String> tokens, final List<String> posTags) {
        List<List<String>> allLemmas = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            allLemmas.add(this.getAllLemmas(tokens.get(i), posTags.get(i)));
        }
        return allLemmas;
    }

    /**
     * Lookup lemma in a dictionary. Outputs "O" if not found.
     *
     * @param word
     *          the token
     * @param postag
     *          the postag
     * @return the lemma
     */
    public String lemmatize(final String word, final String postag) {
        String lemma;
        final List<String> keys = this.getDictKeys(word, postag);
        // lookup lemma as value of the map
        final List<String> keyValues = this.dictMap.get(keys);
        if ( keyValues != null && !keyValues.isEmpty()) {
            lemma = keyValues.get(0);
        } else {
            lemma = word;
        }
        return lemma;
    }

    /**
     * Lookup every lemma for a word,pos tag in a dictionary. Outputs "O" if not
     * found.
     *
     * @param word
     *          the token
     * @param postag
     *          the postag
     * @return every lemma
     */
    private List<String> getAllLemmas(final String word, final String postag) {
        List<String> lemmasList = new ArrayList<>();
        final List<String> keys = this.getDictKeys(word, postag);
        // lookup lemma as value of the map
        final List<String> keyValues = this.dictMap.get(keys);
        if (keyValues != null && !keyValues.isEmpty()) {
            lemmasList.addAll(keyValues);
        } else {
            lemmasList.add(word);
        }
        return lemmasList;
    }
}