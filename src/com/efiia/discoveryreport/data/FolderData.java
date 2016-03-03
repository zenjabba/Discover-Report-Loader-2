package com.efiia.discoveryreport.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FolderData /* implements DataObject */ {

	private final boolean DEBUG = false;

	private final int INVALID = -1;
	private final int NEW = -2;

	int FolderID;
	String BoxFolderID;
	String FolderName;
	String BoxParentFolderID;
	EnumStatus Status;
	String Note;

	FolderData[] Path;
	String CreatedAt;

	private boolean isDuplicate;

	/* database stuff */
	private final static DataBase dbFolder;
	private final static int FOLDERBYID = 0;
	private final static int FOLDERBYBOXID = 1;

	private final static int UPDATENAMEPARENTNOTE = 0;
	//private final static int UPDATEPARENTFOLDERID = 1;		// folders don't have versions...

	static {
		dbFolder = new DataBase( "insert into Folders( BoxFolderID, FolderName, BoxParentFolderID, Status, Note ) values (?,?,?,?::status,?)",
							new String[] {
								"select FolderID, BoxFolderID, FolderName, BoxParentFolderID, Status, Note from Folders where FolderID=?",
								"select FolderID, BoxFolderID, FolderName, BoxParentFolderID, Status, Note from Folders where BoxFolderID=?"
							},
							new String[] {
								"update Folders set (FolderName,BoxParentFolderID,Status,Note)=(?,?,?::status,?) where FolderID=?"
								//"update Folders set BoxParentFoldID=? where BoxFolderID=?"
							}
		);
	}

	public FolderData() {
		FolderID = INVALID;
	}

	public FolderData( String pBoxID ) {

		try {
			PreparedStatement s = dbFolder.getSelectStatement( FOLDERBYBOXID );
			s.setString( 1, pBoxID );

			ResultSet rec = s.executeQuery();
			if ( rec.next() ) {
				// FolderID, BoxFolderID, FolderName, BoxParentFolderID, Note
				FolderID = rec.getInt( "FolderID" );
				BoxFolderID = rec.getString( "BoxFolderID" );
				FolderName = rec.getString( "FolderName" );
				BoxParentFolderID = rec.getString( "BoxParentFolderID" );
				Status = EnumStatus.valueOf( rec.getString( "Status" ));
				Note = rec.getString( "Note" );
			} else {
				FolderID = INVALID;
				if ( DEBUG )
					System.out.printf( "!!! Folder ID: %s Not in DB%n", pBoxID );
			}
		} catch ( SQLException ex ) {
			Logger.getLogger( FolderData.class.getName() ).log( Level.SEVERE, null, ex );
		}
	}

	public FolderData( FolderMiniData pMD ) {
		BoxFolderID = pMD.BoxItemID;
		FolderName = pMD.Name;
		BoxParentFolderID = pMD.ParentFolderID;
		Status = EnumStatus.Active;
		FolderID = NEW;
	}

	public FolderData( HashMap<String,Object> pJSONData ) {
		BoxFolderID = (String)pJSONData.get( "id" );
		FolderName = (String)pJSONData.get( "name" );
		Status = EnumStatus.Active;
		CreatedAt = (String)pJSONData.get( "created_at" );
		FolderID = NEW;
	}

	// internal folderid can't change
	public void setFolderID( String pID ) { BoxFolderID = pID; }
	public void setFolderName( String pName ) { FolderName = pName; }
	public void setParentFolderID( String pParent ) { BoxParentFolderID = pParent; }
	public void setPath( FolderData[] pPath ) { Path = pPath; }
	public void setStatus( EnumStatus pStatus ) { Status = pStatus; }
	public void setNote( String pNote ) { Note = pNote; }
	public void appendNote( String pNote ) {
		if ( Note == null )
			Note = pNote;
		else
			Note += ", " + pNote;
	}
	public void setCreated( String pCreated ) { CreatedAt = pCreated; }

	public String getFolderID() { return BoxFolderID; }
	public String getFolderName() { return FolderName; }
	public String getParentFolderID() { return BoxParentFolderID; }
	public EnumStatus getStatus() { return Status; }

	public FolderData[] getPath() { return Path; }
	public String getNote() { return Note; }
	public String getCreated() { return CreatedAt; }

	public boolean isValid() { return FolderID != INVALID; }
	public boolean isInvalid() { return FolderID == INVALID; }
	public boolean isDuplicate() { return isDuplicate; }

	public void save() throws SQLException {

		isDuplicate = false;

		if ( DEBUG )
			System.out.printf( "Saving Folder: %s [%s]%n", FolderName, BoxFolderID );

		PreparedStatement s = null;
		try {
			//	insert into Folders( BoxFolderID, FolderName, BoxParentFolderID, Status, Note )

			s = dbFolder.getInsertStatement();

			s.setString(1, BoxFolderID );
			s.setString( 2, FolderName );
			if ( BoxParentFolderID != null )
				s.setString( 3, BoxParentFolderID );
			else
				s.setNull( 3, java.sql.Types.VARCHAR );
			s.setString( 4, Status.name() );
			if ( Note != null )
				s.setString( 5, Note );
			else
				s.setNull( 5, java.sql.Types.VARCHAR );

			s.execute();
			int result = s.getUpdateCount();
			if ( DEBUG )
				System.out.printf( "--> Update Result: %d\n", result );

			ResultSet pk = s.getGeneratedKeys();
			if ( pk != null && pk.next() )
				FolderID = pk.getInt( "FolderID" );

		} catch ( SQLException sqx ) {

			if ( !dbFolder.isDuplicateKeyException( sqx )) {
				FolderID = INVALID;
				if ( s != null )
					System.out.println( s.toString() );
				throw ( sqx );
			}

			isDuplicate = true;

			// do a select to get the PK
			s = dbFolder.getSelectStatement( FOLDERBYBOXID );
			s.setString( 1, BoxFolderID );

			ResultSet rec = s.executeQuery();
			if ( rec.next() )
				FolderID = rec.getInt( "FolderID" );
			else
				FolderID = INVALID;
		}
	}

	public void update() throws SQLException {
		// update Folders set (FolderName,BoxParentFolderID,Status,Note)
		PreparedStatement s = dbFolder.getUpdateStatement( UPDATENAMEPARENTNOTE );
		s.setString( 1, FolderName );
		s.setString( 2, BoxParentFolderID );
 		s.setString( 3, Status.name() );
		s.setString( 4, Note );
		s.setInt( 5, FolderID );

		int updated = s.executeUpdate();
	}

	@Override
	public String toString() {
		return String.format("  FolderData:: Folder Name: %s [%s]; Created: %s%n", FolderName, BoxFolderID, CreatedAt );
	}


}
