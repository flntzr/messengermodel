package de.sb.messenger.persistence;

import javax.persistence.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


@Entity
@DiscriminatorValue("Person")
@Table(name = "Person", schema = "messenger")
@PrimaryKeyJoinColumn(name = "personIdentity")
public class Person extends BaseEntity {

	@Id

	@Column(name = "email")
	private String mail;

	@Column(name = "passwordHash")
	private byte[] passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "groupAlias")
	private Group group;

	@Embedded
	private Name name;

	@Embedded
	private Address address;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "avatarReference")
	private Document avatar;

	@OneToMany(mappedBy = "author")
	private Set<Message> messagesAuthored;

	@ManyToMany(mappedBy = "peopleObserved")
	private Set<Person> peopleObserving;

	@ManyToMany
	private Set<Person> peopleObserved;

	static public byte[] passwordHash(String password) {
		return Document.mediaHash(password.getBytes(StandardCharsets.UTF_8));
	}

	public Person() {
		this.passwordHash = new byte[32];
		this.name = new Name();
		this.address = new Address();
		this.group = Group.USER;
		this.messagesAuthored = Collections.emptySet();
		this.peopleObserving = Collections.emptySet();
		this.peopleObserved = new HashSet<>();
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public byte[] getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(byte[] passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public Name getName() {
		return name;
	}

	public Address getAddress() {
		return address;
	}

	public Document getAvatar() {
		return avatar;
	}

	public void setAvatar(Document avatar) {
		this.avatar = avatar;
	}

	public Set<Message> getMessagesAuthored() {
		return messagesAuthored;
	}

	public Set<Person> getPeopleObserving() {
		return peopleObserving;
	}

	public Set<Person> getPeopleObserved() {
		return peopleObserved;
	}

}
