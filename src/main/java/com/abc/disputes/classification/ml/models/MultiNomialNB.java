package com.abc.disputes.classification.ml.models;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abc.disputes.classification.data.models.Corpus;
import com.abc.disputes.classification.data.models.DocumentRow;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.lang.Math.log;

/**
 * Some fundamental terminology used in Naive Bayes which is <b>the best classifier</b> in the world
 * provided you <b>know</b> the class conditional distribution.
 *<br/>
 * Posterior probability = P(Y= yi/X=xi)
 * Marginal = P(X = xi)
 * Class Conditional = P(X = xi/Y=yi)
 * Prior = P(Y =yi)
 */
public class MultiNomialNB {

    private Logger logger = LoggerFactory.getLogger(MultiNomialNB.class);

    private static final int LAPLACE_SMOOTHING=1;

    private Corpus corpus;
    private double[] priorProbabilities;
    private int[] classesTotalTerms;
    private double[][] classConditionalProbabilities;

    private static MultiNomialNB instance;


    private MultiNomialNB(Corpus corpus){
        this.corpus = corpus;
        loadPriorAndConditionalProbabilities(corpus);
    }

    public synchronized static MultiNomialNB getInstance(Corpus corpus) {

        return instance == null ? new MultiNomialNB(corpus) : instance;

    }


    private void loadPriorAndConditionalProbabilities(Corpus corpus) {

        loadPriorProbabilities(corpus);
        loadConditionalProbabilities(corpus);
    }

    private void loadPriorProbabilities(Corpus corpus) {

        Map<Integer,List<DocumentRow>> disputeClassDocsMap = corpus.getDisputeClassDocsMap();

        priorProbabilities = new double[disputeClassDocsMap.size()];

        int totalRecords = disputeClassDocsMap.entrySet().stream().map(entry -> entry.getValue().size()).reduce(0,Integer::sum);
        disputeClassDocsMap.entrySet().stream().forEach(entry -> {
            priorProbabilities[entry.getKey()]= (1.0 * entry.getValue().size() / totalRecords);
        });

    }


    private void loadConditionalProbabilities(Corpus corpus) {

        Map<Integer,List<DocumentRow>> disputeClassDocsMap = corpus.getDisputeClassDocsMap();

        classConditionalProbabilities = new double[disputeClassDocsMap.size()][corpus.getNumberOfTerms()];
        classesTotalTerms = new int[disputeClassDocsMap.size()];

        disputeClassDocsMap.keySet().stream().forEach(disputeClass ->

                    disputeClassDocsMap.get(disputeClass).stream().forEach(documentRow ->

                        documentRow.getTermFreqMap().entrySet().stream().forEach(entry ->
                                {
                                    classConditionalProbabilities[disputeClass][corpus.getKeyColIdMap().get(entry.getKey())] += entry.getValue();
                                    classesTotalTerms[disputeClass] += entry.getValue();
                                }
                        )

                    )
                );

        IntStream.range(0,classesTotalTerms.length).boxed().forEach(
                disputeClass ->
                    IntStream.range(0,corpus.getNumberOfTerms()).boxed().forEach(colIndex ->
                        classConditionalProbabilities[disputeClass][colIndex] = (classConditionalProbabilities[disputeClass][colIndex] + LAPLACE_SMOOTHING)/(classesTotalTerms[disputeClass] +corpus.getNumberOfTerms())
                    )
        );

    }

    public int predictClass(DocumentRow testDocument) {

        Tuple2<Integer,Double> predictedClassProbTuple = IntStream.range(0,classesTotalTerms.length).boxed().map(
                disputeClass ->
                        Tuple.of(disputeClass,log(priorProbabilities[disputeClass]) +corpus.findTfIdfMap(testDocument).entrySet().stream().map(entry -> entry.getValue() * log(classConditionalProbabilities[disputeClass][corpus.getKeyColIdMap().get(entry.getKey())])).reduce(0.0,Double::sum))
        ).reduce(Tuple.of(-1,Double.NEGATIVE_INFINITY),(tuple1,tuple2) -> tuple1._2 > tuple2._2 ? tuple1 :tuple2);

        logger.info("Predicted class and probability tuple is {}",predictedClassProbTuple);

        return predictedClassProbTuple._1;

    }
}
