package com.abc.common.utils;

import java.util.Iterator;

import com.abc.disputes.classification.data.models.SparseVector;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

/**
 * Created by rachikkala on 4/21/18.
 */
public class MLUtils {

    /**
     * Returns the dot product between two vectors.
     */
    public static double dot(double[] x, double[] y) {

        if (x.length != y.length) {
            throw new IllegalArgumentException("Arrays have different length.");
        }

        double p = 0.0;
        for (int i = 0; i < x.length; i++) {
            p += x[i] * y[i];
        }

        return p;
    }

    private static boolean firstRNG = true;
    /**
     * High quality random number generator.
     */
    private static ThreadLocal<Random> random = new ThreadLocal<Random>() {
        protected synchronized Random initialValue() {
            if (firstRNG) {
                // For the first RNG, we use the default seed so that we can
                // get repeatable results for random algorithms.
                // Note that this may or may not be the main thread.
                firstRNG = false;
                return new Random();
            } else {
                // Make sure other threads not to use the same seed.
                // This is very important for some algorithms such as random forest.
                // Otherwise, all trees of random forest are same except the main thread one.

                java.security.SecureRandom sr = new java.security.SecureRandom();
                byte[] bytes = sr.generateSeed(Long.BYTES);
                long seed = 0;
                for (int i = 0; i < Long.BYTES; i++) {
                    seed <<= 8;
                    seed |= (bytes[i] & 0xFF);
                }

                return new Random(seed);
            }
        }
    };

    /**
     * Generates a permutation of 0, 1, 2, ..., n-1, which is useful for
     * sampling without replacement.
     */
    public static int[] permutate(int n) {
        return random.get().permutate(n);
    }

    /**
     * Swap two elements of an array.
     */
    public static void swap(int[] x, int i, int j) {
        int s = x[i];
        x[i] = x[j];
        x[j] = s;
    }

    /**
     * Swap two elements of an array.
     */
    public static void swap(float[] x, int i, int j) {
        float s = x[i];
        x[i] = x[j];
        x[j] = s;
    }

    /**
     * Swap two elements of an array.
     */
    public static void swap(double[] x, int i, int j) {
        double s = x[i];
        x[i] = x[j];
        x[j] = s;
    }

    /**
     * Swap two elements of an array.
     */
    public static void swap(Object[] x, int i, int j) {
        Object s = x[i];
        x[i] = x[j];
        x[j] = s;
    }

    /**
     * Unitize an array so that L1 norm of x is 1.
     *
     * @param x an array of nonnegative double
     */
    public static void unitize1(double[] x) {
        double n = norm1(x);

        for (int i = 0; i < x.length; i++) {
            x[i] /= n;
        }
    }

    /**
     * L1 vector norm.
     */
    public static double norm1(double[] x) {
        double norm = 0.0;

        for (double n : x) {
            norm += abs(n);
        }

        return norm;
    }


    /**
     * The squared Euclidean distance.
     */
    public static double squaredDistance(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("Input vector sizes are different.");
        }

        double sum = 0.0;
        for (int i = 0; i < x.length; i++) {
            sum += sqr(x[i] - y[i]);
        }

        return sum;
    }

    /**
     * Returns x * x.
     */
    public static double sqr(double x) {
        return x * x;
    }

    public static double distance(double[] x, double[] y) {
        return Math.sqrt(squaredDistance(x, y));
    }

    public static double squaredDistance(SparseVector x, SparseVector y) {
        Iterator<SparseVector.TermEntry> it1 = x.iterator();
        Iterator<SparseVector.TermEntry> it2 = y.iterator();
        SparseVector.TermEntry e1 = it1.hasNext() ? it1.next() : null;
        SparseVector.TermEntry e2 = it2.hasNext() ? it2.next() : null;

        double sum = 0.0;
        while (e1 != null && e2 != null) {
            if (e1.index == e2.index) {
                sum += sqr(e1.tfIdf - e2.tfIdf);
                e1 = it1.hasNext() ? it1.next() : null;
                e2 = it2.hasNext() ? it2.next() : null;
            } else if (e1.index > e2.index) {
                sum += sqr(e2.tfIdf);
                e2 = it2.hasNext() ? it2.next() : null;
            } else {
                sum += sqr(e1.tfIdf);
                e1 = it1.hasNext() ? it1.next() : null;
            }
        }

        while (it1.hasNext()) {
            sum += sqr(it1.next().tfIdf);
        }

        while (it2.hasNext()) {
            sum += sqr(it2.next().tfIdf);
        }

        return sum;
    }

    public static double dot(SparseVector x, SparseVector y) {
        Iterator<SparseVector.TermEntry> it1 = x.iterator();
        Iterator<SparseVector.TermEntry> it2 = y.iterator();
        SparseVector.TermEntry e1 = it1.hasNext() ? it1.next() : null;
        SparseVector.TermEntry e2 = it2.hasNext() ? it2.next() : null;

        double s = 0.0;
        while (e1 != null && e2 != null) {
            if (e1.index == e2.index) {
                s += e1.tfIdf * e2.tfIdf;
                e1 = it1.hasNext() ? it1.next() : null;
                e2 = it2.hasNext() ? it2.next() : null;
            } else if (e1.index > e2.index) {
                e2 = it2.hasNext() ? it2.next() : null;
            } else {
                e1 = it1.hasNext() ? it1.next() : null;
            }
        }

        return s;
    }

    public static double distance(SparseVector x, SparseVector y) {
        return sqrt(squaredDistance(x, y));
    }
}