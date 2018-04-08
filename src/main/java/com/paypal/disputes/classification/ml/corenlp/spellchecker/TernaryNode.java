package com.paypal.disputes.classification.ml.corenlp.spellchecker;


public class TernaryNode {

	public final char data;

	private TernaryNode left, equal, right ;

	private boolean isWordChar;
	private Long frequency;

	public TernaryNode(char data) {
		this.data = data;
	}

	public void setLeft(TernaryNode left) {
		this.left = left;
	}

	public TernaryNode getLeft() {
		return this.left;
	}

	public TernaryNode setEqual(TernaryNode equal) {
		return (this.equal = equal);
	}

	public TernaryNode getEqual() {
		return this.equal;
	}

	public void setRight(TernaryNode right) {
		this.right = right;
	}

	public TernaryNode getRight() {
		return this.right;
	}


	public Long getFrequency() {
		return frequency;
	}

	public boolean isWordChar() {
		return isWordChar;
	}

	public void setIsWordCharAndFrequency(Long frequency,boolean isWordChar) {
		this.frequency =frequency;
		this.isWordChar =isWordChar;
	}
}
