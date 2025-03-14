package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        try{
            if(args.length == 0 || args[0].isBlank()){
                throw new GitletException("Please enter a command.");
            }
            String firstArg = args[0];
            if(!firstArg.equals("init")){
                if(!Repository.GITLET_DIR.exists() || !Repository.GITLET_DIR.isDirectory()){
                    throw new GitletException("Not in an initialized Gitlet directory.");
                }
            }
            switch(firstArg) {
                case "init":
                    Repository.init();
                    break;
                case "add":
                    Repository.add(args[1]);
                    break;
                case "commit":
                    if(args.length < 2 || args[1].isBlank()){
                        throw new GitletException("Please enter a commit message.");
                    }
                    Repository.commit(args[1]);
                    break;
                case "rm":
                    Repository.rm(args[1]);
                    break;
                case "log":
                    Repository.log();
                    break;
                case "global-log":
                    Repository.globalLog();
                    break;
                case "find":
                    Repository.find(args[1]);
                    break;
                case "status":
                    Repository.status();
                    break;
                case "checkout":
                    if(args.length == 2){
                        // checkout [branch name]
                        Repository.checkout(args[1]);
                    }else if(args.length == 3){
                        // checkout -- [file name]
                        if(!args[1].equals("--")){
                            throw new GitletException("Incorrect operands.");
                        }
                        Repository.checkout(args[1], args[2]);
                    }else if(args.length == 4){
                        // checkout [commit id] -- [file name]
                        if(!args[2].equals("--")){
                            throw new GitletException("Incorrect operands.");
                        }
                        Repository.checkout(args[1], args[2], args[3]);
                    }else{
                        throw new GitletException("Incorrect operands.");
                    }
                    break;
                case "branch":
                    Repository.branch(args[1]);
                    break;
                case "rm-branch":
                    Repository.rmBranch(args[1]);
                    break;
                case "reset":
                    Repository.reset(args[1]);
                    break;
                case "merge":
                    Repository.merge(args[1]);
                    break;
                case "add-remote":
                    Repository.addRemote(args[1], args[2]);
                    break;
                case "rm-remote":
                    Repository.rmRemote(args[1]);
                    break;
                case "push":
                    Repository.push(args[1], args[2]);
                    break;
                case "fetch":
                    Repository.fetch(args[1], args[2]);
                    break;
                case "pull":
                    Repository.pull(args[1], args[2]);
                    break;
                default:
                    throw new GitletException("No command with that name exists.");
            }
        }catch(GitletException e){
            System.out.println(e.getMessage());
        }
    }
}
