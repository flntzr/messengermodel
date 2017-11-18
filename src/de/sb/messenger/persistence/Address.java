package de.sb.messenger.persistence;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;

@Embeddable
public class Address {
	@Size(min = 0, max = 63)
	@Column(name = "street")
	private String street;
	@Size(min = 0, max = 15)
	@Column(name = "postcode")
	private String postcode;
	@NotNull
	@Size(min = 1, max = 63)
	@Column(name = "city")
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
		if(obj.getClass() == Address.class){

			Address other = (Address) obj;
			return this.getStreet().equalsIgnoreCase(other.getStreet())
					&& this.getPostcode().equalsIgnoreCase(other.getPostcode())
					&& this.getCity().equalsIgnoreCase(other.getCity());

		} else {
			return false;
		}
	}
}
