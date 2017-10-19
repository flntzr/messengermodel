package messengermodel;

public class Address {

	private char[] street;
	private char[] postcode;
	private char[] city;


	public Address() {
		
		setStreet(new char[64]);
		setPostcode(new char[16]);
		setCity(new char[63]);
	}


	public char[] getStreet() {
		return street;
	}


	public void setStreet(char[] street) {
		this.street = street;
	}


	public char[] getPostcode() {
		return postcode;
	}


	public void setPostcode(char[] postcode) {
		this.postcode = postcode;
	}


	public char[] getCity() {
		return city;
	}


	public void setCity(char[] city) {
		this.city = city;
	}
	
}
