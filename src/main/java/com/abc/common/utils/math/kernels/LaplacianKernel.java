package com.abc.common.utils.math.kernels;

import java.io.Serializable;

import com.abc.common.utils.MLUtils;

/**
 * The Laplacian Kernel. k(u, v) = e<sup>-||u-v|| / &sigma;</sup>,
 * where &sigma; &gt; 0 is the scale parameter of the kernel.

 * @author Haifeng Li
 */
public class LaplacianKernel implements MercerKernel<double[]>, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The width of the kernel.
     */
    private double gamma;

    /**
     * Constructor.
     * @param sigma the smooth/width parameter of Laplacian kernel.
     */
    public LaplacianKernel(double sigma) {
        if (sigma <= 0)
            throw new IllegalArgumentException("sigma is not positive.");

        this.gamma = 1.0 / sigma;
    }

    @Override
    public String toString() {
        return String.format("Laplacian Kernel (\u02E0 = %.4f)", 1.0/gamma);
    }

    @Override
    public double k(double[] x, double[] y) {
        if (x.length != y.length)
            throw new IllegalArgumentException(String.format("Arrays have different length: x[%d], y[%d]", x.length, y.length));

        return Math.exp(-gamma * MLUtils.distance(x, y));
    }
}
