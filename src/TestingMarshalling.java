import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import de.sb.messenger.persistence.Document;
import de.sb.messenger.persistence.Person;

public class TestingMarshalling {

    static public void marshalling(){
        try {
            Person person = createTestPerson();
            JAXBContext jc = JAXBContext.newInstance(Person.class);
            Marshaller ms = jc.createMarshaller();
            ms.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
            ms.marshal(person,System.out);
        }
        catch (Exception e){
            System.out.print(e.toString());
        }
    }

    static private Person createTestPerson() {
        Document avatar = new Document();
        avatar.setContent(new byte[3]);
        avatar.setContentType("image/jpeg");
        Person person = new Person(avatar);
        person.getName().setGiven("Alp");
        person.getName().setFamily("Akduman");
        person.setPasswordHash(Person.passwordHash("password"));
        person.setMail("malpaakduman@hotmail.de");
        person.getAddress().setCity("Berlin");
        person.getAddress().setPostcode("12459");
        person.getAddress().setStreet("Deulstraße 10");
        return person;
    }

}
