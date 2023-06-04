public interface IMatrixManipulation {
    double[][] changeMatrix(double[][] samples, int index, String[] distinctBSSID, String currentBSSID, String resultLevel);
    Object[] deterministicSplitMatrix(double[][] samples, int[] labels, double ratio, int splitNumber);
    Object[] randomSplitSamplesAndLabels(double[][] samples, int[] labels, double ratio);
    Object[] shuffleMatrices(double[][] testSamples, int[] labelsTestSample);
}
