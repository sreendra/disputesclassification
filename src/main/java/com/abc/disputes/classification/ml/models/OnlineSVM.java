package com.abc.disputes.classification.ml.models;

import com.abc.common.utils.KernelCache;
import com.abc.common.utils.math.kernels.LinearKernel;
import com.abc.common.utils.math.kernels.MercerKernel;
import com.abc.common.utils.probability.PlattScaling;
import com.abc.disputes.classification.data.models.SVMInitSeedModel;
import com.abc.disputes.classification.data.models.SparseVector;
import com.codepoetics.protonpack.StreamUtils;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.abc.common.utils.MLConstants.TAU;
import static com.abc.common.utils.MLConstants.TOLERANCE;
import static com.abc.common.utils.MLUtils.dot;
import static com.abc.common.utils.MLUtils.permutate;
import static java.lang.Double.*;

/**
 * This is for
 */
public class OnlineSVM<T> implements Serializable {

    private static final long serialVersionUID = -3576022170943190424L;

    private Logger logger = LoggerFactory.getLogger(OnlineSVM.class);

    private List<SupportVector> supportVectors;

    /**
     * The soft margin penalty parameter for positive samples.
     */
    public final double Cp ; //= 1.0;
    /**
     * The soft margin penalty parameter for negative samples.
     */
    public final double Cn ; //= 1.0;

    /**
     * Y= W'x +b
     * Weight vector for linear SVM - As per the formula
     */
    private double[] w;
    /**
     * Bias of SVM - As per the formula
     */
    private double b = 0.0;
    /**
     * The number of support vectors.
     */
    private int nsv = 0;
    /**
     * The number of bounded support vectors.
     */
    private int nbsv = 0;

    //TODO Understand PlattScaling - This is very important for Prediction ....
    //TODO Half cooked food is of no use.
    public PlattScaling plattScaling;

    //Flag to indicate Most Violating Pair.
    private boolean foundMVPFlag;

    private SupportVector minSupportVector;
    private SupportVector maxSupportVector;

    private double minGradientValue = POSITIVE_INFINITY;
    private double maxGradientValue = NEGATIVE_INFINITY;

    public final MercerKernel<T> kernel;
    public final int numberOfFeatures;

    public OnlineSVM(double Cp, double Cn,MercerKernel<T> kernel,int numberOfFeatures) {
        this.Cp = Cp;
        this.Cn = Cn;
        this.kernel = kernel;
        this.numberOfFeatures = numberOfFeatures;
    }

    /**
     *
     * @param input - {Sparse Array}
     * @param output - Binary values, either {1,-1}
     */
    public void  learn(T[] input, int[] output,double[] weight) {

        SVMInitSeedModel initSeedModel = frameSVMInitSeed();

        logger.info("Number of positive support vectors are {} and number of negative support vectors are {}",initSeedModel.getNumberOfPositiveSamples(),initSeedModel.getNumberOfNegativeSamples());

        //TODO In the first phase, i focus only on the above mentioned data types.
        StreamUtils.takeWhile(IntStream.range(0,input.length).boxed(), index -> initSeedModel.requiresMoreSamples()).
                filter(index -> initSeedModel.isMinSeedCountDeficit(output[index])).
                forEach(index -> {
                    process(input[index],output[index],weight == null ?  1.0 : weight[index]);
                    initSeedModel.incrementSeedSamplesFreq(output[index]);
                });


        int[] randomIndices = permutate(input.length);

        for (int index = 0; index < input.length; index++) {

            process(input[randomIndices[index]], output[randomIndices[index]],weight == null ? 1.0: weight[randomIndices[index]]);

            double gmin = MAX_VALUE;
            double gmax = - MAX_VALUE;

            do {
                reprocess(TOLERANCE); // at least one call to reprocess
                Tuple2<Double,Double> minMaxGradients = findMinMaxGradientsTuple();
                gmin= minMaxGradients._1;
                gmax=minMaxGradients._2;

            } while (gmax - gmin > 1000);
        }

    }

    /**
     * Returns the function value after training.
     */
    public double predict(T x) {

        double f = b;

        if (kernel instanceof LinearKernel && w != null) {

            if (x instanceof double[])
                f += dot(w, (double[]) x);
            else if (x instanceof SparseVector)
                for (SparseVector.TermEntry termEntry : (SparseVector) x)
                    f += w[termEntry.index] * termEntry.tfIdf;

            else {
                throw new UnsupportedOperationException("Unsupported data type for linear kernel");
            }
        } else {

            for (SupportVector supportVector : supportVectors)
                if (supportVector != null)
                    f += supportVector.alpha * kernel.k(supportVector.input, x);


        }

        return f;
    }

    /**
     * After calling finish, the user should call this method
     * to train Platt Scaling to estimate posteriori probabilities.
     *
     * @param x training samples.
     * @param y training labels.
     */
    void trainPlattScaling(T[] x, int[] y) {
        int l = y.length;
        double[] scores = new double[l];
        for (int i = 0; i < l; i++) {
            scores[i] = predict(x[i]);
        }
        plattScaling = new PlattScaling(scores, y);
    }

    private SVMInitSeedModel frameSVMInitSeed() {

        //Map<Boolean,Long> map =groupingBy(SupportVector::isPositiveSample,Collectors.counting();

        return IntStream.range(0,supportVectors.size()).boxed().filter(index -> Optional.ofNullable(supportVectors.get(index)).isPresent()).
                map(index -> supportVectors.get(index)).collect(
                SVMInitSeedModel::new,
                (acc,supportVector) -> acc.incrementSeedSamplesFreq(supportVector.output),
                (acc1,acc2) ->  acc1.addPositiveAndNegativeSamples(acc2.getNumberOfPositiveSamples(),acc2.getNumberOfNegativeSamples())
        );


    }

    public static void main(String[] args) {

        /*List<Integer> list = Arrays.asList(1,2,7,3,4,5,6,7,8);
        StreamUtils.skipUntilInclusive(list.stream(),x ->  x == 4 ).forEach(System.out::println);*/

        Arrays.stream(permutate(5)).forEach(System.out::println);
    }

    private boolean process(T x, int y) {
        return process(x, y, 1.0);
    }


    /**
     *
     * @param input : Input document received to be processed.
     * @param label : {1,-1}
     * @param weight
     * @return
     */
    public boolean process(T input, int label, double weight) {

        validateLabelAndWeight(label,weight);

        //Bail out if input instance is available in SupportVectors.

        if(supportVectors.stream().anyMatch(supportVector -> supportVector.input == input)) {

            logger.info("Already processed this input, hence bailing it out.");
            return true;
        }


        KernelCache cache = new KernelCache(supportVectors.size() + 1);

        double alpha =0.0;
        double gradient = label - IntStream.range(0,supportVectors.size()).boxed().map(index -> findAlphaKOfSupportVector(cache,index,input)).reduce(0.0,Double::sum);

        // Decide insertion
        Tuple2<Integer,Integer> minMaxGradientsTuple = findMVPTuple();

        if (minMaxGradientsTuple._1 != -1 && minMaxGradientsTuple._2 != -1 &&
                supportVectors.get(minMaxGradientsTuple._1).gradient  < supportVectors.get(minMaxGradientsTuple._2).gradient) {
            if ((label > 0 && gradient < supportVectors.get(minMaxGradientsTuple._1).gradient) ||
                    (label < 0 && gradient > supportVectors.get(minMaxGradientsTuple._2).gradient)) {

                return false;
            }
        }

        double autoCorrelation = kernel.k(input, input);
        cache.add(autoCorrelation);

        // Insert
        SupportVector supportVector = new SupportVector(input,label,alpha,gradient,label > 0 ? 0 :-weight * Cn , label > 0 ? weight * Cp : 0,cache,autoCorrelation);

        IntStream.range(0,supportVectors.size()).boxed().
                filter(index -> Optional.ofNullable(supportVectors.get(index)).isPresent() && Optional.ofNullable(supportVectors.get(index).cache).isPresent()).
                forEach(index -> supportVectors.get(index).cache.add(cache.get(index)));

        supportVectors.add(supportVector);

        // Process
        sequentialMinimalOptimization(label > 0 ? null:supportVector, label > 0 ? supportVector : null, 0.0);

        foundMVPFlag = false;
        return true;
    }

    /**
     * Reprocess support vectors.
     * @param tolerance the tolerance of convergence test.
     */
    public boolean reprocess(double tolerance) {
        boolean status = sequentialMinimalOptimization(null, null, tolerance);
        evict();
        return status;
    }

    /**
     * Sequential minimal optimization.
     * @param supportVector1 the first vector of working set.
     * @param supportVector2 the second vector of working set.
     * @param toleranceValue the tolerance of convergence test.
     */
    boolean sequentialMinimalOptimization(SupportVector supportVector1, SupportVector supportVector2, double toleranceValue) {
        // SO working set selection -- Second Order Working Set Selection, SOWSS.
        // Determine coordinate to process
        if (supportVector1 == null || supportVector2 == null) {

            if (supportVector1 == null && supportVector2 == null) { // REPROCESS call.

                Tuple2<Integer,Integer> mvpTuple =findMVPTuple();

                if(mvpTuple._1 == -1 || mvpTuple._2 == -1) {

                    logger.info("Couldn't find the MVP for the support vectors. Hence returning ....and tuple is {}",mvpTuple);
                    return false;
                }

                //TODO should i compare the absolute value or why we are choosing only 1 vector. Should it not be both ?
                boolean isSupportVector2WithMaxGradient = supportVectors.get(mvpTuple._2).gradient > -supportVectors.get(mvpTuple._1).gradient;
                supportVector1 =  isSupportVector2WithMaxGradient ? supportVector1 : supportVectors.get(mvpTuple._1);
                supportVector2 = isSupportVector2WithMaxGradient ? supportVectors.get(mvpTuple._2):supportVector2;
            }

            if (supportVector2 == null) {

                populateCache(supportVector1);
                supportVector2 = findMaxSupportVector(supportVector1);

            } else {

                populateCache(supportVector2);
                supportVector1 = findMinSupportVector(supportVector2);
            }
        }

        if (supportVector1 == null || supportVector2 == null)
            return false;


        populateCache(supportVector1);
        populateCache(supportVector2);

        // Determine curvature
        double eta = supportVector1.k + supportVector2.k - 2 * kernel.k(supportVector1.input, supportVector2.input);
        eta = eta <= 0.0 ? TAU : eta;

        double lambda = (supportVector2.gradient - supportVector1.gradient) / eta;

        lambda = lambda >= 0.0 ?
                findMin(lambda,supportVector1.alpha - supportVector1.minAlpha,supportVector2.maxAlpha - supportVector2.alpha):
                findMax(lambda,supportVector2.minAlpha - supportVector2.alpha,supportVector1.alpha - supportVector1.maxAlpha);

        // Perform update
        supportVector1.alpha -= lambda;
        supportVector2.alpha += lambda;

        for (int index = 0; index < supportVectors.size(); index++) {
            SupportVector supportVector = supportVectors.get(index);
            if (supportVector != null)
                supportVector.updateGradient(lambda,supportVector2.cache.get(index),supportVector1.cache.get(index));
        }

        Tuple2<Double,Double> minMaxGradientsTuple = findMinMaxGradientsTuple();
        b = (minMaxGradientsTuple._1 + minMaxGradientsTuple._2) / 2;

        return  minMaxGradientsTuple._2 - minMaxGradientsTuple._1 >= toleranceValue;
    }

    private double findMin(double a, double b, double c) {
        return a < b ? (a < c ? a : c ) :(b < c ? b : c);
    }

    private double findMax(double a, double b, double c) {
        return a > b ? (a > c ? a : c ) :(b > c ? b : c);
    }

    private  SupportVector findMinSupportVector(SupportVector maxSupportVector){

        double maxGain = 0.0;
        SupportVector minSupportVector = null;

        for (int index = 0; index < supportVectors.size(); index++) {

            SupportVector supportVector = supportVectors.get(index);

            if (supportVector == null)
                continue;


            double gradientDiff = maxSupportVector.gradient - supportVector.gradient;
            double eta = maxSupportVector.k + supportVector.k - 2.0 * maxSupportVector.cache.get(index);
            // double curv = 2.0 - 2.0 * k;   // for Gaussian kernel only
            eta = eta <= 0.0 ? TAU : eta;
            double mu = gradientDiff / eta;

            if ((mu > 0.0 && supportVector.alpha > supportVector.minAlpha) || (mu < 0.0 && supportVector.alpha < supportVector.maxAlpha)) {
                double gain = gradientDiff * mu;

                if (gain > maxGain) {

                    maxGain = gain;
                    minSupportVector = supportVector;
                }
            }
        }

        return minSupportVector;
    }

    private  SupportVector findMaxSupportVector(SupportVector minSupportVector){

        double maxGain = 0.0;
        SupportVector maxSupportVector = null;

        for (int index = 0; index < supportVectors.size(); index++) {

            SupportVector supportVector = supportVectors.get(index);

            if (supportVector == null)
                continue;


            double gradientDiff = supportVector.gradient - minSupportVector.gradient;
            double eta = supportVector.k + minSupportVector.k - 2.0 * minSupportVector.cache.get(index);
            // double curv = 2.0 - 2.0 * k;   // for Gaussian kernel only
            eta = eta <= 0.0 ? TAU : eta;
            double mu = gradientDiff / eta;

            if ((mu > 0.0 && supportVector.alpha < supportVector.maxAlpha) || (mu < 0.0 && supportVector.alpha > supportVector.minAlpha)) {
                double gain = gradientDiff * mu;

                if (gain > maxGain) {

                    maxGain = gain;
                    maxSupportVector = supportVector;
                }
            }
        }

        return maxSupportVector;
    }



    private void populateCache(SupportVector supportVector) {

        Optional.ofNullable(supportVector.cache).orElse( populateKernelCache(supportVector) );
    }

    private KernelCache populateKernelCache( SupportVector supportVector) {

        KernelCache cache = new KernelCache();

        IntStream.range(0,supportVectors.size()).boxed().
                forEach(index -> cache.add(Optional.ofNullable(supportVectors.get(index)).isPresent() ? kernel.k(supportVectors.get(index).input,supportVector.input) :0.0 ));
        supportVector.cache = cache;

        return cache;

    }

    private double findAlphaKOfSupportVector(KernelCache cache, int index,T input) {

        SupportVector supportVector = supportVectors.get(index);

        double kernelCorrelation = supportVector != null ? kernel.k(supportVector.input, input) :0.0;
        cache.add(kernelCorrelation);
        return supportVector != null ? supportVector.alpha * kernelCorrelation :0.0 ;
    }


    private void findMostViolatingPair() {

        if (!foundMVPFlag) {

            IntStream.range(0,supportVectors.size()).boxed().filter(index -> Optional.ofNullable(supportVectors.get(index)).isPresent()).
                    filter(index -> supportVectors.get(index).alphaSatisfiesUpperBoxLimit()).
                    reduce((index1,index2) -> supportVectors.get(index1).getGradient() > supportVectors.get(index2).getGradient() ? index1 : index2).
                    ifPresent(index -> {
                        logger.info("Max Gradient index is {}",index);
                        maxSupportVector = supportVectors.get(index);
                    });

            IntStream.range(0,supportVectors.size()).boxed().filter(index -> Optional.ofNullable(supportVectors.get(index)).isPresent()).
                    filter(index -> supportVectors.get(index).alphaSatisfiesLowerBoxLimit()).
                    reduce((index1,index2) -> supportVectors.get(index1).getGradient() < supportVectors.get(index2).getGradient() ? index1 : index2).
                    ifPresent(index -> {
                        logger.info("Min Gradient index is {}",index);
                        minSupportVector = supportVectors.get(index);
                    });

            foundMVPFlag = true;
        }
    }

    private Tuple2<Double,Double> findMinMaxGradientsTuple() {

        int minGradientIndex = findMinGradientIndex();
        int maxGradientIndex = findMaxGradientIndex();

        return Tuple.of(
                minGradientIndex != -1 ? supportVectors.get(minGradientIndex).gradient : MAX_VALUE,
                maxGradientIndex != - 1 ?supportVectors.get(maxGradientIndex).gradient : -MAX_VALUE);
    }

    /**
     * This is to find the TAU(τ) violating pair in the SMO algorithm.
     * (i, j) is a τ-violating pair ⇐⇒  {αi < Bi, αj > Aj and gi−gj > τ
     * @return
     */
    private Tuple2<Integer,Integer> findMVPTuple() {

        return Tuple.of(findMinGradientIndex(),findMaxGradientIndex());
    }

    /**
     *
     * @return Tuple of
     * (Min index(j), Min value(gj)
     * Max index(i),Max value(gi))
     */
    private Optional<Tuple4<Integer,Double,Integer,Double>> findMinMaxGradientIndices() {

        return IntStream.range(0,supportVectors.size()).boxed().filter(index -> Optional.ofNullable(supportVectors.get(index)).isPresent()).
                map(index ->
                        Tuple.of(index,
                                supportVectors.get(index).alphaSatisfiesLowerBoxLimit() ? supportVectors.get(index).gradient : MAX_VALUE,
                                index,
                                supportVectors.get(index).alphaSatisfiesUpperBoxLimit() ? supportVectors.get(index).gradient : -MAX_VALUE)
                ).
                reduce((tuple1,tuple2) ->
                        Tuple.of(tuple1._2 < tuple2._2 ? tuple1._1 : tuple2._1,
                                tuple1._2 < tuple2._2 ? tuple1._2 : tuple2._2,
                                tuple1._4 > tuple2._4 ? tuple1._1 : tuple2._1,
                                tuple1._4 > tuple2._4 ? tuple1._4 : tuple2._4
                        )

                );

    }

    private Integer findMinGradientIndex() {

        return IntStream.range(0,supportVectors.size()).boxed().filter(index -> Optional.ofNullable(supportVectors.get(index)).isPresent()).
                filter(index -> supportVectors.get(index).alphaSatisfiesLowerBoxLimit()).
                reduce((index1,index2) -> supportVectors.get(index1).getGradient() < supportVectors.get(index2).getGradient() ? index1 : index2).orElse(-1);

    }

    private Integer findMaxGradientIndex() {

        return IntStream.range(0,supportVectors.size()).boxed().filter(index -> Optional.ofNullable(supportVectors.get(index)).isPresent()).
                filter(index -> supportVectors.get(index).alphaSatisfiesUpperBoxLimit()).
                reduce((index1,index2) -> supportVectors.get(index1).getGradient() > supportVectors.get(index2).getGradient() ? index1 : index2).orElse(-1);

    }

    private void validateLabelAndWeight(int label, double weight) {

        validateLabel(label);
        validateWeight(weight);
    }

    private void validateLabel(int label) {

        if (label != 1 && label != -1)
            throw new IllegalArgumentException("Invalid label : " + label);
    }

    private void validateWeight(double weight) {

        if (weight <= 0.0)
            throw new IllegalArgumentException("Invalid weight : " + weight);
    }



    /**
     * Call reprocess until converge.
     */
    void finish() {
        finish(TOLERANCE);
    }

    /**
     * Call reprocess until converge.
     * @param tolerance the tolerance of convergence test.
     */
    void finish(double tolerance) {
        logger.info("SVM finializes the training by reprocess.");

        //TODO Test how many times, the inner loop gets executed.

        int count= 1;

        for (; sequentialMinimalOptimization(null, null, tolerance); count++) {
            if (count % 1000 == 0) {
                logger.info("finishing {} reprocess iterations.");
            }
        }
        logger.info("SVM finished the reprocess and count value is {}",count);

        Tuple2<Double,Double> minMaxGradientsTuple = findMinMaxGradientsTuple();

        Iterator<SupportVector> iterator = supportVectors.iterator();

        while (iterator.hasNext()) {

            SupportVector supportVector = iterator.next();

            if (supportVector == null)
                iterator.remove();

            else if (supportVector.alpha == 0) {

                if ((supportVector.output < 0 && supportVector.gradient >= minMaxGradientsTuple._2 ) || (supportVector.output > 0 && supportVector.gradient <= minMaxGradientsTuple._1 ))
                    iterator.remove();
                else {

                    nsv++;
                    nbsv = supportVector.alpha == supportVector.minAlpha || supportVector.alpha == supportVector.maxAlpha ? (nbsv+1) :nbsv;
                }
            }
        }

        if (kernel instanceof LinearKernel) {


            w = new double[numberOfFeatures];

            for (SupportVector supportVector : supportVectors) {

                if (supportVector.input instanceof double[]) {

                    double[] x = (double[]) supportVector.input;

                    for (int index = 0; index < x.length; index++)
                        w[index] += supportVector.alpha * x[index];


                } else if (supportVector.input instanceof int[]) {
                    int[] x = (int[]) supportVector.input;

                    for (int index = 0; index < x.length; index++)
                        w[index] += supportVector.alpha * x[index];


                } else if (supportVector.input instanceof SparseVector) {
                    for (SparseVector.TermEntry termEntry : (SparseVector) supportVector.input)
                        w[termEntry.index] += supportVector.alpha * termEntry.tfIdf;

                }
            }
        }
    }

    /**
     * Removing vectors having zero alpha value and which are at the extremes.
     */
    void evict() {

        Tuple2<Double,Double> minMaxGradients = findMinMaxGradientsTuple();

        for (int index = 0; index < supportVectors.size(); index++) {

            SupportVector supportVector = supportVectors.get(index);

            if (supportVector != null && supportVector.alpha == 0) {

                if((supportVector.output < 0 && supportVector.gradient >= minMaxGradients._2) ||
                        (supportVector.output > 0 && supportVector.gradient <= minMaxGradients._1) )
                    supportVectors.set(index, null);

            }
        }
    }

    /**
     * Cleanup kernel cache to free memory.
     */
    void cleanup() {
        nsv = 0;
        nbsv = 0;

        for (SupportVector supportVector : supportVectors) {

            if (supportVector != null) {

                nsv++;
                supportVector.cache = null;
                nbsv = supportVector.alpha == supportVector.minAlpha || supportVector.alpha == supportVector.maxAlpha ? (nbsv+1) :nbsv;

            }
        }

        logger.info("{} support vectors, {} bounded\n", nsv, nbsv);
    }


    /*
 * This is implementation of the below papers.
 * Antoine Bordes, Seyda Ertekin, Jason Weston and Leon Bottou. Fast Kernel Classifiers with Online and Active Learning, Journal of Machine Learning Research, 6:1579-1619, 2005.
 * Tobias Glasmachers and Christian Igel. Second Order SMO Improves SVM Online and Active Learning.
 * Christopher J. C. Burges. A Tutorial on Support Vector Machines for Pattern Recognition. Data Mining and Knowledge Discovery 2:121-167, 1998.
 * Referred smile library for the coding part.
 *
 * In Multiclass classification,
 */
    public class SupportVector implements Serializable {

        private static final long serialVersionUID = -6479987901333394970L;

        //Map<String, Double> or double[] or SparseVector depending on the type of data.
        public final T input;
        //This would be either +1 or -1 but not the class value.
        public final int output;

        private double alpha;

        //Value of sample xi is gradient gi = yi - Sigma(s)(alpha(s)*K(i,s))
        private double gradient;


        //Soft Margin Penalty Parameter  Min Value is min(0,C.yi) and Max value is max(0,C.yi) ...for any
        public final double minAlpha;
        public final double maxAlpha;

        private KernelCache cache;
        public final double k;

        public SupportVector(T input,int output,double alpha,double gradient,double minAlpha, double maxAlpha,KernelCache cache,double k){

            this.input = input;
            this.output = output;

            this.minAlpha = minAlpha;
            this.alpha = alpha;
            this.maxAlpha = maxAlpha;

            this.gradient = gradient;
            this.cache = cache;
            this.k= k;
        }

        public double getAlpha() {
            return alpha;
        }

        public double getGradient() {
            return gradient;
        }

        public boolean isPositiveSample() {
            return output > 0;
        }

        public boolean alphaSatisfiesUpperBoxLimit() {
            return alpha < maxAlpha;
        }

        public boolean alphaSatisfiesLowerBoxLimit() {
            return alpha > minAlpha;
        }

        public void updateGradient(double lambda, double Kis,double Kjs) {
            gradient -= (lambda * (Kis - Kjs)) ;
        }

    }





}
