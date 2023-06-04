import libsvm.*;

public class ModelSVM implements IModel{
    private IMatrixManipulation matrixManipulation;

    public ModelSVM(IMatrixManipulation matrixManipulation) {
        this.matrixManipulation = matrixManipulation;
    }

    public Object[] bestModel(double[][] samples, int[] labels) {
        double bestScore = Double.NEGATIVE_INFINITY;
        svm_model bestModel = null;

        Object[] newMatrices = matrixManipulation.shuffleMatrices(samples, labels);
        samples = (double[][]) newMatrices[0];
        labels = (int[]) newMatrices[1];

        for (int i = 1; i <= 5; i++) {
            Object[] splitResult = matrixManipulation.deterministicSplitMatrix(samples, labels, 1.0 / 5, i);
            double[][] trainingSamples = (double[][]) splitResult[0];
            double[][] testSamples = (double[][]) splitResult[1];
            int[] trainingLabels = (int[]) splitResult[2];
            int[] testLabels = (int[]) splitResult[3];

            svm_model model = fitModel(trainingSamples, trainingLabels);

            double score = modelTest(model, testSamples, testLabels);

            if (score > bestScore) {
                bestScore = score;
                bestModel = model;
            }
        }

        Object[] result = new Object[2];
        result[0] = bestModel;
        result[1] = bestScore;

        return result;
    }

    private svm_model fitModel(double[][] trainingSamples, int[] labelsTrainingSamples) {
        svm_problem prob = new svm_problem();
        int dataSize = trainingSamples.length;
        prob.l = dataSize;
        prob.x = new svm_node[dataSize][];
        prob.y = new double[dataSize];

        for (int i = 0; i < dataSize; i++) {
            svm_node[] sample = new svm_node[trainingSamples[i].length];
            for (int j = 0; j < trainingSamples[i].length; j++) {
                svm_node node = new svm_node();
                node.index = j + 1;
                node.value = trainingSamples[i][j];
                sample[j] = node;
            }
            prob.x[i] = sample;
            prob.y[i] = labelsTrainingSamples[i];
        }

        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.gamma = 0.000015;
        param.C = 10;
        param.cache_size = 20000;
        param.probability = 0;

        // Set class weights for balanced weight between classes
        double[] classWeights = {1.0, 1.0, 1.0};
        param.nr_weight = classWeights.length;
        param.weight_label = new int[param.nr_weight];
        param.weight = new double[param.nr_weight];
        for (int i = 0; i < classWeights.length; i++) {
            param.weight_label[i] = i + 1;
            param.weight[i] = classWeights[i];
        }

        svm.svm_set_print_string_function(e -> {});

        return svm.svm_train(prob, param);
    }

    public double modelTest(svm_model model, double[][] testSamples, int[] labelsTestSamples) {
        int correct = 0;
        for (int i = 0; i < testSamples.length; i++) {

            double prediction = predict(model, testSamples[i]);

            if (prediction == labelsTestSamples[i]) {
                correct++;
            }
        }
        return (double) correct / testSamples.length;
    }

    public static double predict(svm_model model, double[] datapoint) {
        svm_node[] nodes = new svm_node[datapoint.length];

        for (int i = 0; i < datapoint.length; i++) {
            svm_node node = new svm_node();
            node.index = i + 1; // Index starts from 1 in LibSVM
            node.value = datapoint[i];
            nodes[i] = node;
        }

        return svm.svm_predict(model, nodes);
    }
}
