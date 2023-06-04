import libsvm.svm_model;

public class Main {
    public static void main(String[] args) {
        IFileSystem fileSystem = new FileSystem();
        IMatrixManipulation matrixManipulation = new MatrixManipulation();
        IModel model = new ModelSVM(matrixManipulation);
        ExtractData extractData = new ExtractData();
//        SVMClassifier svmClassifier = new SVMClassifier();


        String[] locations = {"Kontor", "Stue", "Køkken"};
        String dataSet = "/Users/signethomsen/Desktop/Martin - Computer/BachelorProjekt/DataBehandling/2023-05-29T16_43_20.122379.txt";
        double partOfData = 1.0;

        Object[] distinctBSSIDAndDataPoints = extractData.extractDistinctBSSIDAndNumberOfDataPoints(locations, dataSet);
        String[] distinctBSSID = (String[]) distinctBSSIDAndDataPoints[0];
        int dataPoints = (int) distinctBSSIDAndDataPoints[1];

        Object[] samplesAndLabels = extractData.extractData(locations, dataSet, distinctBSSID, dataPoints);
        double[][] samples = (double[][]) samplesAndLabels[0];
        int[] labels = (int[]) samplesAndLabels[1];

        Object[] splitSamplesAndLabels = matrixManipulation.randomSplitSamplesAndLabels(samples, labels, partOfData);
        double[][] trainingSamplesOverall = (double[][]) splitSamplesAndLabels[0];
        double[][] testSamplesOverall = (double[][]) splitSamplesAndLabels[1];
        int[] trainingLabelsOverall = (int[]) splitSamplesAndLabels[2];
        int[] testLabelsOverall = (int[]) splitSamplesAndLabels[3];

        Object[] result = model.bestModelSVM(trainingSamplesOverall, trainingLabelsOverall);
        svm_model trainedModel = (svm_model) result[0];
        double score = (double) result[1];


        // svm_model model = svmClassifier.fitModel(trainingSamplesOverall, trainingLabelsOverall);

        System.out.println("distinctBSSID: ");
        StringBuilder distinctBSSIDstring = new StringBuilder();
        for (String bssid : distinctBSSID) {
            distinctBSSIDstring.append("\"").append(bssid).append("\", ");
        }
        System.out.println(distinctBSSIDstring);
        System.out.println("len(distinctBSSID): " + distinctBSSID.length);

        /*
        if (partOfData != 1.0) {
            score = model.bestModelSVMTest(model, testSamplesOverall, testLabelsOverall);
        }
         */

        System.out.println("Accuracy testing data: " + score);

        fileSystem.storeModel(trainedModel, "svm_model10.json");
    }


}


