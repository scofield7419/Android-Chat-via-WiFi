package com.example.sample72;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;

import android.util.Log;

public class WiFiCommUtils {
	/**
	 * useless
	 * 
	 * @return
	 */
	public static String getLocalIPAddress ( ) {
		try {
			for ( Enumeration < NetworkInterface > en = NetworkInterface.getNetworkInterfaces ( ) ; en.hasMoreElements ( ) ; ) {
				NetworkInterface intf = en.nextElement ( );
				for ( Enumeration < InetAddress > enumIpAddr = intf.getInetAddresses ( ) ; enumIpAddr.hasMoreElements ( ) ; ) {
					InetAddress inetAddress = enumIpAddr.nextElement ( );
					if ( ! inetAddress.isLoopbackAddress ( ) ) {
						return inetAddress.getHostAddress ( ).toString ( );
					}
				}
			}
		} catch ( SocketException ex ) {
			Log.e ( "wifi" , ex.toString ( ) );
		}
		return null;
	}
	
	public static String ip2string ( int i ) {
		return ( i & 0xFF ) + "." + ( ( i >> 8 ) & 0xFF ) + "." + ( ( i >> 16 ) & 0xFF ) + "." + ( ( i >> 24 ) & 0xFF );
		
	}
	
	public static String ip2string ( String ip ) {
		int i = Integer.parseInt ( ip );
		return ( i & 0xFF ) + "." + ( ( i >> 8 ) & 0xFF ) + "." + ( ( i >> 16 ) & 0xFF ) + "." + ( ( i >> 24 ) & 0xFF );
		
	}
	
	public static String getRandomNumber ( ) {
		int num = new Random ( ).nextInt ( 65536 );
		String numString = String.format ( "x" , num );
		return numString;
	}
	
}
