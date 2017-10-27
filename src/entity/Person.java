package entity;
import java.util.Set;

public class Person extends BaseEntity {
	private String mail;
	private byte[] passwordHash;
	private Group group;
	private Name name;
	private Address address;
	private Document avatar;
	private Set<Message> messagesAuthored;
	private Set<Person> peopleObserving;
	private Set<Person> peopleObserved;

	public Person() {
		this.passwordHash = new byte[32];
	}

	public static byte[] passwordHash(String password) {
		return null;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
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

	public void setName(Name name) {
		this.name = name;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
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

	public void setMessagesAuthored(Set<Message> messagesAuthored) {
		this.messagesAuthored = messagesAuthored;
	}

	public Set<Person> getPeopleObserving() {
		return peopleObserving;
	}

	public void setPeopleObserving(Set<Person> peopleObserving) {
		this.peopleObserving = peopleObserving;
	}

	public Set<Person> getPeopleObserved() {
		return peopleObserved;
	}

	public void setPeopleObserved(Set<Person> peopleObserved) {
		this.peopleObserved = peopleObserved;
	}	
	
}
