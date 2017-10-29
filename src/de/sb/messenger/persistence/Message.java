package de.sb.messenger.persistence;

import javax.persistence.*;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
@Entity
@Table(name = "Message")
public class Message extends BaseEntity {


	@ManyToOne
	@JoinColumn(name = "authorReference")
	private Person author;

	@ManyToOne
	@JoinColumn(name = "subjectReference")
	private BaseEntity subject;

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
