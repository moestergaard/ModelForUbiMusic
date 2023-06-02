import libsvm.svm_model;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class Main {
    public static void main(String[] args) {
        IFileSystem fileSystem = new FileSystem();
        SVMClassifier svmClassifier = new SVMClassifier();
        ExtractData extractData = new ExtractData();
        String[] locations = {"Kontor", "Stue", "Køkken"};
        // String dataSet = "/Users/signethomsen/Desktop/Martin - Computer/BachelorProjekt/DataBehandling/Data/WifiData230424_17-21.txt";
        // String dataSet = "/Users/signethomsen/IdeaProjects/ModelUbiMusic/src/WifiData230424_17-21.txt";
        // String dataSet = "/Users/signethomsen/Desktop/Martin - Computer/BachelorProjekt/DataBehandling/2023-05-29T16_43_20.122379.txt";
        String dataSet = "/Users/signethomsen/Desktop/Martin - Computer/BachelorProjekt/DataBehandling/Data/2023-06-02T13_41_29.390439.txt";
        double partOfData = 1.0; // / 3.0; // 15 minutes

        Object[] distinctBSSIDAndDataPoints = extractData.extractDistinctBSSIDAndNumberOfDataPoints(locations, dataSet);
        String[] distinctBSSID = (String[]) distinctBSSIDAndDataPoints[0];
        int dataPoints = (int) distinctBSSIDAndDataPoints[1];

        Object[] samplesAndLabels = extractData.extractData(locations, dataSet, distinctBSSID, dataPoints);
        double[][] samples = (double[][]) samplesAndLabels[0];
        int[] labels = (int[]) samplesAndLabels[1];

        Object[] splitSamplesAndLabels = svmClassifier.randomSplitSamplesAndLabels(samples, labels, partOfData);
        double[][] trainingSamplesOverall = (double[][]) splitSamplesAndLabels[0];
        double[][] testSamplesOverall = (double[][]) splitSamplesAndLabels[1];
        int[] trainingLabelsOverall = (int[]) splitSamplesAndLabels[2];
        int[] testLabelsOverall = (int[]) splitSamplesAndLabels[3];

        Object[] result = svmClassifier.bestModelSVM(trainingSamplesOverall, trainingLabelsOverall);
        svm_model model = (svm_model) result[0];
        double score = (double) result[1];


        // svm_model model = svmClassifier.fitModel(trainingSamplesOverall, trainingLabelsOverall);

        System.out.println("distinctBSSID: ");
        StringBuilder distinctBSSIDstring = new StringBuilder();
        for (String bssid : distinctBSSID) {
            distinctBSSIDstring.append("\"").append(bssid).append("\", ");
        }
        System.out.println(distinctBSSIDstring);
        System.out.println("len(distinctBSSID): " + distinctBSSID.length);

        if (partOfData != 1.0) {
            score = svmClassifier.bestModelSVMTest(model, testSamplesOverall, testLabelsOverall);
        }
        System.out.println("Accuracy testing data: " + score);

        fileSystem.storeModel(model, "svm_model9.json");
    }


}


