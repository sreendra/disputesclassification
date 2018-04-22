package com.abc.common.utils.math.kernels;


import static com.abc.common.utils.MLUtils.distance;

import java.io.Serializable;

import com.abc.disputes.classification.data.models.SparseVector;

/**
 * The Laplacian Kernel. k(u, v) = e<sup>-||u-v|| / &sigma;</sup>,
 * where &sigma; &gt; 0 is the scale parameter of the kernel.

 * @author Haifeng Li
 */
public class SparseLaplacianKernel implements MercerKernel<SparseVector>, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The width of the kernel.
     */
    private double gamma;

    /**
     * Constructor.
     * @param sigma the smooth/width parameter of Laplacian kernel.
     */
    public SparseLaplacianKernel(double sigma) {
        if (sigma <= 0)
            throw new IllegalArgumentException("sigma is not positive.");

        this.gamma = 1.0 / sigma;
    }

    @Override
    public String toString() {
        return String.format("Sparse Laplacian kernel (\u02E0 = %.4f)", 1.0/gamma);
    }

    @Override
    public double k(SparseVector x, SparseVector y) {
        return Math.exp(-gamma * distance(x, y));
    }
}
