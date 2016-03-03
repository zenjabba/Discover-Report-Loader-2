/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

import java.util.HashMap;

/**
 *[START_OBJECT]
    [KEY_NAME]    item_type=>    [VALUE_STRING]file
    [KEY_NAME]    item_id=>    [VALUE_STRING]26920694529
    [KEY_NAME]    item_name=>    [VALUE_STRING]make500
    [KEY_NAME]    parent=>    [START_OBJECT]
     [KEY_NAME]     type=>     [VALUE_STRING]folder
     [KEY_NAME]     name=>     [VALUE_STRING]500
     [KEY_NAME]     id=>     [VALUE_STRING]3201036339
     [END_OBJECT]
    [END_OBJECT]
 * @author larry
 */
public class FileMiniData  {

	String Type;			// file

	String Name;
	String BoxItemID;		// box file id
	String BoxItemVersionID;

	String FolderID;			// box file name

	public FileMiniData( EventData pED ) {
		Type = pED.BoxItemType;			//(String)pJSONData.get( "item_type" );
		Name = pED.BoxItemName;			// (String)pJSONData.get( "item_name" );
		BoxItemID = pED.BoxItemID;		//(String)pJSONData.get("item_id");
		BoxItemVersionID = pED.BoxItemVersionID;	//pVersionID;
		FolderID = pED.BoxParentID;
	}

	/*
	public FileMiniData( HashMap<String,Object> pJSONData, String pVersionID ) {

		Type = (String)pJSONData.get( "item_type" );
		Name = (String)pJSONData.get( "item_name" );
		BoxItemID = (String)pJSONData.get("item_id");
		BoxItemVersionID = pVersionID;

		HashMap<String,Object> xParent = (HashMap<String,Object>)pJSONData.get( "parent" );
		FolderID = (String)xParent.get( "id" );

	}
	*/

	@Override
	public String toString() {
		return String.format("Type: %s; Name: %s[%s]", Type, Name, BoxItemID );
	}


	public String getType() { return Type; }
	public String getID() { return BoxItemID; }
	public String getVersionID() { return BoxItemVersionID; }
	public String getName() { return Name; }
	public String getFolderID() { return FolderID; }

}
