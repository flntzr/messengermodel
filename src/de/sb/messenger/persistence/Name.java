package de.sb.messenger.persistence;


import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@Embeddable
public class Name {


	@NotNull
	@Size(min = 1, max = 31)
	@Column(name = "givenName", nullable = false, updatable = true)
	private String given;

	@NotNull
	@Size(min = 1, max = 31)
	@Column(name = "familyName", nullable = false, updatable = true)
	private String family;

	public Name() {
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

	@Override
	public boolean equals(Object obj) {
		if (obj.getClass() != Name.class) {
			return false;
		}
		Name other = (Name) obj;

		boolean isGivenEqual = false;
		boolean isFamilyEqual = false;
		
		if (this.getGiven() == null && other.getGiven() == null) {
			isGivenEqual = true;
		} else if (this.getGiven() == null || other.getGiven() == null) {
			return false;
		}
		if (this.getFamily() == null && other.getFamily() == null) {
			isFamilyEqual = true;
		} else if (this.getFamily() == null || other.getFamily() == null) {
			return false;
		}
		
		if (!isGivenEqual) {
			isGivenEqual = this.getGiven().equalsIgnoreCase(other.getGiven());
		}
		
		if (!isFamilyEqual) {
			isFamilyEqual = this.getFamily().equalsIgnoreCase(other.getFamily());
		}
		
		return isGivenEqual && isFamilyEqual;
	}

}
