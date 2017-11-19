package de.sb.messenger.rest;

import static de.sb.messenger.persistence.Group.ADMIN;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import java.util.Arrays;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import de.sb.messenger.persistence.BaseEntity;
import de.sb.messenger.persistence.Message;
import de.sb.messenger.persistence.Person;
import de.sb.toolbox.Copyright;
import de.sb.toolbox.net.RestCredentials;
import de.sb.toolbox.net.RestJpaLifecycleProvider;


/**
 * JAX-RS based REST service implementation for polymorphic entity resources. The following path and
 * method combinations are supported:
 * <ul>
 * <li>GET entities/{identity}: Returns the entity matching the given identity.</li>
 * <li>DELETE entities/{identity}: Deletes the entity matching the given identity.</li>
 * <li>GET entities/{identity}/messagesCaused: Returns the messages caused by the entity matching
 * the given identity.</li>
 * </ul>
 */
@Path("entities")
@Copyright(year=2013, holders="Sascha Baumeister")
public class EntityService {

	/**
	 * Returns the entity with the given identity.
	 * @param authentication the HTTP Basic "Authorization" header value
	 * @param identity the entity identity
	 * @return the matching entity (HTTP 200)
	 * @throws ClientErrorException (HTTP 400) if the given HTTP "Authorization" header is malformed
	 * @throws ClientErrorException (HTTP 401) if authentication is lacking or invalid
	 * @throws ClientErrorException (HTTP 404) if the given entity cannot be found
	 * @throws PersistenceException (HTTP 500) if there is a problem with the persistence layer
	 * @throws IllegalStateException (HTTP 500) if the entity manager associated with the current
	 *         thread is not open
	 */
	@GET
	@Path("{identity}")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public BaseEntity queryIdentity (@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity) {
		System.out.println(identity);
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));

		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");
		final BaseEntity entity = messengerManager.find(BaseEntity.class, identity);
		if (entity == null) throw new NotFoundException();
		return entity;
	}


	/**
	 * Deletes the entity matching the given identity, or does nothing if no such entity exists.
	 * @param authentication the HTTP Basic "Authorization" header value
	 * @param identity the identity
	 * @return void (HTTP 204)
	 * @throws ClientErrorException (HTTP 400) if the given HTTP "Authorization" header is malformed
	 * @throws ClientErrorException (HTTP 401) if authentication is lacking or invalid
	 * @throws ClientErrorException (HTTP 403) if authentication is successful, but the requester is
	 *         not an administrator
	 * @throws ClientErrorException (HTTP 404) if the given entity cannot be found
	 * @throws ClientErrorException (HTTP 409) if there is a database constraint violation (like
	 *         conflicting locks)
	 * @throws PersistenceException (HTTP 500) if there is a problem with the persistence layer
	 * @throws IllegalStateException (HTTP 500) if the entity manager associated with the current
	 *         thread is not open
	 */
	@DELETE
	@Path("{identity}")
	public void deleteEntity (@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity) {
		final Person requester = Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));

		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");
		if (requester.getGroup() != ADMIN) throw new ClientErrorException(FORBIDDEN);
		messengerManager.getEntityManagerFactory().getCache().evict(BaseEntity.class, identity);

		// check if getReference() works once https://bugs.eclipse.org/bugs/show_bug.cgi?id=460063 is fixed.
		final BaseEntity entity = messengerManager.find(BaseEntity.class, identity);
		if (entity == null) throw new ClientErrorException(NOT_FOUND);
		messengerManager.remove(entity);

		try {
			messengerManager.getTransaction().commit();
		} catch (final RollbackException exception) {
			throw new ClientErrorException(CONFLICT);
		} finally {
			messengerManager.getTransaction().begin();
		}
	}


	/**
	 * Returns the messages caused by the entity matching the given identity.
	 * TODO: check if getReference() works once this bug is fixed:
	 *    https://bugs.eclipse.org/bugs/show_bug.cgi?id=460063.
	 * @param authentication the HTTP Basic "Authorization" header value
	 * @param identity the entity identity
	 * @return the messages caused by the matching entity (HTTP 200)
	 * @throws ClientErrorException (HTTP 400) if the given HTTP "Authorization" header is malformed
	 * @throws ClientErrorException (HTTP 401) if authentication is lacking or invalid
	 * @throws ClientErrorException (HTTP 404) if the given message cannot be found
	 * @throws PersistenceException (HTTP 500) if there is a problem with the persistence layer
	 * @throws IllegalStateException (HTTP 500) if the entity manager associated with the current
	 *         thread is not open
	 */
	@GET
	@Path("{identity}/messagesCaused")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Message[] queryMessagesCaused (@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity) {
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));

		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");
		final BaseEntity entity = messengerManager.find(BaseEntity.class, identity);
		if (entity == null) throw new ClientErrorException(NOT_FOUND);
		final Message[] messages = entity.getMessagesCaused().toArray(new Message[0]);
		Arrays.sort(messages);
		return messages;
	}
}