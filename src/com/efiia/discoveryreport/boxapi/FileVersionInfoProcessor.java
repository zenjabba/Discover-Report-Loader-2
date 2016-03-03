/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.boxapi;

import com.efiia.discoveryreport.DRException;
import com.efiia.discoveryreport.data.EnumStatus;
import com.efiia.discoveryreport.data.FileData;
import com.efiia.discoveryreport.data.UserData;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * 	curl https://api.box.com/2.0/files/26921134121/versions
 *
 */
public class FileVersionInfoProcessor extends APIProcessor {

	private final static boolean DEBUG = false;

	private String myFileID;

	public FileVersionInfoProcessor( BoxConnection pConnector ) {
		super( "files/%s/versions", pConnector );
		addQuery( "fields", "id,sha1,name,size,created_at" );
	}

	@Override
	public String formatURL( String pFormat ) {
		return String.format( pFormat, myFileID );
	}

	public FileData getFileVersionData( String pFileID, String pVersionID, UserData pAsUser ) throws DRException {

		if ( DEBUG )
			System.out.printf( "Get File Version Data for %s/%s%n", pFileID, pVersionID );

		FileData[] fdd = getFileVersionData( pFileID, pAsUser );

		if ( fdd == null ) {			// error
			if ( DEBUG )
				System.out.println( "  -- Result: NULL" );
			return null;
		}

		if ( fdd.length == 0 ) {
			if ( DEBUG )
				System.out.println( "  -- Result: NO VERSIONS" );
			return null;			// no versions
		}

		FileData retFD = null;

		for ( FileData x : fdd ) {
			if ( /* x.isValid() && */ x.getVersionID().equals( pVersionID )) {
				retFD = x;
				retFD.setStatus( EnumStatus.Replaced );
				break;
			}
		}
		if ( DEBUG )
			System.out.printf(  "  -- Result: %s", fdd == null ? "FAIL" : "SUCCESS" );

		return ( retFD );
	}

	@SuppressWarnings("unchecked")
	public FileData[] getFileVersionData( String pFileID, UserData pAsUser ) throws DRException {

		String BoxUserID = pAsUser.getBoxUserID();
		setAsUser( Connector.getAsUser( BoxUserID ) );

		myFileID = pFileID;			// for formatting the URL
		FileData[] retData = null;

		if ( FetchData() ) {
			// success, update only the missing FileData, create the folder structure
			int items = (int)((long)MetaData.get( "total_count" ));		// down cast
			retData = new FileData[items];
			if ( items > 0 ) {
				ArrayList<HashMap<String,Object>> entries = (ArrayList<HashMap<String,Object>>)MetaData.get( "entries" );
				// move entries into FileData to make it usable
				int ctr = 0;
				for ( HashMap<String,Object> e : entries ) {
					// System.out.println( e.toString() );
					FileData fdx = new FileData();

					fdx.setType( "file" );
					fdx.setBoxFileID( pFileID );
					fdx.setBoxFileVersionID( (String)e.get( "id" ));
					fdx.setName( (String)e.get( "name" ));
					fdx.setSize( String.valueOf( e.get( "size" )));
					fdx.setSHA( (String)e.get( "sha1" ));
					fdx.setFolderID( null );
					fdx.setStatus( EnumStatus.Replaced );		// only way here is if you've been versioned out
					fdx.setNote( null );
					fdx.setCreated( (String)e.get( "created_at" ));

					retData[ctr++] = fdx;
				}
			}
			if ( DEBUG )
				System.out.println( "*** Version Info Success" );
		} else {
			// report the error somewhere...
			if ( DEBUG ) {
				System.out.print( "*** Error on Version Info: ");
				System.out.println( ErrorData.toString() );
			}
		}

		return retData;
	}

	/*
	public FileData getFileData( String pFileID, UserData pAsUser ) throws DRException {

		String BoxUserID = pAsUser.getBoxUserID();
		setAsUser( Connector.getAsUser( BoxUserID ) );

		myFileData = new FileData( pFileID );

		if ( FetchData() ) {

			// success, update the FileData, create the folder structure
			myFileData.setName( (String)MetaData.get( "name" ) );

			// now read through the folder data to make the paths
			// they come in order top down
			HashMap<String,ArrayList<HashMap<String,Object>>> xPathInfo = (HashMap<String,ArrayList<HashMap<String,Object>>>)MetaData.get( "path_collection" );
			ArrayList<HashMap<String,Object>> xFolders = (ArrayList<HashMap<String,Object>>)xPathInfo.get( "entries" );
			ArrayList<FolderData> Path = new ArrayList<>();
			String ParentFolderID = null;

			for( HashMap<String, Object> x : xFolders ) {
				FolderData d = new FolderData( x );
				d.setParentFolderID( ParentFolderID );
				Path.add( d );
				ParentFolderID = d.getFolderID();
			}

			myFileData.setPath( Path.toArray( new FolderData[Path.size()]));

		} else {
			myFileData.setNote( ErrorData.get( "message" ));

		}

		return myFileData;
	}
	*/

}
