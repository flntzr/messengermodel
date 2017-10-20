package entity;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
public class Document extends BaseEntity {
	private byte[] contentHash;
	private String contentType;
	private byte[] content;

	public Document() {
		this.content = new byte[0];
		this.contentHash = Document.mediaHash(this.content);
	}

	public byte[] getContentHash() {
		return contentHash;
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
		this.contentHash = Document.mediaHash(content);
		this.content = content;
	}

	static public byte[] mediaHash(byte[] content) {
		MessageDigest messageDigest;
		byte[] hash = null;
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
			hash = messageDigest.digest(content);
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError(e);
		}
		return hash;
	}
}
