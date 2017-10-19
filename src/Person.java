import java.util.List;

public class Person {
	private char[] mail;
	private byte[] passwordHash;
	private Group group;
	private Name name;
	private Address address;
	private Document avatar;
	private List<Message> messagesAuthored;
	private List<Person> peopleObserving;
	private List<Person> peopleObserved;

	public Person() {
		this.mail = new char[128];
		this.passwordHash = new byte[32];
	}

	public static byte[] passwordHash(String password) {
	}

	public char[] getMail() {
		return mail;
	}

	public void setMail(char[] mail) {
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

	public List<Message> getMessagesAuthored() {
		return messagesAuthored;
	}

	public void setMessagesAuthored(List<Message> messagesAuthored) {
		this.messagesAuthored = messagesAuthored;
	}

	public List<Person> getPeopleObserving() {
		return peopleObserving;
	}

	public void setPeopleObserving(List<Person> peopleObserving) {
		this.peopleObserving = peopleObserving;
	}

	public List<Person> getPeopleObserved() {
		return peopleObserved;
	}

	public void setPeopleObserved(List<Person> peopleObserved) {
		this.peopleObserved = peopleObserved;
	}
	
	
}
