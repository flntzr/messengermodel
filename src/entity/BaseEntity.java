package entity;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
public abstract class BaseEntity implements Comparable<BaseEntity> {
	private long identity;
	private int version;
	private long creationTimestamp;
	Set<Message> messagesCaused;

	public BaseEntity() {
		this.version = 1;
		this.messagesCaused = new HashSet<>();
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
