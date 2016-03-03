/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

import java.util.HashMap;

public class FolderMiniData  {

	String Type;			// folder

	String Name;
	String BoxItemID;

	String ParentFolderID;	// box file name

	public FolderMiniData( EventData pED ) {
		Type = pED.BoxItemType;		// (String)pJSONData.get( "item_type" );
		Name = pED.BoxItemName;		// (String)pJSONData.get( "item_name" );
		BoxItemID = pED.BoxItemID;	// (String)pJSONData.get("item_id");
		//HashMap<String,Object> xParent = (HashMap<String,Object>)pJSONData.get( "parent" );
		ParentFolderID = pED.BoxParentID;	//(String)xParent.get( "id" );
	}
	/*
	public FolderMiniData( HashMap<String,Object> pJSONData ) {

		Type = (String)pJSONData.get( "item_type" );
		Name = (String)pJSONData.get( "item_name" );
		BoxItemID = (String)pJSONData.get("item_id");

		HashMap<String,Object> xParent = (HashMap<String,Object>)pJSONData.get( "parent" );
		ParentFolderID = (String)xParent.get( "id" );

	}
	*/
	
	@Override
	public String toString() {
		return String.format("Type: %s; Name: %s[%s]", Type, Name, BoxItemID );
	}

	public String getType() { return Type; }
	public String getID() { return BoxItemID; }
	public String getName() { return Name; }
	public String getParentFolderID() { return ParentFolderID;	}

}
