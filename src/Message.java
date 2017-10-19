/**
 * Created by Jakob Pfeiffer on 19.10.17.
 */
public class Message extends BaseEntity {

    private Person author;
    private BaseEntity subject;
    private char[] body;

    protected Message(){
        this.body = new char[4093];
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

    public char[] getBody() {
        return body;
    }

    public void setBody(char[] body) {
        this.body = body;
    }
}
