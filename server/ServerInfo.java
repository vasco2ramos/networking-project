import java.io.*;
import java.util.Vector;

public final class ServerInfo {
	private Vector<String> _cities;
	private String _serverName;
	private int _serverPort;
	private String _serverAddr;
	
	public ServerInfo() {
	}
	
	public ServerInfo(String serverName, Vector<String> cities) {
		_cities = cities;
		_serverName = serverName;
		FileReader fileReader = new FileReader("mDBs.dat");
		try {
			String[] hostAddrPort = fileReader.getHostFromFile(_serverName);
			if(!hostAddrPort[1].equals("")) {
				_serverAddr = hostAddrPort[0];
				_serverPort = Integer.parseInt(hostAddrPort[1]);
			}
		} catch(IOException ioe) {
			System.out.println(ioe.toString());
		}
	}
	
	public boolean managesCity(String city) {
		return _cities.contains(city);
	}	
	
	public String getServerName() {
		return _serverName;
	}
	
	public String getServerAddr() {
		return _serverAddr;
	}
	
	public int getServerPort() {
		return _serverPort;
	}
	
}