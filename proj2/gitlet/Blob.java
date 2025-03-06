package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {
    private byte[] content;
    private String id;

    public Blob(byte[] content) {
        this.content = content;
        this.id = Utils.sha1(content);
    }

    public Blob(File file) {
        this(Utils.readContents(file));
    }

    public byte[] getContent() {
        return content;
    }

    public String getId() {
        return id;
    }

    public void save() {
        File writeTo = Utils.join(Repository.OBJECTS_DIR, id);
        if(writeTo.exists()){
            return;
        }
        Utils.writeObject(Utils.join(Repository.BLOB_DIR, id), this);
    }

    public static Blob load(String id) {
        return Utils.readObject(Utils.join(Repository.BLOB_DIR, id), Blob.class);
    }

    public static String blobID(File file){
        return Utils.sha1(Utils.readContents(file));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        Blob other = (Blob) o;
        return id.equals(other.id);
    }
}
