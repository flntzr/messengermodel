package de.sb.messenger.rest;

import de.sb.messenger.persistence.BaseEntity;
import de.sb.messenger.persistence.Message;
import de.sb.messenger.persistence.Person;

import javax.persistence.*;
import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("messages")
@Consumes(APPLICATION_FORM_URLENCODED)
public class MessageService {


    private static final EntityManagerFactory managerFactory = Persistence.createEntityManagerFactory("messenger");
    private static final EntityManager messengerManager = managerFactory.createEntityManager();


    @GET
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public List<Message> getMessages(@HeaderParam("Authorization") final String authentication){
        TypedQuery<Message> queryMessages = messengerManager.createQuery("SELECT m FROM Message m", Message.class);
        List<Message> messages = queryMessages.getResultList();

        if(messages.isEmpty())
            throw new ClientErrorException(NOT_FOUND);

        return messages;
    }

    @PUT
    @Consumes({APPLICATION_FORM_URLENCODED})
    public long createMessage(@HeaderParam("Authorization") final String authentication,
                              @FormParam("body") final String body, @FormParam("authorReference") final long authorReference,
                              @FormParam("subjectReference") final long subjectReference){

        messengerManager.getTransaction().begin();


        Person author = messengerManager.find(Person.class, authorReference);
        BaseEntity subject = messengerManager.find(BaseEntity.class, subjectReference);

        if(author == null || subject == null || body == null){
            throw new ClientErrorException(NOT_ACCEPTABLE);
        }

        Message newMessage = new Message(author, subject);
        newMessage.setBody(body);

        messengerManager.persist(newMessage);
        messengerManager.getTransaction().commit();

        // evict author and subject from second level cache
        Cache cache = managerFactory.getCache();
        cache.evict(Person.class, author.getIdentity());
        cache.evict(BaseEntity.class, subject.getIdentity());

        return newMessage.getIdentity();
    }


    @GET
    @Path("{identity}")
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public Message queryMessage(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
        Message message = messengerManager.find(Message.class, identity);
        if (message == null) {
            throw new ClientErrorException(NOT_FOUND);
        }
        return message;
    }

    @GET
    @Path("{identity}/author")
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public Person queryMessageAuthor(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
        Message message = messengerManager.find(Message.class, identity);
        if (message == null) {
            throw new ClientErrorException(NOT_FOUND);
        }
        return message.getAuthor();
    }

    @GET
    @Path("{identity}/subject")
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public BaseEntity queryMessageSubject(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
        Message message = messengerManager.find(Message.class, identity);
        if (message == null) {
            throw new ClientErrorException(NOT_FOUND);
        }
        return message.getSubject();
    }


}
