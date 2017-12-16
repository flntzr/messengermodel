package de.sb.messenger.rest;

import de.sb.messenger.persistence.BaseEntity;
import de.sb.messenger.persistence.Message;
import de.sb.messenger.persistence.Person;
import de.sb.toolbox.net.RestCredentials;
import de.sb.toolbox.net.RestJpaLifecycleProvider;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import java.util.*;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("messages")
@Consumes(APPLICATION_FORM_URLENCODED)
public class MessageService {

    @PUT
    @Consumes({APPLICATION_FORM_URLENCODED})
    public long createMessage(@HeaderParam("Authorization") final String authentication,
                              @FormParam("body") @NotNull @Size(min = 1, max = 4093) final String body,
                              @FormParam("subjectReference") final long subjectReference){

        final Person requester = Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
        final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

        BaseEntity subject = messengerManager.find(BaseEntity.class, subjectReference);

        if(subject == null || body == null){
            throw new ClientErrorException(BAD_REQUEST);
        }

        // requester is author
        Message newMessage = new Message(requester, subject);
        newMessage.setBody(body);

        messengerManager.persist(newMessage);

        try {
            messengerManager.getTransaction().commit();
        } catch (Exception e){
            e.printStackTrace(); // CloneNotSupportedException......
        }
        finally {
            messengerManager.getTransaction().begin();
        }

        // evict author and subject from second level cache
        final Cache cache = messengerManager.getEntityManagerFactory().getCache();
        cache.evict(Person.class, requester.getIdentity());
        cache.evict(BaseEntity.class, subject.getIdentity());

        return newMessage.getIdentity();
    }


    @GET
    @Path("{identity}")
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public Message queryMessage(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
        Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
        final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");
        final Message message = messengerManager.find(Message.class, identity);
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
        final Message message = messengerManager.find(Message.class, identity);
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
        final Message message = messengerManager.find(Message.class, identity);
        if (message == null) {
            throw new ClientErrorException(NOT_FOUND);
        }
        return message.getSubject();
    }


}
