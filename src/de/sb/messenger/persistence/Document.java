package de.sb.messenger.persistence;

import javax.persistence.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
@Entity
@Table(name = "Document", schema = "messenger")
@PrimaryKeyJoinColumn(name = "documentIdentity")
public class Document extends BaseEntity {
	private static final byte[] EMPTY_CONTENT = new byte[0];
	private static final byte[] EMPTY_CONTENT_HASH = mediaHash(EMPTY_CONTENT);

	@Column(name = "contentHash")
	private byte[] contentHash;

	@Column(name = "contentType")
	private String contentType;

	@Column(name = "content")
	private byte[] content;

	public Document() {
		this.content = EMPTY_CONTENT;
		this.contentHash = EMPTY_CONTENT_HASH;
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
		this.contentHash = mediaHash(content);
		this.content = content;
	}

	static public byte[] mediaHash(byte[] content) {
		try {
			final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			return messageDigest.digest(content);
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError(e);
		}
	}
}
