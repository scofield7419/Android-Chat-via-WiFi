package com.example.sample72;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	public static final int DEFAULT_PORT_HAND1 = 43210;
	public static final int DEFAULT_PORT_HAND2 = 43012;
	public static final int DEFAULT_PORT_SOCKET = 33333;
	public static final int MAX_DATA_PACKET_LENGTH = 1024;
	public static final int SOCKET_TYPE_SERVER = 0;
	public static final int SOCKET_TYPE_CLIENT = 1;
	
	public static final String SECOND_INFORM = "second";
	
	ToggleButton toggle_broaddatagram;
	ToggleButton toggle_permitsearch;
	
	public static boolean start = false;
	boolean isReceived = false;
	boolean fromFirstHand = true;
	
	TextView ip_received;
	TextView ip_selfIP;
	TextView conn_info;
	Button btn_enter;
	
	String receivedIP;
	String receivedData;
	
	Handler mHandler = new Handler ( ) {
		public void handleMessage ( android.os.Message msg ) {
			switch ( msg.what ) {
				case 0x123 :
					// 显示得到的对方的IP
					System.out.println ( "ip_received````````````````````" + receivedIP );
					ip_received.setText ( "好友IP：" + receivedIP );
					
					if ( fromFirstHand ) {
						// 关闭“接受搜索”
						toggle_permitsearch.setChecked ( false );
						// 告诉对方，我已经收到了，并返回给你我的IP
						startSearchSend ( DEFAULT_PORT_HAND2 , SECOND_INFORM );
						fromFirstHand = false;
						// 允许 发射器发
						start = true;
					} else {
						// 关闭“发送搜索”
						toggle_broaddatagram.setChecked ( false );
					}
					break;
				case 0x124 :
					AlertDialog.Builder builder1 = new AlertDialog.Builder ( MainActivity.this );
					builder1.setTitle ( "提示" );
					builder1.setMessage ( "是否开启服务端？" );
					builder1.setPositiveButton ( "确定" , new OnClickListener ( ) {
						public void onClick ( DialogInterface dialog , int which ) {
							startServerSocketConn ( );
							conn_info.setText ( "已开启服务端，等待对方连接" );
						}
					} );
					builder1.setNegativeButton ( "取消" , null );
					builder1.create ( );
					builder1.show ( );
					break;
				case 0x125 :
					AlertDialog.Builder builder2 = new AlertDialog.Builder ( MainActivity.this );
					builder2.setTitle ( "提示" );
					builder2.setMessage ( "是否连接服务端？" );
					builder2.setPositiveButton ( "确定" , new OnClickListener ( ) {
						public void onClick ( DialogInterface dialog , int which ) {
							startClientSocketConn ( );
							conn_info.setText ( "已连接到服务器" );
							btn_enter.setVisibility ( View.VISIBLE );
						}
					} );
					builder2.setNegativeButton ( "取消" , null );
					builder2.create ( );
					builder2.show ( );
					break;
				case 0x126 :
					AlertDialog.Builder dialog = new AlertDialog.Builder ( MainActivity.this );
					dialog.setTitle ( "服务端提示" );
					dialog.setMessage ( "已接收到一个聊天请求，是否接受并进入聊天?" );
					dialog.setPositiveButton ( "进入" , new OnClickListener ( ) {
						public void onClick ( DialogInterface dialog , int which ) {
							Intent intent = new Intent ( MainActivity.this , ChatActivity.class );
							intent.putExtra ( "socketType" , SOCKET_TYPE_SERVER );
							MainActivity.this.startActivityForResult ( intent , 0x888 );
						}
					} );
					dialog.setNegativeButton ( "取消" , new DialogInterface.OnClickListener ( ) {
						public void onClick ( DialogInterface dialog , int which ) {
							// TODO 关闭 socket
						}
					} );
					dialog.show ( );
					break;
				default :
					break;
			}
		}
	};
	
	long timer = 0;
	
	@ Override
	public void onBackPressed ( ) {
		if ( ( System.currentTimeMillis ( ) - timer ) <= 1500 ) {
			mainIsAlive = false;
			finish ( );
		} else {
			Toast.makeText ( MainActivity.this , "按两次退出!" , Toast.LENGTH_SHORT ).show ( );
			timer = System.currentTimeMillis ( );
		}
		// super.onBackPressed ( ); 要想截断事件，必须不能有这条。
	}
	
	@ Override
	protected void onActivityResult ( int requestCode , int resultCode , Intent data ) {
		if ( resultCode != RESULT_OK ) {
			return;
		}
		if ( requestCode == 0x888 ) {
			if ( data.getIntExtra ( "result" , 0 ) == - 1 ) {
				// TODO 显示异常
			}
		} else if ( requestCode == 0x999 ) {
			if ( data.getIntExtra ( "result" , 0 ) == - 1 ) {
				// TODO 显示异常
			}
		}
		
		super.onActivityResult ( requestCode , resultCode , data );
	}
	
	boolean mainIsAlive;
	
	@ Override
	protected void onDestroy ( ) {
		mainIsAlive = false;
		super.onDestroy ( );
	}
	
	private void startServerSocketConn ( ) {
		new ServerSocketThread ( ).start ( );
	}
	
	private void startClientSocketConn ( ) {
		new ClientSocketThread ( ).start ( );
	}
	
	public static boolean isChatting;
	
	WifiManager mWifiManager;
	WifiManager.MulticastLock lock;
	
	@ Override
	protected void onCreate ( Bundle savedInstanceState ) {
		super.onCreate ( savedInstanceState );
		setContentView ( R.layout.activity_main );
		setTitle ( "WiFi Communication" );
		mainIsAlive = true;
		mWifiManager = ( WifiManager ) getSystemService ( Context.WIFI_SERVICE );
		lock = mWifiManager.createMulticastLock ( "test wifi" );
		initView ( );
	}
	
	private void initView ( ) {
		btn_enter = ( Button ) findViewById ( R.id.btn_enter );
		btn_enter.setOnClickListener ( new View.OnClickListener ( ) {
			public void onClick ( View v ) {
				AlertDialog.Builder dialog = new AlertDialog.Builder ( MainActivity.this );
				dialog.setTitle ( "客户端提示" );
				dialog.setMessage ( "确认进入聊天?" );
				dialog.setPositiveButton ( "进入" , new OnClickListener ( ) {
					public void onClick ( DialogInterface dialog , int which ) {
						Intent intent2 = new Intent ( MainActivity.this , ChatActivity.class );
						intent2.putExtra ( "socketType" , SOCKET_TYPE_CLIENT );
						MainActivity.this.startActivityForResult ( intent2 , 0x999 );
					}
				} );
				dialog.setNegativeButton ( "取消" , new DialogInterface.OnClickListener ( ) {
					public void onClick ( DialogInterface dialog , int which ) {
						// TODO 关闭 socket
					}
				} );
				dialog.show ( );
			}
		} );
		conn_info = ( TextView ) findViewById ( R.id.conn_info );
		ip_selfIP = ( TextView ) findViewById ( R.id.ip_selfIP );
		ip_received = ( TextView ) findViewById ( R.id.ip_received );
		toggle_permitsearch = ( ToggleButton ) findViewById ( R.id.toggle_permitsearch );
		toggle_permitsearch.setOnCheckedChangeListener ( new OnCheckedChangeListener ( ) {
			public void onCheckedChanged ( CompoundButton buttonView , boolean isChecked ) {
				if ( ! checkWiFiIsOpen ( ) ) {
					Toast.makeText ( MainActivity.this , "未打开WiFi\n请确保双方处于同一局域网下!" , Toast.LENGTH_SHORT ).show ( );
					toggle_permitsearch.setChecked ( false );
					return;
				}
				if ( isChecked ) {
					// lock.acquire ( );
					isReceived = false;
					startCheckReceived ( DEFAULT_PORT_HAND1 );
					// fromFirstHand = false;
					toggle_broaddatagram.setClickable ( false );
				} else {
					// lock.release ( );
					isReceived = true;
					toggle_broaddatagram.setClickable ( true );
				}
			}
			
		} );
		toggle_broaddatagram = ( ToggleButton ) findViewById ( R.id.toggle_broaddatagram );
		toggle_broaddatagram.setOnCheckedChangeListener ( new OnCheckedChangeListener ( ) {
			public void onCheckedChanged ( CompoundButton buttonView , boolean isChecked ) {
				if ( ! checkWiFiIsOpen ( ) ) {
					Toast.makeText ( MainActivity.this , "未打开WiFi\n请确保双方处于同一局域网下!" , Toast.LENGTH_SHORT ).show ( );
					toggle_broaddatagram.setChecked ( false );
					return;
				}
				if ( isChecked ) {
					// lock.acquire ( );
					
					// 进行第一次握手
					start = true;
					startSearchSend ( DEFAULT_PORT_HAND1 , WiFiCommUtils.ip2string ( mWifiManager.getConnectionInfo ( ).getIpAddress ( ) ) );
					fromFirstHand = true;
					
					// 为第二次握手打开接收器
					isReceived = false;
					startCheckReceived ( DEFAULT_PORT_HAND2 );
					toggle_permitsearch.setClickable ( false );
				} else {
					// lock.release ( );
					start = false;
					toggle_permitsearch.setClickable ( true );
				}
			}
			
		} );
	}
	
	private boolean checkWiFiIsOpen ( ) {
		int status = mWifiManager.getWifiState ( );
		switch ( status ) {
			case WifiManager.WIFI_STATE_DISABLED :
				
				return false;
				
			case WifiManager.WIFI_STATE_ENABLED :
				WifiInfo wifiInfo = mWifiManager.getConnectionInfo ( );
				if ( wifiInfo == null ) {
					return false;
				} else {
					ip_selfIP.setText ( "我的IP：" + WiFiCommUtils.ip2string ( wifiInfo.getIpAddress ( ) ) );
					return true;
				}
				
			default :
				break;
		}
		return false;
	}
	
	private void startCheckReceived ( int port ) {
		new ReceivedThread ( port ).start ( );
		
	}
	
	private void startSearchSend ( int port , String str ) {
		new BroadCastUdpThread ( port , str ).start ( );
		
	}
	
	/**
	 * 接受UDP广播数据包（组播也可以达到目的）
	 * 
	 * @author scott
	 * 
	 */
	class ReceivedThread extends Thread {
		private DatagramSocket udpSocket;
		byte [ ] data = new byte [ MainActivity.MAX_DATA_PACKET_LENGTH ];
		private DatagramPacket udpPacket = new DatagramPacket ( data , MainActivity.MAX_DATA_PACKET_LENGTH );
		int port;
		
		ReceivedThread ( int port ) {
			this.port = port;
		}
		
		public void run ( ) {
			try {
				udpSocket = new DatagramSocket ( port );
			} catch ( SocketException e ) {
				e.printStackTrace ( );
			}
			while ( ! isReceived ) {
				
				try {
					udpSocket.receive ( udpPacket );
				} catch ( Exception e ) {
					e.printStackTrace ( );
				}
				
				if ( ! udpPacket.getData ( ).toString ( ).equals ( "" ) ) {
					InetAddress address = udpPacket.getAddress ( );
					String receiStr = new String ( data , 0 , udpPacket.getLength ( ) );
					String IPaddress = address.getHostAddress ( ).toString ( );
					receivedIP = IPaddress;
					// lock.release ( );
					if ( receiStr.equals ( SECOND_INFORM ) ) {
						start = false;
						fromFirstHand = false;
						mHandler.sendEmptyMessage ( 0x125 );
					} else {
						mHandler.sendEmptyMessage ( 0x124 );
					}
					
					mHandler.sendEmptyMessage ( 0x123 );
					isReceived = true;
				}
			}
		}
		
	}
	
	public static Socket socketFromClient = null;
	public static Socket socketFromServer = null;
	
	class ServerSocketThread extends Thread {
		ServerSocket serverSocket;
		boolean shouldLoop = true;
		
		@ Override
		public void run ( ) {
			try {
				serverSocket = new ServerSocket ( DEFAULT_PORT_SOCKET );
			} catch ( IOException e ) {
				e.printStackTrace ( );
			}
			while ( shouldLoop ) {
				try {
					socketFromServer = serverSocket.accept ( );
					shouldLoop = false;
				} catch ( IOException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace ( );
				}
			}
			
			mHandler.sendEmptyMessage ( 0x126 );
		}
	}
	
	class ClientSocketThread extends Thread {
		@ Override
		public void run ( ) {
			try {
				socketFromClient = new Socket ( receivedIP , DEFAULT_PORT_SOCKET );
			} catch ( UnknownHostException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace ( );
			} catch ( IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace ( );
			}
			
		}
	}
	
	/**
	 * UDP广播数据包（组播也可以达到目的）
	 * 
	 * @author scott
	 * 
	 */
	class BroadCastUdpThread extends Thread {
		private String dataString;
		private DatagramSocket udpSocket;
		private DatagramPacket dataPacket = null;
		
		private byte [ ] buffer = new byte [ MainActivity.MAX_DATA_PACKET_LENGTH ];
		
		int port;
		
		public BroadCastUdpThread ( int port , String dataString ) {
			this.dataString = dataString;
			this.port = port;
		}
		
		public void run ( ) {
			
			try {
				udpSocket = new DatagramSocket ( port );
				
				dataPacket = new DatagramPacket ( buffer , MainActivity.MAX_DATA_PACKET_LENGTH );
				byte [ ] data = dataString.getBytes ( );
				dataPacket.setData ( data );
				dataPacket.setLength ( data.length );
				dataPacket.setPort ( port );
				
				InetAddress broadcastAddr = InetAddress.getByName ( "255.255.255.255" );
				dataPacket.setAddress ( broadcastAddr );
			} catch ( Exception e ) {
				Log.e ( "" , e.toString ( ) );
				e.printStackTrace ( );
			}
			while ( start ) {
				try {
					udpSocket.send ( dataPacket );
					sleep ( 100 );
				} catch ( Exception e ) {
					Log.e ( "" , e.toString ( ) );
					e.printStackTrace ( );
				}
			}
			try {
				udpSocket.close ( );
			} catch ( Exception e ) {
				Log.e ( "" , e.toString ( ) );
				e.printStackTrace ( );
			}
		}
	}
}
