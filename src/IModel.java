import libsvm.svm_model;

public interface IModel {
    Object[] bestModelSVM(double[][] samples, int[] labels);
    svm_model fitModel(double[][] trainingSamples, int[] labelsTrainingSamples);

}
