package de.sb.messenger.persistence;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Person", schema = "messenger")
@PrimaryKeyJoinColumn(name = "personIdentity")
public class Person extends BaseEntity {

	private static final byte[] EMPTY_PASSWORD_HASH = Person.passwordHash("");

	@Column(name = "email")
	@NotNull
	@Size(min = 1, max = 128)
	@Pattern(regexp = ".+@.+")
	private String email;

	@Column(name = "passwordHash")
	@NotNull
	@Size(min = 32, max = 32)
	private byte[] passwordHash;

	@Enumerated(EnumType.STRING)
	@NotNull
	@Column(name = "groupAlias")
	private Group group;

	@NotNull
	@Valid
	@Embedded
	private final Name name;

	@NotNull
	@Valid
	@Embedded
	private final Address address;

	@NotNull
	@ManyToOne
	@JoinColumn(name = "avatarReference")
	private Document avatar;

	@OneToMany(mappedBy = "author")
	private final Set<Message> messagesAuthored;

	@ManyToMany(mappedBy = "peopleObserved")
	private final Set<Person> peopleObserving;

	@ManyToMany
	private final Set<Person> peopleObserved;

	static public byte[] passwordHash(String password) {
		return Document.mediaHash(password.getBytes(StandardCharsets.UTF_8));
	}

	public Person(Document avatar) {
		this.passwordHash = Person.EMPTY_PASSWORD_HASH;
		this.name = new Name();
		this.address = new Address();
		this.group = Group.USER;
		this.messagesAuthored = Collections.emptySet();
		this.peopleObserving = Collections.emptySet();
		this.peopleObserved = new HashSet<>();
		this.avatar = avatar;

	}

	protected Person() {
		this(null);
	}

	public String getMail() {
		return email;
	}

	public void setMail(String mail) {
		this.email = mail;
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
