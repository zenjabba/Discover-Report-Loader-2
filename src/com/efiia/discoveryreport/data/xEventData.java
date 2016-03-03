/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author larry
 */
public class xEventData  {

	// ignore "type" it is always "event"
	int EventID;			// primary key
	String CreatedAt;
	String BoxEventID;
	String EventType;
	String IPAddress;
	// String SessionID;	-- null from Box

	UserData User;

	HashMap<String, zDataObject> GenericData = new HashMap<>();

	// not used
	// AdditionalDetails

	private static DataBase db;

	private final int BYDBEVENTID = 0;
	private final int BYBOXEVENTID = 1;

	static {

		db = new DataBase( "insert into Events (BoxEventID, Type, IPAddress, DateTime, ActorID) values (?, ?, ?, ?, ?)",
							new String[] {
								"select EventID, BoxEventID, Type, IPAddress, DateTime, ActorID, FileID from Events where EventID=?",
								"select EventID, BoxEventID, Type, IPAddress, DateTime, ActorID, FileID from Events where BoxEventID=?"
							});

	}

	public void set( String pField, String pValue ) {
		setString( pField, pValue );
	}

	public void set( String pField, int pValue ) {
		setString( pField, String.valueOf( pValue ));
	}

	public void set( String pField, UserData pValue ) {
		if ( pField.equals( "created_by" ) )
			User = pValue;
	}

	public void set( String pField, zDataObject pValue ) {
		GenericData.put( pField, pValue );
	}

	//@Override
	public void set( String pField, Object pValue ) {
		throw new UnsupportedOperationException( "Field: " + pField + " Not supported yet." );
	}


	protected void setString( String pField, String pValue ) {
		switch ( pField ) {
			case "created_at":
				CreatedAt = pValue;
				break;

			case "event_id":
				BoxEventID = pValue;
				break;

			case "event_type":
				EventType = pValue;
				break;

			case "ip_address":
				IPAddress = pValue;
				break;

			case "session_id":
				// ignore
				break;

			case "type":
				// ignore
				break;

			// just log useless data
			default:
				System.out.printf( "Unknown Event Attribute: %s => %s\n", pField, pValue );
				break;
		}
	}

	public UserData getUser() { return User; }
	public String getEventType() { return EventType; }
	public zDataObject getGenericData( String pLabel ) { return GenericData.get( pLabel ); }

	public void dump( int i, PrintStream p ) {

		p.printf("%5d: %s [%s] @%s/%s \n", i, EventType, BoxEventID, CreatedAt, IPAddress );
		//User.dump( p );

		for ( Map.Entry<String, zDataObject> o : GenericData.entrySet() ) {
			p.printf( "    %s => ", o.getKey() );
			o.getValue().dump( p );
		}

	}
	public void dump( PrintStream p ) {
		throw new UnsupportedOperationException( "Dump Not supported yet." );
	}


	public void save() throws SQLException {

		try {
			PreparedStatement s = db.getInsertStatement();

			s.setString(1, this.BoxEventID );
			s.setString( 2, this.EventType );
			s.setString( 3, this.IPAddress );
			s.setString( 4, this.CreatedAt );
			s.setInt( 5, this.User.UserID );

			s.execute();
			int result = s.getUpdateCount();
			//System.out.printf( "Update Result: %d\n", result );

			ResultSet pk = s.getGeneratedKeys();
			if ( pk != null && pk.next() )
				EventID = pk.getInt( "EventID" );

		} catch ( SQLException sqx ) {

			if ( db.isDuplicateKeyException( sqx )) {
				// do a select to get the PK
				PreparedStatement s = db.getSelectStatement( BYBOXEVENTID );
				s.setString( 1, this.BoxEventID );

				ResultSet rec = s.executeQuery();
				if ( rec.next() )
					EventID = rec.getInt( "EventID" );

			} else {
				throw ( sqx );
			}
		}


	}

}
