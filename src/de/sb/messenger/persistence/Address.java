package de.sb.messenger.persistence;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Embeddable
public class Address {

	@Size(max = 63)
	@Column(nullable = false)
	private String street;
	@Size(max = 15)
	@Column(nullable = false)
	private String postcode;
	@NotNull
	@Size(min = 1, max = 63)
	@Column(nullable = false)
	private String city;

	public Address() {
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getPostcode() {
		return postcode;
	}

	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	@Override
	public boolean equals(Object obj) {
		boolean isStreetEqual = false;
		boolean isCityEqual = false;
		boolean isPostcodeEqual = false;

		if (obj.getClass() != Address.class) {
			return false;
		}
		Address other = (Address) obj;

		if (this.getStreet() == null && other.getStreet() == null) {
			isStreetEqual = true;
		} else if (this.getStreet() == null || other.getStreet() == null) {
			return false;
		}
		if (this.getCity() == null && other.getCity() == null) {
			isCityEqual = true;
		} else if (this.getCity() == null || other.getCity() == null) {
			return false;
		}
		if (this.getPostcode() == null && other.getPostcode() == null) {
			isPostcodeEqual = true;
		} else if (this.getPostcode() == null || other.getPostcode() == null) {
			return false;
		}

		if (!isStreetEqual) {
			isStreetEqual = this.getStreet().equalsIgnoreCase(other.getStreet());
		}
		if (!isCityEqual) {
			isCityEqual = this.getCity().equalsIgnoreCase(other.getCity());
		}
		if (!isPostcodeEqual) {
			isPostcodeEqual = this.getPostcode().equalsIgnoreCase(other.getPostcode());
		}

		return isStreetEqual && isCityEqual && isPostcodeEqual;
	}
}
