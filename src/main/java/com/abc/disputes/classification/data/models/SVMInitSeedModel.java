package com.abc.disputes.classification.data.models;

import static com.abc.common.utils.MLConstants.MIN_SVM_SEED_SET;

import com.abc.common.utils.MLConstants;

public class SVMInitSeedModel {

    private int numberOfPositiveSamples;
    private int numberOfNegativeSamples;

    public void incrementSeedSamplesFreq(int outputClass) {

        numberOfPositiveSamples = outputClass > 0 ? numberOfPositiveSamples+1 :numberOfPositiveSamples;
        numberOfNegativeSamples = outputClass < 0 ? numberOfNegativeSamples+1 :numberOfNegativeSamples;

    }

    public boolean requiresMoreSamples() {

        return numberOfPositiveSamples < MIN_SVM_SEED_SET || numberOfNegativeSamples < MIN_SVM_SEED_SET;
    }

    public boolean isMinSeedCountDeficit(int outputClass) {

    return (outputClass > 0 ? numberOfPositiveSamples : numberOfNegativeSamples)  < MIN_SVM_SEED_SET  ;

    }

    public void addPositiveAndNegativeSamples(int numberOfPositiveSamples,int numberOfNegativeSamples) {
        this.numberOfPositiveSamples +=numberOfPositiveSamples;
        this.numberOfNegativeSamples +=numberOfNegativeSamples;
    }


    public int getNumberOfPositiveSamples() {
        return numberOfPositiveSamples;
    }

    public int getNumberOfNegativeSamples() {
        return numberOfNegativeSamples;
    }
}
