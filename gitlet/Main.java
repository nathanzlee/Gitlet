package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Nathan Lee
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];


        Repository repo = new Repository();
        switch(firstArg) {
            case "init":
                validateArgs(args, 1);
                repo.init();
                break;
            case "add":
                validateArgs(args, 2);
                String file = args[1];
                repo.add(file);
                break;
            case "commit":
                validateArgs(args, 2);
                String message = args[1];
                repo.commit(message);
                break;
            case "log":
                validateArgs(args, 1);
                repo.log();
                break;
            case "rm":
                validateArgs(args, 2);
                repo.rm(args[1]);
                break;
            case "global-log":
                validateArgs(args, 1);
                repo.globalLog();
                break;
            case "find":
                validateArgs(args, 2);
                repo.find(args[1]);
                break;
            case "status":
                validateArgs(args, 1);
                repo.status();
                break;
            case "checkout":
                if (args.length == 2) {
                    repo.checkoutBranch(args[1]);
                } else if (args.length == 3 && args[1].equals("--")) {
                    repo.checkoutFile(args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    if (args[1].length() == 6) {
                        repo.checkoutCommitShort(args[1], args[3]);
                    } else {
                        repo.checkoutCommit(args[1], args[3]);
                    }

                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                break;
            case "branch":
                validateArgs(args, 2);
                repo.branch(args[1]);
                break;
            case "rm-branch":
                validateArgs(args, 2);
                repo.rmBranch(args[1]);
                break;
            case "reset":
                validateArgs(args, 2);
                if (args[1].length() == 6) {
                    repo.resetShort(args[1]);
                } else {
                    repo.reset(args[1]);
                }
                break;
            case "merge":
                validateArgs(args, 2);
                repo.merge(args[1]);
                break;
            case "add-remote":
                validateArgs(args, 3);
                repo.addRemote(args[1], args[2].substring(0, args[2].length() - 8));
                break;
            case "rm-remote":
                validateArgs(args, 2);
                repo.rmRemote(args[1]);
                break;
            case "push":
                validateArgs(args, 3);
                repo.push(args[1], args[2]);
                break;
            case "fetch":
                validateArgs(args, 3);
                repo.fetch(args[1], args[2]);
                break;
            case "pull":
                validateArgs(args, 3);
                repo.pull(args[1], args[2]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }

    public static void validateArgs(String[] args, int n) {
        if (args.length != n) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }
}
