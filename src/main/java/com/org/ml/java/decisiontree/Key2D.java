package com.org.ml.java.decisiontree;

/**
 * 
 * @author rugajendiran
 *
 */
public class Key2D {
	private final String columnName;
	private final String attribute;

	public Key2D(String columnName, String attribute) {
		this.columnName = columnName;
		this.attribute = attribute;
	}
	
	public String getcolumnName() {
		return columnName;
	}

	public String getattribute() {
		return attribute;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
		result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Key2D other = (Key2D) obj;
		if (attribute == null) {
			if (other.attribute != null)
				return false;
		} else if (!attribute.equals(other.attribute))
			return false;
		if (columnName == null) {
			if (other.columnName != null)
				return false;
		} else if (!columnName.equals(other.columnName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Key2D [columnName=" + columnName + ", attribute=" + attribute + "]";
	}
	
}
