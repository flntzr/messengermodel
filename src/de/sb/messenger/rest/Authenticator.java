package de.sb.messenger.rest;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import javax.ws.rs.NotAuthorizedException;

import de.sb.messenger.persistence.Person;
import de.sb.toolbox.net.HttpCredentials;
import de.sb.toolbox.net.RestJpaLifecycleProvider;

import java.util.List;


/**
 * Facade interface for HTTP authentication purposes.
 * TODO: Rename to "Authenticator" before use.
 */
public interface Authenticator {

	/**
	 * Returns the authenticated requester (a person) for the given HTTP Basic authentication credentials.
	 * A SHA-256 hash-code is calculated for the contained password, and uses it in conjunction with the
	 * user email to query and return a suitable Person entity from the database.
	 * @param credentials the HTTP Basic authentication credentials
	 * @return the authenticated requestor
	 * @throws NotAuthorizedException (HTTP 401) if the given credentials are invalid
	 * @throws PersistenceException (HTTP 500) if there is a problem with the persistence layer
	 * @throws IllegalStateException (HTTP 500) if the entity manager associated with the current
	 *         thread is not open
	 * @throws NullPointerException (HTTP 500) if the given credentials are {@code null}
	 */
	@SuppressWarnings("unused")
	static public Person authenticate(final HttpCredentials.Basic credentials) throws NotAuthorizedException, PersistenceException, IllegalStateException, NullPointerException {
		final String pql = "select p.identity from Person as p where p.email = :email and p.passwordHash = :passwordHash";
		final EntityManager messengerManager = Persistence.createEntityManagerFactory("messenger").createEntityManager();

		// Add JPA authentication by calculating the password hash from the given password,
		// creating a query using the constant above, and returning the person if it matches the
		// password hash. If there is none, or if it fails the password hash check, then throw
		// NotAuthorizedException("Basic"). Note that this exception type is a specialized Subclass
		// of ClientErrorException that is capable of storing an authentication challenge.
		String email = credentials.getUsername();
		byte[] passwordHash = Person.passwordHash(credentials.getPassword());

		Long foundIdentity;

		final TypedQuery<Long> query = messengerManager.createQuery(pql, Long.class);
		query.setParameter("email", email);
		query.setParameter("passwordHash", passwordHash);

		try{
			foundIdentity = query.getSingleResult();
			return messengerManager.find(Person.class, foundIdentity);
		} catch (Exception e){
			throw new NotAuthorizedException("Basic");
		}

	}
}