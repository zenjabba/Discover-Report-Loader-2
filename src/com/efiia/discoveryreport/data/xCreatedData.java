/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

/**
 *
 * @author larry
 */
public class xCreatedData {

	String Type;
	String ID;
	String Name;
	String Login;

	public void set( String pField, String pValue ) {
		if ( pField.equals( "type" )) {
			Type = pValue;
		} else if ( pField.equals( "id" )) {
			ID = pValue;
		} else if ( pField.equals(  "name" )) {
			Name = pValue;
		} else if ( pField.equals(  "login" )) {
			Login = pValue;
		}
	}
	
	public String getType() {
		return Type;
	}

	public String getID() {
		return ID;
	}

	public String getName() {
		return Name;
	}

	public String getLogin() {
		return Login;
	}

}
