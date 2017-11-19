package de.sb.messenger.persistence;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Objects;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
@Entity
@Table(name = "Message", schema = "messenger")
@PrimaryKeyJoinColumn(name = "messageIdentity")
public class Message extends BaseEntity {



	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "authorReference", nullable = false, updatable = false)
	private Person author;

	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "subjectReference", nullable = false, updatable = false)
	private BaseEntity subject;

	@NotNull
	@Size(min = 1, max = 4093)
	@Column(name = "body", nullable = false)
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

	@Override
	public boolean equals(Object obj) {
		if(obj.getClass() == Message.class) {

			Message other = (Message) obj;
			boolean sameAuthor = Objects.equals(this.getAuthor(), other.getAuthor());
			boolean sameSubject = Objects.equals(this.getSubject(), other.getSubject());
			boolean sameBody = Objects.equals(this.getBody(), other.getBody());
			return sameAuthor && sameSubject && sameBody;

		} else {
			return false;
		}
	}
}
