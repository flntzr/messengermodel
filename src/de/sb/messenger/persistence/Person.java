package de.sb.messenger.persistence;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.persistence.annotations.CacheIndex;

@XmlRootElement(name = "person")
@Entity
@Table(name = "Person", schema = "messenger")
@PrimaryKeyJoinColumn(name = "personIdentity")
public class Person extends BaseEntity {

	private static final byte[] EMPTY_PASSWORD_HASH = Person.passwordHash("");

	
	@XmlElement
	@Column(nullable = false, updatable = true)
	@NotNull
	@Size(min = 1, max = 128)
	@Pattern(regexp = "^.+@.+$")
	@CacheIndex(updateable = true)
	private String email;

	@Column(nullable = false, updatable = true)
	@NotNull
	@Size(min = 32, max = 32)
	private byte[] passwordHash;

	@XmlElement
	@Enumerated(EnumType.STRING)
	@NotNull
	@Column(name = "groupAlias", nullable = false, updatable = true)
	private Group group;

	@NotNull
	@Valid
	@Embedded
	@XmlElement
	private final Name name;

	@NotNull
	@Valid
	@Embedded
	@XmlElement
	private final Address address;

	@NotNull
	@ManyToOne(optional = false)
	@JoinColumn(name = "avatarReference", nullable = false, updatable = true)
	private Document avatar;

	@NotNull
	@OneToMany(mappedBy = "author", cascade = CascadeType.REMOVE)
	private final Set<Message> messagesAuthored;

	@NotNull
	@ManyToMany(mappedBy = "peopleObserved")
	private final Set<Person> peopleObserving;

	@NotNull
	@ManyToMany
	@JoinTable(schema = "messenger", name = "ObservationAssociation", joinColumns = @JoinColumn(name = "observingReference", nullable = false), inverseJoinColumns = @JoinColumn(name = "observedReference", nullable = false))
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
		this(new Document());
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

	@Override
	public boolean equals(Object obj) {
		if (obj.getClass() == Person.class) {
			Person other = (Person) obj;
			return Objects.equals(this.getName(), other.getName())
					&& Objects.equals(this.getAddress(), other.getAddress())
					&& Objects.equals(this.getMail(), other.getMail())
					&& Objects.equals(this.getAvatar(), other.getAvatar())
					&& this.getMessagesAuthored().size() == other.getMessagesAuthored().size()
					// && this.getMessagesAuthored().containsAll(other.getMessagesAuthored())
					&& this.getPeopleObserving().size() == other.getPeopleObserving().size()
					// && this.getPeopleObserving().containsAll(other.getPeopleObserving())
					&& this.getPeopleObserved().size() == other.getPeopleObserved().size();
			// && this.getPeopleObserved().containsAll(other.getPeopleObserved());
		} else {
			return false;
		}
	}
}
