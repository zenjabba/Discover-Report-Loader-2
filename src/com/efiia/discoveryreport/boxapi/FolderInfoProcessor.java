/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.boxapi;

import com.efiia.discoveryreport.DRException;
import com.efiia.discoveryreport.data.FolderData;
import com.efiia.discoveryreport.data.UserData;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * 	curl https://api.box.com/2.0//folders/{folder id}
 *
 * parent
 *	name=All Files
 *  id=0
 *  type=folder
 * created_at=2014-12-12T11:58:04-08:00
 * description=
 * content_created_at=2014-12-12T11:58:04-08:00
 * owned_by=
 *  name=Efiia Sandbox
 *  id=227572005
 *  type=user
 *  login=box@efiia.com
 * type=folder
 * item_status=active
 * item_collection=
 *  entries=[]
 *  offset=0
 *  total_count=0
 *  limit=100
 *  order=
 *   { by=type
 *     direction=ASC
 *   }, {
 *     by=name
 *     direction=ASC
 *   }]
 * created_by
 *   name=Efiia Sandbox
 *   id=227572005
 *   type=user
 *   login=box@efiia.com
 * size=0
 * sequence_id=0
 * name=Test Folder
 * modified_by
 *   name=Efiia Sandbox
 *   id=227572005
 *   type=user
 *   login=box@efiia.com
 * etag=0
 * content_modified_at=2014-12-12T11:58:04-08:00
 * id=2810263249
 * modified_at=2014-12-12T11:58:04-08:00
 * path_collection
 *   entries [
 *     { name=All Files
 *       id=0
 *       type=folder
 *     }]
 *  total_count=1
 *
 */
public class FolderInfoProcessor extends APIProcessor {

	private FolderData myFolderData;

	public FolderInfoProcessor( BoxConnection pConnector ) {
		super( "folders/%s", pConnector );
	}

	@Override
	public String formatURL( String pFormat ) {
		return String.format( pFormat, myFolderData.getFolderID() );
	}

	@SuppressWarnings("unchecked")
	public FolderData completeFolderData( FolderData pFolderData, UserData pAsUser ) throws DRException {

		String BoxUserID = pAsUser.getBoxUserID();
		setAsUser( Connector.getAsUser( BoxUserID ) );

		myFolderData = pFolderData;			// for formatting the URL

		if ( FetchData() ) {

			// this call will have the current data, which might not match the
			// event that started the process,
			// so, only update only the path info so we can make sure that the
			// current folder structure is complete

			// dont update with the most recent information, we need to document the way we got here
			// myFolderData.setFolderName( (String) MetaData.get( "name") );

			//System.out.println( MetaData.toString() );

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

			pFolderData.setPath( Path.toArray( new FolderData[Path.size()]));

			// originally we set my parent as the last in the array instead of the parent item -> id (better be the same)
			// but its possible that the folder has been moved since created, so ignore the current path
			// and let the folder id from the original event stand as is
			// myFolderData.setParentFolderID( ParentFolderID );

		} else {
			pFolderData.setNote( ErrorData.get( "message" ));

		}

		return pFolderData;
	}

}
