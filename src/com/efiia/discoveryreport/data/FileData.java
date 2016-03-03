/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.efiia.discoveryreport.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileData {

	private final static boolean DEBUG = false;

	private final static int INVALID = -1;
	private final static int NEW = -2;

	int FileID;				// primary key (integer)
	String Type;			// file
	String BoxFileID;				// box file id
	String BoxFileVersionID;
	String Name;			// box file name
	String SHA;
	String Size;
	String FolderID;
	EnumStatus Status;

	String Note;

	FolderData[] Path;
	String CreatedAt;

	private static final DataBase dbFile;

	private final static int FILEBYID = 0;
	private final static int FILEBYBOXID = 1;
	private final static int FILEBYBOXIDVERSIONID = 2;

	private final static int UPDATENAMEFOLDERSTATUSNOTE = 0;
	private final static int UPDATEFILEFOLDER = 1;

	static {
		dbFile = new DataBase( "insert into Files (BoxFileID, BoxFileVersionID, Name, SHA, Type, FileSize, BoxFolderID, Status, Note ) " +
								"values (?,?,?,?,?,?,?,?::status,?)",
							new String[] {
								"select FileID, BoxFileID, BoxFileVersionID, Name, SHA, Type, FileSize, BoxFolderID, Status, Note from Files where FileID=?",
								"select FileID, BoxFileID, BoxFileVersionID, Name, SHA, Type, FileSize, BoxFolderID, Status, Note from Files where BoxFileID=?",
								"select FileID, BoxFileID, BoxFileVersionID, Name, SHA, Type, FileSize, BoxFolderID, Status, Note from Files where BoxFileID=? and BoxFileVersionID=?"
							},
							new String[] {
								"update Files set (Name, BoxFolderID, Status, Note)=(?,?,?::status,?) where FileID=?",		// only items that can change once saved
								"update Files set BoxFolderID=? where BoxFileID=?"							// when a file is moved, update all folders to match current layout
							}
		);
	}

	/*
	public static FileData[] getFileVersions( String pBoxFileID ) {

		ArrayList<FileData> fdl = new ArrayList<>();

		try {
			PreparedStatement s = dbFile.getSelectStatement( FILEBYBOXID );
			s.setString( 1, pBoxFileID );

			ResultSet rec = s.executeQuery();
			while ( rec.next() ) {

				FileData fdx = new FileData();

				fdx.FileID = rec.getInt( "FileID" );
				fdx.BoxFileID = rec.getString( "BoxFileID" );
				fdx.BoxFileVersionID = rec.getString( "BoxFileVersionID" );
				fdx.Name = rec.getString( "Name" );
				fdx.SHA = rec.getString( "SHA" );
				fdx.Type = rec.getString( "Type" );
				fdx.Size = rec.getString( "FileSize" );
				fdx.FolderID = rec.getString( "BoxFolderID" );
				fdx.Note = rec.getString( "Note" );

				fdl.add( fdx );
			}

		} catch ( SQLException ex ) {
			Logger.getLogger( FileData.class.getName() ).log( Level.SEVERE, null, ex );
		}

		return ( fdl.size() > 0 ? fdl.toArray( new FileData[fdl.size()]) : null );

	}
	*/


	/**
	 */
	public FileData() {
		FileID = INVALID;
		Type = "file";
	}

	/**
	 * Get File Information from the Database
	 * @param pBoxFileID
	 * @param pBoxVersionID
	 */
	public FileData( String pBoxFileID, String pBoxVersionID ) {

		try {
			PreparedStatement s = dbFile.getSelectStatement( FILEBYBOXIDVERSIONID );
			s.setString( 1, pBoxFileID );
			s.setString( 2, pBoxVersionID );

			ResultSet rec = s.executeQuery();
			if ( rec.next() ) {
				FileID = rec.getInt( "FileID" );
				BoxFileID = rec.getString( "BoxFileID" );
				BoxFileVersionID = rec.getString( "BoxFileVersionID" );
				Name = rec.getString( "Name" );
				SHA = rec.getString( "SHA" );
				Type = rec.getString( "Type" );
				Size = rec.getString( "FileSize" );
				FolderID = rec.getString( "BoxFolderID" );
				Status = EnumStatus.valueOf( rec.getString( "Status" ) );
				Note = rec.getString( "Note" );
			} else {
				FileID = INVALID;
			}

		} catch ( SQLException ex ) {
			Logger.getLogger( FileData.class.getName() ).log( Level.SEVERE, null, ex );
		}

	}

	/**
	 * Constructor to set up fetching full file information from an event
	 * @param pMD
	 */
	public FileData( FileMiniData pMD ) {
		Type = pMD.Type;
		BoxFileID = pMD.BoxItemID;
		BoxFileVersionID = pMD.BoxItemVersionID;
		Name = pMD.Name;
		FolderID = pMD.FolderID;
		// not in mini data - default to something that can printed
		SHA = "-";
		Size = "-";
		Status = EnumStatus.Active;
		FileID = NEW;
	}

	@Override
	public String toString() {
		return String.format( "Type: %s; Name: %s[%s]; Created: %s", Type, Name, BoxFileID, CreatedAt );
	}

	public void setType( String pType ) { Type = pType;	}
	public void setBoxFileID( String pBoxFileID ) { BoxFileID = pBoxFileID;	}
	public void setBoxFileVersionID( String pBoxFileVersionID ) { BoxFileVersionID = pBoxFileVersionID; }
	public void setName( String pName ) { Name = pName; }
	public void setSHA( String pSHA ) { SHA = pSHA; }
	public void setSize( String pSize ) { Size = pSize; }
	public void setFolderID( String pFolderID ) { FolderID = pFolderID; }
	public void setStatus( EnumStatus pStatus ) { Status = pStatus; }
	public void setNote( String pNote ) { Note = pNote; }
	public void appendNote( String pNote ) {
		if ( Note == null )
			setNote( pNote );
		else
			Note += ", " + pNote;
	}
	public void setPath( FolderData[] pPath ) { Path = pPath; }
	public void setCreated( String pCreated ) { CreatedAt = pCreated; }

	public boolean isValid() { return FileID != INVALID; }
	public boolean isInvalid() { return FileID == INVALID; }

	public String getType() { return Type;	}
	public String getID() { return BoxFileID; }
	public String getVersionID() { return BoxFileVersionID; }
	public String getName() { return Name; }
	public String getFolderID() { return FolderID; }
	public EnumStatus getStatus() { return Status; }
	public FolderData[] getPath() { return Path; }
	public String getPathString() {

		StringBuilder bldr = new StringBuilder();

		if ( Path != null ) {
			for ( FolderData ld : Path ) {
				bldr.append(  "/" );
				bldr.append( ld.FolderName );
			}
		}

		return ( bldr.length() > 0 ? bldr.substring( 1 ) : "" );
	}
	public String getCreated() { return CreatedAt; }

	private boolean isDuplicate;

	public boolean isDuplicate() { return isDuplicate; }

	public void save() throws SQLException {

		isDuplicate = false;
		PreparedStatement s = dbFile.getInsertStatement();

		try {
			//	insert into Files (BoxFileID-1, BoxFileVersionID-2, Name-3, SHA-4, Type-5, FileSize-6, ParentFolderID-7, Note-8 )

			s.setString( 1, BoxFileID );
			s.setString( 2, BoxFileVersionID );			// BoxFileVersionID
			s.setString( 3, Name );
			s.setString( 4, SHA );
			s.setString( 5, Type );
			s.setString( 6, Size );
			s.setString( 7, FolderID );
			s.setString( 8, Status.name() );
			if ( Note != null )
				s.setString( 9, Note);
			else
				s.setNull( 9, java.sql.Types.VARCHAR );

			s.execute();
			int result = s.getUpdateCount();
			//System.out.printf( "Update Result: %d\n", result );

			ResultSet pk = s.getGeneratedKeys();
			if ( pk != null && pk.next() )
				FileID = pk.getInt( "FileID" );

		} catch ( SQLException sqx ) {

			if ( !dbFile.isDuplicateKeyException( sqx )) {
				System.out.println( s );
				FileID = INVALID;
				throw ( sqx );
			}

			isDuplicate = true;

			// do a select to get the PK
			s = dbFile.getSelectStatement( FILEBYBOXIDVERSIONID );
			s.setString( 1, BoxFileID );
			s.setString( 2, BoxFileVersionID );

			ResultSet rec = s.executeQuery();
			if ( rec.next() )
				FileID = rec.getInt( "FileID" );

		}
	}

	public void update() throws SQLException {

		PreparedStatement s = dbFile.getUpdateStatement( UPDATENAMEFOLDERSTATUSNOTE );
		s.setString( 1, Name );
		s.setString( 2, FolderID );
		s.setString( 3, Status.name() );
		s.setString( 4, Note );
		s.setInt( 5, FileID );

		int updated = s.executeUpdate();

	}

	public void syncFolderID() throws SQLException {

		// single sql call to overwrite FolderIDs for all versions
		PreparedStatement s = dbFile.getUpdateStatement( UPDATEFILEFOLDER );
		s.setString( 1, FolderID );
		s.setString( 2, BoxFileID );

		int updated = s.executeUpdate();
	}
}

