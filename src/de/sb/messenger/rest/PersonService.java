package de.sb.messenger.rest;

import de.sb.messenger.persistence.Document;
import de.sb.messenger.persistence.Group;
import de.sb.messenger.persistence.Message;
import de.sb.messenger.persistence.Person;
import de.sb.toolbox.net.RestCredentials;
import de.sb.toolbox.net.RestJpaLifecycleProvider;

import javax.ejb.RemoveException;
import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

// immer nur id zurückgeben lassen aus db und dann über find die entities laden

@Path("people")
public class PersonService {

	private static final EntityManagerFactory MESSENGER_FACTORY = Persistence.createEntityManagerFactory("messenger");
	private static final String SELECT_PERSONS_QUERY = "SELECT p.identity FROM Person p WHERE "
			+ "(:givenName is null OR p.name.given = :givenName) AND "
			+ "(:familyName is null or p.name.family = :familyName) AND " + "(:mail IS null OR p.email = :mail) AND "
			+ "(:street IS null OR p.address.street = :street) AND "
			+ "(:postcode IS null OR p.address.postcode = :postcode) AND"
			+ "(:city IS null OR p.address.city = :city) AND" + "(:group IS null OR p.group = :group)";
	private static final Comparator<Person> PERSON_COMPARATOR = Comparator.comparing((Person p) -> p.getName().getFamily())
			.thenComparing((Person p) -> p.getName().getGiven()).thenComparing(Person::getMail);

	@GET
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Collection<Person> queryPersons(@HeaderParam("Authorization") final String authentication,
			@QueryParam("givenName") final String givenName, @QueryParam("familyName") final String familyName,
			@QueryParam("mail") final String mail, @QueryParam("street") final String street,
			@QueryParam("postcode") final String postcode, @QueryParam("city") final String city,
			@QueryParam("group") final Group group) {
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManagerLife = RestJpaLifecycleProvider.entityManager("messenger");

		// TODO: query result offset(setfirstreslut) und länge(setmaxresult), timestamp
		// bound

		TypedQuery<Long> query = messengerManagerLife.createQuery(PersonService.SELECT_PERSONS_QUERY, Long.class);
		List<Long> results = query.setParameter("mail", mail).setParameter("givenName", givenName)
				.setParameter("familyName", familyName).setParameter("street", street)
				.setParameter("postcode", postcode).setParameter("city", city).setParameter("group", group)
				.getResultList();
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

	// TODO: set passwort
	@PUT
	@Consumes({ APPLICATION_JSON, APPLICATION_XML })
	@Produces(TEXT_PLAIN)
	public long updatePerson(@HeaderParam("Authorization") final String authentication,
			@NotNull @Valid final Person person) {
		final Person requester = Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));

		if (requester.getIdentity() != person.getIdentity() && requester.getGroup() != Group.ADMIN) {
			throw new NotAuthorizedException("Basic");
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
		newPerson.setMail(person.getMail());
		newPerson.setGroup(person.getGroup());
		newPerson.getAddress().setCity(person.getAddress().getCity());
		newPerson.getAddress().setPostcode(person.getAddress().getPostcode());
		newPerson.getAddress().setStreet(person.getAddress().getStreet());
		newPerson.getName().setFamily(person.getName().getFamily());
		newPerson.getName().setGiven(person.getName().getGiven());
		// passwort feld

		if (insertMode) {
			messengerManager.persist(newPerson);
		} else {
			messengerManager.flush();
		}

		try {
			messengerManager.getTransaction().commit();
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
		for (long id : joinedSet) {
			Person oldPerson = oldObserved.get(id);
			boolean inNewSet = observedIDs.contains(id);
			if (oldPerson != null && inNewSet) {
				// do nothing
			} else if (oldPerson != null) {
				// remove
				peopleObserved.remove(oldPerson);
			} else {
				// add
				peopleObserved.add(messengerManager.find(Person.class, id));
			}
		}

		try {
			messengerManager.getTransaction().commit();
		} finally {
			messengerManager.getTransaction().begin();
		}

		Cache cache = MESSENGER_FACTORY.getCache();
		for (long id : joinedSet) {
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

		SortedSet<Message> sortedMessages = new TreeSet<>(messages);

		return sortedMessages;
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

	// byte array statt inputstream
	@PUT
	@Path("{identity}/avatar")
	@Consumes(WILDCARD)
	public void putAvatar(@HeaderParam("Authorization") final String authentication,
			@HeaderParam("Content-Type") final String contentType, final InputStream content,
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

		TypedQuery<Document> queryDocuments = messengerManager.createQuery("SELECT d FROM Document d", Document.class);

		List<Document> documents = queryDocuments.getResultList();

		Document newDocument = null;

		// leerer inputstream
		if (content == null) {
			newDocument = messengerManager.find(Document.class, 1L);
		} else {
			int nRead = 0;
			ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();
			byte[] contentBytes = new byte[1000];
			while ((nRead = content.read(contentBytes, 0, contentBytes.length)) != -1) {
				contentBuffer.write(contentBytes, 0, nRead);
			}
			byte[] newContentHash = Document.mediaHash(contentBuffer.toByteArray());
			boolean docSet = false;

			// check if document with same content hash exists in database
			// if then take that and set is as the persons avatar (newDocument[0])
			// TODO: query nach hash
			for (Document current : documents) {
				if (Arrays.equals(newContentHash, current.getContentHash())) {
					newDocument = current;
					docSet = true;
				}
			}

			// if send content hash not in database, create new document and save it in
			// database
			if (!docSet) {
				newDocument = new Document();
				newDocument.setContent(contentBuffer.toByteArray());
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
