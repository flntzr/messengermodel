package de.sb.messenger.rest;

import de.sb.messenger.persistence.Document;
import de.sb.messenger.persistence.Group;
import de.sb.messenger.persistence.Message;
import de.sb.messenger.persistence.Person;
import de.sb.toolbox.net.RestCredentials;
import de.sb.toolbox.net.RestJpaLifecycleProvider;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

@Path("people")
public class PersonService {

	private static final EntityManagerFactory MESSENGER_FACTORY = Persistence.createEntityManagerFactory("messenger");
	private static final String SELECT_PERSONS_QUERY = "SELECT p.identity FROM Person p WHERE "
			+ "(:givenName is null OR p.name.given = :givenName) AND "
			+ "(:familyName is null or p.name.family = :familyName) AND " + "(:mail IS null OR p.email = :mail) AND "
			+ "(:street IS null OR p.address.street = :street) AND "
			+ "(:postcode IS null OR p.address.postcode = :postcode) AND"
			+ "(:city IS null OR p.address.city = :city) AND" + "(:group IS null OR p.group = :group) AND "
			+ "(:creationTimestampLower = 0 OR p.creationTimestamp >= :creationTimestampLower) AND"
			+ "(:creationTimestampUpper = 0 OR p.creationTimestamp <= :creationTimestampUpper)";
	private static final Comparator<Person> PERSON_COMPARATOR = Comparator
			.comparing((Person p) -> p.getName().getFamily()).thenComparing((Person p) -> p.getName().getGiven())
			.thenComparing(Person::getMail);

	@GET
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Collection<Person> queryPersons(@HeaderParam("Authorization") final String authentication,
			@QueryParam("givenName") final String givenName, @QueryParam("familyName") final String familyName,
			@QueryParam("mail") final String mail, @QueryParam("street") final String street,
			@QueryParam("postcode") final String postcode, @QueryParam("city") final String city,
			@QueryParam("group") final Group group, @QueryParam("resultOffset") final int resultOffset,
			@QueryParam("maxResultLength") int maxResultLength,
			@QueryParam("creationTimestampLower") final long creationTimestampLower,
			@QueryParam("creationTimestampUpper") final long creationTimestampUpper) {
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManagerLife = RestJpaLifecycleProvider.entityManager("messenger");

		// how to handle that? ask baumeister
		if (maxResultLength == 0)
			maxResultLength = 100;

		TypedQuery<Long> query = messengerManagerLife.createQuery(PersonService.SELECT_PERSONS_QUERY, Long.class);
		List<Long> results = query.setParameter("mail", mail).setParameter("givenName", givenName)
				.setParameter("familyName", familyName).setParameter("street", street)
				.setParameter("postcode", postcode).setParameter("city", city).setParameter("group", group)
				.setParameter("creationTimestampLower", creationTimestampLower)
				.setParameter("creationTimestampUpper", creationTimestampUpper).setFirstResult(resultOffset)
				.setMaxResults(maxResultLength).getResultList();
		SortedSet<Person> sortedPersons = new TreeSet<>(PersonService.PERSON_COMPARATOR);

		for (long id : results) {
			sortedPersons.add(messengerManagerLife.find(Person.class, id));
		}
		return sortedPersons;
	}

	@GET
	@Path("{identity}")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Person queryPerson(@HeaderParam("Authorization") final String authentication,
			@PathParam("identity") final long identity) {

		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		final Person person = messengerManager.find(Person.class, identity);
		if (person == null) {
			throw new ClientErrorException(NOT_FOUND);
		}
		return person;
	}

	@GET
	@Path("/requester")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Person getRequester(@HeaderParam("Authorization") final String authentication) {
		return Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
	}

	@PUT
	@Consumes({ APPLICATION_JSON, APPLICATION_XML })
	@Produces(TEXT_PLAIN)
	public long updatePerson(@HeaderParam("Authorization") final String authentication,
			@HeaderParam("password") final String password, @Valid @NotNull final Person person) {

		final Person requester = Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));

		// gives back 403, if requester is not ADMIN and does not alter himself or if he
		// wants to change his group to ADMIN
		if (requester.getGroup() != Group.ADMIN) {
			if (requester.getIdentity() != person.getIdentity() || person.getGroup() == Group.ADMIN) {
				throw new ClientErrorException(403);
			}
		}

		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person newPerson;
		final boolean insertMode = person.getIdentity() == 0;

		if (insertMode) {
			Document avatar = messengerManager.find(Document.class, 1L);
			newPerson = new Person(avatar);
		} else {
			newPerson = messengerManager.find(Person.class, person.getIdentity());
		}

		if (password != null) {
			newPerson.setPasswordHash(Person.passwordHash(password));
		}

		newPerson.setMail(person.getMail());
		newPerson.setGroup(person.getGroup());
		newPerson.getAddress().setCity(person.getAddress().getCity());
		newPerson.getAddress().setPostcode(person.getAddress().getPostcode());
		newPerson.getAddress().setStreet(person.getAddress().getStreet());
		newPerson.getName().setFamily(person.getName().getFamily());
		newPerson.getName().setGiven(person.getName().getGiven());

		if (insertMode) {
			messengerManager.persist(newPerson);
		} else {
			messengerManager.flush();
		}

		try {
			messengerManager.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace(); // throws CloneNotSupportedException, but still writes person into database
		} finally {
			messengerManager.getTransaction().begin();
		}
		return newPerson.getIdentity();
	}

	@GET
	@Path("{identity}/peopleObserving")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Collection<Person> getPeopleObserving(@HeaderParam("Authorization") final String authentication,
			@PathParam("identity") final long identity) {
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person person = messengerManager.find(Person.class, identity);
		if (person == null)
			throw new ClientErrorException(NOT_FOUND);

		SortedSet<Person> sortedPeopleObserving = new TreeSet<>(PersonService.PERSON_COMPARATOR);
		sortedPeopleObserving.addAll(person.getPeopleObserving());

		return sortedPeopleObserving;
	}

	@PUT
	@Path("{identity}/peopleObserved")
	@Consumes({ APPLICATION_FORM_URLENCODED })
	public void setPeopleObserved(@HeaderParam("Authorization") final String authentication,
			@PathParam("identity") final long identity, @FormParam("peopleObserved") final Set<Long> observedIDs) {
		final Person requester = Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		if (requester.getGroup() != Group.ADMIN) {
			throw new NotAuthorizedException("Basic");
		}
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person observer = messengerManager.find(Person.class, identity);

		if (observer == null) {
			throw new ClientErrorException(NOT_FOUND);
		}

		Set<Person> peopleObserved = observer.getPeopleObserved();
		Map<Long, Person> oldObserved = new HashMap<>();
		peopleObserved.forEach(p -> oldObserved.put(p.getIdentity(), p));
		Set<Long> joinedSet = new HashSet<>(observedIDs);
		joinedSet.addAll(oldObserved.keySet());
		Set<Long> idsToEvict = new HashSet<>();
		for (long id : joinedSet) {
			Person oldPerson = oldObserved.get(id);
			boolean inNewSet = observedIDs.contains(id);
			if (oldPerson != null && inNewSet) {
				// do nothing
			} else if (oldPerson != null) {
				// remove
				peopleObserved.remove(oldPerson);
				idsToEvict.add(id);
			} else {
				// add
				peopleObserved.add(messengerManager.find(Person.class, id));
				idsToEvict.add(id);
			}
		}

		try {
			messengerManager.getTransaction().commit();
		} finally {
			messengerManager.getTransaction().begin();
		}

		Cache cache = MESSENGER_FACTORY.getCache();
		for (long id : idsToEvict) {
			cache.evict(Person.class, id);
		}
	}

	@GET
	@Path("{identity}/peopleObserved")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Collection<Person> getPeopleObserved(@HeaderParam("Authorization") final String authentication,
			@PathParam("identity") final long identity) {
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person person = messengerManager.find(Person.class, identity);

		if (person == null)
			throw new ClientErrorException(NOT_FOUND);

		SortedSet<Person> sortedPeopleObserved = new TreeSet<>(PersonService.PERSON_COMPARATOR);
		sortedPeopleObserved.addAll(person.getPeopleObserved());

		return sortedPeopleObserved;
	}

	@GET
	@Path("{identity}/messagesAuthored")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Collection<Message> getMessagesAuthored(@HeaderParam("Authorization") final String authentication,
			@PathParam("identity") final long identity) {
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");
		final Person person = messengerManager.find(Person.class, identity);

		Set<Message> messages = person.getMessagesAuthored();

		return new TreeSet<>(messages);
	}

	@GET
	@Path("{identity}/avatar")
	@Produces(WILDCARD)
	public Response getAvatar(@HeaderParam("Authorization") final String authentication,
			@PathParam("identity") final long identity) {
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");
		final Person person = messengerManager.find(Person.class, identity);

		if (person == null) {
			throw new ClientErrorException(NOT_FOUND);
		}
		Document avatar = person.getAvatar();
		return Response.status(OK).type(avatar.getContentType()).entity(avatar.getContent()).build();
	}

	@PUT
	@Path("{identity}/avatar")
	@Consumes(WILDCARD)
	public void putAvatar(@HeaderParam("Authorization") final String authentication,
			@HeaderParam("Content-Type") final String contentType, final byte[] content,
			@PathParam("identity") final long identity) throws IOException {

		final Person requester = Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		if (requester.getGroup() != Group.ADMIN) {
			throw new NotAuthorizedException("Basic");
		}
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person person = messengerManager.find(Person.class, identity);
		if (person == null) {
			throw new ClientErrorException(NOT_FOUND);
		}

		Document newDocument = null;

		// leerer byte array
		if (content == null) {
			newDocument = messengerManager.find(Document.class, 1L);
		} else {
			byte[] newContentHash = Document.mediaHash(content);

			TypedQuery<Document> queryDocuments = messengerManager
					.createQuery("SELECT d FROM Document d WHERE d.contentHash = :contentHash", Document.class);
			queryDocuments.setParameter("contentHash", newContentHash);

			Document document = null;
			try {
				document = queryDocuments.getSingleResult();
			} catch (Exception e) {
				System.err.println("Document not found, creating new one...");
			}

			if (document != null) {
				newDocument = document;
			} else {
				// if send content hash not in database, create new document and save it in
				// database
				newDocument = new Document();
				newDocument.setContent(content);
				newDocument.setContentType(contentType);

				messengerManager.persist(newDocument);
				try {
					// have to commit otherwise updating the person fails because document id not
					// updated
					messengerManager.getTransaction().commit();
				} finally {
					messengerManager.getTransaction().begin();
				}
			}
		}

		// update persons avatar
		person.setAvatar(newDocument);
		try {
			messengerManager.getTransaction().commit();
		} finally {
			messengerManager.getTransaction().begin();
		}

	}

}
