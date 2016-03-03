package com.efiia.discoveryreport;

/**
 *
 * @author larry
 */
public final class DRException extends Exception {

	private final static boolean DEBUG = false;

	int ErrorCode;
	int SubErrorCode;
	String Module;
	String Info;
	String Extra[];

	String ResponseMessage;

	public DRException( int pErrorCode, String pModule, Exception pEx ) {
		this( pErrorCode, pModule, 0, pEx.getMessage(), null );
		initCause( pEx );
		if ( DEBUG ) print();
	}

	public DRException( int pErrorCode, String pModule, String pMessage, Exception pEx ) {
		this( pErrorCode, pModule, 0, pMessage, pEx.getMessage() );
		initCause( pEx );
		if ( DEBUG ) print();
	}

	public DRException( int pErrorCode, String pModule, String pMessage ) {
		this( pErrorCode, pModule, 0, pMessage, null );
		if ( DEBUG ) print();
	}

	public DRException( int pErrorCode, String pModule, String pMessage, String pInfo ) {
		this( pErrorCode, pModule, 0, pMessage, pInfo );
		if ( DEBUG ) print();
	}

	public DRException( int pErrorCode, String pModule, int pSubErrorCode, String pMessage, String pInfo ) {
		super( pMessage );
		ErrorCode = pErrorCode;
		Module = pModule;
		Info = pInfo;
		ResponseMessage = null;
		Extra = null;
	}

	public void setResponseMessage( String pResponse ) {
		ResponseMessage = pResponse;
	}

	public String getResponseMessage() {
		return ( ResponseMessage );
	}

	public void setExtra( String[] pExtra ) {
		Extra = pExtra;
	}

	public String[] getExtra() {
		return ( Extra );
	}

	void print() {

		System.out.print( "--- Discovery Report Error in Module: " );
		System.out.print( Module );
		System.out.print( ", Error ID: ");
		System.out.println( ErrorCode );

		if ( SubErrorCode > 0 ) {
			System.out.print( " - Response Error: ");
			System.out.println(  SubErrorCode );
		}

		System.out.print( " - Message: " );
		System.out.println( getMessage() );

		if ( Info != null ) {
			System.out.print( "      Info: " );
			System.out.println( Info );
		}

		if ( Extra != null ) {
			String xh = "    Extra: ";
			for ( String x : Extra ) {
				System.out.print( xh );
				System.out.println( x );
				xh = "         : ";
			}
		}

		Throwable cause = getCause();
		if ( cause != null ) {
			System.out.println( " - Original Cause:" );
			cause.printStackTrace( System.out );
		}

	}
}
