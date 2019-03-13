package edu.upenn.cis.cis455.model;

import java.io.Serializable;

public class User implements Serializable {
    private String userName;
    private String password;
    private String firstName;
    private String lastName;
    
    
    public User(String userName, String password, String firstName, String lastName) {
        this.userName = userName;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public String getPassword() {
        return password;
    }

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}
}
