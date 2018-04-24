package com.abc.disputes.classification.data.models;


import io.vavr.control.Try;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.management.ImmutableDescriptor;

import static java.util.stream.Collectors.toList;

public class Corpus {

    //https://en.wikipedia.org/wiki/Tf-idf and some places i saw the factor set to 0.4. We'll
    //try both the options.
    private static final double TFIDF_SMOOTHING_FACTOR = 0.5;
    private Logger logger = LoggerFactory.getLogger(Corpus.class);

    private Map<String,Integer> keyColIdMap;
    private Map<Integer,List<DocumentRow>> disputeClassDocsMap;
    private int index;
    private int totalDocumentsInCorpus;

    private static Corpus instance = new Corpus();

    private Corpus() {
        disputeClassDocsMap = new HashMap<>();
        keyColIdMap= new HashMap<>();
    }

    public static Corpus getInstance() {
        return instance;
    }

    public void addDocument(DocumentRow row) {

        row.getTermFreqMap().keySet().stream().
                forEach(key ->
                 {
                    if(keyColIdMap.get(key) == null)
                        keyColIdMap.put(key,index++);
                 }
                );

        List<DocumentRow> documents = disputeClassDocsMap.get(row.disputeClass);
        documents = documents == null ? new ArrayList<>():documents;
        documents.add(row);
        disputeClassDocsMap.put(row.disputeClass,documents);
        totalDocumentsInCorpus++;
    }

    public int getNumberOfTerms() {
        return keyColIdMap.size();
    }

    public Map<String, Integer> getKeyColIdMap() {
        return keyColIdMap;
    }

    public Map<Integer, List<DocumentRow>> getDisputeClassDocsMap() {
        return disputeClassDocsMap;
    }

    public Map<String,Double> findTfIdfMap(DocumentRow row) {

        int maxTermFreqInDoc = row.getTermFreqMap().get(row.getMaxFreqTerm());
        Map<String,Integer> rowKeyFreqMap = row.getTermFreqMap();

        return rowKeyFreqMap.entrySet().stream().filter(entry -> keyColIdMap.containsKey(entry.getKey())).collect(Collectors.toMap(entry -> entry.getKey(), entry ->

              (TFIDF_SMOOTHING_FACTOR + (1-TFIDF_SMOOTHING_FACTOR) * (entry.getValue()/maxTermFreqInDoc) ) *
                    Math.log((double) totalDocumentsInCorpus / findNumOfDocsHavingTermTuple(entry.getKey()))
        ));
    }

    public Map<Integer,Double> findIndexTfIdfMap(DocumentRow row) {

        int maxTermFreqInDoc = row.getTermFreqMap().get(row.getMaxFreqTerm());
        Map<String,Integer> rowKeyFreqMap = row.getTermFreqMap();

        return rowKeyFreqMap.entrySet().stream().filter(entry -> keyColIdMap.containsKey(entry.getKey())).collect(Collectors.toMap(entry -> keyColIdMap.get(entry.getKey()), entry ->

                (TFIDF_SMOOTHING_FACTOR + (1-TFIDF_SMOOTHING_FACTOR) * (entry.getValue()/maxTermFreqInDoc) ) *
                        Math.log((double) totalDocumentsInCorpus / findNumOfDocsHavingTermTuple(entry.getKey()))
        ));
    }

    /**
     * IDF finding method.
     * @param term
     * @return
     */
    private Integer findNumOfDocsHavingTermTuple(String term) {

        return disputeClassDocsMap.keySet().stream().map(disputeClass ->
                disputeClassDocsMap.get(disputeClass).stream().map(documentRow -> documentRow.getTermFreqMap().containsKey(term) ? 1 :0).reduce(0,Integer::sum)
        ).reduce(0,Integer::sum);

    }

    public List<DisputeDocument> readTrainingDocs() {

        List<String> validSheets = Arrays.asList("bdoToInr","otherToInr","bdoToSnad","otherToSnad","bdoToUnauth");
        Map<String,Integer> sheetNameToClassMap = new HashMap<String,Integer>() {/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

		{
            put("bdoToInr",0);
            put("otherToInr",0);
            put("bdoToSnad",1);
            put("otherToSnad",1);
            put("bdoToUnauth",2);
        }
		};

        return Stream.of("may","jul","aug").
                map(month -> "train_data/"+month+"_disputes.xlsx").
                flatMap(fileName ->
                        Try.of( () ->StreamSupport.stream(WorkbookFactory.create(new File(this.getClass().getClassLoader().getResource(fileName).getFile())).spliterator(),false)).
                                onFailure(throwable -> logger.error("Exception while loading data",throwable)).get().
                                filter(sheet -> validSheets.contains(sheet.getSheetName())).
                                flatMap(sheet -> StreamSupport.stream(sheet.spliterator(),false)).
                                skip(1).
                                filter(row -> row.getCell(1) != null).
                                //limit(10).
                                map(row -> new DisputeDocument(row.getCell(1).toString(),sheetNameToClassMap.get(row.getSheet().getSheetName()))).
                                collect(toList()).stream()
                ).collect(toList());
    }

}
