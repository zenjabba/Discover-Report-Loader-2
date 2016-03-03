/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

import com.efiia.discoveryreport.DRException;
import java.io.PrintStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author larry
 */
public class PropData {

	int PropID;
	String Key;
	String Value;

	private static final DataBase db;
	private static final int NEW = -2;

	static {
		db = new DataBase( "insert into Props (Key, Value) values (?,?)",
						   new String[] { "select PropID, Key, Value from Props where Key=?" },
						   new String[] { "update Props set Value=? where PropID=?" }
		);
	}

	public PropData( String pKey, String pDefaultValue ) throws DRException {

		try {
			PreparedStatement s = db.getSelectStatement( 0 );
			s.setString( 1, pKey );

			ResultSet rec = s.executeQuery();
			if ( rec.next() ) {
				PropID = rec.getInt( "PropID" );
				Key = rec.getString( "Key" );
				Value = rec.getString( "Value" );
			} else {
				PropID = NEW;
				Key = pKey;
				Value = pDefaultValue;
			}
		} catch ( SQLException sqx ) {
			throw new DRException( 2057, "PropData.new", sqx );
		}
	}
	public String getKey() { return Key; }
	public String getValue() { return Value; }
	public String updateValue( String pValue ) throws DRException {

		try {
			Value = pValue;

			if ( PropID == NEW ) {

				PreparedStatement s = db.getInsertStatement();

				s.setString( 1, Key );
				s.setString( 2, Value );

				int result = s.executeUpdate();
				//System.out.printf( "Update Result: %d\n", result );

				ResultSet pk = s.getGeneratedKeys();
				if ( pk != null && pk.next() )
					PropID = pk.getInt( "PropID" );

			} else {
				PreparedStatement s = db.getUpdateStatement( 0 );
				s.setString( 1, Value );
				s.setInt( 2, PropID );

				int result = s.executeUpdate();

			}
		} catch ( SQLException ex ) {
			throw new DRException( 2058, "PropData.update", ex );
		}

		return ( Value );

	}

}
