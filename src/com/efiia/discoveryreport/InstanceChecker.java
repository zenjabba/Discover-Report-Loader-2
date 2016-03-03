/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 *
 * @author larry
 */
public class InstanceChecker {

	final String myProcID;
	final String myProcName;

	public InstanceChecker() {
		myProcID = getProcessID();
		myProcName = getMyName();
	}

	public boolean SelfInstanceFound() {
		
		HashMap<String,String> procList = getProcessesWith( myProcName );

		if ( procList == null )		// nothing found with the same name
			return false;

		procList.remove( myProcID );

		if ( procList.size() == 0 )
			return false;

		return true;
	}

	private String getMyName() {
		try {
			URL codeBase = getClass().getProtectionDomain().getCodeSource().getLocation();
			Path p = Paths.get( codeBase.toURI() );
			return ( p.getFileName().toString() );
		} catch ( URISyntaxException ex ) {
			// ignore
		}
		return null;
	}

	private String getProcessID() {
		String pname = ManagementFactory.getRuntimeMXBean().getName();
		int x = pname.indexOf( '@' );
		if ( x > -1 )
			pname = pname.substring( 0, x );
		return pname;
	}

	private HashMap<String, String> getProcessesWith( String pMatchStr ) {

		HashMap<String, String> result = new HashMap<String,String>() {};

		try {
			ProcessBuilder pb = new ProcessBuilder(new String[] { "ps", "-e", "-o", "pid,command" } );
			Process p = pb.start();
			InputStream is = p.getInputStream();

			BufferedReader br = new BufferedReader( new InputStreamReader( is ));
			String line;
			while (( line = br.readLine() ) != null ) {
				if ( line.indexOf( pMatchStr ) > -1 ) {
					// now get the process id of each element
					String x = line.trim();
					// find first space to split on
					int i = x.indexOf( ' ' );
					String pid = x.substring( 0, i );
					String cmd = x.substring( i+1 );
					result.put( pid, cmd );
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		if ( result.size() == 0 )
			return null;

		return result;
	}

}
