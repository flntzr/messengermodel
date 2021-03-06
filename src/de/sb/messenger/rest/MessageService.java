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
	
	private static final String SELECT_MESSAGES_QUERY = "SELECT m.identity FROM Message m WHERE "
            + "(:authorReference = 0 OR m.author.identity = :authorReference) AND "
            + "(:subjectReference = 0 OR m.subject.identity = :subjectReference) AND "
            + "(:body IS null OR m.body LIKE :body) AND "
            + "(:creationTimestampLower = 0 OR m.creationTimestamp >= :creationTimestampLower) AND"
            + "(:creationTimestampUpper = 0 OR m.creationTimestamp <= :creationTimestampUpper)";

	@GET
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Collection<Message> getMessages(@HeaderParam("Authorization") final String authentication,
			@QueryParam("authorReference") final long authorReference,
			@QueryParam("subjectReference") final long subjectReference,
			@QueryParam("body") @Size(min = 0, max = 4093) final String body,
			@QueryParam("resultOffset") final int resultOffset, @QueryParam("maxResultLength") int maxResultLength,
			@QueryParam("creationTimestampLower") final long creationTimestampLower,
			@QueryParam("creationTimestampUpper") final long creationTimestampUpper) {

		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		final TypedQuery<Long> queryMessageIds = messengerManager.createQuery(SELECT_MESSAGES_QUERY, Long.class);
		queryMessageIds.setParameter("authorReference", authorReference)
				.setParameter("subjectReference", subjectReference).setParameter("body", body)
				.setParameter("creationTimestampLower", creationTimestampLower)
				.setParameter("creationTimestampUpper", creationTimestampUpper);
		if (resultOffset > 0)
			queryMessageIds.setFirstResult(resultOffset);
		if (maxResultLength > 0)
			queryMessageIds.setMaxResults(maxResultLength);

		final List<Long> messagesIds = queryMessageIds.getResultList();

		SortedSet<Message> sortedMessages = new TreeSet<>(Comparator.naturalOrder());

		for (long id : messagesIds) {
			sortedMessages.add(messengerManager.find(Message.class, id));
		}

		return sortedMessages;
	}

	@PUT
	@Consumes({ APPLICATION_FORM_URLENCODED })
	public long createMessage(@HeaderParam("Authorization") final String authentication,
			@FormParam("body") @NotNull @Size(min = 1, max = 4093) final String body,
			@FormParam("subjectReference") final long subjectReference) {

		final Person requester = Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		BaseEntity subject = messengerManager.find(BaseEntity.class, subjectReference);

		if (subject == null) {
			throw new ClientErrorException(BAD_REQUEST);
		}

		// requester is author
		Message newMessage = new Message(requester, subject);
		newMessage.setBody(body);

		messengerManager.persist(newMessage);

		try {
			messengerManager.getTransaction().commit();
		} catch (Exception e) {
			// suppress CloneNotSupportedException!
			if(e.getCause() == null || !(e.getCause() instanceof CloneNotSupportedException)){
				throw e;
			}
		} finally {
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
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Message queryMessage(@HeaderParam("Authorization") final String authentication,
			@PathParam("identity") final long identity) {
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
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Person queryMessageAuthor(@HeaderParam("Authorization") final String authentication,
			@PathParam("identity") final long identity) {
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
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public BaseEntity queryMessageSubject(@HeaderParam("Authorization") final String authentication,
			@PathParam("identity") final long identity) {
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");
		final Message message = messengerManager.find(Message.class, identity);
		if (message == null) {
			throw new ClientErrorException(NOT_FOUND);
		}
		return message.getSubject();
	}

}
