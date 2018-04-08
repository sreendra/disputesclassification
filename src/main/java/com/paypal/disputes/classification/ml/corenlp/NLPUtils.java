package com.paypal.disputes.classification.ml.corenlp;

import io.vavr.Tuple;
import io.vavr.control.Try;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.paypal.common.utils.MLConstants.*;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;


public class NLPUtils {

    private Logger logger = LoggerFactory.getLogger(NLPUtils.class);

    public static final SentenceDetectorME sentenceDetector;
    public static final TokenizerME tokenDetector;

    public static final NameFinderME personFinder;
    public static final NameFinderME locationFinder;
    public static final NameFinderME orgFinder;

    public static final NameFinderME moneyFinder;
    public static final NameFinderME percentFinder;
    public static final NameFinderME dateFinder;
    public static final NameFinderME timeFinder;


    public static final POSTaggerME tagger;
    public static final CustomLemma lemmatizer;

    public static final Map<String,String> contractionsExpansionsMap;
    public static final Set<String> stopWords;



    static {
        sentenceDetector = Try.of(()  ->  new SentenceDetectorME(new SentenceModel(getFilePath(NLP_MODELS_FOLDER+ SENTENCE_MODEL)))).get();
        tokenDetector = Try.of(()  ->  new TokenizerME(new TokenizerModel(getFilePath(NLP_MODELS_FOLDER+ TOKEN_MODEL)))).get();

        personFinder = Try.of(() -> new NameFinderME( new TokenNameFinderModel(getFilePath(NLP_MODELS_FOLDER+NER_PERSON_MODEL)))).get();
        locationFinder = Try.of(() -> new NameFinderME( new TokenNameFinderModel(getFilePath(NLP_MODELS_FOLDER+NER_LOC_MODEL)))).get();
        orgFinder = Try.of(() -> new NameFinderME( new TokenNameFinderModel(getFilePath(NLP_MODELS_FOLDER+NER_ORG_MODEL)))).get();

        moneyFinder = Try.of(() -> new NameFinderME( new TokenNameFinderModel(getFilePath(NLP_MODELS_FOLDER+NER_MONEY_MODEL)))).get();
        percentFinder = Try.of(() -> new NameFinderME( new TokenNameFinderModel(getFilePath(NLP_MODELS_FOLDER+NER_PERCENT_MODEL)))).get();
        dateFinder = Try.of(() -> new NameFinderME( new TokenNameFinderModel(getFilePath(NLP_MODELS_FOLDER+NER_DATE_MODEL)))).get();
        timeFinder = Try.of(() -> new NameFinderME( new TokenNameFinderModel(getFilePath(NLP_MODELS_FOLDER+NER_TIME_MODEL)))).get();


        tagger = Try.of(() -> new POSTaggerME( new POSModel(getFilePath(NLP_MODELS_FOLDER+POS_MODEL)))).get();
        lemmatizer = Try.of(() -> new CustomLemma( getFilePath(NLP_MODELS_FOLDER+LEMMATIZER_MODEL))).get();
        contractionsExpansionsMap = getContractionsExpansionsMap();
        stopWords = getStopWords();
    }

    private static Map<String,String> getContractionsExpansionsMap() {

        return Try.of(() -> Files.lines(getFilePath(CONTRACTIONS_FILE))
                .filter(keyValuePair -> !keyValuePair.isEmpty())
                .map(keyValuePair -> Tuple.of(keyValuePair.split("=")[0].trim(),keyValuePair.split("=")[1].trim()))
                .collect(toMap(tuple -> tuple._1,tuple -> tuple._2)))
                .getOrElse(HashMap::new);

    }

    private static Set<String> getStopWords() {

        return Try.of(() -> Files.lines(getFilePath(STOPWORDS_FILE)).
                filter(keyValuePair -> !keyValuePair.isEmpty()).
                map(String::trim).
                collect(toSet())).
                getOrElse(HashSet::new);

    }

    public static String applyContractionExpansions( String document) {

        for(String key : contractionsExpansionsMap.keySet())
            document = document.replaceAll(key,contractionsExpansionsMap.get(key));

        return document.replaceAll(HYPHEN,SPACE);

    }

    public static Path getFilePath(final String filePath) {
        return Paths.get(NLPUtils.class.getClassLoader().getResource(filePath).getPath());
    }

    public static boolean isNumber(final String token) {
        return Try.of(() -> {
            Double.parseDouble(token);
            return true;
        } ).getOrElse(false);
    }

}
