import java.net.*;
import java.io.*;
import java.util.Vector;

public class Auction {
    public static void main(String[] args) throws IOException { 
		String serverName = "Sl";
		Vector<ServerInfo> serversInfo;
        ServerSocket serverSocket = null;
        boolean listening = true;
        String citiesFile = "cities.dat";
		String usersFile = "users.dat";
		int port = 20000;
		Vector<AuctionInfo> auctions = new Vector<AuctionInfo>(5,1);
		Vector<AuctionProtocol> auctionProtocols = new Vector<AuctionProtocol>(5,1);
		Vector<ServerProtocol> protocols = new Vector<ServerProtocol>(4,1);

		switch (args.length) {
			case 0:
				break;
			case 1:  
				port = Integer.parseInt(args[0]);
				break;
			case 2: 
				port = Integer.parseInt(args[0]);
				citiesFile = args[1];
				break;
			case 3:
				port = Integer.parseInt(args[0]);
				citiesFile = args[1];
				usersFile = args[2];
				break;
			default: 
				System.err.println("Wrong number of arguments: expected 3 or less, got " + args.length);
				System.exit(1);
				break;
		}

		FileReader fileReader = new FileReader(citiesFile);
		serversInfo = fileReader.getServerCities();

		new UdpCommThread(port, auctions, auctionProtocols, protocols, serversInfo, serverName).start();

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 20000.");
            System.exit(-1);
        }

        while (listening)
			new ServerCommThread(serverSocket.accept(), auctions, auctionProtocols, protocols, serverName, serversInfo, usersFile).start();
			
        serverSocket.close();
    }
}
