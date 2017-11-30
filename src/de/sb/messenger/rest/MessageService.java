package de.sb.messenger.rest;

import de.sb.messenger.persistence.BaseEntity;
import de.sb.messenger.persistence.Message;
import de.sb.messenger.persistence.Person;
import de.sb.toolbox.net.RestCredentials;
import de.sb.toolbox.net.RestJpaLifecycleProvider;

import javax.persistence.*;
import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("messages")
@Consumes(APPLICATION_FORM_URLENCODED)
public class MessageService {


    @GET
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public List<Message> getMessages(@HeaderParam("Authorization") final String authentication){
        Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
        final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");
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

        Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
        final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");


        Person author = messengerManager.find(Person.class, authorReference);
        BaseEntity subject = messengerManager.find(BaseEntity.class, subjectReference);

        if(author == null || subject == null || body == null){
            throw new ClientErrorException(BAD_REQUEST);
        }

        Message newMessage = new Message(author, subject);
        newMessage.setBody(body);

        try {
            messengerManager.persist(newMessage);
            messengerManager.getTransaction().commit();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            messengerManager.getTransaction().begin();
        }

        // evict author and subject from second level cache
        Cache cache = messengerManager.getEntityManagerFactory().getCache();
        cache.evict(Person.class, author.getIdentity());
        cache.evict(BaseEntity.class, subject.getIdentity());

        return newMessage.getIdentity();
    }


    @GET
    @Path("{identity}")
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public Message queryMessage(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
        Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
        final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");
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
        Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
        final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");
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
        Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
        final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");
        Message message = messengerManager.find(Message.class, identity);
        if (message == null) {
            throw new ClientErrorException(NOT_FOUND);
        }
        return message.getSubject();
    }


}
