import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

public class SVMClassifier {

    public static double SVMOwnDataSet(String[] locations, String filename, double partOfData) {
        double[][] trainingSamplesOverall;
        double[][] testSamplesOverall;
        int[] trainingLabelsOverall;
        int[] testLabelsOverall;

        Object[] result = getSamplesAndLabelsFromOneFile(locations, filename, partOfData);

        trainingSamplesOverall = (double[][]) result[0];
        testSamplesOverall = (double[][]) result[1];
        trainingLabelsOverall = (int[]) result[2];
        testLabelsOverall = (int[]) result[3];

        Object[] modelResult = bestModelSVM(trainingSamplesOverall, trainingLabelsOverall);
        svm_model bestModel = (svm_model) modelResult[0];
        double score = (double) modelResult[1];

        if (partOfData == 1)
            return score;

        score = bestModelSVMTest(bestModel, testSamplesOverall, testLabelsOverall);
        return score;
    }

    public static Object[] getSamplesAndLabelsFromOneFile(String[] locations, String filename, double partOfData) {
        Object[] result = new Object[4];

        Object[] distinctBSSIDDataPoints = extractDistinctBSSIDAndNumberOfDataPoints(locations, filename);
        String[] distinctBSSID = (String[]) distinctBSSIDDataPoints[0];
        int dataPoints = (int) distinctBSSIDDataPoints[1];

        Object[] dataResult = extractData(locations, filename, distinctBSSID, dataPoints);
        double[][] samples = (double[][]) dataResult[0];
        int[] labels = (int[]) dataResult[1];

        Object[] splitResult = randomSplitSamplesAndLabels(samples, labels, partOfData);
        double[][] trainingSamplesOverall = (double[][]) splitResult[0];
        double[][] testSamplesOverall = (double[][]) splitResult[1];
        int[] trainingLabelsOverall = (int[]) splitResult[2];
        int[] testLabelsOverall = (int[]) splitResult[3];

        result[0] = trainingSamplesOverall;
        result[1] = testSamplesOverall;
        result[2] = trainingLabelsOverall;
        result[3] = testLabelsOverall;

        return result;
    }

    public static Object[] extractDistinctBSSIDAndNumberOfDataPoints(String[] locations, String filename) {
        List<String> distinctBSSIDList = new ArrayList<>();
        int dataPoints = 0;
        boolean dataPointIncluded = false;

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("BSSID")) {
                    if (dataPointIncluded) {
                        String bssid = line.split(": ")[1].strip();
                        boolean alreadyIncludedInDistinctBSSID = distinctBSSIDList.contains(bssid);
                        if (!alreadyIncludedInDistinctBSSID) {
                            distinctBSSIDList.add(bssid);
                        }
                    }
                }
                if (line.contains("Scanning")) {
                    dataPointIncluded = false;
                    for (String location : locations) {
                        if (line.contains(location)) {
                            dataPoints++;
                            dataPointIncluded = true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] distinctBSSID = distinctBSSIDList.toArray(new String[0]);
        Object[] result = new Object[2];
        result[0] = distinctBSSID;
        result[1] = dataPoints;

        return result;
    }

    public static Object[] extractData(String[] locations, String filename, String[] distinctBSSID, int numberOfSamples) {
        double[][] samples = new double[numberOfSamples][distinctBSSID.length];
        int[] labels = new int[numberOfSamples];

        int index = 0;
        String currentBSSID = "";
        boolean dataPointIncluded = false;

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("********")) {
                    index++;
                }
                if (line.contains("BSSID")) {
                    if (dataPointIncluded) {
                        currentBSSID = line.split(": ")[1].split(" ")[0];
                    }
                }
                if (line.contains("Scanning")) {
                    dataPointIncluded = false;
                    String location = line.split(": ")[1].strip();
                    for (int i = 0; i < locations.length; i++) {
                        if (locations[i].equals(location)) {
                            dataPointIncluded = true;
                            labels[index] = i;
                        }
                    }
                    if (!dataPointIncluded) {
                        index--;
                    }
                }
                if (line.contains("ResultLevel")) {
                    if (dataPointIncluded) {
                        String[] resultLevel = line.split(": ")[1].split(" ");
                        samples = changeMatrix(samples, index, distinctBSSID, currentBSSID, resultLevel[0]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Object[] result = new Object[2];
        result[0] = samples;
        result[1] = labels;

        return result;
    }

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
        int m = samples.length;
        int trainingSize = (int) Math.ceil(m * ratio);
        int startIndex = (splitNumber - 1) * trainingSize;
        int endIndex = splitNumber * trainingSize;
        if (endIndex > m) {
            endIndex = m;
        }

        double[][] trainingSamples = new double[endIndex - startIndex][samples[0].length];
        double[][] testSamples = new double[m - (endIndex - startIndex)][samples[0].length];
        int[] trainingLabels = new int[endIndex - startIndex];
        int[] testLabels = new int[m - (endIndex - startIndex)];

        int trainingIndex = 0;
        int testIndex = 0;

        for (int i = 0; i < m; i++) {
            if (i >= startIndex && i < endIndex) {
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

        /*
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.C = 1;
        param.cache_size = 20000;

         */

        // svm_parameter param = new svm_parameter('-t 2 -s 0 -b 1 -c 1 -w1 1 -w-1 1')


        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.gamma = 0.000014522741150737315;
        param.C = 1;
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



        svm.svm_set_print_string_function(s -> {});

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
