package entity;
/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
public class Message extends BaseEntity {

    private Person author;
    private BaseEntity subject;
    private String body;

    protected Message(){
    }

    public Person getAuthor() {
        return author;
    }

    public void setAuthor(Person author) {
        this.author = author;
    }

    public BaseEntity getSubject() {
        return subject;
    }

    public void setSubject(BaseEntity subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
