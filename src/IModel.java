import libsvm.svm_model;

public interface IModel {
    Object[] bestModel(double[][] samples, int[] labels);
    double modelTest(svm_model model, double[][] testSamples, int[] labelsTestSamples);
}
