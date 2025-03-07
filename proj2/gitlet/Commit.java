package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */
    private Date timestamp;
    private List<String> parents;
    private Map<String, String> blobs;

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

    /* TODO: fill in the rest of this class. */
    public Commit(String message, Map<String, String> blobs, List<String> parents){
        this(new Date(), message, blobs, parents);
    }
    public Commit(Date timestamp, String message, Map<String, String> blobs, List<String> parents){
        this.timestamp = timestamp;
        this.message = message;
        this.parents = parents;
        if(parents.isEmpty()){
            this.blobs = blobs;
        }else{
            Commit parent = load(parents.get(0));
            this.blobs = parent.blobs;
//            this.blobs.putAll(blobs);
            blobs.forEach((k, v) -> {
                if(!v.startsWith("-")){
                    this.blobs.put(k, v);
                }else{
                    this.blobs.remove(k);
                }
            });
        }
    }

    public String getTimestamp(){
        return timestampConverter(timestamp);
    }

    public String getMessage(){
        return message;
    }

    public List<String> getParents(){
        return parents;
    }

    private static String timestampConverter(Date date){
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(date);
    }

    public static Commit load(String commitId) {
        List<String> commitIds = Utils.plainFilenamesIn(Repository.COMMIT_DIR);
        List<String> matching = commitIds.stream()
                .filter(id -> id.startsWith(commitId))
                .toList();
        if(matching.isEmpty()){
            throw new GitletException("No commit with that id exists.");
        }else if(matching.size() > 1) {
            throw new GitletException("Ambiguous commit id.");
        }
        return Utils.readObject(Utils.join(Repository.COMMIT_DIR, matching.get(0)), Commit.class);
    }

    public String getID(){
        return Utils.sha1(timestampConverter(timestamp), message, blobs.toString(), parents.toString());
    }

    public void save() {
        Utils.writeObject(Utils.join(Repository.COMMIT_DIR, getID()), this);
    }

    public boolean containsFile(String fileName){
        return blobs.containsKey(fileName);
    }

    public String getBlobId(String fileName){
        return blobs.get(fileName);
    }

    public Map<String, String> getBlobs() {
        return blobs;
    }
}
