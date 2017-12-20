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
import javax.ws.rs.core.Response.Status;

import java.io.IOException;
import java.util.*;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

@Path("people")
public class PersonService {

	private static final String SELECT_PERSONS_QUERY = "SELECT p.identity FROM Person p WHERE "
			+ "(:givenName is null OR p.name.given = :givenName) AND "
			+ "(:familyName is null or p.name.family = :familyName) AND " + "(:mail IS null OR p.email = :mail) AND "
			+ "(:street IS null OR p.address.street = :street) AND "
			+ "(:postcode IS null OR p.address.postcode = :postcode) AND"
			+ "(:city IS null OR p.address.city = :city) AND" + "(:group IS null OR p.group = :group) AND "
			+ "(:creationTimestampLower = 0 OR p.creationTimestamp >= :creationTimestampLower) AND"
			+ "(:creationTimestampUpper = 0 OR p.creationTimestamp <= :creationTimestampUpper)";

	@GET
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Collection<Person> queryPeople(@HeaderParam("Authorization") final String authentication,
			@QueryParam("givenName") final String givenName, @QueryParam("familyName") final String familyName,
			@QueryParam("mail") final String mail, @QueryParam("street") final String street,
			@QueryParam("postcode") final String postcode, @QueryParam("city") final String city,
			@QueryParam("group") final Group group, @QueryParam("resultOffset") final int resultOffset,
			@QueryParam("maxResultLength") int maxResultLength,
			@QueryParam("creationTimestampLower") final long creationTimestampLower,
			@QueryParam("creationTimestampUpper") final long creationTimestampUpper) {
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManagerLife = RestJpaLifecycleProvider.entityManager("messenger");

		TypedQuery<Long> query = messengerManagerLife.createQuery(PersonService.SELECT_PERSONS_QUERY, Long.class);
		query.setParameter("mail", mail).setParameter("givenName", givenName).setParameter("familyName", familyName)
				.setParameter("street", street).setParameter("postcode", postcode).setParameter("city", city)
				.setParameter("group", group).setParameter("creationTimestampLower", creationTimestampLower)
				.setParameter("creationTimestampUpper", creationTimestampUpper);
		if (resultOffset > 0)
			query.setFirstResult(resultOffset);
		if (maxResultLength > 0)
			query.setMaxResults(maxResultLength);
		List<Long> results = query.getResultList();
		SortedSet<Person> sortedPersons = new TreeSet<>(Comparator.comparing((Person p) -> p.getName().getFamily())
				.thenComparing((Person p) -> p.getName().getGiven()).thenComparing(Person::getMail));

		for (long id : results) {
			Person person = messengerManagerLife.find(Person.class, id);
			if (person != null)
				sortedPersons.add(person);
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
			@HeaderParam("password") final String password, @Valid @NotNull final Person personTemplate) {

		final Person requester = Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));

		// gives back 403, if requester is not ADMIN and does not alter himself or if he
		// wants to change his group to ADMIN
		if (requester.getGroup() != Group.ADMIN) {
			if (requester.getIdentity() != personTemplate.getIdentity() || personTemplate.getGroup() == Group.ADMIN) {
				throw new ClientErrorException(403);
			}
		}

		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person person;
		final boolean insertMode = personTemplate.getIdentity() == 0;

		if (insertMode) {
			Document avatar = messengerManager.find(Document.class, 1L);
			person = new Person(avatar);
		} else {
			person = messengerManager.find(Person.class, personTemplate.getIdentity());
		}

		if (password != null) {
			person.setPasswordHash(Person.passwordHash(password));
		}

		person.setMail(personTemplate.getMail());
		person.setGroup(personTemplate.getGroup());
		person.getAddress().setCity(personTemplate.getAddress().getCity());
		person.getAddress().setPostcode(personTemplate.getAddress().getPostcode());
		person.getAddress().setStreet(personTemplate.getAddress().getStreet());
		person.getName().setFamily(personTemplate.getName().getFamily());
		person.getName().setGiven(personTemplate.getName().getGiven());

		if (insertMode) {
			messengerManager.persist(person);
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
		return person.getIdentity();
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

		SortedSet<Person> sortedPeopleObserving = new TreeSet<>(
				Comparator.comparing((Person p) -> p.getName().getFamily())
						.thenComparing((Person p) -> p.getName().getGiven()).thenComparing(Person::getMail));
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
		Map<Long, Person> peopleObservedMap = new HashMap<>();
		peopleObserved.forEach(p -> peopleObservedMap.put(p.getIdentity(), p));
		observedIDs.forEach(id -> peopleObservedMap.put(id, null));

		Set<Long> idsToEvict = new HashSet<>();
		for (Map.Entry<Long, Person> entry : peopleObservedMap.entrySet()) {
			boolean inNewSet = observedIDs.contains(entry.getKey());
			if (entry.getValue() != null && inNewSet) {
				// do nothing
			} else if (entry.getValue() != null) {
				// remove
				peopleObserved.remove(entry.getValue());
				idsToEvict.add(entry.getKey());
			} else {
				// add
				Person person = messengerManager.find(Person.class, entry.getKey());
				if (person != null) {
					peopleObserved.add(person);
					idsToEvict.add(entry.getKey());
				}
			}
		}

		try {
			messengerManager.getTransaction().commit();
		} finally {
			messengerManager.getTransaction().begin();
		}

		Cache cache = messengerManager.getEntityManagerFactory().getCache();
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

		SortedSet<Person> sortedPeopleObserved = new TreeSet<>(
				Comparator.comparing((Person p) -> p.getName().getFamily())
						.thenComparing((Person p) -> p.getName().getGiven()).thenComparing(Person::getMail));
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

		if (person == null)
			throw new ClientErrorException(NOT_FOUND);

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
			@HeaderParam("Content-Type") final String contentType, @NotNull final byte[] content,
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

		final Document avatar;

		// leerer byte array
		if (content.length == 0) {
			avatar = messengerManager.find(Document.class, 1L);
		} else {
			byte[] newContentHash = Document.mediaHash(content);

			TypedQuery<Long> query = messengerManager
					.createQuery("SELECT d.identity FROM Document d WHERE d.contentHash = :contentHash", Long.class);
			query.setParameter("contentHash", newContentHash);

			List<Long> ids = query.getResultList();
			if (ids.isEmpty()) {
				// if send content hash not in database, create new document and save it in
				// database
				avatar = new Document();
				avatar.setContent(content);
				avatar.setContentType(contentType);

				messengerManager.persist(avatar);
				try {
					// have to commit otherwise updating the person fails because document id not
					// updated
					messengerManager.getTransaction().commit();
				} finally {
					messengerManager.getTransaction().begin();
				}
			} else {
				avatar = messengerManager.find(Document.class, ids.get(0));
				if (avatar == null)
					throw new ClientErrorException(Status.CONFLICT);
			}

		}

		// update persons avatar
		person.setAvatar(avatar);
		try {
			messengerManager.getTransaction().commit();
		} finally {
			messengerManager.getTransaction().begin();
		}

	}

}
