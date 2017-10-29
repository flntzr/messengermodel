package de.sb.messenger.persistence;


import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Name {

	@Column(name = "givenName")
	private String given;
	@Column(name = "familyName")
	private String family; 

	public Name(){
	}

	public String getGiven() {
		return given;
	}

	public void setGiven(String given) {
		this.given = given;
	}

	public String getFamily() {
		return family;
	}

	public void setFamily(String family) {
		this.family = family;
	}
	
}
