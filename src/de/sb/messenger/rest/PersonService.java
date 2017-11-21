package de.sb.messenger.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
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
		// TODO authenticate
		// TODO create entity manager only once, not with each HTTP request
		try {
			EntityManager messengerManager = Persistence.createEntityManagerFactory("messenger").createEntityManager();
			// TypedQuery<Person> query = messengerManager.createQuery("select p from Person
			// as p", Person.class);
			TypedQuery<Person> query = messengerManager.createQuery("SELECT p FROM Person p WHERE "
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
			return results;
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

}
