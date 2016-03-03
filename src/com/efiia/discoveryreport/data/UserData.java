/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

import java.io.PrintStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 *
 * @author larry
 */
public class UserData /* implements DataObject */ {

	int UserID;
	String Type;
	String BoxUserID;
	String Name;
	String Login;

	private boolean isDuplicate;

	private static final DataBase db;

	private final int BYDBUSERID = 0;
	private final int BYBOXUSERID = 1;

	static {

		db = new DataBase( "insert into Users (BoxUserID, Name, Login) values (?, ?, ?)",
						   new String[] {
							   "select UserID, BoxUserID, Name, Login from Users where UserID=?",
							   "select UserID, BoxUserID, Name, Login from Users where BoxUserID=?"
						   });

	}

	public UserData( HashMap<String,Object> pJSONData ) {
		Name = (String)pJSONData.get( "name" );
		Login = (String)pJSONData.get( "login" );
		BoxUserID = (String)pJSONData.get(  "id" );
	}

	@Override
	public String toString() {
		return String.format( "BoxUserID: %s; Name: %s; Login: %s", BoxUserID, Name, Login );
	}

	public int getUserID() { return UserID; }
	public String getType() { return Type; }
	public String getBoxUserID() { return BoxUserID; }
	public String getName() { return Name; }
	public String getLogin() { return Login; }

	public boolean isDuplicate() { return isDuplicate; }

	public void save() throws SQLException {

		isDuplicate = false;

		try {
			PreparedStatement s = db.getInsertStatement();

			s.setString( 1, this.BoxUserID );
			s.setString( 2, this.Name );
			s.setString( 3, this.Login );

			s.execute();
			int result = s.getUpdateCount();
			//System.out.printf( "Update Result: %d\n", result );

			ResultSet pk = s.getGeneratedKeys();
			if ( pk != null && pk.next() )
				UserID = pk.getInt( "UserID" );

		} catch ( SQLException sqx ) {

			if ( !db.isDuplicateKeyException( sqx )) {
				throw ( sqx );
			}

			isDuplicate = true;

			// do a select to get the PK
			PreparedStatement s = db.getSelectStatement( BYBOXUSERID );
			s.setString( 1, this.BoxUserID );

			ResultSet rec = s.executeQuery();
			if ( rec.next() )
				UserID = rec.getInt( "UserID" );

		}
	}

}
