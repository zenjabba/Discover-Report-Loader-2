/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.boxapi;

import com.efiia.discoveryreport.DRException;
import com.efiia.discoveryreport.DiscoveryReportDBLoader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Stack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import static com.efiia.discoveryreport.DiscoveryReportDBLoader.DEBUG;
import static com.efiia.discoveryreport.data.UserData.SERVICEPREFIX;

/**
 *
 * @author larry
 */
public abstract class APIProcessor {

	private final static boolean DEBUG = DiscoveryReportDBLoader.DEBUG;

	private final static int TIMEOUTSECS = 180;

	// request
	private final HashMap<String, String> Headers;
	private final HashMap<String, String> Query;
	private final HashMap<String, String> PostData;
	private final String EndPoint;
	protected final BoxConnection Connector;

	// response
	protected HashMap<String,Object> MetaData;
	protected HashMap<String,String> ErrorData;

	private String asUserID;

	protected APIProcessor( String pEndPoint, BoxConnection pConnector ) {
		EndPoint = pEndPoint;
		Connector = pConnector;
		Headers = new HashMap<>();
		Query = new HashMap<>();
		PostData = new HashMap<>();
		asUserID = null;

		// Connector.addHeaders( Headers );
		//Connector.addQuery( Query );
	}

	public void addHeader( String pHeader, String pValue ) {
		Headers.put( pHeader, pValue);
	}

	public String addQuery( String pField, String pValue ) {
		return Query.put( pField, pValue );			// by design, replaces old value
	}

	public void addPostData( String pField, String pValue ) {
		PostData.put( pField, pValue );
	}

	public void setAsUser( String pUID ) {

		// if the User is a Service, don't setUser
		// check added April 2016
		if ( pUID == null || pUID.startsWith( SERVICEPREFIX ) ) {		// rely on Java short-circut evaluation for null
			asUserID = null;
			return;
		}

		asUserID = pUID;
	}

	public HashMap<String,Object> getMetaData() {
		return ( MetaData );
	}

	public HashMap<String,String> getErrorData() {
		return ( ErrorData );
	}

	// intended for overrides...
	public String formatURL( String pEndPoint ) { return pEndPoint; }
	public String getAuthHeader() throws DRException { return Connector.getAuthHeader(); }
	protected boolean postProcess() throws DRException { return true; }

	public boolean FetchData() throws DRException {
		boolean retval = makeCall();
		if ( retval )
			retval = postProcess();
		return ( retval );
	}

	protected final boolean makeCall() throws DRException {

		HttpURLConnection xConnect = null;
		boolean retval = false;

		try {

			String xEP = formatURL( EndPoint );

			URL xURL = Connector.getURL( xEP, Query );

			StringBuilder pdBuilder = new StringBuilder();
			byte[] PostDataBytes = { 0 };
			for ( Map.Entry<String, String> pdItem : this.PostData.entrySet() ) {
				pdBuilder.append( "&" );
				pdBuilder.append( URLEncoder.encode( pdItem.getKey(), "UTF-8"));
				pdBuilder.append(  "=" );
				pdBuilder.append( URLEncoder.encode(  pdItem.getValue(), "UTF-8"));
			}
			if ( pdBuilder.length() > 0 ) {
				PostDataBytes = pdBuilder.substring( 1 ).getBytes();
			}

			xConnect = (HttpURLConnection) xURL.openConnection();

			xConnect.setRequestProperty( "Accept", "application/json" );

			// added 22-March-2016
			xConnect.setConnectTimeout( TIMEOUTSECS * 1000 );
			xConnect.setReadTimeout( TIMEOUTSECS *100 );

			String AuthHeader = getAuthHeader();
			if ( AuthHeader != null )
				xConnect.setRequestProperty( "Authorization", AuthHeader );

			for( Map.Entry<String, String> xHeader : Headers.entrySet() )
				xConnect.setRequestProperty( xHeader.getKey(), xHeader.getValue() );

			if ( asUserID != null )
				xConnect.setRequestProperty( "As-User", asUserID );

			if ( DEBUG ) {
				System.out.println( "Calling: " + xURL.toString() );
				String oAuth = xConnect.getRequestProperty( "Authorization" );
				if ( AuthHeader != null )
					System.out.println( "Authorization: " + AuthHeader );
				if ( asUserID != null )
					System.out.println( "As-User: " + asUserID );
			}

			if ( PostData.isEmpty() ) {
				// its a GET
				xConnect.setRequestMethod( "GET" );
				xConnect.setDoOutput( false );

			} else {
				xConnect.setRequestMethod( "POST" );
				xConnect.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
				xConnect.setRequestProperty( "Content-Length", String.valueOf( PostDataBytes.length ));
				xConnect.setDoOutput( true );
				xConnect.getOutputStream().write( PostDataBytes );
			}

			// make the call
			xConnect.connect();
			Connector.incCallCtr();

			int status = xConnect.getResponseCode();
			if ( HttpURLConnection.HTTP_OK <= status && status <= HttpURLConnection.HTTP_PARTIAL ) {

				// process the response
				try ( BufferedReader br = new BufferedReader( new InputStreamReader( xConnect.getInputStream() )) ) {
					ProcessResponse( br );
					retval = true;
// Redundant
//				} catch ( DRException dex ) {
//					throw ( dex );

				} catch ( IOException iox ) {
					Logger.getLogger(APIProcessor.class.getName() ).log( Level.SEVERE, String.format( "IOException Calling URL:%s", xEP ), iox );
					throw new DRException( 1114, "APIProcessor: ProcessResponse", iox );

				} catch ( Exception ex ) {
					Logger.getLogger(APIProcessor.class.getName() ).log( Level.SEVERE, null, ex );
					throw new DRException( 1111, "APIProcessor: ProcessResponse", ex );
				}

			//} else if ( status == xConnect.HTTP_UNAUTHORIZED ) {
			//	throw new IOException( "Unauthorized to Call: " + xURL.toString() +  " ; "+ xConnect.getResponseMessage() + " [" + xConnect.getResponseCode() + "]" );

			} else if ( status >= HttpURLConnection.HTTP_INTERNAL_ERROR) {
				if ( DEBUG ) {
					System.out.println( "Error calling URL: " + xConnect.getURL().toString() );
					System.out.println( xConnect.getHeaderFields().toString() );
				}
				ErrorData.put( "status", String.valueOf( status ));
				ErrorData.put( "url", xConnect.getURL().toString() );
				throw new DRException( 1112, "APIProcessor:ProcessResponse", String.format( "Server Returned: %d", status), String.format( "Calling URL: %s", xConnect.getURL().toString() ));

			} else {

				// process error response
				try ( BufferedReader br = new BufferedReader( new InputStreamReader( xConnect.getErrorStream() )) ) {
					// print info about the request
					if ( DEBUG ) {
						System.out.println( "Error calling URL: " + xConnect.getURL().toString() );
						System.out.println( xConnect.getHeaderFields().toString() );
					}
					ProcessErrorResponse( xURL, br );
					ErrorData.put( "url", xConnect.getURL().toString() );
					/* this bit added March 2016
					 * fails and causes system stop for some reason
					 * and does not report full error data
					 */
					/*
					throw new DRException( 1113, "APIProcessor:ProcessResponse", String.format(  "Server Returned: %d", status ), ErrorData.get( "error_description" ) );
				} catch ( DRException dex ) {
					throw ( dex );
					 */
					/* end of bit added March 2016 */

				} catch ( Exception ex ) {
					Logger.getLogger(APIProcessor.class.getName() ).log( Level.SEVERE, null, ex );
				}

			}
// Redundant
//		} catch ( DRException dx ) {
//			throw ( dx );

		} catch ( MalformedURLException x1 ) {
			System.out.println( x1.toString() );

		} catch ( ProtocolException ex ) {
			Logger.getLogger(APIProcessor.class.getName() ).log( Level.SEVERE, null, ex );

		} catch ( IOException ex ) {
			Logger.getLogger(APIProcessor.class.getName() ).log( Level.SEVERE, null, ex );

		} finally {

			if ( xConnect != null )
				xConnect.disconnect();

		}
		return ( retval );
	}

	/**
	 * This is the entry point to processing a Box API Call (or any JSON call)
	 * @param pResponse
	 * @throws IOException
	 */
	public void ProcessResponse( BufferedReader pResponse ) throws IOException, DRException {

		boolean LOCALDEBUG = false;			// DEBUG
		boolean flag = false;
		if ( flag ) {
		dumpResponse( pResponse, false );
		return;
		}

		// process the event stream data into the database
		JsonParser jp = Json.createParser( pResponse );
		//MetaData = new HashMap<>();

		Stack<String> KeyStack = new Stack<>();

		Stack<HashMap<String,Object>> focusObject = new Stack<>();
		focusObject.push( MetaData );

		// move past the initial object start
		jp.next();

		MetaData = processObject( jp );

		if ( LOCALDEBUG )
			System.out.println( MetaData );
	}

	protected HashMap<String,Object> processObject( JsonParser jp ) throws IOException, DRException {

		final boolean LOCALDEBUG = false;			// DEBUG

		if ( LOCALDEBUG )
			System.out.printf( "processObject()" );

		String curKey = null;
		HashMap<String, Object> curObject = new HashMap<>();

		// read the event stream for entries
done:	while( jp.hasNext() ) {

			// process each entry as its read
			Event e = jp.next();

			switch ( e ) {
				case START_OBJECT:
					// create new object to get values
					if ( LOCALDEBUG )
						System.out.println( "START_OBJECT: " + curKey );
					HashMap<String,Object> newMap = processObject( jp );
					curObject.put( curKey, newMap );
					break;

				case END_OBJECT:
					// need these tests to cover the fact that we skiped the initial object
					if ( LOCALDEBUG )
						System.out.println( "END_OBJECT: " + curKey );
					break done;

				case KEY_NAME:
					curKey = jp.getString();
					break;

				case VALUE_STRING:
					String curValue = jp.getString();
					//MetaData.put( curKey, curValue );
					curObject.put( curKey, curValue );
					break;

				case VALUE_NUMBER:
					long lngValue = jp.getLong();
					if ( curKey.equals( "version_id" ))
						curObject.put( curKey, String.valueOf( lngValue ));
					else
						curObject.put( curKey, lngValue );
					break;

				case START_ARRAY:
					if ( LOCALDEBUG )
						System.out.println( "START ARRAY: " + curKey );
					ArrayList<HashMap<String,Object>> xArray = processArray( curKey, jp );
					if ( xArray != null )
						curObject.put( curKey, xArray );
					break;

				default:

			}

		}

		return ( curObject );
	}

		// overridable...
	protected HashMap<String, Object> processArrayItem( String pKey, HashMap<String, Object> pItem ) throws DRException {
		final boolean LOCALDEBUG = false;		// DEBUG
		if ( LOCALDEBUG )
			System.out.println( "default processArrayItem( " + pKey + ")" );
		return pItem;
	}

	// array doesn't need access to the upper elements
	protected ArrayList<HashMap<String,Object>> processArray( String pKey, JsonParser pJP ) throws DRException, IOException {

		final boolean LOCALDEBUG = false;		// DEBUG

		if ( LOCALDEBUG )
			System.out.printf( "processArray( \"%s\")\n", pKey );

		String curKey = null;
		ArrayList<HashMap<String,Object>> targetArray = new ArrayList<>();

		HashMap<String, Object> curItem = null;

fin:	while( pJP.hasNext() ) {

			// process each entry as its read
			Event e = pJP.next();

			switch ( e ) {
				case START_OBJECT:
					if ( LOCALDEBUG )
						System.out.println( "Start Array Object" );
					curItem = processObject( pJP );
					curItem = processArrayItem( pKey, curItem );
					if (curItem != null )
						targetArray.add( curItem );
					break;

				case END_OBJECT:
					if ( LOCALDEBUG )
						System.out.println( "END OBJECT: " + pKey );
					break;

				case END_ARRAY:
					break fin;

				case KEY_NAME:
					curKey = pJP.getString();
					break;

				case VALUE_STRING:
					String curValue = pJP.getString();
					curItem.put( curKey, curValue );
					break;

				case VALUE_NUMBER:
					long longValue = pJP.getLong();
					curItem.put( curKey, longValue );
					break;

				case VALUE_NULL:
					curItem.put( curKey, null );
					break;

			}
		}

		return targetArray;
	}

	void ProcessErrorResponse( URL pURL, BufferedReader pResponse ) throws IOException {

		final boolean LOCALDEBUG = false;

		if ( DEBUG || LOCALDEBUG ) {
			System.err.println( "!!! ProcessErrorResponse" );
			System.err.println( "!!! " + pURL.toString() );
			pResponse.mark( 100000 );
			String output;
			while (( output = pResponse.readLine())  != null ) {
				System.err.println( output );
			}
			//dumpResponse( pResponse, false );
			pResponse.reset();
			//return;
		}

		// process the event stream data into the database

		JsonParser jp = Json.createParser( pResponse );
		ErrorData = new HashMap<>();
		String curKey = null;

		// read the event stream for entries
		while( jp.hasNext() ) {

			// process each entry as its read
			Event e = jp.next();

			switch ( e ) {
				case START_OBJECT:
					break;

				case END_OBJECT:
					break;

				case KEY_NAME:
					curKey = jp.getString();
					break;

				case VALUE_STRING:
					String curValue = jp.getString();
					ErrorData.put( curKey, curValue );
					break;

				case VALUE_NUMBER:
					long longValue = jp.getLong();
					ErrorData.put( curKey, String.valueOf( longValue ));
					break;

				case START_ARRAY:
					break;

			}

		}
	}

	void dumpResponse( BufferedReader pResponse, boolean pShowTags ) throws IOException {

		// process the event stream data into the database

		String output;
		pResponse.mark( 500000 );
		System.out.println( "Server Response");
		while (( output = pResponse.readLine())  != null ) {
			System.out.println( output );
		}
		pResponse.reset();
		// read the event stream metadata

		JsonParser jp = Json.createParser( pResponse );
		//String curKey = null;

		StringBuilder indent = new StringBuilder();
		Event p = Event.END_OBJECT;

		// read the event stream for entries
		while( jp.hasNext() ) {
			// process each entry as its read
			Event e = jp.next();
			if ( ( e == Event.START_OBJECT || e == Event.START_ARRAY ) && p == Event.KEY_NAME )
				System.out.println();
			if ( e == Event.END_OBJECT || e == Event.END_ARRAY )
				indent.deleteCharAt( 0 );
			System.out.print( indent );
			if ( pShowTags ) {
				System.out.print( '[' );
				System.out.print( e.toString() );
				System.out.print( ']' );
			}
			switch ( e ) {
				case KEY_NAME:
					System.out.print( indent );
					System.out.print( jp.getString() );
					System.out.print( "=>");
					break;
				case VALUE_STRING:
					System.out.println( jp.getString() );
					break;
				case VALUE_NUMBER:
					System.out.println(  jp.getInt() );
					break;
				case VALUE_FALSE:
					System.out.println( "FALSE" );
					break;
				case VALUE_TRUE:
					System.out.println(  "TRUE" );
					break;
				case VALUE_NULL:
					System.out.println(  "NULL" );
					break;
				case START_OBJECT:
					if ( !pShowTags )
						System.out.println( "{" );
					else
						System.out.println();
					indent.append( " " );
					break;
				case START_ARRAY:
					indent.append( " " );
					if ( !pShowTags )
						System.out.print( "[" );
					else
						System.out.println();
					break;
				case END_ARRAY:
					if ( !pShowTags )
						System.out.print( "]" );
					else
						System.out.println();
					break;
				case END_OBJECT:
					if ( !pShowTags )
						System.out.print( "}" );
					System.out.println();
					break;
				default:
					System.out.println( "UNKNOWN JSON EVENT TYPE");
					break;
			}
			p = e;
		}
	}

//	protected HashMap<String, Object> processArray( JsonParser pJP ) {
//
//		HashMap<String, Object> allEntry = new HashMap<>();
//		HashMap<String, Object> curEntry = null;
//		Event e;
//		String curKey = null;
//
//		while ( ( e = pJP.next()) != JsonParser.Event.END_ARRAY ) {
//			// process entries
//			switch( e ) {
//				case START_OBJECT:
//					curEntry = new HashMap<>();
//					break;
//				case END_OBJECT:
//					// process current entry
//					System.out.println( "- Entry -");
//					for ( Map.Entry<String, Object> x : curEntry.entrySet() ) {
//						System.out.print( x.getKey() );
//						System.out.print(  '=' );
//						Object xv = x.getValue();
//						if ( xv instanceof String ) {
//							System.out.println( x.getValue() );
//						} else if ( xv instanceof HashMap ) {
//							System.out.println( "Map");
//						} else {
//							System.out.println( xv.toString() );
//						}
//					}
//					allEntry.put(  curKey, curEntry );
//					curEntry = null;
//					break;
//
//				case KEY_NAME:
//					curKey = pJP.getString();
//					break;
//
//				case VALUE_STRING:
//					String curVal = pJP.getString();
//					curEntry.put( curKey, curVal );
//					break;
//
//				case START_ARRAY:
//					HashMap<String, Object> details = processArray( pJP );
//					curEntry.put( curKey, details );
//					break;
//
//				default:
//					System.out.println( "= ??? =" );
//					System.out.print( curKey );
//					System.out.print(  ' ' );
//					System.out.println( e.toString() );
//					break;
//			}
//		}
//
//		return ( allEntry );
//	}
}
