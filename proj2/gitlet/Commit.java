package gitlet;


import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/** Represents a gitlet commit object.
 *
 *  @author Cheuring
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */
    private Date timestamp;
    private List<String> parents;
    private TreeMap<String, String> blobs;

    /** The message of this Commit. */
    private String message;

    @Override
    public String toString() {
        return "Commit{" +
                "timestamp=" + timestamp +
                ", parents=" + parents +
                ", blobs=" + blobs +
                ", message='" + message + '\'' +
                '}';
    }

    public Commit(String message, TreeMap<String, String> blobs, List<String> parents) {
        this(new Date(), message, blobs, parents);
    }

    public Commit(Date timestamp, String message, TreeMap<String, String> blobs, List<String> parents) {
        this.timestamp = timestamp;
        this.message = message;
        this.parents = parents;
        if (parents.isEmpty()) {
            this.blobs = blobs;
        } else {
            Commit parent = load(parents.get(0));
            this.blobs = parent.blobs;
            this.blobs.putAll(blobs);
        }
    }

    public String getTimestamp() {
        return timestampConverter(timestamp);
    }

    public String getMessage() {
        return message;
    }

    public List<String> getParents() {
        return parents;
    }

    private static String timestampConverter(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(date);
    }

    public static Commit load(String commitId) {
        if (commitId.length() < Utils.UID_LENGTH) {
            commitId = getFullCommitId(commitId);
        }
        File commitFile = Utils.join(Repository.COMMIT_DIR, commitId);
        if (!commitFile.exists()) {
            throw new GitletException("No commit with that id exists.");
        }
        return Utils.readObject(commitFile, Commit.class);
    }

    public static String getFullCommitId(String commitId) {
        List<String> commitIds = Utils.plainFilenamesIn(Repository.COMMIT_DIR);
        List<String> matching = commitIds.stream()
                .filter(id -> id.startsWith(commitId))
                .collect(Collectors.toList());
        if (matching.isEmpty()) {
            throw new GitletException("No commit with that id exists.");
        } else if (matching.size() > 1) {
            throw new GitletException("Ambiguous commit id.");
        }
        return matching.get(0);
    }

    public static Commit remoteLoad(File remoteCommitFile, String commitId) {
        return Utils.readObject(Utils.join(remoteCommitFile, commitId), Commit.class);
    }

    public String getID() {
        return Utils.sha1(timestampConverter(timestamp), message, blobs.toString(), parents.toString());
    }

    public void save() {
        Utils.writeObject(Utils.join(Repository.COMMIT_DIR, getID()), this);
    }

    public boolean containsFile(String fileName) {
        return blobs.containsKey(fileName) && !blobs.get(fileName).startsWith("-");
    }

    public String getBlobId(String fileName) {
        return blobs.get(fileName);
    }

    public Map<String, String> getBlobs() {
        return blobs;
    }

    public void log() {
        System.out.println("===");
        System.out.println("commit " + getID());
        if (parents.size() > 1) {
            System.out.println("Merge: " + parents.get(0).substring(0, 7) + " " + parents.get(1).substring(0, 7));
        }
        System.out.println("Date: " + getTimestamp());
        System.out.println(message);
        System.out.println();
    }
}
