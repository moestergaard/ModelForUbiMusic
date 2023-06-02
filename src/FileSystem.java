import libsvm.svm_model;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class FileSystem implements IFileSystem {
    public void storeModel(svm_model model, String filename) {
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(filename));
            outputStream.writeObject(model);
            outputStream.close();
        } catch (IOException e) {
            System.err.println("Error storing SVM model: " + e.getMessage());
        }
    }
}
