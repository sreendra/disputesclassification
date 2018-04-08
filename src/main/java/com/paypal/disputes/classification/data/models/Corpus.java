package com.paypal.disputes.classification.data.models;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Corpus {

    private Map<String,Integer> keyColIdMap;
    private List<DocumentRow> rows;
    private int index;

    private static Corpus instance = new Corpus();

    private Corpus() {
        rows = new ArrayList<>();
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
        rows.add(row);

    }


}
