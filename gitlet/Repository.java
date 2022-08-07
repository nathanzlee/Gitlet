package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;

import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 * does at a high level.
 *
 * @author Nathan Lee
 */
public class Repository {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    private HashMap<String, Commit> commits;
    private HashMap<String, Branch> branches;
    private Stage stagingArea;
    private String head;
    private String currentBranch;
    private boolean mergeConflict;

    /** Remote stuff */
    private HashMap<String, File> remoteRepos;

    public Repository() {
        /** If data already exists then retrieve it. If not, instantiate instance variables.*/

        File commitsFile = Utils.join(GITLET_DIR, "commits");
        try {
            commits = Utils.readObject(commitsFile, HashMap.class);
        } catch (IllegalArgumentException a) {
            commits = new HashMap<>();
        }

        File branchesFile = Utils.join(GITLET_DIR, "branches");
        try {
            branches = Utils.readObject(branchesFile, HashMap.class);
        } catch (IllegalArgumentException a) {
            branches = new HashMap<>();
        }

        File stageFile = Utils.join(GITLET_DIR, "stage");
        try {
            stagingArea = Utils.readObject(stageFile, Stage.class);
        } catch (IllegalArgumentException a) {
            stagingArea = new Stage();
        }

        File headFile = Utils.join(GITLET_DIR, "head.txt");
        try {
            head = Utils.readContentsAsString(headFile);
        } catch (IllegalArgumentException a) {
            head = "";
        }

        File currentBranchFile = Utils.join(GITLET_DIR, "currentbranch.txt");
        try {
            currentBranch = Utils.readContentsAsString(currentBranchFile);
        } catch (IllegalArgumentException a) {
            currentBranch = "";
        }

        File remoteFile = Utils.join(GITLET_DIR, "remote");
        try {
            remoteRepos = Utils.readObject(remoteFile, HashMap.class);
        } catch (IllegalArgumentException a) {
            remoteRepos = new HashMap<>();
        }
    }

    public void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control "
                    + "system already exists in the current directory.");
            return;
        }

        /** Make .gitlet directory */
        GITLET_DIR.mkdir();

        /** Make commits file */
        File commitsFile = Utils.join(GITLET_DIR, "commits");
        try {
            commitsFile.createNewFile();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }

        /** Make branches file */
        File branchesFile = Utils.join(GITLET_DIR, "branches");
        try {
            branchesFile.createNewFile();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }

        /** Make staging area*/
        File stageFile = Utils.join(GITLET_DIR, "stage");
        try {
            stageFile.createNewFile();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }

        /** Make head file*/
        File headFile = Utils.join(GITLET_DIR, "head.txt");
        try {
            headFile.createNewFile();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }

        /** Make currentBranch file */
        File currentBranchFile = Utils.join(GITLET_DIR, "currentbranch.txt");
        try {
            currentBranchFile.createNewFile();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }

        /** Make remote file */
        File remoteFile = Utils.join(GITLET_DIR, "remote");
        try {
            remoteFile.createNewFile();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }

        /** Create initial commit and master branch */
        Commit newCommit = new Commit("initial commit", null, new HashMap<>());
        Branch newBranch = new Branch("master", newCommit);
        commits.put(newCommit.getShortHash(), newCommit);
        branches.put(newBranch.getName(), newBranch);

        head = newCommit.getShortHash();
        currentBranch = newBranch.getName();

        /** Saves everything in .gitlet directory */
        serialize();
    }

    public void add(String fileName) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        /** Check to see if file exists
         * If not, print "File does not exist."
         * */
        File newFile = Utils.join(CWD, fileName);
        if (!newFile.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        /** Adds file to staging area for addition * */

        Commit currentCommit = commits.get(head);
        Blob fileContents = new Blob(newFile);
        HashMap<String, Blob> addFiles = stagingArea.getAddFiles();
        HashMap<String, Blob> removeFiles = stagingArea.getRemoveFiles();

        if (removeFiles.containsKey(fileName)) {
            stagingArea.removeFromRemoval(fileName);
        }

        if (currentCommit.hasFile(fileName)
                && currentCommit.getFile(fileName).sameContents(fileContents)) {
            /** File is identical to the one in the current commit */
            if (addFiles.containsKey(fileName)) {
                stagingArea.removeFromAdd(fileName);
            }
        } else {
            if (addFiles.containsKey(fileName)) {
                stagingArea.removeFromAdd(fileName);
            }
            stagingArea.addFileToAdd(newFile);
        }

        /** Save */
        serialize();
    }

    public void commit(String msg) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        /** Create a new commit
         * This commit's tracked files should be identical to the parent's tracked files
         * Add all files staged for addition
         * Remove all files staged for removal
         * Move the currentbranch pointer to the new commit
         * Move the head pointer to the new commit
         * */
        if (msg.equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }

        if (stagingArea.noChanges()) {
            System.out.println("No changes added to the commit.");
            return;
        }

        Commit prevCommit = commits.get(head);
        HashMap<String, Blob> filesToTrack = new HashMap<>();
        filesToTrack.putAll(prevCommit.getFiles());
        filesToTrack.putAll(stagingArea.getAddFiles());
        for (String key : stagingArea.getRemoveFiles().keySet()) {
            if (filesToTrack.containsKey(key)) {
                filesToTrack.remove(key);
            }
        }
        Commit newCommit = new Commit(msg, head, filesToTrack);
        commits.put(newCommit.getShortHash(), newCommit);
        branches.get(currentBranch).switchPointer(newCommit.getShortHash());
        head = newCommit.getShortHash();

        stagingArea.clearStage();
        serialize();
    }

    public void rm(String fileName) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        /** If file is staged for addition, remove it
         * If file is tracked in the current commit, stage for removal and remove the file from CWD
         * */

        File file = Utils.join(CWD, fileName);
        HashMap<String, Blob> addFiles = stagingArea.getAddFiles();
        Commit currentCommit = commits.get(head);


        if (!addFiles.containsKey(fileName) && !currentCommit.hasFile(fileName)) {
            System.out.println("No reason to remove the file.");
            return;
        }

        if (addFiles.containsKey(fileName)) {
            stagingArea.removeFromAdd(fileName);
        }

        if (currentCommit.hasFile(fileName)) {
            if (file.exists()) {
                stagingArea.addFileToRemoval(file);
                Utils.restrictedDelete(file);
            } else {
                stagingArea.addFileToRemoval(fileName, currentCommit.getFile(fileName));
            }
        }

        serialize();
    }

    public void log() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        Commit currentCommit = commits.get(head);
        while (currentCommit != null) {
            System.out.println("===");
            System.out.println("commit " + currentCommit.getHash());
            System.out.println("Date: " + currentCommit.getTimestamp());
            System.out.println(currentCommit.getMessage() + "\n");

            currentCommit = commits.get(currentCommit.getParent());
        }
    }

    public void globalLog() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        Iterator<Commit> iter = commits.values().iterator();
        while (iter.hasNext()) {
            Commit nextCommit = iter.next();
            System.out.println("===");
            System.out.println("commit " + nextCommit.getHash());
            System.out.println("Date: " + nextCommit.getTimestamp());
            System.out.println(nextCommit.getMessage() + "\n");
        }
    }

    public void find(String message) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        int count = 0;
        Iterator<Commit> iter = commits.values().iterator();
        while (iter.hasNext()) {
            Commit nextCommit = iter.next();
            if (nextCommit.getMessage().equals(message)) {
                count += 1;
                System.out.println(nextCommit.getHash());
            }
        }

        if (count == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        Commit currentCommit = commits.get(head);
        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        HashMap<String, Blob> addFiles = stagingArea.getAddFiles();
        HashMap<String, Blob> removeFiles = stagingArea.getRemoveFiles();

        System.out.println("=== Branches ===");
        List<String> branchList = new ArrayList<String>(branches.keySet());
        Collections.sort(branchList);
        for (String name : branchList) {
            if (name.equals(currentBranch)) {
                System.out.println("*" + name);
            } else {
                System.out.println(name);
            }
        }
        System.out.print("\n");
        System.out.println("=== Staged Files ===");

        List<String> addFileList = new ArrayList<String>(addFiles.keySet());
        Collections.sort(addFileList);
        for (String fileName : addFileList) {
            System.out.println(fileName);
        }
        System.out.print("\n");
        System.out.println("=== Removed Files ===");
        List<String> removeFileList = new ArrayList<String>(removeFiles.keySet());
        Collections.sort(removeFileList);
        for (String fileName : removeFileList) {
            System.out.println(fileName);
        }
        System.out.print("\n");
        System.out.println("=== Modifications Not Staged For Commit ===");
        printModifiedFiles();
        System.out.print("\n");
        System.out.println("=== Untracked Files ===");
        for (String fileName : allFiles) {
            File file = Utils.join(CWD, fileName);

            if (!currentCommit.hasFile(fileName)
                    && !addFiles.containsKey(fileName) && file.isFile()) {
                System.out.println(fileName);
            }
        }
        System.out.print("\n");
    }

    private void printModifiedFiles() {
        Commit currentCommit = commits.get(head);
        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        HashMap<String, Blob> addFiles = stagingArea.getAddFiles();
        HashMap<String, Blob> removeFiles = stagingArea.getRemoveFiles();

        HashSet<String> everyFile = new HashSet<String>();
        everyFile.addAll(allFiles);
        for (String file : addFiles.keySet()) {
            everyFile.add(file);
        }
        for (String file : currentCommit.getFiles().keySet()) {
            everyFile.add(file);
        }
        List<String> sortedFiles = new ArrayList<>(everyFile);
        Collections.sort(sortedFiles);

        for (String fileName : sortedFiles) {
            File file = Utils.join(CWD, fileName);
            String currentContents = null;
            String commitContents = null;

            if (currentCommit.hasFile(fileName)) {
                commitContents = currentCommit.getFile(fileName).getContents();
            } else {
                commitContents = null;
            }

            if (file.exists()) {
                currentContents = Utils.readContentsAsString(file);
            } else {
                currentContents = null;
            }

            if (!file.exists() && addFiles.containsKey(fileName)) {
                System.out.println(fileName + " (deleted)");
            } else if (!file.exists() && !removeFiles.containsKey(fileName)
                    && currentCommit.hasFile(fileName)) {
                System.out.println(fileName + " (deleted)");
            } else if (file.exists() && currentCommit.hasFile(fileName)
                    && !currentContents.equals(commitContents)
                    && !addFiles.containsKey(fileName)) {
                System.out.println(fileName + " (modified)");
            } else if (file.exists() && addFiles.containsKey(fileName)
                    && !currentContents.equals(addFiles.get(fileName).getContents())) {
                System.out.println(fileName + " (modified)");
            }
        }
    }

    public void checkoutFile(String fileName) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        /** If file doesn't exist in commit, print "File does not exist in that commit."
         *
         * Takes version of file in head commit and puts it in the working directory
         * If the file is already in the working directory, overwrite it
         * */
        Commit currentCommit = commits.get(head);
        File file = Utils.join(CWD, fileName);
        Blob commitFile = currentCommit.getFile(fileName);

        if (!currentCommit.hasFile(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        if (file.exists()) {
            Utils.writeContents(file, commitFile.getContents());
        } else {
            File newFile = Utils.join(CWD, fileName);
            try {
                newFile.createNewFile();
            } catch (IOException | ClassCastException excp) {
                throw new IllegalArgumentException(excp.getMessage());
            }
            Utils.writeContents(newFile, commitFile.getContents());
        }

        /** Serialize? */
    }

    public void checkoutCommitShort(String commitId, String fileName) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        /** If no commit with the given id exits, print "No commit with that id exits."
         * If file doesn't exist in given commit, print "File does not exist in that commit."
         *
         * Takes version of file in given commit and puts it in the working directory
         * If file is already in working directory, overwrite it
         * */
        if (!commits.containsKey(commitId)) {
            System.out.println("No commit with that id exists.");
            return;
        }

        Commit currentCommit = commits.get(commitId);
        File file = Utils.join(CWD, fileName);
        Blob commitFile = currentCommit.getFile(fileName);

        if (!currentCommit.hasFile(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        if (file.exists()) {
            Utils.writeContents(file, commitFile.getContents());
        } else {
            File newFile = Utils.join(CWD, fileName);
            try {
                newFile.createNewFile();
            } catch (IOException | ClassCastException excp) {
                throw new IllegalArgumentException(excp.getMessage());
            }
            Utils.writeContents(newFile, commitFile.getContents());
        }
    }

    public void checkoutCommit(String commitId, String fileName) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        if (!commits.containsKey(commitId.substring(0, 6))) {
            System.out.println("No commit with that id exists.");
            return;
        }

        Commit currentCommit = commits.get(commitId.substring(0, 6));
        File file = Utils.join(CWD, fileName);
        Blob commitFile = currentCommit.getFile(fileName);

        if (!currentCommit.hasFile(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        if (file.exists()) {
            Utils.writeContents(file, commitFile.getContents());
        } else {
            File newFile = Utils.join(CWD, fileName);
            try {
                newFile.createNewFile();
            } catch (IOException | ClassCastException excp) {
                throw new IllegalArgumentException(excp.getMessage());
            }
            Utils.writeContents(newFile, commitFile.getContents());
        }

        /** Serialize? */
    }

    public void checkoutBranch(String branchName) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        /** Checking to see if branch exists */
        if (!branches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            return;
        }

        /** Checking to see if given branch is current branch */
        if (currentBranch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        /** Variables */
        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        Iterator<String> fileIter = allFiles.iterator();
        Branch current = branches.get(currentBranch);
        Branch given = branches.get(branchName);
        Commit currentBranchCommit = commits.get(current.getCommit());
        Commit givenBranchCommit = commits.get(given.getCommit());
        HashMap<String, Blob> filesInGivenCommit = givenBranchCommit.getFiles();
        HashMap<String, Blob> filesInCurrentCommit = currentBranchCommit.getFiles();

        /** Checking to see if any file is not being tracked by current commit */
        while (fileIter.hasNext()) {
            String nextFile = fileIter.next();
            if (!currentBranchCommit.hasFile(nextFile) && givenBranchCommit.hasFile(nextFile)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }

        /** Delete all files that are tracked by current commit but not by given branch commit */
        for (String fileName : filesInCurrentCommit.keySet()) {
            File file = Utils.join(CWD, fileName);
            if (!filesInGivenCommit.containsKey(fileName)) {
                Utils.restrictedDelete(file);
            }
        }

        /** Writes all files in given branch commit into the directory */
        for (String fileName : filesInGivenCommit.keySet()) {
            File file = Utils.join(CWD, fileName);
            if (file.exists()) {
                Utils.writeContents(file, givenBranchCommit.getFile(fileName).getContents());
            } else {
                File newFile = Utils.join(CWD, fileName);
                try {
                    newFile.createNewFile();
                } catch (IOException | ClassCastException excp) {
                    throw new IllegalArgumentException(excp.getMessage());
                }
                Utils.writeContents(newFile, givenBranchCommit.getFile(fileName).getContents());
            }
        }

        /** Changing current branch, head, and clearing staging area*/
        currentBranch = branchName;
        head = branches.get(branchName).getCommit();
        stagingArea.clearStage();

        /** Save */
        serialize();
    }

    public void branch(String branchName) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        /** Creates a new branch with the given name, and points it at the current head commit. */

        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }

        Branch newBranch = new Branch(branchName, commits.get(head));
        branches.put(branchName, newBranch);

        serialize();
    }

    public void rmBranch(String branchName) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }

        if (branchName.equals(currentBranch)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }

        branches.remove(branchName);

        serialize();
    }

    public void resetShort(String commitId) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        Iterator<String> fileIter = allFiles.iterator();
        Commit givenCommit = commits.get(commitId);
        Commit currentCommit = commits.get(head);
        HashMap<String, Blob> filesInGivenCommit = givenCommit.getFiles();
        HashMap<String, Blob> filesInCurrentCommit = currentCommit.getFiles();
        Branch branch = branches.get(currentBranch);

        if (!commits.containsKey(commitId)) {
            System.out.println("No commit with that id exists.");
            return;
        }

        /** Checking to see if any file is not being tracked by current commit */
        while (fileIter.hasNext()) {
            String nextFile = fileIter.next();
            if (!currentCommit.hasFile(nextFile) && givenCommit.hasFile(nextFile)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }

        for (String fileName : filesInGivenCommit.keySet()) {
            File file = Utils.join(CWD, fileName);
            if (file.exists()) {
                Utils.writeContents(file, givenCommit.getFile(fileName).getContents());
            } else {
                File newFile = Utils.join(CWD, fileName);
                try {
                    newFile.createNewFile();
                } catch (IOException | ClassCastException excp) {
                    throw new IllegalArgumentException(excp.getMessage());
                }
                Utils.writeContents(newFile, givenCommit.getFile(fileName).getContents());
            }
        }

        /** Delete all files that are tracked by current commit but not by given branch commit */
        for (String fileName : filesInCurrentCommit.keySet()) {
            File file = Utils.join(CWD, fileName);
            if (!filesInGivenCommit.containsKey(fileName)) {
                Utils.restrictedDelete(file);
            }
        }

        head = commitId.substring(0, 6);
        branch.switchPointer(commitId);
        stagingArea.clearStage();

        serialize();
    }

    public void reset(String commitId) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        String shortCommitId = commitId.substring(0, 6);
        if (!commits.containsKey(shortCommitId)) {
            System.out.println("No commit with that id exists.");
            return;
        }

        /** Variables */
        List<String> allFiles = Utils.plainFilenamesIn(CWD);
        Iterator<String> fileIter = allFiles.iterator();
        Commit givenCommit = commits.get(shortCommitId);
        Commit currentCommit = commits.get(head);
        HashMap<String, Blob> filesInGivenCommit = givenCommit.getFiles();
        HashMap<String, Blob> filesInCurrentCommit = currentCommit.getFiles();
        Branch branch = branches.get(currentBranch);

        /** Checking to see if any file is not being tracked by current commit */
        while (fileIter.hasNext()) {
            String nextFile = fileIter.next();
            if (!currentCommit.hasFile(nextFile) && givenCommit.hasFile(nextFile)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }

        for (String fileName : filesInGivenCommit.keySet()) {
            File file = Utils.join(CWD, fileName);
            if (file.exists()) {
                Utils.writeContents(file, givenCommit.getFile(fileName).getContents());
            } else {
                File newFile = Utils.join(CWD, fileName);
                try {
                    newFile.createNewFile();
                } catch (IOException | ClassCastException excp) {
                    throw new IllegalArgumentException(excp.getMessage());
                }
                Utils.writeContents(newFile, givenCommit.getFile(fileName).getContents());
            }
        }

        /** Delete all files that are tracked by current commit but not by given branch commit */
        for (String fileName : filesInCurrentCommit.keySet()) {
            File file = Utils.join(CWD, fileName);
            if (!filesInGivenCommit.containsKey(fileName)) {
                Utils.restrictedDelete(file);
            }
        }

        head = shortCommitId;
        branch.switchPointer(shortCommitId);
        stagingArea.clearStage();

        serialize();
    }

    public void merge(String branchName) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        /** Failure cases*/
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (currentBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself");
            return;
        }
        Iterator<String> fileIter = Utils.plainFilenamesIn(CWD).iterator();
        Branch current = branches.get(currentBranch);
        Branch given = branches.get(branchName);
        Commit currentCommit = commits.get(head);
        Commit givenCommit = commits.get(given.getCommit());
        HashMap<String, Blob> stagedFiles = stagingArea.getAddFiles();
        HashMap<String, Blob> removedFiles = stagingArea.getRemoveFiles();
        Commit splitPoint = getSplitPoint(branchName);

        /** Checking to see if any file is not being tracked by current commit */
        while (fileIter.hasNext()) {
            String nextFile = fileIter.next();
            if (!givenCommit.hasFile(nextFile)) {
                continue;
            }
            String currentContents = Utils.readContentsAsString(Utils.join(CWD, nextFile));
            String givenContents = givenCommit.getFile(nextFile).getContents();
            if (!currentCommit.hasFile(nextFile) && givenCommit.hasFile(nextFile)
                    && !currentContents.equals(givenContents)
                    && !stagedFiles.containsKey(nextFile)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }
        if (stagedFiles.size() != 0 || removedFiles.size() != 0) {
            System.out.println("You have uncommitted changes.");
            return;
        }

        if (splitPoint.getShortHash().equals(given.getCommit())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        if (splitPoint.getShortHash().equals(current.getCommit())) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        HashMap<String, Blob> files = new HashMap<>();
        files.putAll(splitPoint.getFiles());
        files.putAll(currentCommit.getFiles());
        files.putAll(givenCommit.getFiles());
        mergeFiles(splitPoint, branchName, files);
        mergeCommit("Merged " + branchName + " into " + currentBranch + ".", branchName);
    }

    private Commit getSplitPoint(String branchName) {
        /** Gets split point of current branch and given branch */
        Branch current = branches.get(currentBranch);
        Branch given = branches.get(branchName);
        Commit currentCommit = commits.get(head);
        Commit givenCommit = commits.get(given.getCommit());
        ArrayList<Commit> currentBranchCommits = new ArrayList<>();
        ArrayList<Commit> givenBranchCommits = new ArrayList<>();
        Commit splitPoint = null;

        if (given.getCommit().equals(head)) {
            splitPoint = commits.get(head);
        } else {
            Commit currentBranchCommit = commits.get(current.getCommit());
            ArrayList<Commit> currentHeadCommits = new ArrayList<>();
            currentHeadCommits.add(currentBranchCommit);
            addCommitBranch(currentHeadCommits, currentBranchCommits);

            Commit givenBranchCommit = commits.get(given.getCommit());
            ArrayList<Commit> givenHeadCommits = new ArrayList<>();
            givenHeadCommits.add(givenBranchCommit);
            addCommitBranch(givenHeadCommits, givenBranchCommits);

            for (int i = 0; i < currentBranchCommits.size(); i++) {
                if (givenBranchCommits.contains(currentBranchCommits.get(i))) {
                    splitPoint = currentBranchCommits.get(i);
                    break;
                }
            }
        }

        return splitPoint;
    }

    private void addCommitBranch(ArrayList<Commit> headCommits, ArrayList<Commit> list) {
        if (headCommits.get(0).getParent() == null) {
            list.add(headCommits.get(0));
            return;
        }

        ArrayList<Commit> parentCommits = new ArrayList<>();

        for (Commit commit : headCommits) {
            list.add(commit);

            if (commit.isMergeCommit()) {
                parentCommits.add(commits.get(commit.getParent()));
                parentCommits.add(commits.get(commit.getSecondParent()));
            } else {
                if (!parentCommits.contains(commits.get(commit.getParent()))) {
                    parentCommits.add(commits.get(commit.getParent()));
                }
            }
        }

        addCommitBranch(parentCommits, list);

    }

    private void mergeFiles(Commit splitPoint, String givenBranch, HashMap<String, Blob> files) {
        Branch given = branches.get(givenBranch);
        Commit currentCommit = commits.get(head);
        Commit givenCommit = commits.get(given.getCommit());

        for (String fileName : files.keySet()) {
            File file = Utils.join(CWD, fileName);

            boolean presentAtSplitPoint = false;
            boolean presentAtCurrentBranch = false;
            boolean presentAtGivenBranch = false;
            boolean modifiedInCurrentBranch = false;
            boolean modifiedInGivenBranch = false;
            String contentsAtSplitPoint = null;
            String contentsAtCurrentBranch = null;
            String contentsAtGivenBranch = null;

            if (splitPoint.hasFile(fileName)) {
                presentAtSplitPoint = true;
                contentsAtSplitPoint = splitPoint.getFile(fileName).getContents();
            }

            if (currentCommit.hasFile(fileName)) {
                presentAtCurrentBranch = true;
                contentsAtCurrentBranch = currentCommit.getFile(fileName).getContents();
                modifiedInCurrentBranch = !contentsAtCurrentBranch.equals(contentsAtSplitPoint);
            } else {
                modifiedInCurrentBranch = contentsAtSplitPoint != null;
            }

            if (givenCommit.hasFile(fileName)) {
                presentAtGivenBranch = true;
                contentsAtGivenBranch = givenCommit.getFile(fileName).getContents();
                modifiedInGivenBranch = !contentsAtGivenBranch.equals(contentsAtSplitPoint);
            } else {
                modifiedInGivenBranch = contentsAtSplitPoint != null;
            }


            if (presentAtSplitPoint && presentAtCurrentBranch && presentAtGivenBranch
                    && modifiedInGivenBranch && !modifiedInCurrentBranch) {
                Utils.writeContents(file, contentsAtGivenBranch);
                stagingArea.addFileToAdd(file);
            } else if (modifiedInCurrentBranch && modifiedInGivenBranch) {
                if ((presentAtCurrentBranch != presentAtGivenBranch)
                        || (presentAtCurrentBranch && presentAtGivenBranch
                        && !contentsAtCurrentBranch.equals(contentsAtGivenBranch))) {
                    if (contentsAtCurrentBranch == null) {
                        contentsAtCurrentBranch = "";
                    }
                    if (contentsAtGivenBranch == null) {
                        contentsAtGivenBranch = "";
                    }

                    String newContents = "<<<<<<< HEAD" + "\n"
                            + contentsAtCurrentBranch + "=======" + "\n"
                            + contentsAtGivenBranch + ">>>>>>>" + "\n";
                    Utils.writeContents(file, newContents);
                    stagingArea.addFileToAdd(file);
                    mergeConflict = true;
                }
            } else if (!presentAtSplitPoint && !presentAtCurrentBranch && presentAtGivenBranch) {
                /** Checked out and staged */
                try {
                    file.createNewFile();
                } catch (IOException | ClassCastException excp) {
                    throw new IllegalArgumentException(excp.getMessage());
                }
                Utils.writeContents(file, contentsAtGivenBranch);
                stagingArea.addFileToAdd(file);
            } else if (presentAtSplitPoint && !modifiedInCurrentBranch && !presentAtGivenBranch) {
                /** Removed and untracked */
                stagingArea.addFileToRemoval(file);
                Utils.restrictedDelete(file);
            }
        }
    }

    private void mergeCommit(String message, String givenBranch) {
        if (mergeConflict) {
            System.out.println("Encountered a merge conflict.");
            mergeConflict = false;
        }

        Commit prevFirstCommit = commits.get(head);
//        Commit prevSecondCommit = commits.get(branches.get(givenBranch).getCommit());
        HashMap<String, Blob> filesToTrack = new HashMap<>();
        filesToTrack.putAll(prevFirstCommit.getFiles());
//        filesToTrack.putAll(prevSecondCommit.getFiles());
        filesToTrack.putAll(stagingArea.getAddFiles());
        for (String key : stagingArea.getRemoveFiles().keySet()) {
            if (filesToTrack.containsKey(key)) {
                filesToTrack.remove(key);
            }
        }
        Commit newCommit = new Commit(message, head,
                branches.get(givenBranch).getCommit(), filesToTrack);
        commits.put(newCommit.getShortHash(), newCommit);
        branches.get(currentBranch).switchPointer(newCommit.getShortHash());
        head = newCommit.getShortHash();

        stagingArea.clearStage();

        serialize();
    }

    public void addRemote(String name, String dir) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        if (remoteRepos.containsKey(name)) {
            System.out.println("A remote with that name already exists.");
            return;
        }

        File newDir = Utils.join(dir, ".gitlet");
        remoteRepos.put(name, newDir);
        serialize();
    }

    public void rmRemote(String name) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        if (!remoteRepos.containsKey(name)) {
            System.out.println("A remote with that name does not exist.");
            return;
        }
        remoteRepos.remove(name);
        serialize();
    }

    public void push(String name, String branchName) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        if (!remoteRepos.containsKey(name)) {
            System.out.println("Invalid remote name");
            return;
        }

        File remoteDir = remoteRepos.get(name);
        if (!remoteDir.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }

        HashMap<String, Commit> remoteCommits = Utils.readObject(Utils.join(remoteDir, "commits"),
                HashMap.class);
        HashMap<String, Branch> remoteBranches = Utils.readObject(Utils.join(remoteDir, "branches"),
                HashMap.class);
        Stage remoteStagingArea = Utils.readObject(Utils.join(remoteDir, "stage"),
                Stage.class);
        String remoteHead = Utils.readContentsAsString(Utils.join(remoteDir, "head.txt"));
        String remoteCurrentBranch = Utils.readContentsAsString(Utils.join(remoteDir,
                "currentbranch" + ".txt"));
        Branch branchCurrent = branches.get(branchName);
        Commit currentCommit = commits.get(branchCurrent.getCommit());
        if (!remoteBranches.containsKey(branchName)) {
            /** Add branch */
            ArrayList<Commit> allBranchCommits = new ArrayList<>();
            ArrayList<Commit> headCommits = new ArrayList<>();
            headCommits.add(currentCommit);
            addCommitBranch(headCommits, allBranchCommits);
            for (Commit commit : allBranchCommits) {
                if (!remoteCommits.containsKey(commit)) {
                    remoteCommits.put(commit.getShortHash(), commit);
                }
            }
            Branch newBranch = new Branch(branchName, currentCommit);
            remoteBranches.put(branchName, newBranch);
        } else {
            /** Checking to see if remote branch's head is in the history of current local head */
            Branch remoteBranch = remoteBranches.get(branchName);
            Commit remoteCommit = remoteCommits.get(remoteBranch.getCommit());
            ArrayList<Commit> allBranchCommits = new ArrayList<>();
            ArrayList<Commit> headCommits = new ArrayList<>();
            headCommits.add(currentCommit);
            addCommitBranch(headCommits, allBranchCommits);
            if (!allBranchCommits.contains(remoteCommit)) {
                System.out.println("Please pull down remote changes before pushing.");
                return;
            } else {
                /** Add future commits to remote branch */
                ArrayList<ArrayList<Commit>> branchTraversal = new ArrayList<>();
                ArrayList<Commit> headCommit = new ArrayList<>();
                headCommit.add(currentCommit);
                addBranchCommits(headCommit, branchTraversal);
                ArrayList<Commit> futureCommits = new ArrayList<>();
                for (ArrayList<Commit> commitList : branchTraversal) {
                    if (commitList.contains(remoteCommit)) {
                        break;
                    } else {
                        for (Commit commit : commitList) {
                            futureCommits.add(commit);
                        }
                    }
                }

                for (Commit commit : futureCommits) {
                    String hash = commit.getShortHash();
                    remoteCommits.put(hash, commit);
                }
            }

        }
        serializeRemote(remoteDir, remoteCommits, remoteBranches, remoteStagingArea,
                remoteHead, remoteCurrentBranch);
    }

    private void addBranchCommits(ArrayList<Commit> headCommits,
                                  ArrayList<ArrayList<Commit>> list) {

        list.add(headCommits);
        if (headCommits.get(0).getParent() == null) {
            return;
        }
        ArrayList<Commit> newHeadCommits = new ArrayList<>();
        for (Commit commit : headCommits) {
            if (commit.isMergeCommit()) {
                Commit firstParent = commits.get(commit.getParent());
                Commit secondParent = commits.get(commit.getSecondParent());
                newHeadCommits.add(firstParent);
                newHeadCommits.add(secondParent);
            } else {
                Commit parent = commits.get(commit.getParent());
                if (!newHeadCommits.contains(parent)) {
                    newHeadCommits.add(parent);
                }
            }
        }

        addBranchCommits(newHeadCommits, list);
    }
    public void fetch(String name, String branchName) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        if (!remoteRepos.containsKey(name)) {
            System.out.println("Invalid remote name");
            return;
        }

        File remoteDir = remoteRepos.get(name);
        if (!remoteDir.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }

        HashMap<String, Commit> remoteCommits = Utils.readObject(Utils.join(remoteDir, "commits"),
                HashMap.class);
        HashMap<String, Branch> remoteBranches = Utils.readObject(Utils.join(remoteDir, "branches"),
                HashMap.class);
        Stage remoteStagingArea = Utils.readObject(Utils.join(remoteDir, "stage"),
                Stage.class);
        String remoteHead = Utils.readContentsAsString(Utils.join(remoteDir, "head.txt"));
        String remoteCurrentBranch = Utils.readContentsAsString(Utils.join(remoteDir,
                "currentbranch" + ".txt"));

        if (!remoteBranches.containsKey(branchName)) {
            System.out.println("That remote does not have that branch.");
            return;
        }

        Branch remoteBranch = remoteBranches.get(branchName);
        Commit remoteCommit = remoteCommits.get(remoteBranch.getCommit());

        if (!branches.containsKey(name + "/" + branchName)) {
            ArrayList<Commit> allBranchCommits = new ArrayList<>();
            ArrayList<Commit> headCommits = new ArrayList<>();
            headCommits.add(remoteCommit);
            addCommitBranch(headCommits, allBranchCommits);

            for (Commit commit : allBranchCommits) {
                if (!commits.containsKey(commit)) {
                    commits.put(commit.getShortHash(), commit);
                }
            }

            Branch newBranch = new Branch(name + "/" + branchName, remoteCommit);
            branches.put(name + "/" + branchName, newBranch);
        } else {
            Branch branchCurrent = branches.get(name + "/" + branchName);
            Commit currentCommit = commits.get(branchCurrent.getCommit());
            ArrayList<Commit> allBranchCommits = new ArrayList<>();
            ArrayList<Commit> headCommits = new ArrayList<>();
            headCommits.add(remoteCommit);
            addCommitBranch(headCommits, allBranchCommits);

            for (Commit commit : allBranchCommits) {
                if (!commits.containsKey(commit)) {
                    commits.put(commit.getShortHash(), commit);
                }
            }

            branchCurrent.switchPointer(remoteCommit.getShortHash());
        }
        serialize();
    }

    public void pull(String name, String branchName) {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        if (!remoteRepos.containsKey(name)) {
            System.out.println("Invalid remote name");
            return;
        }

        File remoteDir = remoteRepos.get(name);
        if (!remoteDir.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }

        HashMap<String, Commit> remoteCommits = Utils.readObject(Utils.join(remoteDir, "commits"),
                HashMap.class);
        HashMap<String, Branch> remoteBranches = Utils.readObject(Utils.join(remoteDir, "branches"),
                HashMap.class);
        Stage remoteStagingArea = Utils.readObject(Utils.join(remoteDir, "stage"),
                Stage.class);
        String remoteHead = Utils.readContentsAsString(Utils.join(remoteDir, "head.txt"));
        String remoteCurrentBranch = Utils.readContentsAsString(Utils.join(remoteDir,
                "currentbranch" + ".txt"));

        if (!remoteBranches.containsKey(branchName)) {
            System.out.println("That remote does not have that branch.");
            return;
        }

        fetch(name, branchName);
        merge(name + "/" + branchName);

    }

    private void serialize() {

        /** Writes everything into their respective directories/files for persistence */
        File commitsFile = Utils.join(GITLET_DIR, "commits");
        Utils.writeObject(commitsFile, commits);
        File branchesFile = Utils.join(GITLET_DIR, "branches");
        Utils.writeObject(branchesFile, branches);
        File stageFile = Utils.join(GITLET_DIR, "stage");
        Utils.writeObject(stageFile, stagingArea);
        File headFile = Utils.join(GITLET_DIR, "head.txt");
        Utils.writeContents(headFile, head);
        File currentBranchFile = Utils.join(GITLET_DIR, "currentbranch.txt");
        Utils.writeContents(currentBranchFile, currentBranch);
        File remoteFile = Utils.join(GITLET_DIR, "remote");
        Utils.writeObject(remoteFile, remoteRepos);
    }

    private void serializeRemote(File dir, HashMap<String, Commit> remoteCommits, HashMap<String,
            Branch> remoteBranches, Stage remoteStage, String remoteHead, String remoteBranch) {
        File commitsFile = Utils.join(dir, "commits");
        Utils.writeObject(commitsFile, remoteCommits);
        File branchesFile = Utils.join(dir, "branches");
        Utils.writeObject(branchesFile, remoteBranches);
        File stageFile = Utils.join(dir, "stage");
        Utils.writeObject(stageFile, remoteStage);
        File headFile = Utils.join(dir, "head.txt");
        Utils.writeContents(headFile, remoteHead);
        File currentBranchFile = Utils.join(dir, "currentbranch.txt");
        Utils.writeContents(currentBranchFile, remoteBranch);
    }
}
