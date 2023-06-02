import libsvm.svm_model;

public interface IFileSystem {
    void storeModel(svm_model model, String filename);
}
