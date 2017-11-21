package de.sb.messenger.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType
@XmlSeeAlso({Document.class, Message.class, Person.class})
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
@Table(name = "BaseEntity", schema = "messenger")
public abstract class BaseEntity implements Comparable<BaseEntity> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private final long identity;

	//@Version -> search in wiki: optimistic locking
	@Column(nullable = false, updatable = false)
	private int version;

	@Column(nullable = false, updatable = false)
	private final long creationTimestamp;

	@OneToMany(mappedBy = "subject", cascade = CascadeType.REMOVE)
	private final Set<Message> messagesCaused;

	public BaseEntity() {
		this.identity = 0;
		this.version = 1;
		this.messagesCaused = new HashSet<>();
		this.creationTimestamp = System.currentTimeMillis();
	}

	@XmlElement(name = "identity")
	public long getIdentity() {
		return identity;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	@XmlElement(name = "creationTimestamp")
	public long getCreationTimestamp() {
		return creationTimestamp;
	}

	//@XmlElement -> Relationsfeld
	public Set<Message> getMessagesCaused() {
		return messagesCaused;
	}

	@Override
	public String toString() {
		return this.getClass().getName() + "@" + this.getIdentity();
	}

	@Override
	public int compareTo(BaseEntity o) {
		return Long.compare(this.identity, o.identity);
	}
}
