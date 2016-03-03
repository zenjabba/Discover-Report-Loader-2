/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.boxapi;

import com.efiia.discoveryreport.DRException;
import com.efiia.discoveryreport.data.EnumStatus;
import com.efiia.discoveryreport.data.FileData;
import com.efiia.discoveryreport.data.FolderData;
import com.efiia.discoveryreport.data.FolderItemData;
import com.efiia.discoveryreport.data.UserData;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * 	curl https://api.box.com/2.0//folders/{folder id}/items?fields=
 * {
 *  "total_count": 2,
 *  "entries": [
 *   {
 *     "type": "folder",
 *     "id": "3441517496",
 *     "etag": "0",
 *     "size": 166,
 *     "name": "New Folder Web Interface"
 *   },
 *   {
 *     "type": "file",
 *     "id": "28788411394",
 *     "etag": "0",
 *     "file_version": {
 *       "type": "file_version",
 *       "id": "27444838854",
 *       "sha1": "ab7dffe9705dc7089f80d5cd2a4720e14a8093ed"
 *     },
 *     "sha1": "ab7dffe9705dc7089f80d5cd2a4720e14a8093ed",
 *     "size": 11883,
 *     "name": "test3.csv"
 *   }
 *  ],
 *  "offset": 0,
 *  "limit": 100,
 *  "order": [
 *   {
 *     "by": "type",
 *     "direction": "ASC"
 *   },
 *   {
 *     "by": "name",
 *     "direction": "ASC"
 *   }
 *  ]
 * }
 */
public class FolderItemsProcessor extends APIProcessor {

	private FolderData myFolderData;

	public FolderItemsProcessor( BoxConnection pConnector ) {
		super( "folders/%s/items", pConnector );
		addQuery( "fields", "id,file_version,sha1,size,name,created_at" );
	}

	@Override
	public String formatURL( String pFormat ) {
		return String.format( pFormat, myFolderData.getFolderID() );
	}

	@SuppressWarnings("unchecked")
	public FolderItemData getItems( FolderData pFolderData, UserData pAsUser ) throws DRException {

		String BoxUserID = pAsUser.getBoxUserID();
		setAsUser( Connector.getAsUser( BoxUserID ) );

		myFolderData = pFolderData;			// for formatting the URL

		// set up for multiple calls in case of big folders
		long offset = 0;
		long limit = 500;
		long itemsRecd = limit;

		addQuery( "limit", String.valueOf( limit ));

		FolderItemData retData = new FolderItemData();

		do {
			addQuery( "offset", String.valueOf( offset ));

			if ( FetchData() ) {

				// this call will have the current data, which might not match the
				// items that were originally in the folder

				//System.out.println( MetaData.toString() );

				itemsRecd = (long)MetaData.get( "total_count" );

				if ( itemsRecd == 0 )		// just in case...
					break;

				// now read through the entries data to make the files and folders

				ArrayList<HashMap<String,Object>> xItems = (ArrayList<HashMap<String,Object>>)MetaData.get( "entries" );

				for( HashMap<String, Object> x : xItems ) {
					String xType = (String)x.get( "type" );
					String xID = (String)x.get( "id" );
					String xName = (String)x.get( "name" );
					switch( xType ) {
						case "file":
							FileData fd = new FileData();
							fd.setBoxFileID( (String)x.get( "id" ));
							HashMap<String,Object> fv = (HashMap<String,Object>)x.get( "file_version" );
							fd.setBoxFileVersionID( (String)fv.get( "id" ));
							fd.setName( (String)x.get( "name" ));
							fd.setSHA( (String)x.get( "sha1" ));
							fd.setType( xType );
							fd.setSize( String.valueOf( x.get( "size" )));
							fd.setFolderID( pFolderData.getFolderID() );
							fd.setStatus( EnumStatus.Active );
							fd.setCreated( (String)x.get( "created_at" ));
							retData.Files.add( fd );
							break;

						case "folder":
							FolderData fx = new FolderData();
							fx.setFolderID( (String)x.get( "id" ) );
							fx.setFolderName( (String)x.get( "name" ) );
							fx.setStatus( EnumStatus.Active );
							fx.setCreated( (String)x.get( "created_at" ));
							retData.Folders.add( fx );
							break;

						case "web_link":
							FileData wl = new FileData();
							wl.setType( xType );
							wl.setBoxFileID( (String)x.get( "id" ));
							wl.setName( (String)x.get( "name" ));
							retData.WebLinks.add( wl );
							break;
					}
				}

				offset += limit;
			} else {
				pFolderData.setNote( ErrorData.get( "message" ));
				break;
			}

		} while ( itemsRecd == limit );

		return retData;
	}

}
