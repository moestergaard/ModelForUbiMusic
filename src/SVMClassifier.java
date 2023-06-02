import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;


public class SVMClassifier {


    public static double[][] changeMatrix(double[][] samples, int index, String[] distinctBSSID, String currentBSSID,
                                           String resultLevel) {
        for (int i = 0; i < distinctBSSID.length; i++) {
            if (distinctBSSID[i].equals(currentBSSID)) {
                samples[index][i] = Double.parseDouble(resultLevel);
                break;
            }
        }
        return samples;
    }

    public static Object[] randomSplitSamplesAndLabels(double[][] samples, int[] labels, double ratio) {
        Random random = new Random();

        int m = samples.length;
        int trainingRows = (int) Math.ceil(m * ratio);
        int[] trainingIndices = new int[trainingRows];
        for (int i = 0; i < trainingRows; i++) {
            int randomIndex = random.nextInt(m);
            boolean indexAlreadySelected = false;
            for (int j = 0; j < i; j++) {
                if (trainingIndices[j] == randomIndex) {
                    indexAlreadySelected = true;
                    break;
                }
            }
            if (!indexAlreadySelected) {
                trainingIndices[i] = randomIndex;
            } else {
                i--;
            }
        }

        double[][] trainingSamples = new double[trainingRows][samples[0].length];
        double[][] testSamples = new double[m - trainingRows][samples[0].length];
        int[] trainingLabels = new int[trainingRows];
        int[] testLabels = new int[m - trainingRows];

        int trainingIndex = 0;
        int testIndex = 0;

        for (int i = 0; i < m; i++) {
            if (contains(trainingIndices, i)) {
                trainingSamples[trainingIndex] = samples[i];
                trainingLabels[trainingIndex] = labels[i];
                trainingIndex++;
            } else {
                testSamples[testIndex] = samples[i];
                testLabels[testIndex] = labels[i];
                testIndex++;
            }
        }

        Object[] result = new Object[4];
        result[0] = trainingSamples;
        result[1] = testSamples;
        result[2] = trainingLabels;
        result[3] = testLabels;

        return result;
    }

    public static boolean contains(int[] arr, int target) {
        for (int i : arr) {
            if (i == target) {
                return true;
            }
        }
        return false;
    }

    public static Object[] bestModelSVM(double[][] samples, int[] labels) {
        double bestScore = Double.NEGATIVE_INFINITY;
        svm_model bestModel = null;

        Object[] newMatrices = shuffleMatrices(samples, labels);
        samples = (double[][]) newMatrices[0];
        labels = (int[]) newMatrices[1];

        for (int i = 1; i <= 5; i++) {
            Object[] splitResult = deterministicSplitMatrix(samples, labels, 1.0 / 5, i);
            double[][] trainingSamples = (double[][]) splitResult[0];
            double[][] testSamples = (double[][]) splitResult[1];
            int[] trainingLabels = (int[]) splitResult[2];
            int[] testLabels = (int[]) splitResult[3];

            svm_model model = fitModel(trainingSamples, trainingLabels);

            double score = bestModelSVMTest(model, testSamples, testLabels);

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

    public static Object[] deterministicSplitMatrix(double[][] samples, int[] labels, double ratio, int splitNumber) {
        int numRows = samples.length;
        int numCols = samples[0].length;
        int numSamples = numRows;

        int numSplits = (int) Math.ceil(1.0 / ratio);

        int numTrainRows = (int) Math.ceil((numSplits - 1) * ratio * numRows);
        int numTestRows = numRows - numTrainRows;

        int startRow = numTestRows * (splitNumber - 1);
        // int startRow = (int) Math.ceil((splitNumber - 1) * ratio * numRows);
        int endRow = numTestRows * splitNumber;
        // int endRow = startRow + numTrainRows;

        double[][] trainingSamples = new double[numTrainRows][numCols];
        double[][] testSamples = new double[numTestRows][numCols];
        int[] trainingLabels = new int[numTrainRows];
        int[] testLabels = new int[numTestRows];

        int trainingIndex = 0;
        int testIndex = 0;

        for (int i = 0; i < numSamples; i++) {
            if (i >= startRow && i < endRow) {
                testSamples[testIndex] = Arrays.copyOf(samples[i], numCols);
                testLabels[testIndex] = labels[i];
                testIndex++;
            } else {
                trainingSamples[trainingIndex] = Arrays.copyOf(samples[i], numCols);
                trainingLabels[trainingIndex] = labels[i];
                trainingIndex++;
            }
        }

        return new Object[] { trainingSamples, testSamples, trainingLabels, testLabels };
    }


    public static svm_model fitModel(double[][] trainingSamples, int[] labelsTrainingSamples) {
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
        param.gamma = 0.000014522741150737315;
        param.C = 10;
        param.cache_size = 20000;
        param.probability = 0;

        // Set class weights for balanced weight between classes
        double[] classWeights = {1.0, 1.0, 1.0};  // Adjust weights based on the number of classes
        param.nr_weight = classWeights.length;
        param.weight_label = new int[param.nr_weight];
        param.weight = new double[param.nr_weight];
        for (int i = 0; i < classWeights.length; i++) {
            param.weight_label[i] = i + 1;
            param.weight[i] = classWeights[i];
        }

        svm.svm_set_print_string_function( e -> {});

        /*
        final double[] target = new double[prob.l];
        System.out.println("Target accuracy: " + target);

        svm.svm_cross_validation(prob, param, 5, target);

        // Work out how many classifications were correct.
        int totalCorrect = 0;
        for( int i = 0; i < prob.l; i++ )
            if( target[i] == prob.y[i] )
                totalCorrect++;
        // Calculate the accuracy
        final double accuracy = 100.0 * totalCorrect / prob.l;
        System.out.print("Cross Validation Accuracy = "+accuracy+"%\n");

         */
        // System.out.println("Target accuracy after cross validation: " + target);

        // svm_model bestModel = bestModelSVM();


        return svm.svm_train(prob, param);
    }

    public static double bestModelSVMTest(svm_model model, double[][] testSamples, int[] labelsTestSamples) {
        int correct = 0;
        for (int i = 0; i < testSamples.length; i++) {
            svm_node[] sample = new svm_node[testSamples[i].length];
            for (int j = 0; j < testSamples[i].length; j++) {
                svm_node node = new svm_node();
                node.index = j + 1;
                node.value = testSamples[i][j];
                sample[j] = node;
            }
            double prediction = svm.svm_predict(model, sample);
            if (prediction == labelsTestSamples[i]) {
                correct++;
            }
        }
        return (double) correct / testSamples.length;
    }

    /**
     * Tested here before used in UbiMusic
     * @param model
     * @param datapoint
     * @return
     */
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

    public static Object[] shuffleMatrices(double[][] testSamples, int[] labelsTestSample) {
        int rows = testSamples.length;
        int cols = testSamples[0].length;

        double[][] combinedMatrix = new double[rows][cols + 1];

        // Combining the test samples and labels horizontally
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                combinedMatrix[i][j] = testSamples[i][j];
            }
            combinedMatrix[i][cols] = labelsTestSample[i];
        }

        // Shuffling the combined matrix
        Random rand = new Random();
        for (int i = rows - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            double[] temp = combinedMatrix[i];
            combinedMatrix[i] = combinedMatrix[j];
            combinedMatrix[j] = temp;
        }

        // Splitting the shuffled matrix back into separate test samples and labels
        double[][] shuffledTestSamples = new double[rows][cols];
        int[] shuffledLabelsTestSample = new int[rows];

        for (int i = 0; i < rows; i++) {
            shuffledTestSamples[i] = Arrays.copyOfRange(combinedMatrix[i], 0, cols);
            shuffledLabelsTestSample[i] = (int) combinedMatrix[i][cols];
        }

        Object[] result = new Object[2];
        result[0] = shuffledTestSamples;
        result[1] = shuffledLabelsTestSample;

        return result;
    }
}
