
package com.efiia.discoveryreport.boxapi;

import com.efiia.discoveryreport.data.UserData;
import java.util.Map;

/**
 *
 * @author larry
 */
public class CurrentUserProcessor extends APIProcessor {

	UserData ld;	// = new UserData();

	public CurrentUserProcessor( BoxConnection pConnection ) {
		super( "users/me", pConnection );
	}

	@Override
	public boolean postProcess() {
		ld = new UserData( this.MetaData );
		return true;
	}

	public UserData getUser() {
		return ( ld );
	}
}
