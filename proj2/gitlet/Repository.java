package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Commit.getFullCommitId;
import static gitlet.Utils.*;


/**
 * Represents a gitlet repository.
 *
 * @author Cheuring
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

    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    public static final File HEADS_DIR = join(REFS_DIR, "heads");
    public static final File COMMIT_DIR = join(OBJECTS_DIR, "commit");
    public static final File BLOB_DIR = join(OBJECTS_DIR, "blob");
    public static final File STAGE_FILE = join(GITLET_DIR, "stage");
    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");
    public static final File CONFIG_FILE = join(GITLET_DIR, "config");
    public static final File REMOTE_DIR = join(REFS_DIR, "remotes");

    public static void init() {
        if (GITLET_DIR.exists()) {
            throw new GitletException("A Gitlet version-control system already exists in the current directory.");
        }
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        REFS_DIR.mkdir();
        HEADS_DIR.mkdir();
        COMMIT_DIR.mkdir();
        BLOB_DIR.mkdir();
        REMOTE_DIR.mkdir();
        Stage.clear();
        Config.clear();
        Utils.writeContents(HEAD_FILE, "master");

        Commit initialCommit = new Commit(new Date(0), "initial commit", new TreeMap<>(), new ArrayList<>());
        initialCommit.save();

        forwardBranch("master", initialCommit.getID());
    }

    public static void add(String fileName) {
        File file = join(CWD, fileName);
        if (!file.exists()) {
            throw new GitletException("File does not exist.");
        }

        Stage stage = Stage.load();
        Blob blob = new Blob(file);
        Commit currentCommit = Commit.load(getBranchPointer(readContentsAsString(HEAD_FILE)));
        if (currentCommit.containsFile(fileName) && currentCommit.getBlobId(fileName).equals(blob.getId())) {
            stage.blobs.remove(fileName);
            stage.save();
            return;
        }

        stage.add(fileName, blob.getId());
        blob.save();
        stage.save();
    }

    public static void commit(String message) {
        _commit(message, getBranchPointer(readContentsAsString(HEAD_FILE)));
    }

    private static void _commit(String message, String... parents) {
        Stage stage = Stage.load();
        if (stage.blobs.isEmpty()) {
            throw new GitletException("No changes added to the commit.");
        }

        String currentBranch = readContentsAsString(HEAD_FILE);
        Commit commit = new Commit(message, stage.blobs, Arrays.asList(parents));
        commit.save();
        forwardBranch(currentBranch, commit.getID());
        Stage.clear();
    }

    public static void rm(String filename) {
        File file = join(CWD, filename);
        Stage stage = Stage.load();
        String currentBranch = readContentsAsString(HEAD_FILE);
        Commit currentCommit = Commit.load(getBranchPointer(currentBranch));

        if (!stage.blobs.containsKey(filename) && !currentCommit.getBlobs().containsKey(filename)) {
            throw new GitletException("No reason to remove the file.");
        }
        // unstage the file if it is staged
        if (stage.blobs.containsKey(filename)) {
            stage.blobs.remove(filename);
            stage.save();
        }

        if (currentCommit.getBlobs().containsKey(filename)) {
            stage.add(filename, "-" + currentCommit.getBlobId(filename));
            stage.save();
            Utils.restrictedDelete(file);
        }
    }

    public static void log() {
        String currentBranch = readContentsAsString(HEAD_FILE);
        Commit currentCommit = Commit.load(getBranchPointer(currentBranch));
        while (currentCommit != null) {
            currentCommit.log();
            List<String> parents = currentCommit.getParents();
            currentCommit = parents.isEmpty() ? null : Commit.load(parents.get(0));
        }
    }

    public static void globalLog() {
        Utils.plainFilenamesIn(COMMIT_DIR).forEach(commitId -> {
            Commit commit = Commit.load(commitId);
            commit.log();
        });
    }

    public static void find(String message) {
        boolean found = false;
        for (String commitId : Utils.plainFilenamesIn(COMMIT_DIR)) {
            Commit commit = Commit.load(commitId);
            if (commit.getMessage().equals(message)) {
                System.out.println(commit.getID());
                found = true;
            }
        }
        if (!found) {
            throw new GitletException("Found no commit with that message.");
        }
    }

    public static void status() {
        System.out.println("=== Branches ===");
        for (String branch : Utils.plainFilenamesIn(HEADS_DIR)) {
            if (branch.equals(readContentsAsString(HEAD_FILE))) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        Stage stage = Stage.load();
        List<String> removedFiles = new ArrayList<>();
        for (String fileName : stage.blobs.keySet()) {
            if (stage.blobs.get(fileName).startsWith("-")) {
                removedFiles.add(fileName);
            } else {
                System.out.println(fileName);
            }
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        for (String fileName : removedFiles) {
            System.out.println(fileName);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        List<String> modifiedFiles = getModifiedFiles();
        modifiedFiles.forEach(System.out::println);
        System.out.println();

        System.out.println("=== Untracked Files ===");
        List<String> untrackedFiles = getUntrackedFiles();
        untrackedFiles.forEach(System.out::println);
        System.out.println();
    }

    private static List<String> getModifiedFiles() {
        List<String> modifiedFiles = new ArrayList<>();
        String currentBranch = readContentsAsString(HEAD_FILE);
        Commit currentCommit = Commit.load(getBranchPointer(currentBranch));
        Stage stage = Stage.load();

        Set<String> allFiles = new HashSet<>(currentCommit.getBlobs().keySet());
        allFiles.addAll(stage.blobs.keySet());

        for (String filename : allFiles) {
            String stagedBlobId = stage.blobs.get(filename);
            String commitBlobId = currentCommit.getBlobs().get(filename);
            File wdFile = join(CWD, filename);
            boolean fileExists = wdFile.exists();

            // Tracked in the current commit, changed in the working directory, but not staged;
            if (commitBlobId != null && fileExists && stagedBlobId == null) {
                if (!Blob.blobID(wdFile).equals(commitBlobId)) {
                    modifiedFiles.add(filename + " (modified)");
                    continue;
                }
            }
            // Staged for addition, but with different contents than in the working directory;
            if (stagedBlobId != null && fileExists) {
                if (!Blob.blobID(wdFile).equals(stagedBlobId)) {
                    modifiedFiles.add(filename + " (modified)");
                    continue;
                }
            }
            // Staged for addition, but deleted in the working directory;
            if (stagedBlobId != null && !stagedBlobId.startsWith("-") && !fileExists) {
                modifiedFiles.add(filename + " (deleted)");
                continue;
            }
            // Not staged for removal, but tracked in the current commit and deleted from the working directory.
            if (!(stagedBlobId != null && stagedBlobId.startsWith("-")) && commitBlobId != null && !fileExists) {
                if (!commitBlobId.startsWith("-")) {
                    modifiedFiles.add(filename + " (deleted)");
                }
            }
        }

        return modifiedFiles;
    }

    private static List<String> getUntrackedFiles() {
        List<String> untrackedFiles = new ArrayList<>();
        String currentBranch = readContentsAsString(HEAD_FILE);
        Commit currentCommit = Commit.load(getBranchPointer(currentBranch));
        for (String fileName : plainFilenamesIn(CWD)) {
            if (isIgnored(fileName)) {
                continue;
            }
            if (!currentCommit.containsFile(fileName) && !Stage.load().blobs.containsKey(fileName)) {
                untrackedFiles.add(fileName);
            }
        }
        return untrackedFiles;
    }

    public static void checkout(String... args) {
        if (args.length == 1) {
            checkoutBranch(args[0]);
        } else if (args.length == 2) {
            checkoutFile(args[1]);
        } else {
            checkoutFile(args[2], args[0]);
        }
    }

    private static void checkoutBranch(String branchName) {
        File prefix = HEADS_DIR;
        if (branchName.contains("/")) {
            branchName = branchName.replace("/", File.separator);
            prefix = REMOTE_DIR;
        }
        if (!join(prefix, branchName).exists()) {
            throw new GitletException("No such branch exists.");
        }
        if (branchName.equals(readContentsAsString(HEAD_FILE))) {
            throw new GitletException("No need to checkout the current branch.");
        }
        checkoutCommit(getBranchPointer(prefix, branchName));
        changeBranch(branchName);
    }

    private static void changeBranch(String branchName) {
        writeContents(HEAD_FILE, branchName);
    }

    private static boolean isIgnored(String fileName) {
        return fileName.equals(".gitlet") || fileName.equals("Makefile") || fileName.equals("proj2.iml") || fileName.equals("pom.xml");
    }

    private static void checkoutFile(String filename) {
        String currentBranch = readContentsAsString(HEAD_FILE);
        checkoutFile(filename, getBranchPointer(currentBranch));
    }

    private static void checkoutFile(String filename, String commitId) {
        Commit commit = Commit.load(commitId);
        if (!commit.containsFile(filename)) {
            throw new GitletException("File does not exist in that commit.");
        }
        Blob blob = Blob.load(commit.getBlobId(filename));
        writeContents(join(CWD, filename), blob.getContent());
    }

    private static void forwardBranch(String branch, String commitId) {
        File prefix = HEADS_DIR;
        if (branch.contains(File.separator)) {
            prefix = REMOTE_DIR;
        }
        File branchFile = join(prefix, branch);
        writeContents(branchFile, commitId);
    }

    private static String getBranchPointer(String branch) {
        File prefix = HEADS_DIR;
        if (branch.contains(File.separator)) {
            prefix = REMOTE_DIR;
        }
        return getBranchPointer(prefix, branch);
    }

    private static String getBranchPointer(File prefix, String branch) {
        File branchFile = join(prefix, branch);
        return readContentsAsString(branchFile);
    }

    public static void branch(String branchName) {
        File branchFile = join(HEADS_DIR, branchName);
        if (branchFile.exists()) {
            throw new GitletException("A branch with that name already exists.");
        }
        forwardBranch(branchName, getBranchPointer(readContentsAsString(HEAD_FILE)));
    }

    public static void rmBranch(String branchName) {
        File branchFile = join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            throw new GitletException("A branch with that name does not exist.");
        }
        if (branchName.equals(readContentsAsString(HEAD_FILE))) {
            throw new GitletException("Cannot remove the current branch.");
        }
        branchFile.delete();
    }

    public static void reset(String commitId) {
        if (commitId.length() < 40) {
            commitId = getFullCommitId(commitId);
        }
        checkoutCommit(commitId);
        forwardBranch(readContentsAsString(HEAD_FILE), commitId);
    }

    private static void checkoutCommit(String commitId) {
        Commit commit = Commit.load(commitId);
        List<String> untrackedFiles = getUntrackedFiles();
        if (!untrackedFiles.isEmpty()) {
            throw new GitletException("There is an untracked file in the way; delete it, or add and commit it first.");
        }

        for (String fileName : plainFilenamesIn(CWD)) {
            if (isIgnored(fileName)) {
                continue;
            }
            if (!commit.containsFile(fileName)) {
                Utils.restrictedDelete(join(CWD, fileName));
            }
        }

        commit.getBlobs().forEach((fileName, blobId) -> {
            if (blobId.startsWith("-")) {
                Utils.restrictedDelete(join(CWD, fileName));
                return;
            }
            Blob blob = Blob.load(blobId);
            writeContents(join(CWD, fileName), blob.getContent());
        });

        Stage.clear();
    }

    public static void merge(String mergeBranch) {
        _merge(HEADS_DIR, mergeBranch);
    }

    private static void _merge(File prefix, String mergeBranch) {
        Stage stage = Stage.load();
        if (!stage.isEmpty()) {
            throw new GitletException("You have uncommitted changes.");
        }

        if (!join(prefix, mergeBranch).exists()) {
            throw new GitletException("A branch with that name does not exist.");
        }

        String currentBranch = readContentsAsString(HEAD_FILE);
        if (currentBranch.equals(mergeBranch)) {
            throw new GitletException("Cannot merge a branch with itself.");
        }

        String mergeBranchPointer = getBranchPointer(prefix, mergeBranch);
        String currentBranchPointer = getBranchPointer(currentBranch);
        String splitPoint = findSplitPoint(currentBranchPointer, mergeBranchPointer);

        if (splitPoint == null) {
            throw new GitletException("No common ancestor between the current branch and the given branch.");
        }
        if (splitPoint.equals(currentBranchPointer)) {
            checkoutCommit(mergeBranchPointer);
            forwardBranch(currentBranch, mergeBranchPointer);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        if (splitPoint.equals(mergeBranchPointer)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        Commit splitCommit = Commit.load(splitPoint);
        Commit currentCommit = Commit.load(currentBranchPointer);
        Commit mergeCommit = Commit.load(mergeBranchPointer);
        boolean conflict = false;

        /*
         * 1. modified/created in given, not modified in current -> checkout and stage
         * 2. not modified in given, modified/created/removed in current -> do nothing
         * 3. content same -> do nothing
         * 4. removed in given, not modified in current -> git rm
         * 5. both modified and different -> conflict
         */
        Set<String> allFiles = new HashSet<>(splitCommit.getBlobs().keySet());
        allFiles.addAll(currentCommit.getBlobs().keySet());
        allFiles.addAll(mergeCommit.getBlobs().keySet());

        List<String> untrackedFiles = getUntrackedFiles();

        for (String filename : allFiles) {
            String splitBlobId = splitCommit.getBlobId(filename);
            String currentBlobId = currentCommit.getBlobId(filename);
            String mergeBlobId = mergeCommit.getBlobId(filename);

            boolean currentModified = currentBlobId != null && !currentBlobId.equals(splitBlobId);
            boolean mergeModified = mergeBlobId != null && !mergeBlobId.equals(splitBlobId);
            boolean mergeRemoved = mergeBlobId != null && mergeBlobId.startsWith("-");
            boolean contentSame = currentBlobId != null && currentBlobId.equals(mergeBlobId);

            // 1. modified/created in given, not modified in current -> checkout and stage
            if (!mergeRemoved && mergeModified && !currentModified) {
                if (untrackedFiles.contains(filename)) {
                    throw new GitletException("There is an untracked file in the way; delete it, or add and commit it first.");
                }
                checkoutFile(filename, mergeBranchPointer);
                Stage.addStage(filename, mergeBlobId);
                continue;
            }
            // 2. not modified in given, modified/created/removed in current -> do nothing
            if (!mergeModified && currentModified) {
                continue;
            }
            // 3. content same -> do nothing
            if (contentSame) {
                continue;
            }
            // 4. removed in given, not modified in current -> git rm
            if (mergeRemoved && !currentModified) {
                if (untrackedFiles.contains(filename)) {
                    throw new GitletException("There is an untracked file in the way; delete it, or add and commit it first.");
                }
                rm(filename);
                continue;
            }
            // 5. both modified and different -> conflict
            if (mergeModified && currentModified && !contentSame) {
                StringBuilder conflictContent = new StringBuilder();
                conflictContent.append("<<<<<<< HEAD\n");
                if (currentBlobId != null && !currentBlobId.startsWith("-")) {
                    conflictContent.append(new String(Blob.load(currentBlobId).getContent()));
                }
                conflictContent.append("=======\n");
                if (mergeBlobId != null && !mergeBlobId.startsWith("-")) {
                    conflictContent.append(new String(Blob.load(mergeBlobId).getContent()));
                }
                conflictContent.append(">>>>>>>\n");
                writeContents(join(CWD, filename), conflictContent.toString().getBytes());

                conflict = true;
                add(filename);
                continue;
            }

            System.out.println("should not be here");
        }

        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }

        _commit("Merged " + mergeBranch + " into " + currentBranch + ".", currentBranchPointer, mergeBranchPointer);
    }

    private static String findSplitPoint(String currentCommitId, String mergeCommitId) {
        Set<String> mergeAncestors = new HashSet<>();
        Stack<String> another = new Stack<>();
        another.push(mergeCommitId);

        while (!another.isEmpty()) {
            mergeCommitId = another.pop();
            mergeAncestors.add(mergeCommitId);
            List<String> parents = Commit.load(mergeCommitId).getParents();
            for (String parent : parents) {
                another.push(parent);
            }
        }

        Queue<String> queue = new LinkedList<>();
        queue.add(currentCommitId);
        while (!queue.isEmpty()) {
            currentCommitId = queue.poll();
            if (mergeAncestors.contains(currentCommitId)) {
                return currentCommitId;
            }
            List<String> parents = Commit.load(currentCommitId).getParents();
            for (String parent : parents) {
                queue.add(parent);
            }
        }

        return null;
    }

    public static void addRemote(String remoteName, String remotePath) {
        Config config = Config.load();
        if (config.remote.containsKey(remoteName)) {
            throw new GitletException("A remote with that name already exists.");
        }
        config.add(remoteName, remotePath.replace("/", File.separator));
        config.save();
    }


    public static void rmRemote(String remoteName) {
        Config config = Config.load();
        if (!config.remote.containsKey(remoteName)) {
            throw new GitletException("A remote with that name does not exist.");
        }
        config.remote.remove(remoteName);
        config.save();
    }

    public static void push(String remoteName, String remoteBranch) {
        File remoteFile = getRemoteFile(remoteName);

        String localBranchPointer = getBranchPointer(readContentsAsString(HEAD_FILE));
        String temp = localBranchPointer;
        File remoteBranchFile = join(remoteFile, "refs", "heads", remoteBranch);

        String remotePointer = readContentsAsString(remoteBranchFile);
        Stack<String> stack = new Stack<>();
        while (temp != null) {
            if (temp.equals(remotePointer)) {
                break;
            }
            stack.push(temp);

            List<String> parents = Commit.load(temp).getParents();
            if (parents.isEmpty()) {
                temp = null;
            } else {
                temp = parents.get(0);
            }
        }

        if (temp == null && remoteBranchFile.exists()) {
            throw new GitletException("Please pull down remote changes before pushing.");
        }

        File remoteCommitDir = join(remoteFile, "objects", "commit");
        File remoteBlobDir = join(remoteFile, "objects", "blob");
        while (!stack.isEmpty()) {
            String commitId = stack.pop();
            Commit commit = Commit.load(commitId);
            commit.getBlobs().forEach((fileName, blobId) -> {
                copy(join(remoteBlobDir, blobId), join(BLOB_DIR, blobId));
            });
            copy(join(remoteCommitDir, commitId), join(COMMIT_DIR, commitId));
        }

        Utils.writeContents(remoteBranchFile, localBranchPointer);
        forwardBranch(remoteName + File.separator + remoteBranch, localBranchPointer);
    }

    private static File getRemoteFile(String remoteName) {
        Config config = Config.load();
        if (!config.remote.containsKey(remoteName)) {
            throw new GitletException("Remote name not found.");
        }

        File remoteFile = join(CWD, config.remote.get(remoteName));
        if (!remoteFile.exists()) {
            throw new GitletException("Remote directory not found.");
        }
        return remoteFile;
    }

    public static void fetch(String remoteName, String remoteBranch) {
        File remoteFile = getRemoteFile(remoteName);
        File remoteBranchFile = join(remoteFile, "refs", "heads", remoteBranch);
        if (!remoteBranchFile.exists()) {
            throw new GitletException("That remote does not have that branch.");
        }

        File remoteCommitFile = join(remoteFile, "objects", "commit");
        File remoteBlobFile = join(remoteFile, "objects", "blob");
        String remotePointer = readContentsAsString(remoteBranchFile);
        String currentRemotePointer = getRemotePointer(remoteName, remoteBranch);
        while (!remotePointer.equals(currentRemotePointer)) {
            Commit commit = Commit.remoteLoad(remoteCommitFile, remotePointer);
            commit.getBlobs().forEach((fileName, blobId) -> {
                copy(join(BLOB_DIR, blobId), join(remoteBlobFile, blobId));
            });
            copy(join(COMMIT_DIR, remotePointer), join(remoteCommitFile, remotePointer));

            List<String> parents = commit.getParents();
            if (parents.isEmpty()) {
                break;
            }
            remotePointer = parents.get(0);
        }

        copyContents(join(REMOTE_DIR, remoteName, remoteBranch), remoteBranchFile);
    }

    private static String getRemotePointer(String remoteName, String remoteBranch) {
        try {
            return readContentsAsString(join(REMOTE_DIR, remoteName, remoteBranch));
        } catch (Exception e) {
            return null;
        }
    }

    public static void pull(String remoteName, String remoteBranch) {
        fetch(remoteName, remoteBranch);
        _merge(REMOTE_DIR, remoteName + File.separator + remoteBranch);
    }
}
