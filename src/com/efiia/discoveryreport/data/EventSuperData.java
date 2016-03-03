/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

import java.util.HashMap;

/**
 *
 * @author larry
 */
public class EventSuperData {

	public EventData Event;
	public UserData User;

	// retired public String SourceType;
	public FileMiniData File;
	public FolderMiniData Folder;

	@SuppressWarnings("unchecked")
	public EventSuperData( HashMap<String,Object> pJSONData ) {

		Event = new EventData( pJSONData );
		User = new UserData( (HashMap<String, Object>)pJSONData.get( "created_by"));
		Event.setUser( User );

		// retired HashMap<String,Object> xSource = (HashMap<String,Object>)pJSONData.get( "source" );

		// all the events we're interested in have item data
		// retired - SourceType = (String)xSource.get( "item_type" );

		// if its a item event
		if ( Event.BoxItemType != null ) {

			switch ( Event.BoxItemType ) {
				case "file":
					File = new FileMiniData( Event );
					break;

				case "folder":
					Folder = new FolderMiniData( Event );
					break;

				case "web_link":
					File = new FileMiniData( Event );
					break;

				default:
					System.out.println( "Extra Source Item Type Found: " + Event.BoxItemType );
			}
		}
	}

	public boolean isFileEvent() { return Event.BoxItemType.equals( "file" ) || Event.BoxItemType.equals( "web_link" ); }
	public boolean isFolderEvent() { return Event.BoxItemType.equals( "folder" ); }

	public Object getItem() {
		if ( File != null )
			return File;
		if ( Folder != null )
			return Folder;
		return null;
	}

}
