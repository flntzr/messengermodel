package entity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
public class Document extends BaseEntity {
    private byte[] contentHash;
    private String contentType;
    private byte[] content;

    protected Document(){
        this.contentHash = new byte[32];
        this.content = new byte[16777215];
    }

    public byte[] getContentHash() {
        return contentHash;
    }

    public void setContentHash(byte[] contentHash) {
        this.contentHash = contentHash;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
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
