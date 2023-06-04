import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExtractData {
    public Object[] extractDistinctBSSIDAndNumberOfDataPoints(String[] locations, String filename) {
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

    public Object[] extractData(String[] locations, String filename, String[] distinctBSSID, int numberOfSamples) {
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
                        MatrixManipulation matrixManipulation = new MatrixManipulation();
                        samples = matrixManipulation.changeMatrix(samples, index, distinctBSSID, currentBSSID, resultLevel[0]);
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
}
