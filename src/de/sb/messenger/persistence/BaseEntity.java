package de.sb.messenger.persistence;

import javax.persistence.*;
import java.util.Collections;
import java.util.Set;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name="discriminator")
@Table(name = "BaseEntity", schema = "messenger")
public abstract class BaseEntity implements Comparable<BaseEntity> {

	@Id
	@GeneratedValue
	private final long identity;

	@Column(name = "version")
	private int version;

	@Column(name = "creationTimestamp")
	private final long creationTimestamp;

	@OneToMany(mappedBy = "subject")
	private final Set<Message> messagesCaused;

	public BaseEntity() {
		this.identity = 0;
		this.version = 1;
		this.messagesCaused = Collections.emptySet();
		this.creationTimestamp = System.currentTimeMillis();
	}

	public long getIdentity() {
		return identity;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public long getCreationTimestamp() {
		return creationTimestamp;
	}

	public Set<Message> getMessagesCaused() {
		return messagesCaused;
	}

	@Override
	public int compareTo(BaseEntity o) {
		if (this.identity == o.identity) {
			return 0;
		}
		return this.identity < o.identity ? -1 : +1;
	}
}
