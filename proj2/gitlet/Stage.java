package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Stage implements Serializable {
    public Map<String, String> blobs = new HashMap<>();

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
