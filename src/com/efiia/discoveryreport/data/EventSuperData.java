/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

import static com.efiia.discoveryreport.data.UserData.SERVICEPREFIX;
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

		// new logic to accomodate service kludge
		HashMap<String,Object> xJSONData = (HashMap<String, Object>)pJSONData.get( "created_by" );

		String xName = (String)xJSONData.get( "name" );
		String xLogin = (String)xJSONData.get( "login" );
		String xBoxUserID = (String)xJSONData.get(  "id" );

		/* per Box teleconference - no longer required - needs testing
		 * 2016-Jul-9
		 */
		if ( xBoxUserID == null ) {
			/* might be a service, check additional_info
			 *
		     * "additional_details": {
			 *	"version_id": "14449061888",
			 *	"service_id": "52260",
			 *	"service_name": "BoxContentWorkflow"
			 * }
			 */
			HashMap<String,Object> xAD = (HashMap<String,Object>)pJSONData.get( "additional_details" );

			String x = (String)xAD.get( "service_name" );
			if ( x != null && x.equals( "BoxContentWorkflow" ) ) {
				xName = "Retention Manager";
				xLogin = x;
				xBoxUserID = SERVICEPREFIX + (String)xAD.get( "service_id" );
			}
		}
		User = new UserData( xName, xLogin, xBoxUserID );
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
