package com.paypal.disputes.classification.data.models;

public class DisputeDocument {

    public final String document;
    public final int disputeClass;

    public DisputeDocument(String document, int disputeClass){

        this.document = document;
        this.disputeClass = disputeClass;
    }
}