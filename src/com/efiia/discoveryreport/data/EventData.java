/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

import java.util.HashMap;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author larry
 */
public class EventData {

	private final static boolean DEBUG = false;

	// ignore "type" it is always "event"
	int EventID;			// primary key
	String CreatedAt;
	String BoxEventID;
	String EventType;
	String IPAddress;
	// String SessionID;	-- null from Box

	// Standard Items
	String BoxItemType;
	String BoxItemID;
	String BoxItemVersionID;
	String BoxItemName;
	String BoxParentID;

	HashMap<String,Object> Source;
	HashMap<String,Object> Parent;
	UserData User;

	String Note;

	private boolean isDuplicate;

	private static final DataBase db;

	private final int BYDBEVENTID = 0;
	private final int BYBOXEVENTID = 1;

	static {

		db = new DataBase( "insert into Events (BoxEventID, Type, IPAddress, DateTime, BoxUserID, BoxItemType, BoxItemID, BoxItemVersionID, Note) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
							new String[] {
								"select EventID, BoxEventID, Type, IPAddress, DateTime, BoxUserID, BoxItemType, BoxItemID, BoxItemVersionID, Note from Events where EventID=?",
								"select EventID, BoxEventID, Type, IPAddress, DateTime, BoxUserID, BoxItemType, BoxItemID, BoxItemVersionID, Note from Events where BoxEventID=?"
							});

	}

	@SuppressWarnings("unchecked")
	public EventData( HashMap<String, Object> pJSONData ) {

		CreatedAt = (String)pJSONData.get( "created_at" );
		BoxEventID = (String)pJSONData.get( "event_id" );
		EventType = (String)pJSONData.get( "event_type" );
		IPAddress = (String)pJSONData.get(  "ip_address" );

		// now get the more compound data here so we don't have to do it over and over
		Source = (HashMap<String, Object>)pJSONData.get( "source" );
		if ( Source != null ) {
			BoxItemType = (String)Source.get( "item_type" );
			BoxItemID = (String)Source.get( "item_id" );
			BoxItemName = (String)Source.get( "item_name" );

			Parent = (HashMap<String,Object>)Source.get( "parent" );
			if ( Parent != null )				// just in case for some events w/o parent are added at a later date
				BoxParentID = (String)Parent.get( "id" );

		}
		// BoxItemVersionID is in "additional_details
		HashMap<String,Object> xAD = (HashMap<String,Object>)pJSONData.get( "additional_details" );
		BoxItemVersionID = ( xAD != null ? (String)xAD.get( "version_id" ) : "0" );
	}

	// for subevents like when we copy a folder
	public EventData( EventData pMaster, int pSubID, String pItemType, String pItemID, String pItemVersionID, String pItemName, String pParentID ) {

		// copy from master event
		CreatedAt = pMaster.CreatedAt;
		EventType = pMaster.EventType;
		IPAddress = pMaster.IPAddress;
		User = pMaster.User;

		// new pseudo-event
		BoxEventID = String.format( "%s:%d", pMaster.BoxEventID, pSubID );
		BoxItemType = pItemType;
		BoxItemID = pItemID;
		BoxItemVersionID = pItemVersionID;
		BoxItemName = pItemName;
		BoxParentID = pParentID;
	}

	public void setUser( UserData pUser ) { User = pUser; }
	public void setNote( String pNote ) { Note = pNote; }

	public UserData getUser() { return User; }
	public String getEventType() { return EventType; }
	public String getEventID() { return BoxEventID; }		// for debugging only
	public String getBoxItemType() { return BoxItemType; }

	public boolean isDuplicate() { return isDuplicate; }

	@Override
	public String toString() {
		return String.format( "[%s] %s by %s[%s] for %s[%s]", BoxEventID, EventType, User.getName(), User.getBoxUserID(), BoxItemID, BoxItemType );
	}

	public void save() throws SQLException {

		if ( DEBUG )
			System.out.print( "Saving Event: " + BoxEventID );

		isDuplicate = false;

		try {
			// BoxEventID, Type, IPAddress, DateTime, BoxUserID, BoxItemType, BoxItemID, BoxItemVersionID, Note
			PreparedStatement s = db.getInsertStatement();

			s.setString( 1, BoxEventID );
			s.setString( 2, EventType );
			s.setString( 3, IPAddress );
			s.setString( 4, CreatedAt );
			s.setString( 5, User.getBoxUserID() );
			s.setString( 6, BoxItemType );
			s.setString( 7, BoxItemID );
			s.setString( 8, BoxItemVersionID );
			if ( Note == null )
				s.setNull(  9, java.sql.Types.VARCHAR );
			else
				s.setString(  9, Note );

			s.execute();
			int result = s.getUpdateCount();

			ResultSet pk = s.getGeneratedKeys();
			if ( pk != null && pk.next() )
				EventID = pk.getInt( "EventID" );

			if ( DEBUG )
				System.out.println( " -- Saved; EventID: " + EventID );

		} catch ( SQLException sqx ) {

			if ( !db.isDuplicateKeyException( sqx )) {
				System.out.println( sqx.toString() );
				System.exit( 1);
				throw ( sqx );
			}

			isDuplicate = true;

			// do a select to get the PK
			PreparedStatement s = db.getSelectStatement( BYBOXEVENTID );
			s.setString( 1, this.BoxEventID );

			ResultSet rec = s.executeQuery();
			if ( rec.next() )
				EventID = rec.getInt( "EventID" );

		}


	}

}

