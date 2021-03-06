package de.sb.messenger.persistence;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
@XmlRootElement(name = "avatar")
@Entity
@Table(name = "Document", schema = "messenger")
@PrimaryKeyJoinColumn(name = "documentIdentity")
public class Document extends BaseEntity {
	private static final byte[] EMPTY_CONTENT = new byte[0];
	private static final byte[] EMPTY_CONTENT_HASH = mediaHash(EMPTY_CONTENT);


	@XmlElement
	@NotNull
	@Column(nullable = false, updatable = true)
	@Size(min = 32, max = 32)
	private byte[] contentHash;

	@XmlElement
	@NotNull
	@Column(nullable = false, updatable = true)
	@Size(min = 1, max = 63)
	@Pattern(regexp = "^[a-z]+/[a-z.+-]+$")
	private String contentType;

	@XmlTransient
	@NotNull
	@Column(nullable = false, updatable = true)
	@Size(min = 1, max = 16777215)
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

	@Override
	public boolean equals(Object obj) {
		if(obj.getClass() == Document.class) {

			Document other = (Document) obj;
			return  this.getContentType().equalsIgnoreCase(other.getContentType())
					&& Arrays.equals(this.getContent(), other.getContent())
					&& Arrays.equals(this.getContentHash(), other.getContentHash());

		} else {
			return false;
		}
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
