package com.abc.common.utils.math.kernels;


import static com.abc.common.utils.MLUtils.squaredDistance;

import java.io.Serializable;

import com.abc.disputes.classification.data.models.SparseVector;

/**
 * The Gaussian Mercer Kernel. k(u, v) = e<sup>-||u-v||<sup>2</sup> / (2 * &sigma;<sup>2</sup>)</sup>,
 * where &sigma; &gt; 0 is the scale parameter of the kernel.
 * <p>
 * The Gaussian kernel is a good choice for a great deal of applications,
 * although sometimes it is remarked as being over used.

 * @author Haifeng Li
 */
public class SparseGaussianKernel implements MercerKernel<SparseVector>, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The width of the kernel.
     */
    private double gamma;
    
    /**
     * Constructor.
     * @param sigma the smooth/width parameter of Gaussian kernel.
     */
    public SparseGaussianKernel(double sigma) {
        if (sigma <= 0)
            throw new IllegalArgumentException("sigma is not positive.");

        this.gamma = 0.5 / (sigma * sigma);
    }

    @Override
    public String toString() {
        return String.format("Sparse Gaussian Kernel (\u02E0 = %.4f)", Math.sqrt(0.5/gamma));
    }

    @Override
    public double k(SparseVector x, SparseVector y) {
        return Math.exp(-gamma *squaredDistance(x, y));
    }
}
