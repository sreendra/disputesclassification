package com.abc.common.utils.math.kernels;

/**
 *Copied from Smile
 * 
 * @author Haifeng Li
 */
public interface MercerKernel<T> {

    /**
     * Kernel function.
     */
    public double k(T x, T y);
}
