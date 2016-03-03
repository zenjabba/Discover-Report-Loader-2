/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

import com.efiia.discoveryreport.data.zDataObject;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 *[START_OBJECT]
    [KEY_NAME]    item_type=>    [VALUE_STRING]file
    [KEY_NAME]    item_id=>    [VALUE_STRING]26920694529
    [KEY_NAME]    item_name=>    [VALUE_STRING]make500
    [KEY_NAME]    parent=>    [START_OBJECT]
     [KEY_NAME]     type=>     [VALUE_STRING]folder
     [KEY_NAME]     name=>     [VALUE_STRING]500
     [KEY_NAME]     id=>     [VALUE_STRING]3201036339
     [END_OBJECT]
    [END_OBJECT]
 * @author larry
 */
public class xGeneralData  {

	HashMap<String, String> gData = new HashMap<>();

	//@Override
	public void set( String pField, Object pValue ) {
		if ( pValue instanceof String )
			gData.put( pField, (String)pValue );
		else
			throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
	}

	public void dump( PrintStream p ) {
		if ( gData.isEmpty() ) {
			p.println( "  (no data)");
		} else {
			for ( Map.Entry<String, String> g : gData.entrySet() )
				p.printf( "  %s => %s\n", g.getKey(), g.getValue() );
		}
	}

	public void save() throws SQLException {
		throw new UnsupportedOperationException( "Save Not supported yet." );
	}

}
