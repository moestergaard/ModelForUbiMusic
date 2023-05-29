import libsvm.svm_model;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class Main {
    public static void main(String[] args) {
        SVMClassifier svmClassifier = new SVMClassifier();
        String[] locations = {"Kontor", "Stue", "Køkken"};
        // String dataSet = "/Users/signethomsen/Desktop/Martin - Computer/BachelorProjekt/DataBehandling/Data/WifiData230424_17-21.txt";
        String dataSet = "/Users/signethomsen/IdeaProjects/ModelUbiMusic/src/WifiData230424_17-21.txt";
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

        svm_model model = svmClassifier.fitModel(trainingSamplesOverall, trainingLabelsOverall);

        System.out.println("distinctBSSID: ");
        StringBuilder distinctBSSIDstring = new StringBuilder();
        for (String bssid : distinctBSSID) {
            distinctBSSIDstring.append("\"").append(bssid).append("\", ");
        }
        System.out.println(distinctBSSIDstring);
        System.out.println("len(distinctBSSID): " + distinctBSSID.length);

        double score = svmClassifier.bestModelSVMTest(model, testSamplesOverall, testLabelsOverall);
        System.out.println("Accuracy testing data: " + score);

        storeModel(model, "svm_model6.json");
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


