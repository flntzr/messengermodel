import java.util.List;

/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
public class BaseEntity implements Comparable{
    private long identity;
    private int version;
    private long creationTimestamp;
    List<Message> messagesCaused;

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
