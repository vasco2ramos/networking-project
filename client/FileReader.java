import java.io.*;
import java.util.Scanner;
import java.util.Vector;

public final class FileReader {
	
	private String _fileName;
	private boolean _verbose = false;
	
	public void setVerbose(boolean verbose) {
		_verbose = verbose;
	}
	
	/** Devolve um vector de arrays de strings com as cidades que pertencem a cada servidor */
	public Vector<ServerInfo> getServerCities() throws IOException {
		Vector<ServerInfo> answer = new Vector<ServerInfo>(5,1);
		StringBuilder text = new StringBuilder();
		Scanner scanner = new Scanner(new FileInputStream(_fileName));
		Vector<String> cities = new Vector<String>(5,1);
		String serverName = null;
		String NL = System.getProperty("line.separator");
		try {
			String next;
			while (scanner.hasNext()){
				next = scanner.next();
				if(serverName == null) {
					serverName = next;
					continue;
				}
				if(next.equals("|")) {
					answer.add(new ServerInfo(serverName, cities));
					cities = new Vector<String>(5,1);
					serverName = null;
				} else {
					cities.add(next);
				}
			}
		}
		finally{
			scanner.close();
		}
		return answer;
	}
	
	/** Devolve um array de strings em que o primeiro e o address e o segundo e a port  */
	public String[] getHostFromFile(String hostName) throws IOException {
		if(_verbose)
			System.out.println("Reading server from file.");
		StringBuilder text = new StringBuilder();
		String NL = System.getProperty("line.separator");
		Scanner scanner = new Scanner(new FileInputStream(_fileName));
		String[] hostAddressPort = new String[2];
		hostAddressPort[0] = "";
		hostAddressPort[1] = "";
		
		try {

			while (scanner.hasNext()){
				if(scanner.next().equals(hostName)) {
					hostAddressPort[0] = scanner.next();
					hostAddressPort[1] = scanner.next();
					break;
				} else
					scanner.next();
					scanner.next();
			}
			if(_verbose)
				System.out.println("Read Server: " + hostAddressPort[0] + " on port " + hostAddressPort[1]);
		}
		finally{
			scanner.close();
		}
		return hostAddressPort;
	}

	/** Devolve um array de strings em que o primeiro e o address e o segundo e a port  */
	public boolean userPassCorrect(String userName, String userPass) {
		System.out.println("Reading userpass from file.");
		StringBuilder text = new StringBuilder();
		try {
			Scanner scanner = new Scanner(new FileInputStream(_fileName));	
			while (scanner.hasNext()){
				if(scanner.next().equals(userName)) {
					if(scanner.next().equals(userPass)) {
						return true;
					}
				} else
					System.out.println(" Pass " + scanner.next());
			}
			scanner.close();
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the file: " + _fileName);
			System.exit(1);
		}
		return false;
	}
	

	/** Construtor. */
	FileReader(){
		_fileName = "";
	}	
	
	/** Construtor. */
	FileReader(String fileName){
		_fileName = fileName;
	}

}
