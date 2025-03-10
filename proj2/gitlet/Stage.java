package gitlet;

import java.io.Serializable;
import java.util.TreeMap;

public class Stage implements Serializable {
    public TreeMap<String, String> blobs = new TreeMap<>();

    public static Stage load() {
        return Utils.readObject(Repository.STAGE_FILE, Stage.class);
    }

    public void add(String fileName, String blobId) {
        blobs.put(fileName, blobId);
    }

    public void save() {
        Utils.writeObject(Repository.STAGE_FILE, this);
    }

    public static void clear() {
        Utils.writeObject(Repository.STAGE_FILE, new Stage());
    }

    public static void addStage(String fileName, String blobId) {
        Stage stage = load();
        stage.add(fileName, blobId);
        stage.save();
    }

    public boolean isEmpty() {
        return blobs.isEmpty();
    }

}
