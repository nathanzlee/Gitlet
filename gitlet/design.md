# Gitlet Design Document

**Name**: Nathan Lee

## Classes and Data Structures

### Commit

#### Instance Variables
* Hash (string): hash code that represents the commit
* Timestamp (string): time when the commit was created
* Message (string): commit message
* Parent (string): hash code of the parent commit
* Files (hashmap): hashmap of strings (file names) and blobs (file contents) that contain a list of files that the commit tracks

### Stage

#### Instance Variables
* Addition (hashmap): hashmap of strings and blobs that contain a list of files that are staged for addition
* Removal (hashmap): hashmap of strings and blobs that contain a list of files that are staged for removal

### Blob

#### Instance Variables
* Filename (string): name of the file
* Contents (bytes): contents of the file

### Branch

#### Instance Variables
* Name (string): name of the branch
* Commit (string): hash code of the commit that the branch points to

## Algorithms

## Persistence

Each class is written into their respective files and saved in the .gitlet directory.
Every time the user makes a gitlet command that changes the commits, branches, staging area, head branch, or current branch, all data is updated with the serialize() function.

### Files
* commits: stores hashmap of commits
* branches: stores hashmap of branches
* stage: stores the staging area
* currentbranch.txt: contains the name of current branch
* head.txt: contains head commit hash code