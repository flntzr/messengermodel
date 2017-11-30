package de.sb.messenger.rest;

import de.sb.messenger.persistence.Document;
import de.sb.messenger.persistence.Group;
import de.sb.messenger.persistence.Message;
import de.sb.messenger.persistence.Person;
import de.sb.toolbox.net.RestCredentials;
import de.sb.toolbox.net.RestJpaLifecycleProvider;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

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

		TypedQuery<Person> query = messengerManagerLife.createQuery("SELECT p FROM Person p WHERE "
				+ "(:givenName is null OR p.name.given = :givenName) AND "
				+ "(:familyName is null or p.name.family = :familyName) AND " 
				+ "(:mail IS null OR p.email = :mail) AND "
				+ "(:street IS null OR p.address.street = :street) AND "
				+ "(:postcode IS null OR p.address.postcode = :postcode) AND"
				+ "(:city IS null OR p.address.city = :city) AND"
				+ "(:group IS null OR p.group = :group)"
				+ " ORDER BY p.name.family, p.name.given, p.email",
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
		return results;
	}
	
	@GET
	@Path("{identity}")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public Person queryPerson(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity) {

		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person person = messengerManager.find(Person.class, identity);
		if (person == null) {
			throw new ClientErrorException(NOT_FOUND);
		}
		return person;
	}
	
	@PUT
	@Consumes( {APPLICATION_JSON, APPLICATION_XML} )
	public long createPerson(@HeaderParam("Authorization") final String authentication, @NotNull final Person person) {
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person newPerson;
		final boolean insert = person.getIdentity() == 0;

		if (insert) {
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

		try {
			if (insert) {
				messengerManager.persist(newPerson);
			} else {
				messengerManager.flush();
			}
			messengerManager.getTransaction().commit();
		} catch (Exception e){
			System.err.println(e.toString());
		} finally {
			messengerManager.getTransaction().rollback();
			messengerManager.getTransaction().begin();
		}
		return newPerson.getIdentity();
	}

	@GET
	@Path("{identity}/peopleObserving")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public List<Person> getPeopleObserving(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person person = messengerManager.find(Person.class, identity);
		if(person == null || person.getPeopleObserving().isEmpty())
			throw new ClientErrorException(NOT_FOUND);

		TypedQuery<Person> queryPeopleObeserving = messengerManager.createQuery("SELECT p FROM Person p WHERE "
						+ "(p.identity in :peopleObserving)"
						+ "ORDER BY p.name.family, p.name.given, p.email",
				Person.class);

		Set<Long> observingIDs = new HashSet<>();

		person.getPeopleObserving().forEach(currentPerson -> observingIDs.add(currentPerson.getIdentity()));

		List<Person> sortedPeopleObserving = queryPeopleObeserving.setParameter("peopleObserving", observingIDs).getResultList();

		if (sortedPeopleObserving == null) {
			throw new ClientErrorException(NOT_FOUND);
		}
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

		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
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
		} catch (Exception e){
			System.err.println(e);
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
	public List<Person> getPeopleObserved(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person person = messengerManager.find(Person.class, identity);

		if(person == null || person.getPeopleObserved().isEmpty())
			throw new ClientErrorException(NOT_FOUND);

		TypedQuery<Person> queryPeopleObeserved = messengerManager.createQuery("SELECT p FROM Person p WHERE "
						+ "(p.identity in :peopleObserved)"
						+  "ORDER BY p.name.family, p.name.given, p.email",
				Person.class);

		Set<Long> observedIDs = new HashSet<>();

		person.getPeopleObserved().forEach(currentPerson -> observedIDs.add(currentPerson.getIdentity()));

		List<Person> sortedPeopleObserved = queryPeopleObeserved.setParameter("peopleObserved", observedIDs).getResultList();

		if (sortedPeopleObserved.size() == 0 || sortedPeopleObserved == null) {
			throw new ClientErrorException(NOT_FOUND);
		}

		return sortedPeopleObserved;
	}

	@GET
	@Path("{identity}/messagesAuthored")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public List<Message> getMessagesAuthored(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		TypedQuery<Message> queryMessagesAuthored = messengerManager.createQuery("SELECT m FROM Message m WHERE "
						+ "m.author.identity = :identity "
						+ "ORDER BY m.identity",
				Message.class);

		List<Message> messages = queryMessagesAuthored.setParameter("identity", identity).getResultList();

		if(messages.isEmpty())
			throw new ClientErrorException(NOT_FOUND);

		return messages;
	}


	@GET
	@Path("{identity}/avatar")
	@Produces(WILDCARD)
	public Response getAvatar(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");
		Person person = messengerManager.find(Person.class, identity);

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
						  final InputStream content,
						  @PathParam("identity") final long identity) throws IOException {
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("messenger");

		Person person = messengerManager.find(Person.class, identity);

		TypedQuery<Document> queryDocuments = messengerManager.createQuery("SELECT d FROM Document d", Document.class);

		List<Document> documents = queryDocuments.getResultList();

		if(person == null){
			throw new ClientErrorException(NOT_FOUND);
		}

		Document newDocument = null;

		if(content == null){
			newDocument = messengerManager.find(Document.class, 1L);
		} else {
			int nRead = 0;
			ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();
			byte[] contentBytes = new byte[1000];
			while((nRead = content.read(contentBytes, 0, contentBytes.length)) != -1){
				contentBuffer.write(contentBytes, 0, nRead);
			}
			byte[] newContentHash = Document.mediaHash(contentBuffer.toByteArray());
			boolean docSet = false;
			// check if document with same content hash exists in database
			// if then take that and set is as the persons avatar (newDocument[0])

			for (Document current : documents) {
				if(Arrays.equals(newContentHash, current.getContentHash())){
					newDocument = current;
					docSet = true;
				}
			}

			// if send content hash not in database, create new document and save it in database
			if(!docSet){
				newDocument = new Document();
				newDocument.setContent(contentBuffer.toByteArray());
				newDocument.setContentType(contentType);
				try{
					messengerManager.persist(newDocument);
					// have to commit otherwise updating the person fails because document id not updated
					messengerManager.getTransaction().commit();
				} catch (Exception e){
					e.printStackTrace();
				} finally {
					messengerManager.getTransaction().begin();
				}
			}
		}

		// update persons avatar
		person.setAvatar(newDocument);
		try{
			messengerManager.merge(person);
			messengerManager.getTransaction().commit();

		} catch (Exception e){
			e.printStackTrace();
		} finally {
			messengerManager.getTransaction().begin();
		}

	}



}
