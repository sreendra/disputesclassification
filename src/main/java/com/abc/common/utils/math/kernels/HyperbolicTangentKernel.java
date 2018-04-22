package com.abc.common.utils.math.kernels;

import static com.abc.common.utils.MLUtils.dot;

import java.io.Serializable;

/**
 *Copied from Smile
 *
 * @author Haifeng Li
 */
public class HyperbolicTangentKernel implements MercerKernel<double[]>, Serializable {
    private static final long serialVersionUID = 1L;

    private double scale;
    private double offset;

    /**
     * Constructor.
     */
    public HyperbolicTangentKernel() {
        this(1, 0);
    }

    /**
     * Constructor.
     */
    public HyperbolicTangentKernel(double scale, double offset) {
        this.scale = scale;
        this.offset = offset;
    }

    @Override
    public String toString() {
        return String.format("Hyperbolic Tangent Kernel (scale = %.4f, offset = %.4f)", scale, offset);
    }

    @Override
    public double k(double[] x, double[] y) {
        if (x.length != y.length)
            throw new IllegalArgumentException(String.format("Arrays have different length: x[%d], y[%d]", x.length, y.length));

        return Math.tanh(scale * dot(x,y) + offset);
    }
}
