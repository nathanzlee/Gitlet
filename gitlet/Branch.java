package gitlet;

import java.io.Serializable;

public class Branch implements Serializable {

    private String name;
    private String commit;

    public Branch(String name, Commit commit) {
        this.name = name;
        this.commit = commit.getShortHash();
    }

    public String getName() {
        return name;
    }

    public String getCommit() {
        return commit;
    }

    public void switchPointer(String newCommit) {
        commit = newCommit;
    }
}
