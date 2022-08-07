package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

public class Stage implements Serializable {

    private HashMap<String, Blob> addition;
    private HashMap<String, Blob> removal;

    public Stage() {
        addition = new HashMap<>();
        removal = new HashMap<>();
    }

    public void addFileToAdd(File file) {

        String fileName = file.getName();
        Blob fileContents = new Blob(file);

        addition.put(fileName, fileContents);
    }

    public void addFileToRemoval(File file) {

        String fileName = file.getName();
        Blob fileContents = new Blob(file);

        removal.put(fileName, fileContents);
    }

    public void addFileToRemoval(String fileName, Blob contents) {
        removal.put(fileName, contents);
    }

    public HashMap<String, Blob> getAddFiles() {
        return addition;
    }

    public HashMap<String, Blob> getRemoveFiles() {
        return removal;
    }

    public void removeFromAdd(String fileName) {
        addition.remove(fileName);
    }

    public void removeFromRemoval(String fileName) {
        removal.remove(fileName);
    }

    public void clearStage() {
        addition = new HashMap<>();
        removal = new HashMap<>();
    }

    public boolean noChanges() {
        return addition.size() == 0 && removal.size() == 0;
    }

}
