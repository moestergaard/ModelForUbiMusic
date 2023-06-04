import libsvm.svm_model;

public class Main {
    public static void main(String[] args) {
        IFileSystem fileSystem = new FileSystem();
        IMatrixManipulation matrixManipulation = new MatrixManipulation();
        IModel model = new ModelSVM(matrixManipulation);
        ExtractData extractData = new ExtractData(matrixManipulation);

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

        Object[] result = model.bestModel(trainingSamplesOverall, trainingLabelsOverall);
        svm_model trainedModel = (svm_model) result[0];
        double score = (double) result[1];

        if (partOfData != 1.0) {
            score = model.modelTest(trainedModel, testSamplesOverall, testLabelsOverall);
        }

        printDistinctBSSID(distinctBSSID);
        System.out.println("Nøjagtighed trænet model: " + score);

        fileSystem.storeModel(trainedModel, "svm_model11.json");
    }

    private static void printDistinctBSSID(String[] distinctBSSID) {
        StringBuilder distinctBSSIDstring = new StringBuilder();
        for (String bssid : distinctBSSID) {
            distinctBSSIDstring.append("\"").append(bssid).append("\", ");
        }
        System.out.println(distinctBSSIDstring);
        System.out.println("Antal distinkte BSSID: " + distinctBSSID.length);
    }
}


