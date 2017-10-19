import sun.plugin2.message.Message;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
public class Document extends BaseEntity {
    private byte[] contentHash;
    private char[] contentType;
    private byte[] content;

    protected Document(){
        contentHash = new byte[32];
        contentType = new char[64];
        content = new byte[16777215];
    }

    public byte[] getContentHash() {
        return contentHash;
    }

    public void setContentHash(byte[] contentHash) {
        this.contentHash = contentHash;
    }

    public char[] getContentType() {
        return contentType;
    }

    public void setContentType(char[] contentType) {
        this.contentType = contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    static public byte[] mediaHash(byte[] content){
        MessageDigest messageDigest;
        byte[] hash = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA256");
            hash = messageDigest.digest(content);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hash;
    }
}
