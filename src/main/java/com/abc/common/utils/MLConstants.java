package com.abc.common.utils;

/**
 * Created by rachikkala on 2/6/18.
 */
public class MLConstants {

    public static final String UNDERSCORE = "_";
    public static final String HYPHEN = "-";
    public static final String SPACE = " ";

    public static final String NLP_MODELS_FOLDER ="opennlp/models/";
    public static final String SENTENCE_MODEL ="en-sent.bin";
    public static final String TOKEN_MODEL ="en-token.bin";

    public static final String NER_PERSON_MODEL ="en-ner-person.bin";

    public static final String NER_LOC_MODEL ="en-ner-location.bin";
    public static final String NER_ORG_MODEL ="en-ner-organization.bin";

    public static final String NER_MONEY_MODEL ="en-ner-money.bin";
    public static final String NER_PERCENT_MODEL ="en-ner-percentage.bin";

    public static final String NER_TIME_MODEL ="en-ner-date.bin";
    public static final String NER_DATE_MODEL ="en-ner-time.bin";

    public static final String POS_MODEL ="en-pos-maxent.bin";
    public static final String LEMMATIZER_MODEL ="en-lemmatizer.txt";

    public static final String CHUNKER_MODEL ="en-chunker.bin";


    public static final String CONTRACTIONS_FILE ="contractions_map.txt";
    public static final String STOPWORDS_FILE ="stop-words_en.txt";
    public static final String WORD_FREQ_MAP ="word_freq_en.txt";

    public static final int MAX_EDIT_DISTANCE =3;
    public static final int MAX_SUGGESTIONS =10;
    public static final int MIN_SVM_SEED_SET =5;

    public static final double TAU =1E-12;
    public static final double TOLERANCE =1E-3;


}
