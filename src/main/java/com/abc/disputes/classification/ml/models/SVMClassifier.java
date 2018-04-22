package com.abc.disputes.classification.ml.models;

import com.abc.common.utils.math.kernels.MercerKernel;
import com.abc.common.utils.parallel.computation.ExecutorSvc;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.abc.common.utils.MLUtils.unitize1;
import static java.lang.Double.*;
import static java.util.stream.Collectors.toList;

/**
 * Created by rachikkala on 4/22/18.
 */
public class SVMClassifier<T> implements Serializable {

    private Logger logger = LoggerFactory.getLogger(SVMClassifier.class);

    public final List<OnlineSVM> svms = new ArrayList<>();
    private OnlineSVM svm;
    // Length of weights equal to number of classes.
    private double[] weights;
    private int numberOfClasses;


    public SVMClassifier(double C, MercerKernel<T> kernel, int numberOfFeatures, int numberOfClasses,double[] weights) {

        //Using 1 vs ALL classification type.
        if(numberOfClasses == 2)
            svm = new OnlineSVM(C,C,kernel,numberOfFeatures);
        else
            IntStream.range(0,numberOfClasses).boxed().forEach(index -> svms.add(new OnlineSVM(C,C,kernel,numberOfFeatures)));
        this.weights = weights;
        this.numberOfClasses = numberOfClasses;

    }

    //Unlike smile, if weight is not passed, we use the weight that is used at the time of initialization.
    public void learnOnline(T x, int y) {
        learnOnline(x, y, weights[y]);
    }

    public void learnOnline(T input, int output, double weight) {

        if (numberOfClasses == 2)
            svm.process(input, output == 1? +1:-1, weight);
        else if (numberOfClasses > 2)
            for (int index = 0; index < numberOfClasses; index++)
                svms.get(index).process(input, output == index ? +1 :-1, weight* weights[output]);
    }

    public void learn(T[] input, int[] output) {
        learn(input,output,null);
    }

    public void learn(T[] input,final int[] output,final double[] sampleWeights) {

        if (input.length != output.length)
            throw new IllegalArgumentException(String.format("The sizes of X and Y don't match: %d != %d", input.length, output.length));


        if (sampleWeights != null && input.length != sampleWeights.length)
            throw new IllegalArgumentException(String.format("The sizes of X and instance weight don't match: %d != %d", input.length, sampleWeights.length));


        if (numberOfClasses == 2) {

            Tuple2<int[],double[]> formattedOutputSampleWeightsTuple = formatOutputAndWeights(output,sampleWeights,1);
            svm.learn(input, formattedOutputSampleWeightsTuple._1, formattedOutputSampleWeightsTuple._2);

        } else if (numberOfClasses > 2)
            Try.of(() -> ExecutorSvc.run(IntStream.range(0,numberOfClasses).boxed().map(classIndex -> formatClassData(classIndex,input,output,sampleWeights)).collect(toList()))).onFailure(throwable -> {

                logger.error("Exception while learning Multi class svm.",throwable);

            });
    }

    public void finish() {
        if (numberOfClasses == 2)
            svm.finish();
        else

            Try.of(() -> ExecutorSvc.run(IntStream.range(0,numberOfClasses).boxed().map(classIndex -> formFinishSupplier(classIndex)).collect(toList()))).onFailure(throwable -> {

                logger.error("Exception while learning Multi class svm.",throwable);

            });
    }

    public void trainPlattScaling(T[] input, int[] output) {

        if (numberOfClasses == 2)
            svm.trainPlattScaling(input, output);
        else if (numberOfClasses > 2)

            Try.of(() -> ExecutorSvc.run(
                    IntStream.
                            range(0, numberOfClasses).boxed().
                            map(classIndex -> formPlattScalingSupplier(classIndex, input, output)).
                            collect(toList())
                    )
            ).onFailure(throwable -> {
                logger.error("Exception while learning Multi class svm.", throwable);
            });
    }

    private Supplier<OnlineSVM> formatClassData(int classIndex,T[] input, int[] output, double[] sampleWeights) {

        Tuple2<int[],double[]> formattedOutputSampleWeightsTuple = formatOutputAndWeights(output,sampleWeights,classIndex);

        return formLearningSupplier(classIndex,input,formattedOutputSampleWeightsTuple._1,formattedOutputSampleWeightsTuple._2);

    }


    private Supplier<OnlineSVM> formLearningSupplier(int classIndex, T[] input, int[] output, double[] sampleWeights) {
        return  () -> {
            OnlineSVM onlineSVM = svms.get(classIndex);
            onlineSVM.learn(input, output, sampleWeights);
            return onlineSVM;
        };

    }

    private Supplier<OnlineSVM> formFinishSupplier(int classIndex) {
        return  () -> {
            OnlineSVM onlineSVM = svms.get(classIndex);
            onlineSVM.finish();
            return onlineSVM;
        };
    }

    private Supplier<OnlineSVM> formPlattScalingSupplier(int classIndex,T[] input, int[] output) {
        return  () -> {
            OnlineSVM onlineSVM = svms.get(classIndex);
            onlineSVM.trainPlattScaling(input, Arrays.stream(output).map(label -> label == classIndex ? 1 : -1).toArray());
            return onlineSVM;
        };
    }

    private Tuple2<int[],double[]> formatOutputAndWeights(int[] output, double[] weights,int classIndex) {

        double[] sampleWeights =weights != null ? weights : new double[output.length];
        int[] formattedOutput = new int[output.length];

        if(weights == null)
            for(int index =0; index < output.length; index++) {

                formattedOutput[index]= output[index] == classIndex ? 1 : -1;
                sampleWeights[index] = weights[output[index]];
            }
        else
            for(int index =0; index < output.length; index++)
                formattedOutput[index]= output[index] == classIndex ? 1 : -1;


        return Tuple.of(formattedOutput,sampleWeights);

    }

    public int predict(final T testDoc) {

        return numberOfClasses > 2  ? IntStream.range(0, numberOfClasses).boxed().
                map(classIndex -> Tuple.of(classIndex,svms.get(classIndex).predict(testDoc))).
                reduce(Tuple.of(-1, NEGATIVE_INFINITY),(tuple1, tuple2) -> tuple1._2 > tuple2._2 ? tuple1 : tuple2)._1 :
               svm.predict(testDoc) > 0 ? 1: 0;
    }

    /** Calculate the posterior probability. */
    private double posterior(OnlineSVM onlineSVM, double predictedClass) {
        final double minProb = 1e-7;
        final double maxProb = 1 - minProb;

        return min(max(onlineSVM.plattScaling.predict(predictedClass), minProb), maxProb);
    }


    public int predict(T input, double[] prob) {

        if (numberOfClasses == 2) {

            if (svm.plattScaling == null)
                throw new UnsupportedOperationException("PlattScaling was not trained yet. Please call SVM.trainPlattScaling() first.");

            // two class
            double predictedClass = svm.predict(input);

            prob[1] = posterior(svm, predictedClass);
            prob[0] = 1.0 - prob[1];


            return predictedClass > 0 ? 1 :0;

        } else  {
            // one-vs-all
            int label = 0;
            double maxPredictedValue = NEGATIVE_INFINITY;

            for (int classIndex = 0; classIndex < svms.size(); classIndex++) {

                OnlineSVM onlineSVM = svms.get(classIndex);

                if (onlineSVM.plattScaling == null)
                    throw new UnsupportedOperationException("PlattScaling was not trained yet. Please call SVM.trainPlattScaling() first.");

                double predictedValue = onlineSVM.predict(input);

                prob[classIndex] = posterior(onlineSVM, predictedValue);

                if (predictedValue > maxPredictedValue) {
                    label = classIndex;
                    maxPredictedValue = predictedValue;
                }
            }

            unitize1(prob);

            return label;
        }
    }
}
