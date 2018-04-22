package com.abc.disputes.classification.data.models;

import io.vavr.control.Try;
import opennlp.tools.util.Span;

import static com.abc.disputes.classification.ml.corenlp.NLPUtils.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by rachikkala on 4/7/18.
 */
public class NamedEntityWrapper {

    public final String[] tokens;

    private Set<Integer> personIndices;

    private Set<Integer> locationIndices;
    private Set<Integer> orgIndices;

    private Set<Integer> moneyIndices;
    private Set<Integer> percentIndices;

    private Set<Integer> dateIndices;
    private Set<Integer> timeIndices;

    public NamedEntityWrapper(final String[] tokens){

        this.tokens = tokens;

        personIndices= Arrays.stream(personFinder.find(tokens)).map(Span::getStart).collect(Collectors.toSet());

        locationIndices= Arrays.stream(locationFinder.find(tokens)).map(Span::getStart).collect(Collectors.toSet());
        orgIndices= Arrays.stream(orgFinder.find(tokens)).map(Span::getStart).collect(Collectors.toSet());

        moneyIndices= Arrays.stream(moneyFinder.find(tokens)).map(Span::getStart).collect(Collectors.toSet());
        percentIndices= Arrays.stream(percentFinder.find(tokens)).map(Span::getStart).collect(Collectors.toSet());

        dateIndices= Arrays.stream(dateFinder.find(tokens)).map(Span::getStart).collect(Collectors.toSet());
        timeIndices= Arrays.stream(timeFinder.find(tokens)).map(Span::getStart).collect(Collectors.toSet());

    }

    public boolean isNamedEntity(int index) {

        return personIndices.contains(index) ||

                locationIndices.contains(index) ||
                orgIndices.contains(index) ||

                moneyIndices.contains(index) ||
                percentIndices.contains(index) ||

                dateIndices.contains(index) ||
                timeIndices.contains(index) ;


    }

    @Override
    public String toString() {
        return "NamedEntityWrapper{" +
                "tokens=" + Arrays.toString(tokens) +
                ", personIndices=" + personIndices +
                ", locationIndices=" + locationIndices +
                ", orgIndices=" + orgIndices +
                ", moneyIndices=" + moneyIndices +
                ", percentIndices=" + percentIndices +
                ", dateIndices=" + dateIndices +
                ", timeIndices=" + timeIndices +
                '}';
    }
}
