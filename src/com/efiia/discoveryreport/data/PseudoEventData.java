/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

import java.sql.SQLException;

/**
 *
 * @author larry
 */
public class PseudoEventData {

	EventData MasterEvent;
	int Ctr;

	public PseudoEventData( EventData pMaster ) {
		MasterEvent = pMaster;
		Ctr = 0;
	}

	public UserData getUser() { return MasterEvent.getUser(); }
	public String getEventTimeStamp() { return MasterEvent.CreatedAt; }

	public void saveEvent( FileData pFX ) throws SQLException {
		EventData x = new EventData( MasterEvent, Ctr++, pFX.getType(), pFX.getID(), pFX.getVersionID(), pFX.getName(), pFX.getFolderID() );
		x.save();
	}

	public void saveEvent( FolderData pFL ) throws SQLException {
		EventData x = new EventData( MasterEvent, Ctr++, "folder", pFL.getFolderID(), "0", pFL.getFolderName(), pFL.getParentFolderID() );
		x.save();
	}
}
