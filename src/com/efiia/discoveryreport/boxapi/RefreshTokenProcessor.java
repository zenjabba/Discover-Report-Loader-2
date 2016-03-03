/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.boxapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 *
 * @author larry
 */
public class RefreshTokenProcessor extends APIProcessor {

	public String AccessToken;
	public String RefreshToken;
	public long Expiry;

	/**
	 *
	 * @param pConnector
	 */
	public RefreshTokenProcessor( BoxConnection pConnector ) {
		// use a complete URL as this API call has its own URL starting point
		super( "https://app.box.com/api/oauth2/token", pConnector );
		addPostData( "grant_type", "refresh_token" );
	}

	public void setInfo( String pRefreshToken, String pClientID, String pClientSecret ) {
		addPostData( "refresh_token", pRefreshToken );
		addPostData( "client_id", pClientID );
		addPostData( "client_secret" , pClientSecret );
	}

	/**
	 * Override the usual authorization header as using it will cause an error here
	 * @return
	 */
	@Override
	public String getAuthHeader() { return null; }

	@Override
	public boolean postProcess() {
		// move the metadata to the user data
		AccessToken = (String)MetaData.get( "access_token" );
		RefreshToken = (String)MetaData.get( "refresh_token" );
		long exp = (long)MetaData.get( "expires_in");
		Expiry = System.currentTimeMillis() + (exp*1000);
		return ( true );
	}
}
