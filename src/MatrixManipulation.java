public class MatrixManipulation {

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
}
