package com.abc.disputes.classification.data.models;


import com.abc.common.utils.KernelCache;
import com.abc.disputes.classification.data.models.DocumentRow;

import java.io.Serializable;
import java.util.Map;

/*
 * This is implementation of the below papers.
 * Antoine Bordes, Seyda Ertekin, Jason Weston and Leon Bottou. Fast Kernel Classifiers with Online and Active Learning, Journal of Machine Learning Research, 6:1579-1619, 2005.
 * Tobias Glasmachers and Christian Igel. Second Order SMO Improves SVM Online and Active Learning.
 * Christopher J. C. Burges. A Tutorial on Support Vector Machines for Pattern Recognition. Data Mining and Knowledge Discovery 2:121-167, 1998.
 * Referred smile library for the coding part.
 *
 * In Multiclass classification,
 */
public class SupportVector<T> implements Serializable {

    private static final long serialVersionUID = -6479987901333394970L;

    //Map<String, Double> or double[] or SparseVector depending on the type of data.
    public final T input;
    //This would be either +1 or -1 but not the class value.
    public final int output;

    private double alpha;

    //Value of sample xi is gradient gi = yi - Sigma(s)(alpha(s)*K(i,s))
    private double gradient;


    //Soft Margin Penalty Parameter  Min Value is min(0,C.yi) and Max value is max(0,C.yi) ...for any
    public final int minAlpha;
    public final int maxAlpha;

    public final KernelCache cache;
    public final double k;

    public SupportVector(T input,int output,double alpha,double gradient,int minAlpha, int maxAlpha,double k){

        this.input = input;
        this.output = output;

        this.minAlpha = minAlpha;
        this.alpha = alpha;
        this.maxAlpha = maxAlpha;

        this.gradient = gradient;

        this.cache = new KernelCache();
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

}
