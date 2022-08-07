package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {

    private String fileName;
    private String contents;

    public Blob(File file) {
        fileName = file.getName();
        contents = Utils.readContentsAsString(file);
    }

    public boolean sameContents(Blob otherBlob) {
        if (otherBlob.contents.equals(this.contents)) {
            return true;
        }
        return false;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContents() {
        return contents;
    }

}
