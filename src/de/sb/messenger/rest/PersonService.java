package de.sb.messenger.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import de.sb.messenger.persistence.Document;
import de.sb.messenger.persistence.Group;
import de.sb.messenger.persistence.Message;
import de.sb.messenger.persistence.Person;
import de.sb.toolbox.net.RestCredentials;

@Path("people")
public class PersonService {
	
	private static final EntityManager messengerManager = Persistence.createEntityManagerFactory("messenger").createEntityManager();

	@GET
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public List<Person> queryPersons(@HeaderParam("Authorization") final String authentication,
		@QueryParam("givenName") final String givenName, @QueryParam("familyName") final String familyName,
		@QueryParam("mail") final String mail, @QueryParam("street") final String street,
		@QueryParam("postcode") final String postcode, @QueryParam("city") final String city, @QueryParam("group") final Group group) {
		TypedQuery<Person> query = messengerManager.createQuery("SELECT p FROM Person p WHERE "
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
		final EntityManager messengerManager = Persistence.createEntityManagerFactory("messenger").createEntityManager();
		Person person = messengerManager.find(Person.class, identity);
		if (person == null) {
			throw new ClientErrorException(NOT_FOUND);
		}
		return person;
	}
	
	@PUT
	@Consumes( {APPLICATION_JSON, APPLICATION_XML} )
	public long createPerson(@NotNull final Person person) {
		Person newPerson;
		final boolean insert = person.getIdentity() == 0;
		messengerManager.getTransaction().begin();
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
		
		if (insert) {
			messengerManager.persist(newPerson);
		} else {
			messengerManager.flush();				
		}
		messengerManager.getTransaction().commit();
		return newPerson.getIdentity();
	}

	@GET
	@Path("{identity}/peopleObserving")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public List<Person> getPeopleObserving(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));

		TypedQuery<Person> queryPeopleObeserving = messengerManager.createQuery("SELECT p.peopleObserving FROM Person p WHERE "
						+ "p.identity = :identity",
				Person.class);

		List<Person> peopleObserving= queryPeopleObeserving.setParameter("identity", identity).getResultList();

		if(peopleObserving.isEmpty())
			throw new ClientErrorException(NOT_FOUND);

		List<Person> sortedPeopleObserving = getSortedPersonListByIds(peopleObserving);

		if (sortedPeopleObserving == null) {
			throw new ClientErrorException(NOT_FOUND);
		}


		return sortedPeopleObserving;

	}

	@GET
	@Path("{identity}/peopleObserved")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public List<Person> getPeopleObserved(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));

		TypedQuery<Person> queryPeopleObeserved = messengerManager.createQuery("SELECT p.peopleObserved FROM Person p WHERE "
						+ "p.identity = :identity",
				Person.class);

		List<Person> peopleObserved = queryPeopleObeserved.setParameter("identity", identity).getResultList();

		if(peopleObserved.isEmpty())
			throw new ClientErrorException(NOT_FOUND);

		List<Person> sortedPeopleObserved = getSortedPersonListByIds(peopleObserved);

		if (sortedPeopleObserved == null) {
			throw new ClientErrorException(NOT_FOUND);
		}


		return sortedPeopleObserved;
	}

	@GET
	@Path("{identity}/messagesAuthored")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public List<Message> getMessagesAuthored(@HeaderParam("Authorization") final String authentication, @PathParam("identity") final long identity){
		Authenticator.authenticate(RestCredentials.newBasicInstance(authentication));

		TypedQuery<Message> queryMessagesAuthored = messengerManager.createQuery("SELECT m FROM Message m WHERE "
						+ "m.author.identity = :identity "
						+ "ORDER BY m.identity",
				Message.class);

		List<Message> messages = queryMessagesAuthored.setParameter("identity", identity).getResultList();

		if(messages.isEmpty())
			throw new ClientErrorException(NOT_FOUND);

		return messages;
	}

	// HELPER

	private List<Person> getSortedPersonListByIds(List<Person> peopleIds){

		List<Long> identities = new ArrayList<>();
		peopleIds.forEach(person -> identities.add(person.getIdentity()));

		TypedQuery<Person> queryPeopleOrdered = messengerManager.createQuery(createDynamicPersonQueryById(identities),
				Person.class);

		for (int i = 0, identitiesSize = identities.size(); i < identitiesSize; i++) {
			queryPeopleOrdered.setParameter("identity"+i, identities.get(i));
		}

		return queryPeopleOrdered.getResultList();

	}

	private String createDynamicPersonQueryById(List<Long> identities){
		StringBuilder queryString = new StringBuilder("SELECT p FROM Person p WHERE ");

		for (int i = 0, identitiesSize = identities.size(); i < identitiesSize; i++) {
			if(i == identitiesSize - 1){
				queryString.append(" (p.identity = :identity").append(i).append(") ").append("ORDER BY p.name.family, p.name.given, p.email");
			} else {
				queryString.append(" (p.identity = :identity").append(i).append(") OR ");
			}
		}

		return queryString.toString();
	}


}
