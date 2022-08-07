package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.text.SimpleDateFormat;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author Nathan Lee
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The sha hash code of this Commit. */
    private String hash;
    private String shortHash;

    /** The timestamp of this Commit. */
    private String timestamp;

    /** The message of this Commit. */
    private String message;

    /** The hash code of the parent Commit. */
    private String parent;

    /** Second parent for merge commits */
    private String secondParent;
    /** The files that this Commit tracks. */
    private HashMap<String, Blob> files;

    public Commit(String msg, String parentCommit, HashMap<String, Blob> trackedFiles) {
        parent = parentCommit;
        secondParent = null;
        message = msg;
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

        if (parentCommit == null) {
            timestamp = sdf.format(new Date(0));
        } else {
            timestamp = sdf.format(new Date());
        }

        hash = Utils.sha1(message + timestamp);
        shortHash = hash.substring(0, 6);
        files = trackedFiles;
    }

    public Commit(String msg, String parentCommit, String secondParentCommit,
                  HashMap<String, Blob> trackedFiles) {
        parent = parentCommit;
        secondParent = secondParentCommit;
        message = msg;
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

        if (parentCommit == null) {
            timestamp = sdf.format(new Date(0));
        } else {
            timestamp = sdf.format(new Date());
        }

        hash = Utils.sha1(message + timestamp);
        shortHash = hash.substring(0, 6);
        files = trackedFiles;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getParent() {
        return parent;
    }

    public HashMap<String, Blob> getFiles() {
        return files;
    }

    public String getHash() {
        return hash;
    }

    public String getShortHash() {
        return shortHash;
    }

    public boolean hasFile(String fileName) {
        return files.containsKey(fileName);
    }

    public Blob getFile(String fileName) {
        return files.get(fileName);
    }

    public boolean sameCommit(Commit commit) {
        if (commit == null) {
            return false;
        }
        return commit.getHash().equals(hash);
    }

    public boolean isMergeCommit() {
        return secondParent != null;
    }

    public String getSecondParent() {
        return secondParent;
    }
}
