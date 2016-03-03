/* patterned after BOX Java API
*/
package com.efiia.discoveryreport.boxapi;

import com.efiia.discoveryreport.DRException;
import com.efiia.discoveryreport.data.UserData;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author larry
 */
public class BoxConnection {

	private final static boolean DEBUG = false;

	private final static long EXPIRYSHOULDER = 1000*60*5;		// in milliseconds - 5 minutes before expiry to enable auto renew

	/* Error Tracking */
	private final static int PROPFILECANTREAD = 2001;
	private final static int PROPFILECANTWRITE = 2002;
	private final static int PROPFILEREADERROR = 2003;
	private final static int PROPFILEWRITEERROR = 2004;
	private final static int PROPFILEMISSINGCLIENTID = 2005;
	private final static int PROPFILEMISSINGCLIENTSECRET = 2006;
	private final static int PROPFILEMISSINGREFRESHTOKEN = 2007;
	private final static int INVALIDREFRESHTOKEN = 2008;

	private final static String BOXAPIURL = "https://api.box.com/2.0/";

	private final String BoxClientID;
	private final String BoxClientSecret;

	private String BoxAccessToken;
	private String BoxRefreshToken;
	private long BoxAccessExpiry;

	private UserData curUser;

	static private File confFile;
	static private Properties propsAPI;

	enum BoxAccessMode { DEVELOPER, USER, ADMIN };
	private final BoxAccessMode BoxMode;

	private int APICallCtr = 0;

	public synchronized void incCallCtr() { APICallCtr++; }
	public int getCallCtr() { return APICallCtr; }

	/**
	 * Use when the Access Token, etc are in a Java Property file
	 * @param pConfigDir
	 * @return
	 * @throws com.efiia.discoveryreport.DRException
	 */
	public static BoxConnection Config( File pConfigDir ) throws DRException {

		confFile = new File( pConfigDir, "loader.boxapi.config" );
		if ( !confFile.canRead() )
			throw new DRException( PROPFILECANTREAD, "BoxConnection:new", String.format( "Config File %s Not Found/Readable", confFile.toString()) );
		if ( !confFile.canWrite() )
			throw new DRException( PROPFILECANTWRITE, "BoxConnection:new", String.format( "Config File %s Not Found/Readable", confFile.toString()) );

		// connect to box
		propsAPI = new Properties();
		try (FileInputStream in = new FileInputStream(confFile )) {
			propsAPI.load( in );
		} catch ( IOException ex ) {
			throw new DRException( PROPFILEREADERROR, "BoxConnection:new", "Read Property File", ex );
		}

		String ClientID = propsAPI.getProperty( "ClientID" );
		String ClientSecret = propsAPI.getProperty( "ClientSecret" );
		String AccessToken = propsAPI.getProperty( "AccessToken" );
		String RefreshToken = propsAPI.getProperty( "RefreshToken" );

		if ( ClientID == null )
			throw new DRException( PROPFILEMISSINGCLIENTID, "BoxConnection:new", String.format( "Config File %s Missing Box API ClientID", confFile.toString() ));
		if ( ClientSecret == null )
			throw new DRException( PROPFILEMISSINGCLIENTSECRET, "BoxConnection:new", String.format( "Config File %s Missing Box API Client Secret", confFile.toString() ));
		if ( RefreshToken == null )
			throw new DRException( PROPFILEMISSINGREFRESHTOKEN, "BoxConnection:new", String.format( "Config File %s Missing Box API Refresh Token", confFile.toString() ));

		BoxConnection bc = new BoxConnection( ClientID, ClientSecret, AccessToken, RefreshToken );

		bc.RefreshToken();

		return ( bc );

	}

	/**
	 * Use for developer mode access to the API
	 * @param pDevToken
	 */
	public BoxConnection( String pDevToken ) {
		this( BoxAccessMode.DEVELOPER, null, null, pDevToken, null );
	}

	/**
	 * Use when an Access Token was provided when called from Box
	 * @param pClientID
	 * @param pClientSecret
	 * @param pAccessToken
	 */
	public BoxConnection( String pClientID, String pClientSecret, String pAccessToken ) {
		this( BoxAccessMode.USER, pClientID, pClientSecret, pAccessToken, null );
	}

	/**
	 * Use when creating an OAuth Connection that needs to be refreshed
	 * @param pClientID
	 * @param pClientSecret
	 * @param pAccessToken
	 * @param pRefreshToken
	 */
	public BoxConnection( String pClientID, String pClientSecret, String pAccessToken, String pRefreshToken ) {
		this( BoxAccessMode.ADMIN, pClientID, pClientSecret, pAccessToken, pRefreshToken );
	}

	private BoxConnection( BoxAccessMode pMode, String pClientID, String pClientSecret, String pAccessToken, String pRefreshToken ) {
		BoxMode = pMode;
		BoxClientID = pClientID;
		BoxClientSecret = pClientSecret;
		BoxAccessToken = pAccessToken;
		BoxRefreshToken = pRefreshToken;
		curUser = null;
	}

	public URL getURL( String pEndPoint, HashMap<String, String> pQuery ) throws MalformedURLException {
		StringBuilder bld = new StringBuilder();
		for( Map.Entry<String, String> x : pQuery.entrySet() ) {
			bld.append(  '&' );
			bld.append( x.getKey() );
			bld.append( "=");
			bld.append( x.getValue() );
		}
		if ( bld.length() > 0 )
			bld.setCharAt( 0, '?' );

		return new URL(( pEndPoint.startsWith( "http" ) ? "" : BOXAPIURL ) + pEndPoint + bld.toString() );
	}

	/*
	public HashMap<String, String> addHeaders( HashMap<String, String> pHeaders ) throws DRException {
		if ( BoxMode == BoxAccessMode.DEVELOPER || BoxMode == BoxAccessMode.ADMIN )
			pHeaders.put( "Authorization", "Bearer " + BoxAccessToken );
		return ( pHeaders );
	}
	*/

	public String getAuthHeader() throws DRException {
		if ( BoxMode == BoxAccessMode.DEVELOPER )
			return "Bearer " + BoxAccessToken;
		long now = System.currentTimeMillis();
		if ( now > BoxAccessExpiry )
			RefreshToken();
		if ( BoxMode == BoxAccessMode.ADMIN )
			return "Bearer " + BoxAccessToken;
		return ( null );
	}

	public synchronized UserData getCurrentUser() throws DRException {

		if ( curUser == null ) {
			CurrentUserProcessor up = new CurrentUserProcessor( this );
			up.FetchData();
			curUser = up.getUser();
		}

		return ( curUser );
	}

	/**
	 * If the Event Actor is the Admin, no As-User header required nor allowed
	 * @param pBoxUserID
	 * @return
	 */
	public String getAsUser( String pBoxUserID ) {
		// kludge for admin as user errors
		// if ( true && pBoxUserID.equals( "227572005" ))
		// 	return ( "234652714" );
		String retUser = ( pBoxUserID.equals( curUser.getBoxUserID() ) ? null : pBoxUserID );
		if ( DEBUG ) {
			if ( retUser != null )
				System.out.printf( "Connected As: %s, As-User: %s%n", curUser.getBoxUserID(), pBoxUserID );
		}
		return ( retUser );
	}

	public void RefreshToken() throws DRException {

		// now use the Refresh Token to get a New Access Token, and write that info back to the property file for next time
		RefreshTokenProcessor rtp = new RefreshTokenProcessor( this );
		rtp.setInfo( BoxRefreshToken, BoxClientID, BoxClientSecret );
		if ( !rtp.FetchData() ) {
			// error refreshing token - most likely out of sync
			if ( DEBUG ) {
				System.out.println( rtp.getErrorData().toString() );
				throw new DRException( INVALIDREFRESHTOKEN, "BoxConnection:RefreshToken", rtp.ErrorData.get("error_description") );
			}
		}

		// update current connection information
		BoxAccessToken = rtp.AccessToken;
		BoxRefreshToken = rtp.RefreshToken;
		BoxAccessExpiry = rtp.Expiry - EXPIRYSHOULDER;

		// for testing safety
		if ( false && DEBUG ) {
			System.out.println(  "Access Token: " + BoxAccessToken );
			System.out.println( "Refresh Token: " + BoxRefreshToken );
		}

		// re-write the property file
		propsAPI.setProperty( "AccessToken", BoxAccessToken );
		propsAPI.setProperty( "RefreshToken", BoxRefreshToken );

		try (FileOutputStream fos = new FileOutputStream( confFile )) {
			propsAPI.store( fos, "Updated from Box API" );
		} catch ( IOException ex ) {
			throw new DRException( PROPFILEWRITEERROR, "BoxConnection:RefreshToken", ex);
		}

		// finally update the log
		System.out.printf( "%s updated with new credentials at %s\n", confFile.getName(), new Date().toString() );

	}
}
