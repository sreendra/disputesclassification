package com.abc.common.utils.math.kernels;


import static com.abc.common.utils.MLUtils.dot;

import java.io.Serializable;

/**
 * The linear dot product kernel. When using a linear kernel, input space is
 * identical to feature space.
 *
 * @author Haifeng Li
 */
public class LinearKernel implements MercerKernel<double[]>, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public LinearKernel() {
    }

    @Override
    public String toString() {
        return "Linear Kernel";
    }

    @Override
    public double k(double[] x, double[] y) {
        return dot(x, y);
    }
}
