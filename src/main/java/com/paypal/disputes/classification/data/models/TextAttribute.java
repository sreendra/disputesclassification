package com.paypal.disputes.classification.data.models;

import java.util.Objects;

public class TextAttribute {

    public final String name;
    public final String partOfSpeech;
    public final String lemma;
    public final boolean isNamedEntity;

    private int tokenIndex;


    public TextAttribute(final String name,final String partOfSpeech,final String lemma,final boolean isNamedEntity,final int tokenIndex) {

        this.name = name;
        this.partOfSpeech = partOfSpeech;
        this.lemma = lemma;
        this.isNamedEntity = isNamedEntity;
        this.tokenIndex = tokenIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextAttribute attribute = (TextAttribute) o;
        return Objects.equals(lemma, attribute.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lemma);
    }

    @Override
    public String toString() {
        return "TextAttribute{" +
                "name='" + name + '\'' +
                ", partOfSpeech='" + partOfSpeech + '\'' +
                ", lemma='" + lemma + '\'' +
                ", isNamedEntity=" + isNamedEntity +
                ", tokenIndex=" + tokenIndex +
                '}';
    }
}
