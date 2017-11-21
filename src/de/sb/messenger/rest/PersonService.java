package de.sb.messenger.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import de.sb.messenger.persistence.Group;
import de.sb.messenger.persistence.Person;

@Path("people")
public class PersonService {

	@GET
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public List<Person> queryPersons(@HeaderParam("Authorization") final String authentication,
		@QueryParam("givenName") final String givenName, @QueryParam("familyName") final String familyName,
		@QueryParam("mail") final String mail, @QueryParam("street") final String street,
		@QueryParam("postcode") final String postcode, @QueryParam("city") final String city, @QueryParam("group") final Group group) {
		final EntityManager messengerManager = Persistence.createEntityManagerFactory("messenger").createEntityManager();
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
		System.err.println("identity: " +identity);
		final EntityManager messengerManager = Persistence.createEntityManagerFactory("messenger").createEntityManager();
		Person person = messengerManager.find(Person.class, identity);
		if (person == null) {
			throw new ClientErrorException(NOT_FOUND);
		}
		return person;
	}

}
