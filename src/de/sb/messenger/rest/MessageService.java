package de.sb.messenger.rest;

import de.sb.messenger.persistence.BaseEntity;
import de.sb.messenger.persistence.Group;
import de.sb.messenger.persistence.Message;
import de.sb.messenger.persistence.Person;
import de.sb.toolbox.net.RestCredentials;
import de.sb.toolbox.net.RestJpaLifecycleProvider;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;

import java.util.Comparator;
import java.util.List;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("messages")
@Consumes(APPLICATION_FORM_URLENCODED)
public class MessageService {


    // array statt liste (besser bei Response)
    // collection als rückgabewert

    @GET
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public List<Message> getMessages(@HeaderParam("Authorization") final String authentication){
        // TODO: filter query: resultoffset und -länge, creationTimestamp Bound (lower upper), body fragment (like), subjectRef, authorRef
        // -> sowie bei Person GET
        Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
        final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");
        final TypedQuery<Message> queryMessages = messengerManager.createQuery("SELECT m FROM Message m", Message.class);
        final List<Message> messages = queryMessages.getResultList();

        // leere liste ist auch ein Ergebnis
        /*if(messages.isEmpty())
            throw new ClientErrorException(NOT_FOUND);*/

        messages.sort(Comparator.naturalOrder());

        return messages;
    }

    @PUT
    @Consumes({APPLICATION_FORM_URLENCODED})
    public long createMessage(@HeaderParam("Authorization") final String authentication,
                              @FormParam("body") @NotNull @Size(min = 1, max = 4093) final String body, @FormParam("authorReference") final long authorReference,
                              @FormParam("subjectReference") final long subjectReference){

        // TODO: requester als author setzen, parameter weg
        final Person requester = Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
        if(requester.getIdentity() != authorReference){
            throw new NotAuthorizedException("Basic");
        }
        final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

        // author kann weg
        Person author = messengerManager.find(Person.class, authorReference);
        BaseEntity subject = messengerManager.find(BaseEntity.class, subjectReference);

        if(author == null || subject == null || body == null){
            throw new ClientErrorException(BAD_REQUEST);
        }

        Message newMessage = new Message(author, subject);
        newMessage.setBody(body);

        messengerManager.persist(newMessage);

        try {
            messengerManager.getTransaction().commit();
        } finally {
            messengerManager.getTransaction().begin();
        }

        // evict author and subject from second level cache
        final Cache cache = messengerManager.getEntityManagerFactory().getCache();
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
