import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;

public class UdpComm{
	
	private Vector<Integer> _portVector;
	private Vector<ServerInfo> _serversInfo;
	private ServerInfo _thisServerInfo;
	
	public UdpComm(Vector<ServerInfo> serversInfo, ServerInfo thisServerInfo) {
		findPorts(serversInfo);
		_serversInfo = serversInfo;
		_thisServerInfo = thisServerInfo;
	}
	
	public UdpComm() {
	}

	public void findPorts (Vector<ServerInfo> serversInfo) {
		_portVector = new Vector<Integer>(3 ,1);
		for(ServerInfo a : serversInfo) {
			if(! _portVector.contains(a.getServerPort()))
				_portVector.add(a.getServerPort());
		}
	}
	
	//Broadcasts the given pdu to all the servers.
    
    public void broadcastPdu(String pdu){
		byte[] buf = new byte[256];
		String dString = null;
		buf = pdu.getBytes();
		for(ServerInfo s : _serversInfo) {	
			if(!s.getServerName().equals(_thisServerInfo.getServerName())) {
				transmitPdu(pdu, s.getServerAddr(), s.getServerPort());
			}
		}
	}
	
	//Transmits the given pdu to the server with the given adress.
	
	public void transmitPdu(String pdu, String address, int port){ 
        try {
				InetAddress inetAddress = InetAddress.getByName(address);
				DatagramSocket socket  = new DatagramSocket();
				byte[] buf = new byte[256];
                buf = pdu.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, inetAddress, port);
                socket.send(packet);
                socket.close();
			} catch (UnknownHostException e) {
				System.out.println("");
			} catch (IOException e) {
                e.printStackTrace();
                System.out.println("Erro no transmit");
			}
	}
}