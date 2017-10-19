package messengermodel;

public class Name {

	private char[] given;
	private char[] family; 

	public Name(){
		
		given = new char[31];
		family = new char[31];
	}
	
	public char[] getGiven() {
		return given;
	}

	public void setGiven(char[] street) {
		this.given = street;
	}

	public char[] getFamily() {
		return family;
	}

	public void setFamily(char[] family) {
		this.family = family;
	}
	
}
