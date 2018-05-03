package com.org.ml.java.decisiontree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class Node
{
    private List<Node> children = new ArrayList<Node>();
    private String attributeName;
    private String attributeValue;
    private String[][] newDataSet;

    public Node(String attributeName, String attributeValue, String parentAttributeName, String[][] dataSet, int count)
    {
    	
    	this.attributeName = attributeName;
    	this.attributeValue = attributeValue;
    	this.newDataSet = new String[count+1][dataSet[0].length-1]; // +1 for dataset header, -1 to remove the filtered attribute eg)sunny,hot etc..
    	
		int currentAttributeCol = -1;
		for (int i = 0; i < dataSet[1].length; i++) {
			if (StringUtils.equalsIgnoreCase(attributeName, dataSet[0][i])) {
				currentAttributeCol = i;
			}
		}
		int counter=0;
		for(int i=0; i<dataSet.length; i++){
			if(attributeValue != null && dataSet[i][currentAttributeCol].equalsIgnoreCase(attributeValue) || attributeName.equalsIgnoreCase(dataSet[i][currentAttributeCol])) {
				for(int j=0; j < dataSet[0].length; j++) {
					if(j==currentAttributeCol) continue;
					newDataSet[counter][j-1] = dataSet[i][j];
				}
				counter++;
			}
		}
		System.out.println("SUCCESS" + attributeValue);

//    	this.dataSet = new String[count][dataSet[0].length];
//        this.children = new ArrayList<>();
//        this.attributeName = attributeName;
//        this.attributeValue = attributeValue;
//        this.parentAttributeName = parentAttributeName;
//        // Construct the data set based on the parent attribute name and value
//        int currentAttributeCol = -1, parentAttributeCol = -1;
//        for(int i=0; i< dataSet[1].length; i++) {
//        	if(StringUtils.equalsIgnoreCase(attributeName, dataSet[0][i])) {
//        		currentAttributeCol = i;
//        	}else if(StringUtils.equalsIgnoreCase(parentAttributeName, dataSet[0][i])) {
//        		parentAttributeCol = i;
//        	}
//        }
//        
//        for(int i=0; i<dataSet.length; i++){
//        	for(int j=0; j<dataSet[0].length; j++){
//        		if(StringUtils.equalsIgnoreCase(attributeValue,dataSet[i][currentAttributeCol]) || i==0){
//        			this.dataSet[i][j] = dataSet[i][j];
//        		}
//        	}
//        }
        
//        if(parentAttributeCol != -1) {
//	        for(int i=0; i < dataSet.length; i++) {
//	        	if(StringUtils.equalsIgnoreCase(dataSet[i][parentAttributeCol], parentAttributeValue)){
//	        		Long counterValue = attributeSet.get(dataSet[i][currentAttributeCol]) == null ? 1L : attributeSet.get(dataSet[i][currentAttributeCol]) + 1;
//	        		attributeSet.put(dataSet[i][currentAttributeCol], counterValue);
//	        	}
//	        }
//        }
        
    }

    public void addChild(Node child)
    {
        children.add(child);
    }

	public List<Node> getChildren() {
		return children;
	}

	public void setChildren(List<Node> children) {
		this.children = children;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public String getAttributeValue() {
		return attributeValue;
	}

	public void setAttributeValue(String attributeValue) {
		this.attributeValue = attributeValue;
	}

	public String[][] getNewDataSet() {
		return newDataSet;
	}

	public void setNewDataSet(String[][] newDataSet) {
		this.newDataSet = newDataSet;
	}

}