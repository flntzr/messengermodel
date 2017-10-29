package de.sb.messenger.persistence;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
@Entity
@Table(name = "Document")
public class Document extends BaseEntity {
	@Column(name = "contentHash")
	private byte[] contentHash;

	@Column(name = "contentType")
	private String contentType;

	@Column(name = "content")
	private byte[] content;

	private static final byte[] EMPTY_CONTENT_HASH = Document.mediaHash(new byte[0]);

	public Document() {
		this.content = new byte[0];
		this.contentHash = Document.EMPTY_CONTENT_HASH;
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
