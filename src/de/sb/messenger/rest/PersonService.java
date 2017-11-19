package de.sb.messenger.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import de.sb.messenger.persistence.Person;
import de.sb.toolbox.net.RestJpaLifecycleProvider;

@Path("people")
public class PersonService {

	@GET
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public List<Person> queryPersons(
			@HeaderParam("Authorization") final String authentication,
			@QueryParam("givenName") final String name,
			@QueryParam("familyName") final String firstName,
			@QueryParam("mail") final String mail,
			@QueryParam("street") final String street,
			@QueryParam("postcode") final String postcode,
			@QueryParam("city") final String city) {
		// TODO authenticate
		// TODO create entity manager only once, not with each HTTP request
		EntityManager messengerManager = Persistence.createEntityManagerFactory("messenger").createEntityManager();
		TypedQuery<Person> query = messengerManager.createQuery("select p from Person as p", Person.class);
//		TypedQuery<Person> query = messengerManager.createQuery("select p from Person as p where "
//				+ "(:givenName is null or p.given = :givenName) and"
//				+ "(:familyName is null or p.family = :familyName) and"
//				+ "(:mail is null or p.mail = :mail)", Person.class);
		List<Person> results = query.getResultList();
		Person person = new Person(null);
		if (results.isEmpty()) {
			throw new ClientErrorException(NOT_FOUND);
		}
		return results;
	}

}
