/*
import libsvm.*;
import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        String filePath = "/Users/signethomsen/IdeaProjects/ModelUbiMusic/src/WifiData230424_17-21.txt";
        double partOfData = 1.0 / 3.0; // Percentage of data to use for training

        List<WifiDataEntry> wifiData = parseWifiDataFile(filePath);
        int dataSize = wifiData.size();
        int trainingSize = (int) (dataSize * partOfData);
        List<WifiDataEntry> trainingData = wifiData.subList(0, trainingSize);
        List<WifiDataEntry> testingData = wifiData.subList(trainingSize, dataSize);

        // Get distinct BSSIDs from the training data
        Set<String> distinctBSSIDs = getDistinctBSSIDs(trainingData);
        String[] classLabels = distinctBSSIDs.toArray(new String[0]);

        svm_model bestModel = trainAndEvaluateModels(trainingData, testingData, classLabels);
        saveModel(bestModel, "wifi_model.model");
        saveDistinctBSSIDs(distinctBSSIDs, "distinct_bssids.txt");
    }

    private static List<WifiDataEntry> parseWifiDataFile(String filePath) {
        List<WifiDataEntry> wifiData = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            WifiDataEntry entry = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("SSID:")) {
                    if (entry == null) {
                        entry = new WifiDataEntry();
                    }
                    String[] parts = line.split(":");
                    entry.setSSID(parts[1].trim());
                } else if (line.startsWith("BSSID:")) {
                    if (entry != null) {
                        String[] parts = line.split(":");
                        entry.setBSSID(parts[1].trim());
                    }
                } else if (line.startsWith("ResultLevel:")) {
                    if (entry != null) {
                        String[] parts = line.split(":");
                        entry.setResultLevel(Integer.parseInt(parts[1].trim()));
                    }
                } else if (line.startsWith("Frequency:")) {
                    if (entry != null) {
                        String[] parts = line.split(":");
                        entry.setFrequency(Integer.parseInt(parts[1].trim()));
                    }
                } else if (line.startsWith("Timestamp:")) {
                    if (entry != null) {
                        String[] parts = line.split(":");
                        entry.setTimestamp(Long.parseLong(parts[1].trim()));
                        wifiData.add(entry);
                        entry = null;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return wifiData;
    }

    private static svm_model trainAndEvaluateModels(List<WifiDataEntry> trainingData, List<WifiDataEntry> testingData, String[] classLabels) {
        svm_problem problem = createSVMProblem(trainingData, classLabels);

        int nrFold = 5; // Number of folds for cross-validation
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.gamma = 0.5;
        // param.gamma = 0.000014522741150737315;
        param.C = 1;

        svm_model bestModel = null;
        double bestAccuracy = 0d;

        // Perform cross-validation and select the best model
        for (int i = 0; i < nrFold; i++) {
            svm_model model = svm.svm_train(problem, param);
            double accuracy = evaluateModel(model, testingData, classLabels);

            if (accuracy > bestAccuracy) {
                bestAccuracy = accuracy;
                bestModel = model;
            }
        }

        System.out.println("Best accuracy: " + (bestAccuracy * 100) + "%");
        return bestModel;
    }

    private static svm_problem createSVMProblem(List<WifiDataEntry> trainingData, String[] classLabels) {
        svm_problem problem = new svm_problem();
        int dataSize = trainingData.size();
        problem.l = dataSize;
        problem.x = new svm_node[dataSize][classLabels.length * 2];
        problem.y = new double[dataSize];

        for (int i = 0; i < dataSize; i++) {
            WifiDataEntry entry = trainingData.get(i);
            problem.x[i] = createSVMNodeArray(entry.getFeatureVector(classLabels));
            problem.y[i] = entry.getTrueClass(classLabels);
        }

        return problem;
    }

    private static svm_node[] createSVMNodeArray(double[] featureVector) {
        svm_node[] svmNodes = new svm_node[featureVector.length];
        for (int i = 0; i < featureVector.length; i++) {
            svmNodes[i] = new svm_node();
            svmNodes[i].index = i + 1;
            svmNodes[i].value = featureVector[i];
        }
        return svmNodes;
    }

    private static double evaluateModel(svm_model model, List<WifiDataEntry> testingData, String[] classLabels) {
        int correct = 0;
        int total = testingData.size();

        for (WifiDataEntry entry : testingData) {
            double[] featureVector = entry.getFeatureVector(classLabels);
            svm_node[] svmNodes = createSVMNodeArray(featureVector);
            double predictedClass = svm.svm_predict(model, svmNodes);

            if (predictedClass == entry.getTrueClass(classLabels)) {
                correct++;
            }
        }

        double accuracy = (double) correct / total;
        return accuracy;
    }

    private static Set<String> getDistinctBSSIDs(List<WifiDataEntry> data) {
        Set<String> distinctBSSIDs = new HashSet<>();
        for (WifiDataEntry entry : data) {
            distinctBSSIDs.add(entry.getBSSID());
        }
        return distinctBSSIDs;
    }

    private static void saveModel(svm_model model, String filePath) {
        try {
            svm.svm_save_model(filePath, model);
            System.out.println("Model saved to: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveDistinctBSSIDs(Set<String> distinctBSSIDs, String filePath) {
        try (PrintWriter writer = new PrintWriter(filePath)) {
            for (String bssid : distinctBSSIDs) {
                writer.println(bssid);
            }
            System.out.println("Distinct BSSIDs saved to: " + filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static class WifiDataEntry {
        private String SSID;
        private String BSSID;
        private int resultLevel;
        private int frequency;
        private long timestamp;

        public void setSSID(String SSID) {
            this.SSID = SSID;
        }

        public void setBSSID(String BSSID) {
            this.BSSID = BSSID;
        }

        public void setResultLevel(int resultLevel) {
            this.resultLevel = resultLevel;
        }

        public void setFrequency(int frequency) {
            this.frequency = frequency;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getBSSID() {
            return BSSID;
        }

        public double[] getFeatureVector(String[] classLabels) {
            double[] featureVector = new double[classLabels.length * 2];
            Arrays.fill(featureVector, 0.0);

            int index = Arrays.asList(classLabels).indexOf(BSSID);
            if (index != -1) {
                featureVector[index] = resultLevel;
                featureVector[classLabels.length + index] = frequency;
            }

            return featureVector;
        }

        public double getTrueClass(String[] classLabels) {
            int index = Arrays.asList(classLabels).indexOf(BSSID);
            if (index != -1) {
                return index + 1; // Class labels in LibSVM start from 1
            }
            return -1;
        }
    }
}

*/

/*
import libsvm.*;
import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        // Set the file path for the input text file
        String filePath = "/Users/signethomsen/IdeaProjects/ModelUbiMusic/src/WifiData230424_17-21.txt";

        // Set the names for the classes
        String[] classLabels = {"Kontor", "Stue", "Køkken"};

        // Set the ratio of data to be used for training
        double partOfData = 1.0 / 3.0;

        // Parse the data from the text file
        List<WifiDataEntry> wifiData = parseWifiData(filePath);

        // Preprocess the data and split into training and testing sets
        List<List<WifiDataEntry>> trainingAndTestingData = splitWifiData(wifiData, partOfData);

        List<WifiDataEntry> trainingData = trainingAndTestingData.get(0);
        List<WifiDataEntry> testingData = trainingAndTestingData.get(1);

        // List<WifiDataEntry> trainingData = wifiData.subList(0, (int) (wifiData.size() * partOfData));
        // List<WifiDataEntry> testingData = wifiData.subList((int) (wifiData.size() * partOfData), wifiData.size());
        // Perform cross-validation and train the model
        // double bestAccuracy = crossValidation(trainingData, classLabels);

        // Train the final model on the full training data
        svm_model model = trainModel(trainingData, classLabels);

        // Evaluate the model on the testing data
        double accuracy = evaluateModel(model, testingData, classLabels);

        // Output the distinct BSSID values
        Set<String> distinctBSSIDs = getDistinctBSSIDs(wifiData);

        // Store the best model and distinct BSSIDs in separate files
        String modelFilePath = "best_model.model";
        String distinctBSSIDsFilePath = "distinct_bssids.txt";
        saveModel(model, modelFilePath);
        saveDistinctBSSIDs(distinctBSSIDs, distinctBSSIDsFilePath);

        // Print the accuracy of the model on the testing data
        System.out.println("Model accuracy on testing data: " + accuracy);
    }

    private static List<List<WifiDataEntry>> splitWifiData(List<WifiDataEntry> wifiData, double ratio) {
        Random random = new Random();
        random.setSeed(43);

        int dataSize = wifiData.size();
        int trainingSize = (int) Math.ceil(dataSize * ratio);
        List<WifiDataEntry> trainingData = new ArrayList<>(trainingSize);
        List<WifiDataEntry> testingData = new ArrayList<>(dataSize - trainingSize);

        List<Integer> trainingIndices = new ArrayList<>(trainingSize);
        for (int i = 0; i < trainingSize; i++) {
            int randomIndex = random.nextInt(dataSize);
            while (trainingIndices.contains(randomIndex)) {
                randomIndex = random.nextInt(dataSize);
            }
            trainingIndices.add(randomIndex);
            trainingData.add(wifiData.get(randomIndex));
        }

        for (int i = 0; i < dataSize; i++) {
            if (!trainingIndices.contains(i)) {
                testingData.add(wifiData.get(i));
            }
        }

        List<List<WifiDataEntry>> result = new ArrayList<>();
        result.add(trainingData);
        result.add(testingData);

        return result;
    }

    private static List<WifiDataEntry> parseWifiData(String filePath) {
        List<WifiDataEntry> wifiData = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            WifiDataEntry entry = null;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("BSSID: ")) {
                    String bssid = line.substring(7);
                    entry = new WifiDataEntry(bssid);
                    wifiData.add(entry);
                } else if (line.startsWith("ResultLevel: ")) {
                    int resultLevel = Integer.parseInt(line.substring(13));
                    if (entry != null) {
                        entry.getBssidResultLevels().put(entry.getBSSID(), resultLevel);
                    }
                } else if (line.startsWith("Scanning: ")) {
                    String trueClass = line.substring(10);
                    if (entry != null) {
                        entry.setTrueClass(trueClass);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return wifiData;
    }

    private static Set<String> getDistinctBSSIDs(List<WifiDataEntry> data) {
        Set<String> distinctBSSIDs = new HashSet<>();
        for (WifiDataEntry entry : data) {
            distinctBSSIDs.add(entry.getBSSID());
        }
        return distinctBSSIDs;
    }

    private static double crossValidation(List<WifiDataEntry> trainingData, String[] classLabels) {
        svm_problem problem = createSVMProblem(trainingData, classLabels);

        int nrFold = 5; // Number of folds for cross-validation
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.gamma = 1.3703956890699743e-05;
        param.C = 1;

        double[] target = new double[problem.l];
        svm.svm_cross_validation(problem, param, nrFold, target);

        // Compute accuracy for each fold
        double correct = 0.0;
        for (int i = 0; i < problem.l; i++) {
            if (target[i] == problem.y[i]) {
                correct++;
            }
        }

        double accuracy = correct / problem.l;
        System.out.println("Cross-validation accuracy: " + accuracy);

        return accuracy;
    }

    private static svm_model trainModel(List<WifiDataEntry> trainingData, String[] classLabels) {
        svm_problem problem = createSVMProblem(trainingData, classLabels);

        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.gamma = 1.3703956890699743e-05;
        param.C = 1;

        svm_model model = svm.svm_train(problem, param);
        return model;
    }

    private static double evaluateModel(svm_model model, List<WifiDataEntry> data, String[] classLabels) {
        svm_problem problem = createSVMProblem(data, classLabels);

        int correct = 0;
        for (int i = 0; i < problem.l; i++) {
            double trueClass = problem.y[i];
            svm_node[] nodes = problem.x[i];
            double predictedClass = svm.svm_predict(model, nodes);

            if (predictedClass == trueClass) {
                correct++;
            }
        }

        double accuracy = (double) correct / problem.l;
        return accuracy;
    }

    private static svm_problem createSVMProblem(List<WifiDataEntry> data, String[] classLabels) {
        svm_problem problem = new svm_problem();
        int dataSize = data.size();

        problem.l = dataSize;
        problem.x = new svm_node[dataSize][];
        problem.y = new double[dataSize];

        for (int i = 0; i < dataSize; i++) {
            System.out.println("i: " + i + ". datasize: " + data.size());
            WifiDataEntry entry = data.get(i);
            problem.x[i] = createSVMNodes(entry.getBssidResultLevels());
            problem.y[i] = getClassLabelIndex(entry.getTrueClass(), classLabels);
        }

        return problem;
    }

    private static svm_node[] createSVMNodes(Map<String, Integer> bssidResultLevels) {
        int numFeatures = bssidResultLevels.size();
        svm_node[] nodes = new svm_node[numFeatures];
        int index = 0;

        for (Map.Entry<String, Integer> entry : bssidResultLevels.entrySet()) {
            svm_node node = new svm_node();
            node.index = index + 1; // SVM node index starts from 1
            node.value = entry.getValue();
            nodes[index] = node;
            index++;
        }

        return nodes;
    }

    private static double getClassLabelIndex(String trueClass, String[] classLabels) {
        for (int i = 0; i < classLabels.length; i++) {
            if (trueClass.equals(classLabels[i])) {
                return i;
            }
        }
        return -1; // Class label not found
    }

    private static void saveModel(svm_model model, String filePath) {
        try {
            svm.svm_save_model(filePath, model);
            System.out.println("Model saved to file: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveDistinctBSSIDs(Set<String> distinctBSSIDs, String filePath) {
        try (PrintWriter writer = new PrintWriter(filePath)) {
            for (String bssid : distinctBSSIDs) {
                writer.println(bssid);
            }
            System.out.println("Distinct BSSIDs saved to file: " + filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static class WifiDataEntry {
        private String bssid;
        private Map<String, Integer> bssidResultLevels;
        private String trueClass;

        public WifiDataEntry(String bssid) {
            this.bssid = bssid;
            this.bssidResultLevels = new HashMap<>();
        }

        public String getBSSID() {
            return bssid;
        }

        public Map<String, Integer> getBssidResultLevels() {
            return bssidResultLevels;
        }

        public String getTrueClass() {
            return trueClass;
        }

        public void setTrueClass(String trueClass) {
            this.trueClass = trueClass;
        }
    }
}

*/







/*
import libsvm.*;
import java.io.*;
import java.util.*;

public class Main {
    private Map<String, Integer> bssidResultLevels;

    public static void main(String[] args) {
        // Set the file path for the input text file
        String filePath = "/Users/signethomsen/IdeaProjects/ModelUbiMusic/src/WifiData230424_17-21.txt";

        // Set the names for the classes
        String[] classLabels = {"Kontor", "Stue", "Køkken"};

        // Set the ratio of data to be used for training
        double partOfData = 1.0 / 3.0;

        // Parse the data from the text file
        List<WifiDataEntry> wifiData = parseWifiData(filePath);

        // Preprocess the data and split into training and testing sets
        List<List<WifiDataEntry>> trainingAndTestingData = splitWifiData(wifiData, partOfData);

        List<WifiDataEntry> trainingData = trainingAndTestingData.get(0);
        List<WifiDataEntry> testingData = trainingAndTestingData.get(1);

        // List<WifiDataEntry> trainingData = wifiData.subList(0, (int) (wifiData.size() * partOfData));
        // List<WifiDataEntry> testingData = wifiData.subList((int) (wifiData.size() * partOfData), wifiData.size());
        // Perform cross-validation and train the model
        double bestAccuracy = crossValidation(trainingData, classLabels);

        // Train the final model on the full training data
        svm_model model = trainModel(trainingData, classLabels);

        // Evaluate the model on the testing data
        double accuracy = evaluateModel(model, testingData, classLabels);

        // Output the distinct BSSID values
        Set<String> distinctBSSIDs = getDistinctBSSIDs(wifiData);

        // Store the best model and distinct BSSIDs in separate files
        String modelFilePath = "best_model.model";
        String distinctBSSIDsFilePath = "distinct_bssids.txt";
        saveModel(model, modelFilePath);
        saveDistinctBSSIDs(distinctBSSIDs, distinctBSSIDsFilePath);

        // Print the accuracy of the model on the testing data
        System.out.println("Model accuracy on testing data: " + accuracy);
    }

    private static List<List<WifiDataEntry>> splitWifiData(List<WifiDataEntry> wifiData, double ratio) {
        Random random = new Random();
        random.setSeed(43);

        int dataSize = wifiData.size();
        int trainingSize = (int) Math.ceil(dataSize * ratio);
        List<WifiDataEntry> trainingData = new ArrayList<>(trainingSize);
        List<WifiDataEntry> testingData = new ArrayList<>(dataSize - trainingSize);

        List<Integer> trainingIndices = new ArrayList<>(trainingSize);
        for (int i = 0; i < trainingSize; i++) {
            int randomIndex = random.nextInt(dataSize);
            while (trainingIndices.contains(randomIndex)) {
                randomIndex = random.nextInt(dataSize);
            }
            trainingIndices.add(randomIndex);
            trainingData.add(wifiData.get(randomIndex));
        }

        for (int i = 0; i < dataSize; i++) {
            if (!trainingIndices.contains(i)) {
                testingData.add(wifiData.get(i));
            }
        }

        List<List<WifiDataEntry>> result = new ArrayList<>();
        result.add(trainingData);
        result.add(testingData);

        return result;
    }

    private static List<WifiDataEntry> parseWifiData(String filePath) {
        List<WifiDataEntry> wifiData = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            WifiDataEntry entry = null;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("SSID: ")) {
                    if (entry == null) {
                        entry = new WifiDataEntry();
                    }
                    entry.setSSID(line.substring(6));
                } else if (line.startsWith("BSSID: ")) {
                    entry.setBSSID(line.substring(7));
                } else if (line.startsWith("ResultLevel: ")) {
                    entry.setResultLevel(Integer.parseInt(line.substring(13)));
                } else if (line.startsWith("Frequency: ")) {
                    entry.setFrequency(Integer.parseInt(line.substring(11)));
                } else if (line.startsWith("Timestamp: ")) {
                    entry.setTimestamp(Long.parseLong(line.substring(12)));
                    wifiData.add(entry);
                    entry = null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wifiData;
    }

    private static double crossValidation(List<WifiDataEntry> trainingData, String[] classLabels) {
        svm_problem problem = createSVMProblem(trainingData, classLabels);

        int nrFold = 5; // Number of folds for cross-validation
        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.gamma = 1.3703956890699743e-05;
        param.C = 1;

        double[] target = new double[problem.l];
        svm.svm_cross_validation(problem, param, nrFold, target);

        // Compute accuracy for each fold
        double correct = 0.0;
        for (int i = 0; i < problem.l; i++) {
            if (target[i] == problem.y[i]) {
                correct++;
            }
        }

        double accuracy = correct / problem.l;
        System.out.println("Cross-validation accuracy: " + accuracy);

        return accuracy;
    }

    private static svm_model trainModel(List<WifiDataEntry> trainingData, String[] classLabels) {
        svm_problem problem = createSVMProblem(trainingData, classLabels);

        svm_parameter param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.gamma = 1.3703956890699743e-05;
        param.C = 1;

        svm_model model = svm.svm_train(problem, param);
        return model;
    }

    private static double evaluateModel(svm_model model, List<WifiDataEntry> testingData, String[] classLabels) {
        int correct = 0;
        int total = 0;

        for (WifiDataEntry entry : testingData) {
            double[] values = entry.getFeatureVector(classLabels);
            svm_node[] nodes = new svm_node[values.length];
            for (int i = 0; i < values.length; i++) {
                nodes[i] = new svm_node();
                nodes[i].index = i + 1;
                nodes[i].value = values[i];
            }

            double predictedClass = svm.svm_predict(model, nodes);
            double trueClass = entry.getTrueClass(classLabels);

            if (predictedClass == trueClass) {
                correct++;
            }
            total++;
        }

        return (double) correct / total;
    }

    private static svm_problem createSVMProblem(List<WifiDataEntry> data, String[] classLabels) {
        svm_problem problem = new svm_problem();
        int numFeatures = classLabels.length;

        problem.l = data.size();
        problem.y = new double[problem.l];
        problem.x = new svm_node[problem.l][];

        for (int i = 0; i < problem.l; i++) {
            WifiDataEntry entry = data.get(i);
            problem.y[i] = entry.getTrueClass(classLabels);

            double[] values = entry.getFeatureVector(classLabels);
            problem.x[i] = new svm_node[numFeatures];

            for (int j = 0; j < numFeatures; j++) {
                svm_node node = new svm_node();
                node.index = j + 1;
                node.value = values[j];
                problem.x[i][j] = node;
            }
        }

        return problem;
    }

    private static Set<String> getDistinctBSSIDs(List<WifiDataEntry> data) {
        Set<String> distinctBSSIDs = new HashSet<>();
        for (WifiDataEntry entry : data) {
            distinctBSSIDs.add(entry.getBSSID());
        }
        return distinctBSSIDs;
    }

    private static void saveModel(svm_model model, String filePath) {
        try {
            svm.svm_save_model(filePath, model);
            System.out.println("Model saved to: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveDistinctBSSIDs(Set<String> distinctBSSIDs, String filePath) {
        try (PrintWriter writer = new PrintWriter(filePath)) {
            for (String bssid : distinctBSSIDs) {
                writer.println(bssid);
            }
            System.out.println("Distinct BSSIDs saved to: " + filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static class WifiDataEntry {
        private String SSID;
        private String BSSID;
        private int resultLevel;
        private int frequency;
        private long timestamp;

        public void setSSID(String SSID) {
            this.SSID = SSID;
        }

        public void setBSSID(String BSSID) {
            this.BSSID = BSSID;
        }

        public void setResultLevel(int resultLevel) {
            this.resultLevel = resultLevel;
        }

        public void setFrequency(int frequency) {
            this.frequency = frequency;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getBSSID() {
            return BSSID;
        }

        public double[] getFeatureVector(String[] classLabels) {
            double[] featureVector = new double[classLabels.length * 3];
            int index = 0;

            for (String classLabel : classLabels) {
                featureVector[index++] = SSID.equals(classLabel) ? 1 : 0;
                featureVector[index++] = resultLevel;
                featureVector[index++] = frequency;
            }

            return featureVector;
        }

        public double getTrueClass(String[] classLabels) {
            for (int i = 0; i < classLabels.length; i++) {
                if (SSID.equals(classLabels[i])) {
                    return i + 1;
                }
            }
            return 0;
        }
    }
}


*/



import libsvm.svm_model;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class Main {
    public static void main(String[] args) {
        SVMClassifier svmClassifier = new SVMClassifier();
        String[] locations = {"Kontor", "Stue", "Køkken"};
        // String dataSet = "WifiData230424_17-21.txt";
        String dataSet = "/Users/signethomsen/Desktop/Martin - Computer/BachelorProjekt/DataBehandling/Data/WifiData230424_17-21.txt";
        // String dataSet = "/Users/signethomsen/IdeaProjects/ModelUbiMusic/src/WifiData230424_17-21.txt";
        double partOfData = 1.0 / 3.0; // 15 minutes

        Object[] distinctBSSIDAndDataPoints = svmClassifier.extractDistinctBSSIDAndNumberOfDataPoints(locations, dataSet);
        String[] distinctBSSID = (String[]) distinctBSSIDAndDataPoints[0];
        int dataPoints = (int) distinctBSSIDAndDataPoints[1];

        Object[] samplesAndLabels = svmClassifier.extractData(locations, dataSet, distinctBSSID, dataPoints);
        double[][] samples = (double[][]) samplesAndLabels[0];
        int[] labels = (int[]) samplesAndLabels[1];

        Object[] splitSamplesAndLabels = svmClassifier.randomSplitSamplesAndLabels(samples, labels, partOfData);
        double[][] trainingSamplesOverall = (double[][]) splitSamplesAndLabels[0];
        double[][] testSamplesOverall = (double[][]) splitSamplesAndLabels[1];
        int[] trainingLabelsOverall = (int[]) splitSamplesAndLabels[2];
        int[] testLabelsOverall = (int[]) splitSamplesAndLabels[3];

        /*

        double bestScore = Double.NEGATIVE_INFINITY;
        double[][] bestSamples = null;
        int[] bestLabels = null;
        svm_model bestModel = null;

        for (int i = 1; i <= 5; i++) {
            Object[] splitResult = svmClassifier.deterministicSplitMatrix(trainingSamplesOverall, trainingLabelsOverall, 1.0 / 5.0, i);
            double[][] trainingSamples = (double[][]) splitResult[0];
            double[][] testSamples = (double[][]) splitResult[1];
            int[] trainingLabels = (int[]) splitResult[2];
            int[] testLabels = (int[]) splitResult[3];

            svm_model model = svmClassifier.fitModel(trainingSamples, trainingLabels);

            double score = svmClassifier.bestModelSVMTest(model, testSamples, testLabels);

            if (score > bestScore) {
                bestScore = score;
                bestSamples = trainingSamples;
                bestLabels = trainingLabels;
                bestModel = model;
            }
        }

         */

        svm_model model = svmClassifier.fitModel(trainingSamplesOverall, trainingLabelsOverall);

        double correct = 0.0;
        for (int i = 0; i < testLabelsOverall.length; i++) {
            double prediction = svmClassifier.predict(model, testSamplesOverall[i]);
            if (prediction == testLabelsOverall[i]) { correct++; }
        }
        System.out.println("Accuracy model: " + correct/testLabelsOverall.length);

        // double score = svmClassifier.bestModelSVMTest(bestModel, testSamplesOverall, testLabelsOverall);
        double score = svmClassifier.bestModelSVMTest(model, testSamplesOverall, testLabelsOverall);
        System.out.println("Accuracy testing data: " + score);
        /*
        System.out.println("distinctBSSID: ");
        for (String bssid : distinctBSSID) {
            System.out.println(bssid);
        }
        System.out.println("len(distinctBSSID): " + distinctBSSID.length);

         */

        storeModel(model, "svm_model4.json");
        storeModel(model, "model.dat");

        /**
         * Test of model accuracy to compare to accuracy in UbiMusic
         */



        double[][] trainingSamples = trainingSamplesOverall;
        int[] trainingLabels = trainingLabelsOverall;

        splitSamplesAndLabels = svmClassifier.randomSplitSamplesAndLabels(testSamplesOverall, testLabelsOverall, 1.0 / 18.0);
        // double[][] testSamples = (double[][]) splitSamplesAndLabels[0];
        // int[] testLabels = (int[]) splitSamplesAndLabels[1];

        double[][] storedSamples = trainingSamples;
        int[] storedLabels = trainingLabels;

        /*
        double accuracy = svmClassifier.bestModelSVMTest(bestModel, trainingSamples, trainingLabels);
        System.out.println("Accuracy: " + accuracy);

        storeModel(bestModel, "svm_model3.json", storedSamples, storedLabels);

         */
    }

    public static void storeModel(svm_model model, String filename) {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(filename));
            outputStream.writeObject(model);
            outputStream.close();
        } catch (IOException e) {
            System.err.println("Error storing SVM model: " + e.getMessage());
        }
    }



}


