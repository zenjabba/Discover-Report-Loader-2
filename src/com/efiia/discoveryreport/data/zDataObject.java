/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

import java.io.PrintStream;
import java.sql.SQLException;

/**
 *
 * @author larry
 */
public interface zDataObject {

	//void set( String pField, Object pValue );
	void dump( PrintStream p );
	void save() throws SQLException;
	boolean isDuplicate();
	
}
