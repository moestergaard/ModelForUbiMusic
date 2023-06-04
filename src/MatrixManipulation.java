import java.util.Arrays;
import java.util.Random;

public class MatrixManipulation implements IMatrixManipulation {

    public double[][] changeMatrix(double[][] samples, int index, String[] distinctBSSID, String currentBSSID, String resultLevel) {
        for (int i = 0; i < distinctBSSID.length; i++) {
            if (distinctBSSID[i].equals(currentBSSID)) {
                samples[index][i] = Double.parseDouble(resultLevel);
                break;
            }
        }
        return samples;
    }

    public Object[] deterministicSplitMatrix(double[][] samples, int[] labels, double ratio, int splitNumber) {
        int numRows = samples.length;
        int numCols = samples[0].length;

        int numSplits = (int) Math.ceil(1.0 / ratio);

        int numTrainRows = (int) Math.ceil((numSplits - 1) * ratio * numRows);
        int numTestRows = numRows - numTrainRows;

        int startRow = numTestRows * (splitNumber - 1);
        int endRow = numTestRows * splitNumber;

        double[][] trainingSamples = new double[numTrainRows][numCols];
        double[][] testSamples = new double[numTestRows][numCols];
        int[] trainingLabels = new int[numTrainRows];
        int[] testLabels = new int[numTestRows];

        int trainingIndex = 0;
        int testIndex = 0;

        for (int i = 0; i < numRows; i++) {
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

    public Object[] randomSplitSamplesAndLabels(double[][] samples, int[] labels, double ratio) {
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

    public Object[] shuffleMatrices(double[][] testSamples, int[] labelsTestSample) {
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

    private boolean contains(int[] arr, int target) {
        for (int i : arr) {
            if (i == target) {
                return true;
            }
        }
        return false;
    }
}
