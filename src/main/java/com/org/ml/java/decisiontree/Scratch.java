package com.org.ml.java.decisiontree;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.StringUtils;

import javaslang.Tuple2;

public class Scratch {

//    public static void main(String[] args) {
//
//        System.out.println(calculateEntropy(1,6));
//
//    }

    public static Tuple2<Integer, Integer> getProbability(List<String> attributex, List<String> attributey, String[] attributeValues) {
    	Tuple2<Integer, Integer> finalVaues = null;
        Map<String, Long> attributesProb = new HashMap<String, Long>();
        if(attributey == null) {
            attributesProb = attributex.stream().collect(Collectors.groupingBy(e-> e, Collectors.counting()));
        } else {
        	javaslang.collection.List<Tuple2<String,String>> zippedValues = javaslang.collection.List.ofAll(attributex).zip(attributey);
        	zippedValues.toJavaStream().filter(e -> e._1.equals(e._2)).collect(Collectors.groupingBy(e -> e , Collectors.counting()));
        }
        
        return null;

    }
    
	public static String[][] readCSV(String path) throws FileNotFoundException, IOException {
		try (FileReader fr = new FileReader(path); BufferedReader br = new BufferedReader(fr)) {
			Collection<String[]> lines = new ArrayList<>();
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				lines.add(line.split(","));
			}
			return lines.toArray(new String[lines.size()][]);
		}
	}

    public static List<String> getClassAttributes(int rowCount) {

        List<String> attributes = new ArrayList<String>();
        String csvFile = "/Users/rugajendiran/Downloads/data/sample.csv";
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try {
            br = new BufferedReader(new FileReader(csvFile));
            int count = 0;
            while ((line = br.readLine()) != null) {
            	count++;
                if(count == 1)
                    continue;
                String[] values = line.split(cvsSplitBy);
                attributes.add(values[rowCount-1]);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return attributes;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
    	
    	String[][] dataSetCollection = readCSV("/Users/rugajendiran/Downloads/data/sample.csv");
    	
    	executeDecisionTree(dataSetCollection, null);
    	
    }

	private static void executeDecisionTree(String[][] dataSetCollection, Node parentNode) {
		MultivaluedMap<String, String> attributesValues = new MultivaluedHashMap<>();
    	attributesValues = getDistinctValuesOfAttribues(dataSetCollection);
    	
    	HashMap<Key2D, Long> counterMap1 = getAttributes(dataSetCollection, "play", "yes");
    	HashMap<Key2D, Long> counterMap2 = getAttributes(dataSetCollection, "play", "no");
    	
    	Map<Key2D, Double> gainMap = calculateGain(counterMap1,counterMap2);
    	
    	Map<String, Double> entropyMap = calculateEntropy(counterMap1, counterMap2, gainMap, dataSetCollection.length-1); // -1 from the total values
    	
    	if(entropyMap.isEmpty()) return;
    	String key = Collections.min(entropyMap.entrySet(), Map.Entry.comparingByValue()).getKey();
    	
    	// Create Node
    	List<String> values = attributesValues.get(key);
    	
    	Node rootNode = parentNode == null ? new Node(key, null, null, dataSetCollection, dataSetCollection.length) : parentNode;
    	
    	values.stream().forEach(e -> {
    		Key2D newKey = new Key2D(key, e);
    		System.out.println(e);
    		int value1 = counterMap1.get(newKey) !=null ? counterMap1.get(newKey).intValue() : 0;
    		int value2 = counterMap2.get(newKey) !=null ? counterMap2.get(newKey).intValue() : 0;
    		
    		Node node = new Node(key, e, key, dataSetCollection, value1+value2);
    		rootNode.addChild(node);
    	});
    	System.out.println("***********Root AttributeName "+rootNode.getAttributeName());
    	rootNode.getChildren().stream().forEach(e -> {
    		System.out.println("***********Root Dataset "+ e.getAttributeValue());
    		executeDecisionTree(e.getNewDataSet(), e);
    	});
    	System.out.println(rootNode);
	}
    
    private static MultivaluedMap<String, String> getDistinctValuesOfAttribues(String[][] dataSetCollection) {
    	MultivaluedMap<String, String> attributesValues = new MultivaluedHashMap<>();
    	
    	for(int i =0; i< dataSetCollection[0].length; i++){
    		HashSet<String> value = new HashSet<String>();
    		for(int j=1; j<dataSetCollection.length; j++) {
    			value.add(dataSetCollection[j][i]);
    			//attributesValues.put(dataSetCollection[0][i], dataSetCollection[0][i]);
    		}
    		attributesValues.put(dataSetCollection[0][i], new ArrayList(value));
    	}
    	
		return attributesValues;
	}

	private static HashMap<Key2D, Long> getAttributes(String[][] dataSet, String className, String classValue) {
    	HashMap<Key2D, Long> attributesMap = new HashMap<Key2D, Long>();
    	
    	for(int i=0; i< dataSet[0].length; i++) {
    		if(!StringUtils.equalsIgnoreCase(dataSet[0][i], className)) {
    			attributesMap.putAll(getValueCounters(dataSet, dataSet[0][i], className, classValue));
    		}
    	}
    	return attributesMap;
    }
    
    /*
     * Calculates gain for each values
     * 
     */
    private static Map<Key2D, Double> calculateGain(HashMap<Key2D, Long> counterMap, HashMap<Key2D, Long> counterMap1) {
    	Map<Key2D, Double> gainValue = new HashMap<Key2D, Double>();
    	// TODO handle the other map too so that we won't miss other attribute
    	counterMap.forEach((k,v) -> {
    		Long class2Value = counterMap1.get(k) == null ? 0 : counterMap1.get(k);
    		gainValue.put(k, calculateInfoGain(v, class2Value));
    		});
    	return gainValue;
	}
    
    /*
     * Calculates entropy for each values
     * 
     */
    private static Map<String, Double> calculateEntropy(HashMap<Key2D, Long> counterMap1, HashMap<Key2D, Long> counterMap2, Map<Key2D, Double> gainMap, int total) {
    	Map<String, Double> value = new HashMap<String, Double>();
    	
    	HashMap<Key2D, Long> totalTempMap = new HashMap<Key2D, Long>();
    	totalTempMap.putAll(counterMap1);
    	
    	counterMap2.forEach((k, v) -> totalTempMap.merge(k, v, Long::sum));
    	
    	gainMap.forEach((k, v) -> {
    		if(!v.isNaN()) {
    			Double value1 = (double) (counterMap1.get(k) == null ? 0L : counterMap1.get(k));
    			Double value2 = (double) (counterMap2.get(k) == null ? 0L : counterMap2.get(k));
    			System.out.println(k.toString() +""+entropyHelper(value1+value2, (double)total, v));
    			if(value.get(k.getcolumnName()) == null)
    				value.put(k.getcolumnName(), entropyHelper(value1+value2, (double)total, v));
    			else
    				value.put(k.getcolumnName(), value.get(k.getcolumnName()) + entropyHelper(value1+value2, (double)total, v));
    		}
    	});
    	
    	return value;
	}
    
    private static Double entropyHelper(Double value, Double total, Double infoGain) {
    	return (value/total)*infoGain;
    }
    
    /*
     * Helper method to calculate info gain
     */
	private static double calculateInfoGain(double class1Value, double class2Value) {
		double total = class1Value + class2Value;
		return (-1) * (class1Value/total) * (Math.log10(class1Value/total) / Math.log10(2)) + (-1) * (class2Value/total) * (Math.log10(class2Value/total) / Math.log10(2));
	}

	/*
     * Returns the number of values or occurences in the particular dataset.
     * Supports 2 use cases
     * 1. Calculates no of occurences for a single column
     * 2. Calculates no of occurences against a class column
     * 
     * conditionalAttributeName = "outlook"
     * className = "play"
     * classValue = "YES"
     * 
     */
    public static HashMap<Key2D, Long> getValueCounters(String[][] dataSet, String conditionalAttributeName, String className, String classValue) {
    	HashMap<Key2D, Long> counterMap = new HashMap<Key2D, Long>();
    	
    	int classAttributeCol = -1, conditionalAttributeCol = -1;
    	for(int i=0; i< dataSet[0].length; i++){
    		if(StringUtils.equalsIgnoreCase(className, dataSet[0][i])) classAttributeCol = i;
    		if(StringUtils.equalsIgnoreCase(conditionalAttributeName, dataSet[0][i])) conditionalAttributeCol = i;
    	}
    	if(className == null) {
    		for(int i=0; i< dataSet.length; i++) {
    			Long counter = counterMap.get(dataSet[i][conditionalAttributeCol]) == null ? 1 : counterMap.get(dataSet[i][conditionalAttributeCol])+1;
    			counterMap.put(new Key2D(conditionalAttributeName, dataSet[i][conditionalAttributeCol]), counter);
    		}
    	}else{
    		for(int i=0; i < dataSet.length; i++) {
            	if(StringUtils.equals(dataSet[i][classAttributeCol], classValue)){
            		Key2D key2d = new Key2D(conditionalAttributeName, dataSet[i][conditionalAttributeCol]);
            		
            		Long counterValue = counterMap.get(key2d) == null ? 1L : counterMap.get(key2d) + 1;
            		counterMap.put(key2d, counterValue);
            	}
            }
    	}
    	return counterMap;
    }
}

