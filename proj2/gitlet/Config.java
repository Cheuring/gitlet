package gitlet;

import java.io.Serializable;
import java.util.HashMap;

public class Config implements Serializable {
    public HashMap<String, String> remote = new HashMap<>();

    public static Config load() {
        return Utils.readObject(Repository.CONFIG_FILE, Config.class);
    }

    public void add(String remoteName, String remotePath) {
        remote.put(remoteName, remotePath);
    }

    public void save() {
        Utils.writeObject(Repository.CONFIG_FILE, this);
    }

    public static void clear() {
        Utils.writeObject(Repository.CONFIG_FILE, new Config());
    }

    public static void addRemote(String remoteName, String remotePath) {
        Config config = load();
        config.add(remoteName, remotePath);
        config.save();
    }

}
