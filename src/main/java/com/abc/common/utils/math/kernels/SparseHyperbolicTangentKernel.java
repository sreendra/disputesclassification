package com.abc.common.utils.math.kernels;


import static com.abc.common.utils.MLUtils.dot;

import java.io.Serializable;

import com.abc.disputes.classification.data.models.SparseVector;

/**
 * Copied from Smile
 *
 * @author Haifeng Li
 */
public class SparseHyperbolicTangentKernel implements MercerKernel<SparseVector>, Serializable {
    private static final long serialVersionUID = 1L;

    private double scale;
    private double offset;

    /**
     * Constructor with scale 1.0 and offset 0.0.
     */
    public SparseHyperbolicTangentKernel() {
        this(1.0, 0.0);
    }

    /**
     * Constructor.
     */
    public SparseHyperbolicTangentKernel(double scale, double offset) {
        this.scale = scale;
        this.offset = offset;
    }

    @Override
    public String toString() {
        return String.format("Sparse Hyperbolic Tangent Kernel (scale = %.4f, offset = %.4f)", scale, offset);
    }

    @Override
    public double k(SparseVector x, SparseVector y) {
        return Math.tanh(scale * dot(x, y) + offset);
    }
}
