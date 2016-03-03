/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.boxapi;

import com.efiia.discoveryreport.DRException;
import com.efiia.discoveryreport.data.FolderData;
import com.efiia.discoveryreport.data.FileData;
import com.efiia.discoveryreport.data.UserData;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * 	curl https://api.box.com/2.0/files/26921134121
 *
 * file_version={
 *	sha1=c577f7a37657053275f3e3ecc06ec22e6b909366
 *  id=25373921477
 *  type=file_version
 * }
 * parent={
 *  sequence_id=0
 *  name=10000
 *  etag=0
 *  id=3202504813
 *  type=folder
 * }
 * description=
 * created_at=2015-03-02T20:07:48-08:00
 * content_created_at=2015-03-02T20:07:48-08:00
 * owned_by={
 *  name=Dump User 1
 *  id=233432237
 *  type=user
 *  login=stephen.thompson.au+dump@gmail.com
 * }
 * type=file
 * item_status=active
 * created_by={
 *  name=Dump User 1
 *  id=233432237
 *  type=user
 *  login=stephen.thompson.au+dump@gmail.com
 * }
 * sha1=c577f7a37657053275f3e3ecc06ec22e6b909366
 * size=1000
 * sequence_id=0
 * name=xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaajso
 * modified_by={
 *  name=Dump User 1
 *  id=233432237
 *  type=user
 *  login=stephen.thompson.au+dump@gmail.com
 * }
 * etag=0
 * content_modified_at=2015-03-02T14:28:14-08:00
 * id=26930848893
 * modified_at=2015-03-02T20:07:49-08:00
 * path_collection={
 *  entries=[{
 *   name=All Files
 *   id=0
 *   type=folder
 *  },{
 *   sequence_id=0
 *   name=Folder Tests
 *   etag=0
 *   id=3202326599
 *   type=folder
 *  },{
 *   sequence_id=0
 *   name=10000
 *   etag=0
 *   id=3202504813
 *   type=folder
 *  }]
 *  total_count=3
 * }
 */
public class FileInfoProcessor extends APIProcessor {

	private final static boolean DEBUG = false;

	private FileData myFileData;

	public FileInfoProcessor( BoxConnection pConnector ) {
		super( "files/%s", pConnector );
	}

	@Override
	public String formatURL( String pFormat ) {
		return String.format( pFormat, myFileData.getID() );
	}

	@SuppressWarnings("unchecked")
	public FileData completeFileData( FileData pFileData, UserData pAsUser ) throws DRException {

		String BoxUserID = pAsUser.getBoxUserID();
		setAsUser( Connector.getAsUser( BoxUserID ) );

		myFileData = pFileData;			// for formatting the URL

		if ( FetchData() ) {

			// this call will have the current data, which might not match the
			// event that started the process,
			// so, only update only the missing FileData, create the folder structure
			// check the version here
			//System.out.println( "Fetch File Data: ");
			//System.out.println( MetaData.toString() );
			//System.out.println( "-- End Data --" );

			String testVersion = pFileData.getVersionID();
			if ( !testVersion.equals( "0" ) ) {
				// skip check if there isn't any version information
				// file_version => {sha1=65be13ad27108d7b1bb852550e1c892a3631b223, id=26686254822, type=file_version}
				HashMap<String,Object> vInfo = (HashMap<String,Object>)MetaData.get("file_version");
				String boxVersion = (String)vInfo.get( "id" );
				if ( !testVersion.equals( boxVersion )) {
					if ( DEBUG ) {
						System.out.printf( "Version MisMatch: Current File ID: %s; Version %s%n", pFileData.getID(), boxVersion );
						System.out.printf( "Required ID: %s; Version %s%n", pFileData.getID(), testVersion );
					}
					return null;
				}
			}

			pFileData.setSHA( (String)MetaData.get( "sha1" ));
			pFileData.setSize( String.valueOf( MetaData.get( "size" )));

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

			pFileData.setPath( Path.toArray( new FolderData[Path.size()]));

		} else {
			pFileData.setNote( FixMessage( ErrorData.get( "message" )));

		}

		return pFileData;
	}

	private String FixMessage( String pMsg ) {
		if ( pMsg.equals( "Item is trashed") )
			return "Item was deleted; Extra file data unavailable";
		if ( pMsg.equals( "Not Found"))
			return "Extra file data unavailable";
		return ( pMsg );
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
