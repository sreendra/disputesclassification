package com.paypal.disputes.classification.data.models;

import java.util.HashMap;
import java.util.Map;

public class DocumentRow {

    private String maxFreqTerm;
    private Map<String,Integer> termFreqMap= new HashMap<>();

    public void addDocumentTerm(String term,Integer termFrequency) {

        Integer frequency = termFreqMap.get(term);
        frequency = (frequency == null ? 0 :frequency )+termFrequency ;

        maxFreqTerm =  maxFreqTerm == null  ? term :
                !maxFreqTerm.equals(term)  && frequency > termFreqMap.get(maxFreqTerm)  ? term : maxFreqTerm;

        termFreqMap.put(term,frequency);
    }

    public Map<String, Integer> getTermFreqMap() {
        return termFreqMap;
    }

    @Override
    public String toString() {
        return "DocumentRow{" +
                "maxFreqTerm=" + maxFreqTerm +
                ", termFreqMap=" + termFreqMap +
                '}';
    }
}
