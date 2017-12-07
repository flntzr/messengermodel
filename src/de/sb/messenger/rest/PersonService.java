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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

// immer nur id zurückgeben lassen aus db und dann über find die entities laden

@Path("people")
public class PersonService {

	private static final EntityManagerFactory messengerFactory = Persistence.createEntityManagerFactory("messenger");

	@GET
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public List<Person> queryPersons(@HeaderParam("Authorization") final String authentication,
		@QueryParam("givenName") final String givenName, @QueryParam("familyName") final String familyName,
		@QueryParam("mail") final String mail, @QueryParam("street") final String street,
		@QueryParam("postcode") final String postcode, @QueryParam("city") final String city, @QueryParam("group") final Group group) {
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManagerLife = RestJpaLifecycleProvider.entityManager("messenger");

		// TODO: query result offset(setfirstreslut) und länge(setmaxresult), timestamp bound
		// query als string static
		// order by rausschmeißen

		TypedQuery<Person> query = messengerManagerLife.createQuery("SELECT p.identity FROM Person p WHERE "
				+ "(:givenName is null OR p.name.given = :givenName) AND "
				+ "(:familyName is null or p.name.family = :familyName) AND " 
				+ "(:mail IS null OR p.email = :mail) AND "
				+ "(:street IS null OR p.address.street = :street) AND "
				+ "(:postcode IS null OR p.address.postcode = :postcode) AND"
				+ "(:city IS null OR p.address.city = :city) AND"
				+ "(:group IS null OR p.group = :group)",
				Person.class);
		List<Person> results = query
				.setParameter("mail", mail)
				.setParameter("givenName", givenName)
				.setParameter("familyName", familyName)
				.setParameter("street", street)
				.setParameter("postcode", postcode)
				.setParameter("city", city)
				.setParameter("group", group)
				.getResultList();
		if (results.isEmpty()) {
			throw new ClientErrorException(NOT_FOUND);
		}

		//results.sort(Comparator.comparing(Person::getName::getFirst).thenComparing(Person::getName::getGiven).thenComparing(Person::getMail));
		// für jede id die Person laden über messengerManager.find()
		return results;
	}
	
	@GET
	@Path("{identity}")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Person queryPerson(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity) {

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
	public Person getRequester(@HeaderParam("Authorization") final String authentication){
		return Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
	}

	//TODO: set passwort
	@PUT
	@Consumes( {APPLICATION_JSON, APPLICATION_XML} )
	@Produces(TEXT_PLAIN)
	public long updatePerson(@HeaderParam("Authorization") final String authentication, @NotNull @Valid final Person person) {
		final Person requester = Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));

		if(requester.getIdentity() != person.getIdentity() && requester.getGroup() != Group.ADMIN){
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
	public Collection<Person> getPeopleObserving(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person person = messengerManager.find(Person.class, identity);
		if(person == null)
			throw new ClientErrorException(NOT_FOUND);

		/*TypedQuery<Person> queryPeopleObeserving = messengerManager.createQuery("SELECT p FROM Person p WHERE "
						+ "(p.identity in :peopleObserving)"
						+ "ORDER BY p.name.family, p.name.given, p.email",
				Person.class);

		Set<Long> observingIDs = new HashSet<>();*/

		Set<Person> peopleObserving = person.getPeopleObserving();

		SortedSet<Person> sortedPeopleObserving = new TreeSet<>(peopleObserving);

		return sortedPeopleObserving;

	}
	
	@PUT
	@Path("{identity}/peopleObserved")
	@Consumes({APPLICATION_FORM_URLENCODED})
	public void setPeopleObserved(
			@HeaderParam("Authorization") final String authentication, 
			@PathParam("identity") final long identity, 
			@FormParam("peopleObserved") final Set<Long> observedIDs
			) {


		// TODO: abfrage über entity
		// TODO: drei teilmengen: welche hinzu, welche weg, welche sich ändern
		// TODO: peopleObserved ändern (addAll removeAll)
		// TODO: commit
		// TODO: second level cache (evict)
		final Person requester = Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		if(requester.getGroup() != Group.ADMIN){
			throw new NotAuthorizedException("Basic");
		}
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person observer = messengerManager.find(Person.class, identity);
		if(observedIDs.isEmpty()){
			throw new ClientErrorException(BAD_REQUEST);
		}
		TypedQuery<Person> findObservedQuery = messengerManager.createQuery("SELECT p FROM Person p where (p.identity in :peopleObserved)", Person.class);
		findObservedQuery.setParameter("peopleObserved", observedIDs);
		List<Person> observed = findObservedQuery.getResultList();
		if (observed.size() != observedIDs.size()) {
			throw new ClientErrorException(NOT_FOUND);
		}
		if (observer == null) {
			throw new ClientErrorException(NOT_FOUND);
		}
		EntityTransaction tx = messengerManager.getTransaction();

		// delete old references
		Query deleteOldQuery = messengerManager.createNativeQuery("DELETE FROM ObservationAssociation WHERE observingReference = ?1");
		deleteOldQuery.setParameter(1, identity);
		deleteOldQuery.executeUpdate();

		// add new references
		for (long observedID: observedIDs) {
			Query insertQuery = messengerManager.createNativeQuery("INSERT INTO ObservationAssociation (observingReference, observedReference) "
					+ "VALUES (?1, ?2)");
			insertQuery.setParameter(1, identity);
			insertQuery.setParameter(2, observedID);
			insertQuery.executeUpdate();

		}

		// refresh 2nd level cache for observer and observed
		try{
			tx.commit();
		} finally {
			tx.rollback();
			tx.begin();
		}
		/*messengerManager.refresh(observer);
		tx.begin();
		observed.forEach((o) -> messengerManager.refresh(o));
		tx.commit();*/
		Cache cache = messengerFactory.getCache();
		cache.evict(Person.class, observer.getIdentity());
		observed.forEach((o) -> cache.evict(Person.class, o.getIdentity()));

	}

	@GET
	@Path("{identity}/peopleObserved")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Collection<Person> getPeopleObserved(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person person = messengerManager.find(Person.class, identity);

		if(person == null)
			throw new ClientErrorException(NOT_FOUND);

		/*TypedQuery<Person> queryPeopleObeserved = messengerManager.createQuery("SELECT p FROM Person p WHERE "
						+ "(p.identity in :peopleObserved)"
						+  "ORDER BY p.name.family, p.name.given, p.email",
				Person.class);

		Set<Long> observedIDs = new HashSet<>();

		person.getPeopleObserved().forEach(currentPerson -> observedIDs.add(currentPerson.getIdentity()));

		List<Person> sortedPeopleObserved = queryPeopleObeserved.setParameter("peopleObserved", observedIDs).getResultList();*/

		Set<Person> peopleObs = person.getPeopleObserved();

		SortedSet<Person> sortedPeoplsObs = new TreeSet<>(peopleObs);

		return sortedPeoplsObs;
	}

	@GET
	@Path("{identity}/messagesAuthored")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Collection<Message> getMessagesAuthored(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
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
	public Response getAvatar(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");
		final Person person = messengerManager.find(Person.class, identity);

		if(person == null){
			throw new ClientErrorException(NOT_FOUND);
		}
		Document avatar = person.getAvatar();
		return Response.status(OK).type(avatar.getContentType()).entity(avatar.getContent()).build();
	}


	@PUT
	@Path("{identity}/avatar")
	@Consumes(WILDCARD)
	public void putAvatar(@HeaderParam("Authorization") final String authentication,
						  @HeaderParam("Content-Type") final String contentType,
						  final byte[] content,
						  @PathParam("identity") final long identity) throws IOException {

		final Person requester = Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		if(requester.getGroup() != Group.ADMIN){
			throw new NotAuthorizedException("Basic");
		}
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person person = messengerManager.find(Person.class, identity);
		if(person == null){
			throw new ClientErrorException(NOT_FOUND);
		}

		Document newDocument = null;

		// leerer byte array
		if(content == null){
			newDocument = messengerManager.find(Document.class, 1L);
		} else {
			byte[] newContentHash = Document.mediaHash(content);

			TypedQuery<Document> queryDocuments = messengerManager.createQuery("SELECT d FROM Document d WHERE d.contentHash = :contentHash", Document.class);
			queryDocuments.setParameter("contentHash", newContentHash);

			Document document = null;
			try {
				document = queryDocuments.getSingleResult();
			} catch (Exception e){
				System.err.println("Document not found, creating new one...");
			}

			if(document != null){
				newDocument = document;
			} else {
				// if send content hash not in database, create new document and save it in database
				newDocument = new Document();
				newDocument.setContent(content);
				newDocument.setContentType(contentType);

				messengerManager.persist(newDocument);
				try {
					// have to commit otherwise updating the person fails because document id not updated
					messengerManager.getTransaction().commit();
				} finally {
					messengerManager.getTransaction().begin();
				}
			}
		}

		// update persons avatar
		person.setAvatar(newDocument);
		try{
			messengerManager.getTransaction().commit();
		} finally {
			messengerManager.getTransaction().begin();
		}

	}



}
