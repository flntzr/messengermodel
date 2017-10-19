import java.util.List;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
public class BaseEntity implements Comparable{
    private long identity;
    private int version;
    private long creationTimestamp;
    List<Message> messagesCaused;

    protected BaseEntity(){

    }

    public long getIdentity() {
        return identity;
    }

    public void setIdentity(long identity) {
        this.identity = identity;
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

    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public List<Message> getMessagesCaused() {
        return messagesCaused;
    }

    public void setMessagesCaused(List<Message> messagesCaused) {
        this.messagesCaused = messagesCaused;
    }

    @Override
    public int compareTo(Object o) {
        return this.identity < ((BaseEntity) o).identity ? -1 : this.identity > ((BaseEntity) o).identity ? 1 : 0;
    }
}
