package com.abc.disputes.classification.data.models;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Corpus {

    //https://en.wikipedia.org/wiki/Tf-idf
    private static final double TFIDF_SMOOTHING_FACTOR = 0.5;

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

}
