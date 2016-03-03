/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

import com.efiia.discoveryreport.data.UserData;

/**
 *
 * @author larry
 */
public class xLoginData {

	UserData Source;
	UserData Created;

	String CreatedAt;
	String EventID;
	String EventType;
	String IPAddress;

	String SessionID;

	public void set( String pField, Object pValue ) {
		if ( pValue instanceof String ) {
			setString( pField, (String) pValue );
		} else if ( pValue instanceof UserData ) {
			setIdentity(pField, (UserData) pValue );
		} else {
			// log unused value
		}
	}

	private void setIdentity( String pField, UserData pValue ) {
		switch ( pField ) {
			case "source":
				Source = pValue;
				break;
			case "created":
				Created = pValue;
				break;
		// log unused value
			default:
				break;
		}
	}

	public void setString( String pField, String pValue ) {
		switch ( pField ) {
			case "created_at":
				CreatedAt = pValue;
				break;
			case "event_id":
				EventID = pValue;
				break;
			case "event_type":
				EventType = pValue;
				break;
			case "ip_address":
				IPAddress = pValue;
				break;
			case "session_id":
				SessionID = pValue;
				break;
		// just log useless data
			default:
				break;
		}
	}

}
