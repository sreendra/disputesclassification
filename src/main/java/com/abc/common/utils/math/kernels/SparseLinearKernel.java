package com.abc.common.utils.math.kernels;

import static com.abc.common.utils.MLUtils.dot;

import java.io.Serializable;

import com.abc.disputes.classification.data.models.SparseVector;

/**
 * The linear dot product kernel on sparse arrays. When using a linear kernel, input space is
 * identical to feature space.
 *
 * @author Haifeng Li
 */
public class SparseLinearKernel implements MercerKernel<SparseVector>, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public SparseLinearKernel() {
    }

    @Override
    public String toString() {
        return "Sparse Linear Kernel";
    }

    @Override
    public double k(SparseVector x, SparseVector y) {
        return dot(x, y);
    }
}
