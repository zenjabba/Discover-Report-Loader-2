/*
 * From the Box API info provided by Kelsey Dutton (Box):
 * Admin Event Object
 *
 **   event_id - ID of the event in question
 **   source - Can be user (in case of collaboration or OAuth actions), or object (file or folder)
 **   item_id - Object Item ID
 **   item_name - Object name
 **   item_type - type of the object (file, folder, user, etc.)
 **   parent - details of the parent object (folder)
 **   created_by - person who created this event (not the object)
 **   name - name of object (file/folder) or person
 **   login - email ID of user who generated the event or of the user on whom the action was taken
 **   type - determines the kind of object being defined in the log (user, object or event)
 **   event_type - The type of action taken (upload, download, share, unshare, etc). For full list, see
 **   id - internal Box ID of the object (user or file or folder or comment, etc)
 **   session_id - a deprecated field that we no longer use. It was previously populated only by web app events but has since been removed. I have placed a request to remove the field entirely from the event object.
 **   additional_details - populated by collaboration events: COLLABORATION_INVITE, COLLABORATION_ROLE_CHANGE, COLLABORATION_REMOVE, populated with the following event attributes:  "type": "box://event/additional_details/collaboration","collab_id", "is_performed_by_admin", "role"
 **   ip_address - IP address of the user who created the event
 **   created_at - Time when the event was generated
 **   folder_id – unique Box folder ID
 **   collab_id – Box ID linked to the collaboration event
 **   role – role of the user (user/co-admin/admin)
 **   accessible_by – details of the recipient of a collaboration event (user object containing type, id, name, login)
 */
package com.efiia.discoveryreport.boxapi;

import com.efiia.discoveryreport.DRException;
import com.efiia.discoveryreport.data.EnumStatus;
import com.efiia.discoveryreport.data.EventData;
import com.efiia.discoveryreport.data.EventSuperData;
import com.efiia.discoveryreport.data.FileData;
import com.efiia.discoveryreport.data.FolderData;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author larry
 */
public class EventProcessor extends APIProcessor {

	private final static boolean DEBUG = false;

	/* Error Tracking */
	private final static int PROPFILECANTREAD = 2101;
	private final static int PROPFILECANTWRITE = 2102;
	private final static int PROPFILEREADERROR = 2103;
	private final static int PROPFILEWRITEERROR = 2104;
	private final static int EVENTPROCESSORSQLERROR = 2105;

	private final static int FILENOTKNOWNANYWHERERENAME = 2106;
	private final static int FILENOTKNOWNANYWHEREUNDELETE = 2107;

	ArrayList<EventData> EventQueue = new ArrayList<>( 500 );		// Box's Stream limits to 500

	String StreamPosition = "0";
	int BoxLimit = 500;				// max for this API call per docs
	int BoxChunkSize = 0;
	int ProcessedCtr = 0;
	int totCtr = 0;

	int UploadCtr = 0;
	int EditCtr = 0;
	int DownloadCtr = 0;
	int PreviewCtr = 0;
	int RenameCtr = 0;
	int DeleteCtr = 0;
	int UndeleteCtr = 0;
	int MoveCtr = 0;
	int UnknownEventCtr = 0;

	int TotalEventCtr = 0;
	int ErrorCtr = 0;

	FileInfoProcessor fip;
	FolderInfoProcessor lip;
	FileVersionInfoProcessor fvip;

	private File confFile;
	private Properties propsEvents;

	/* optimizators */
	HashSet<String> importUserID = new HashSet<>();
	HashSet<String> importFolderID = new HashSet<>();

	public EventProcessor( BoxConnection pConnection, File pConfigDir ) throws DRException {
		super( "events", pConnection );
		addQuery( "stream_type", "admin_logs" );
		addQuery( "event_type", "UPLOAD,PREVIEW,DOWNLOAD,EDIT,RENAME,DELETE,UNDELETE,MOVE" );
		addQuery( "limit", String.valueOf( BoxLimit ));		// BOX API Max is 500

		// check for config
		propsEvents = new Properties();

		confFile = new File( pConfigDir, "boxevents.config" );
		if ( confFile.exists() ) {
			// file is optional
			if ( !confFile.canRead() )
				throw new DRException( PROPFILECANTREAD, "EventProcessor:new", String.format( "Config File %s Not Found/Readable", confFile.toString()) );
			if ( !confFile.canWrite() )
				throw new DRException( PROPFILECANTWRITE, "EventProcessor:new", String.format( "Config File %s Not Found/Readable", confFile.toString()) );

			try (FileInputStream in = new FileInputStream(confFile )) {
				propsEvents.load( in );
			} catch ( IOException ex ) {
				throw new DRException( PROPFILEREADERROR, "EventProcess:new", "Read Property File", ex );
			}
		}
		String setPos = propsEvents.getProperty( "EventStreamPosition", "0" );
		setStreamPosition( setPos );

		fip = new FileInfoProcessor( pConnection );
		lip = new FolderInfoProcessor( pConnection );
		fvip = new FileVersionInfoProcessor( pConnection );

	}

	public void setStreamPosition( String pPos ) {
		if ( DEBUG )
			System.out.println( "Set Event Stream Position: " + pPos );
		StreamPosition = pPos;
		addQuery( "stream_position", StreamPosition );
	}

	public boolean hasMore() {
		return ( BoxChunkSize < BoxLimit ? false : true );
	}

	@Override
	public boolean postProcess() throws DRException {

		BoxChunkSize = (int)((long)MetaData.get( "chunk_size" ));

		// get the stream position and save
		StreamPosition = (String)MetaData.get( "next_stream_position" );
		addQuery( "stream_position", StreamPosition );

		// save config for later
		propsEvents.setProperty( "EventStreamPosition", StreamPosition );

		if ( false ) {
		try (FileOutputStream fos = new FileOutputStream( confFile )) {
			propsEvents.store( fos, "Updated from Box API" );
		} catch ( IOException ex ) {
			throw new DRException( PROPFILEWRITEERROR, "EventProcessor:PostProcess", ex);
		}

		// finally update the log
		System.out.printf( "%s updated with new event stream position: %s; at %s%n", confFile.getName(), StreamPosition, new Date().toString() );
		}

		ProcessedCtr = 0;

		return true;
	}

	public void WriteStats() {
		System.out.println( "Events Processed: ");
		System.out.printf( "     Upload: %,8d%n", UploadCtr );
		System.out.printf( "     Edited: %,8d%n", EditCtr );
		System.out.printf( "   Download: %,8d%n", DownloadCtr );
		System.out.printf( "  Previewed: %,8d%n", PreviewCtr );
		System.out.printf( "    Renamed: %,8d%n", RenameCtr );
		System.out.printf( "    Deleted: %,8d%n", DeleteCtr );
		System.out.printf( "  Undeleted: %,8d%n", UndeleteCtr );
		System.out.printf( "      Moved: %,8d%n", MoveCtr );
		if ( UnknownEventCtr > 0 )
			System.out.printf( "     Unknow: %,8d%n", UnknownEventCtr );
		System.out.printf("      Total: %,8d%n", TotalEventCtr );
		if ( ErrorCtr > 0 )
			System.out.printf("     Errors: %,8d%n", TotalEventCtr );
		System.out.println();
		System.out.println( "Database Stats:" );
		System.out.printf( "     New Users: %,4d%n", usersNew );
		System.out.printf( "   Known Users: %,4d%n", usersDuplicate );
		System.out.printf( "     New Files: %,4d%n", filesNew );
		System.out.printf( "   Known Files: %,4d%n", filesDuplicate );
		System.out.printf( "   New Folders: %,4d%n", foldersNew );
		System.out.printf( " Known Folders: %,4d%n", foldersDuplicate );
	}

	int filesNew = 0;
	int filesDuplicate = 0;
	int foldersNew = 0;
	int foldersDuplicate = 0;
	int usersNew = 0;
	int usersDuplicate = 0;

	/**
	 * Sample Data:
	 * event_id => 189458870
	 * event_type => UPLOAD
	 * created_at => 2015-03-02T19:27:25-08:00
	 * source =>
	 *   parent =>
	 *     name => 500
	 *     id => 3202388113
	 *     type => folder
	 *   item_id => 26929752555
	 *   item_type => file
	 *   item_name => xaaaaaaaaoc
	 * ip_address => 108.31.10.224
	 * type => event
	 * created_by =>
	 *   name => Dump User 1
	 *   id => 233432237
	 *   type => user
	 *   login => stephen.thompson.au+dump@gmail.com
	 *
	 * @param pKey
	 * @param pItem
	 * @return
	 * @throws com.efiia.discoveryreport.DRException
	 */
	@Override
	protected HashMap<String, Object> processArrayItem( String pKey, HashMap<String, Object> pItem ) throws DRException {

		try {

			if ( DEBUG ) {
				System.out.printf( ">>> Event [#%d]%n", ProcessedCtr, pKey );
				System.out.println( pItem.toString() );
				System.out.println( "-v-v-v-" );
			}
			//System.out.println("   " + pItem.toString() );

			EventSuperData esd = new EventSuperData( pItem );

			// dup checking belongs to the processor, not the data
			String eventUserID = esd.User.getBoxUserID();
			if ( !importUserID.contains( eventUserID )) {
				// never seen user, need to save them
				if ( DEBUG ) System.out.println( "=> New User: " + esd.User.toString() );
				esd.User.save();

				if ( esd.User.isDuplicate() )
					usersDuplicate++;
				else
					usersNew++;

				//if ( DEBUG ) System.out.println( "   UserID: " + esd.User.getUserID() );
				importUserID.add( eventUserID );
				//if ( DEBUG ) dumpItem( pItem, "User:" );
			}

			// dumpItem( pItem, "" );
			// process the event source
			if ( DEBUG )
				System.out.println( "Processing: " + esd.Event.toString() );
//			if ( esd.isFileEvent() && esd.File.getName().equals( "120.txt" ))
//				System.out.println( "120.txt Event" );
//			if ( esd.isFolderEvent() && esd.Folder.getName().equals( "Larry Folder A" ))
//				System.out.println( esd.Folder.getName() + " Event" );
			switch ( esd.Event.getEventType() ) {

				// a new file
				case "UPLOAD":
					UploadCtr++;
					if ( DEBUG )
						System.out.println( "UPLOAD:" + esd.getItem().toString() );

					FolderData[] filePath = null;
					// check if its a file or folder upload event
					if ( esd.isFileEvent() ) {

						// expand the file info
						FileData fd = fip.completeFileData( new FileData( esd.File ), esd.User );

						if ( fd.isInvalid() ) {
							// version not current
							fd = fvip.getFileVersionData( esd.File.getID(), esd.File.getVersionID(), esd.User );
						}

						if ( fd.isInvalid() ) {
							// nothing found
							break;
						}

						// kludge here for old versions without folders
						if ( fd.getFolderID() == null )
							fd.setFolderID( esd.File.getFolderID() );

						// save the file
						fd.save();
						if ( fd.isDuplicate() )
							filesDuplicate++;
						else
							filesNew++;

						// set up to save the folders
						filePath = fd.getPath();

					} else if ( esd.isFolderEvent() ) {
						// get the folder info
						// unfortunate for us, Box will return way more than we need
						// which we will ignore
						FolderData lx = lip.completeFolderData( new FolderData( esd.Folder ), esd.User );
						if ( lx.isInvalid())
							break;

						// save current folder
						if ( !importFolderID.contains( lx.getFolderID() )) {
							lx.save();
							if ( lx.isDuplicate() )
								foldersDuplicate++;
							else
								foldersNew++;
							importFolderID.add(  lx.getFolderID() );
						}

						// save path folders
						filePath = lx.getPath();
					}

					if ( filePath != null ) {
						for ( FolderData ld : filePath ) {
							String eventFolderID = ld.getFolderID();
							if ( importFolderID.contains( eventFolderID ))
								continue;
							ld.save();
							if ( ld.isDuplicate() )
								foldersDuplicate++;
							else
								foldersNew++;
							importFolderID.add(  eventFolderID );
						}
					}

					break;

				// untested
				// test for overwriting a file
				// folder overwrite is actually a delete and upload
				/* an EDIT event is actually when a new version of a file is put in place */
				case "EDIT":
					EditCtr++;
					if ( DEBUG )
						System.out.println( "EDIT:" + esd.getItem().toString() );
					if ( esd.isFileEvent() ) {

						// a new version is really just a new record in the file database
						// expand the file info
						FileData fd = fip.completeFileData( new FileData( esd.File ), esd.User );

						if ( fd == null ) {
							// version mismatch
							fd = fvip.getFileVersionData( esd.File.getID(), esd.File.getVersionID(), esd.User );
						}
						if ( fd == null ) {
							// nothing found
							break;
						}

						// save the file
						fd.save();
						if ( fd.isDuplicate() )
							filesDuplicate++;
						else
							filesNew++;

					}
					break;

				// untested
				case "PREVIEW":
					PreviewCtr++;
					if ( DEBUG )
						System.out.println( "PREVIEW:" + esd.getItem().toString() );
					if ( esd.isFileEvent() )
						ensureFileExists( esd );
					break;

				// untested
				case "DOWNLOAD":
					DownloadCtr++;
					if ( DEBUG )
						System.out.println( "DOWNLOAD:" + esd.getItem().toString() );
					if ( esd.isFileEvent() )
						ensureFileExists( esd );
					else if ( esd.isFolderEvent() )
						ensureFolderExists( esd );
					break;

				case "RENAME":
					RenameCtr++;
					if ( DEBUG )
						System.out.println( "RENAME:" + esd.getItem().toString() );
					// get the current name, stick "Renamed From:" in the Event Note
					if ( esd.isFileEvent() ) {
						FileData fd = ensureFileExists( esd );
						String note = "Renamed from: " + fd.getName();
						esd.Event.setNote( note );
						fd.setName( esd.File.getName() );
						fd.appendNote( note );
						fd.update();

					} else if ( esd.isFolderEvent() ) {
						FolderData ld = ensureFolderExists( esd );
						String note = "Renamed from: " + ld.getFolderName();
						esd.Event.setNote( note );
						ld.setFolderName( esd.Folder.getName() );
						ld.appendNote( note );
						ld.update();
					}
					// update the current item with the new name
					break;

				case "DELETE":
					DeleteCtr++;
					if ( DEBUG )
						System.out.println( "DELETE:" + esd.getItem().toString() );
					/* per box
					 * deleted files have no information available, so we have to
					 * depend upon whats already in the database
					 */
					if ( esd.isFileEvent() ) {
						FileData fd = ensureFileExists( esd );
						fd.setStatus( EnumStatus.Deleted );
						fd.update();

					} else if ( esd.isFolderEvent() ) {
						FolderData ld = ensureFolderExists( esd );
						ld.setStatus( EnumStatus.Deleted );
						ld.update();
					}
					break;

				// untested
				case "UNDELETE":
					UndeleteCtr++;
					if ( DEBUG )
						System.out.println( "UNDELETE:" + esd.getItem().toString() );
					// todo ensure that we have file info
					if ( esd.isFileEvent() ) {
						FileData fd = ensureFileExists( esd );
						fd.setStatus( EnumStatus.Active );
						fd.update();

					} else if ( esd.isFolderEvent() ) {
						FolderData ld = ensureFolderExists( esd );
						ld.setStatus( EnumStatus.Active );
						ld.update();
					}
					break;

				case "MOVE":
					MoveCtr++;
					if ( DEBUG )
						System.out.println( "MOVE:" + esd.getItem().toString() );
					// update the current and past versions with the new Parent
					if ( esd.isFileEvent() ) {
						// the event's file version first
						FileData fd = new FileData( esd.File.getID(), esd.File.getVersionID() );
						FolderData oldld = new FolderData( fd.getFolderID() );
						String note = "Moved from: " + oldld.getFolderName();
						esd.Event.setNote( note );
						fd.setFolderID( esd.File.getFolderID() );
						fd.appendNote( note );
						fd.update();
						// now sync the older folder ID's so that the database matches the current
						// folder layout
						if ( fd.getStatus() == EnumStatus.Active)
							fd.syncFolderID();

					} else if ( esd.isFolderEvent() ) {
						FolderData ld = new FolderData( esd.Folder.getID() );
						FolderData parentld = new FolderData( ld.getParentFolderID() );
						String note = "Moved from: " + parentld.getFolderName();
						esd.Event.setNote( note );
						ld.setParentFolderID( esd.Folder.getParentFolderID() );
						ld.appendNote( note );
						ld.update();
					}

					break;

				default:
					UnknownEventCtr++;
					// unknown event somehow...
					if ( DEBUG )
						System.out.println( "==> Unknown Event: " + esd.Event.getEventType() );
					break;
			}

			// finally, save the event
			if ( esd.Event != null )
				esd.Event.save();

			ProcessedCtr++;
			TotalEventCtr++;

		} catch ( SQLException sex ) {
			ErrorCtr++;
			if ( DEBUG ) {
				System.out.println( sex.toString() );
			}
			throw new DRException( EVENTPROCESSORSQLERROR, "EventProcessor:processArrayItem", sex);

		} catch ( Exception ex ) {
			ErrorCtr++;
			System.out.println( ex.toString() );
			dumpItem( pItem, pKey );
			throw ( ex );
		}
		return ( null );		// consume the object
	}

	/* old code to check on prior version information
	if ( esd.isFileEvent() ) {
		FileData fd = new FileData( esd.File.getID(), esd.File.getVersionID() );
		if ( fd.isInvalid() || !fd.getVersionID().equals( esd.File.getVersionID()) ) {
			// find older versions from Box
			if ( fvip == null )
				fvip = new FileVersionInfoProcessor( Connector );
			FileData[] fvd = fvip.getFileVersionData( esd.File.getID(), esd.User );
			if ( DEBUG ) {
				if ( fvd != null ) {
					System.out.println( "File Versions Found:");
					for( FileData fdx : fvd ) {
						System.out.println( fdx.toString() );
					}
				} else {
					System.out.println( "No File Versions Found for: " + esd.File.toString() );
				}
			}
		}
	} else if ( esd.isFolderEvent() ) {

	}

	*/

	private FileData getFile( EventSuperData pESD ) throws DRException {
		// check database
		FileData fd = new FileData( pESD.File.getID(), pESD.File.getVersionID() );

		// check current version on box that might of slipped by...
		if ( fd.isInvalid() )
			fd = fip.completeFileData( new FileData( pESD.File), pESD.User );

		// check history on box
		if ( fd.isInvalid() )
			fd = fvip.getFileVersionData( pESD.File.getID(), pESD.File.getVersionID(), pESD.User );

		return ( fd.isValid() ? fd : null );
	}

	private FolderData getFolder( EventSuperData pESD ) throws DRException {
		FolderData ld = new FolderData( pESD.Folder.getID() );		// database
		if ( ld.isInvalid() )
			ld = lip.completeFolderData( new FolderData( pESD.Folder ), pESD.User );
		return ( ld );
	}

	private FileData ensureFileExists( EventSuperData pESD ) throws DRException {
		// ensure we have file data to report against
		FileData fd = getFile( pESD );
		if ( fd == null ) {		// no prior knowledge
			fd = new FileData( pESD.File );
			try {
				fd.save();
			} catch ( SQLException sqx ) {

			}
		}
		return fd;
	}

	private FolderData ensureFolderExists( EventSuperData pESD ) throws DRException {
		// check database
		FolderData ld = getFolder( pESD );
		if ( ld.isInvalid() ) {
			// folder never seen before
			ld = new FolderData( pESD.Folder );
			try {
				ld.save();
			} catch ( SQLException sqx ) {

			}
		}
		return ( ld );
	}

	private void dumpItem( HashMap<String, Object> pItem, String pPrefix ) {
		for( Map.Entry<String, Object> tag : pItem.entrySet() ) {
			Object o = tag.getValue();
			if ( o instanceof String ) {
				System.out.printf( "%s%s => %s%n", pPrefix, tag.getKey(), (String)o );
			} else if ( o instanceof HashMap ) {
				System.out.printf( "%s%s =>%n", pPrefix, tag.getKey() );
				dumpItem((HashMap<String, Object>)o, pPrefix+"  " );
			} else if ( o instanceof ArrayList ) {

			}
		}
	}

}
