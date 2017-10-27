package entity;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
public class Message extends BaseEntity {

	private Person author;
	private BaseEntity subject;
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
