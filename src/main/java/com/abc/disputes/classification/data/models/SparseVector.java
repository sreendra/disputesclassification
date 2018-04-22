package com.abc.disputes.classification.data.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class SparseVector implements Iterable<SparseVector.TermEntry>, Serializable {

    private static final long serialVersionUID = 5059232025441326622L;

    private List<TermEntry> entries;

    public SparseVector() {
        this.entries = new ArrayList<>();
    }

    @Override
    public Iterator<TermEntry> iterator() {
        return entries.iterator();
    }

    public void addTerm(int index, double tfIdf) {
        entries.add(new TermEntry(index,tfIdf));
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public static class TermEntry implements Serializable {

        private static final long serialVersionUID = -1557315318790706555L;

        public final int index;
        public final double tfIdf;

        public TermEntry(int index, double tfIdf) {
            this.index = index;
            this.tfIdf = tfIdf;
        }
    }




}
