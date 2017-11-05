package de.sb.messenger.persistence;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
@Entity
@Table(name = "Message", schema = "messenger")
@PrimaryKeyJoinColumn(name = "messageIdentity")
public class Message extends BaseEntity {

	@NotNull
	@ManyToOne
	@JoinColumn(name = "authorReference")
	private Person author;

	@NotNull
	@ManyToOne
	@JoinColumn(name = "subjectReference")
	private BaseEntity subject;

	@NotNull
	@Size(min = 1, max = 4093)
	@Column(name = "body")
	private String body;

	public Message(Person author, BaseEntity subject) {
		this.author = author;
		this.subject = subject;
	}

	protected Message() {
		this(null, null);
	}

	public Person getAuthor() {
		return author;
	}

	public BaseEntity getSubject() {
		return subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
}
