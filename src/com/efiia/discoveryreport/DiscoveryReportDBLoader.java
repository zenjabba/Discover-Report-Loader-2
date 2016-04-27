/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport;

import com.efiia.discoveryreport.boxapi.BoxConnection;
import com.efiia.discoveryreport.boxapi.EventProcessor;
import java.io.File;
import java.util.Date;

import com.efiia.discoveryreport.data.DataBase;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author larry
 */
public class DiscoveryReportDBLoader {

	private final static boolean DEVMODE = false;		// for ignoring file handling when testing

	public final static boolean DEBUG = false;

	private final static String Version =
			//"1.0 (26-March-2015)";
			// "1.2 (Dev)";
			// "1.3 (10-Apr-2015)";
			// "1.4 (16-Apr-2015)";		// add fix for copy events
			// "2.0 (3-Mar-2016)";			// added process check on startup
			//"2.0.1 (20-Mar-2016)";		// back out Box Error Response (APIProcessor) Exception 1113
			//"2.0.2 (22-Mar-2016)";		// add connection and read timeout for http
			// "2.0.3 (??-Mar-2016)";		// add connection and read timeout for http
			// "2.0.4 (20-Apr-2016)";		// kludge for Retention Manager service
			"2.0.5 (27-Apr-2016)";		// clean-up logic for Version ID confusing folder data

	/**
	 * @param args the command line arguments
	 */
	public static void main( String[] args ) {

		try {

			File confDir = new File( "/etc/discoveryreport" );

			if ( !DEVMODE ) {
				// check for sentenial unless in debugging mode
				File sentenial = new File( confDir, "drloader.lock" );
				if ( !sentenial.createNewFile() ) {
					/* new Jan 2016
					 * check for running process
					 * if there is an already running version, quit
					 * clean up the old lock, make a new one
					 */
					InstanceChecker ic = new InstanceChecker();
					if ( ic.SelfInstanceFound() ) {
						System.out.println( "Discovery Report Loader Already Running" );
						return;
					}
					// update the last modified for cleanlyness
					sentenial.setLastModified( System.currentTimeMillis() );
				}
				/* new Jan 2016
				 * smarter shutdown will delete file on any type of shutdown
				 * deleteOnExit on works on normal shutdowns
				 */
				//sentenial.deleteOnExit();
				Runtime.getRuntime().addShutdownHook(new Thread() {
                    // destroy the lock when the JVM is closing
					@Override
                    public void run() {
						try {
							sentenial.delete();
						} catch ( Exception ex ) {
							System.out.printf( "Unable to delete " + sentenial.getName() + ": " + ex.getMessage() );
						}
                    }
                });
			}
			System.out.printf( "Starting Discovery Report Loader v%s%n", Version );

			// database connection
			DataBase.Config( confDir );
			DataBase.Connect();

			// connect to box
			BoxConnection bc = ( args.length == 0 ? BoxConnection.Config( confDir ) : new BoxConnection( args[0] ));
			bc.getCurrentUser();

			int testctr = 1;
			EventProcessor ep = new EventProcessor( bc, confDir );

			long xStart = new Date().getTime();
			for( ;; ) {
				System.out.printf( "Calling Box Event Stream #%d [%s]\n", testctr++, new Date().toString() );
				ep.FetchData();
				if ( !ep.hasMore() )
					break;

				// for debugging things
				//if ( testctr > 2 )
				//	break;
				ep.WriteStats();
			}
			long xEnd = new Date().getTime();
			int xCalls = bc.getCallCtr();
			System.out.printf( "Total Elapsed Time for %d API Calls: %.4f seconds (avg: %.4f/sec)%n", xCalls, (xEnd-xStart)/1000.00, xCalls/((xEnd-xStart)/1000.00) );
			ep.WriteStats();

		} catch ( DRException ex ) {
			ex.print();

		} catch ( SQLException ex ) {
			Logger.getLogger(DiscoveryReportDBLoader.class.getName() ).log( Level.SEVERE, null, ex );
		} catch ( Exception ex ) {
			Logger.getLogger(DiscoveryReportDBLoader.class.getName() ).log( Level.SEVERE, null, ex );
		}
	}

}
