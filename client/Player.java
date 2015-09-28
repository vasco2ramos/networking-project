import java.io.*;
import java.net.*;

public class Player {

	//Process o input do utilizador e devolve a pdu correspondente
	static char[] processInput(String input) {
		return Pdu.stringToPdu(input);
	}
	
    public static void main(String[] args) throws IOException {

		boolean verbose = false;
		FileReader fileReader = new FileReader("mDBs.dat");
		String localServerAddr = "Sl";
		int localServerPort = 20000;
		String userName = "user";
		String userPass = "pass";

		switch (args.length) {
			case 0:
				break;
			case 1:  
				if(args[0].equals("/v"))
					verbose = true;
				else 
					userName = args[0];
				break;
			case 2: 
				if(args[0].equals("/v")) {
					verbose = true;
					userName = args[1];
				} else {
					userName = args[0];
					userPass = args[1];
				}
				break;
			case 3:
				if(args[0].equals("/v")) {
					verbose = true;
					userName = args[1];
					userPass = args[2];
				} else {
					userName = args[0];
					userPass = args[1];
					localServerAddr = args[2];
				}
				break;
			case 4:
				if(args[0].equals("/v")) {
					verbose = true;
					userName = args[1];
					userPass = args[2];
					localServerAddr = args[3];
				} else {
					System.err.println("Argumentos errados: " + args[0] + ", " + args[1] + ", " + args[2] + ", " + args[3]);
					System.err.println("Verifique se queria escrever /v em vez de " + args[0]);
					System.exit(1);
				}
				break;
			default: 
				System.err.println("Numero de argumentos errado: esperados 4 ou menos, inseridos " + args.length);
				System.exit(1);
				break;
		}
		
		fileReader.setVerbose(verbose);
		
		try {
			String[] hostAddrPort = fileReader.getHostFromFile(localServerAddr);
			if(!hostAddrPort[1].equals("")) {
				localServerAddr = hostAddrPort[0];
				localServerPort = Integer.parseInt(hostAddrPort[1]);
			}
		} catch (IOException e) {
            System.err.println("Couldn't get I/O for the serverfile. Server: " + localServerAddr);
            System.exit(1);
        }

        String fromServer;
        String fromUser;

		System.out.println("Para terminar o programa, use o comando \"Quit\".");
		System.out.println("\t\t\t### Aguardando comando ###");
		
		//Classe que contem metodos de comunicacao com os servidores
		TcpComm communication = new TcpComm();
		// Classe que contem o estado do cliente
		PlayerProtocol protocol = new PlayerProtocol(communication, localServerAddr, localServerPort, userName, userPass, verbose);
		// Comecar a ler da consola
		PlayerInput playerInput = new PlayerInput(communication, protocol);
		playerInput.start();
		// Classe que invoca os metodos de comunicacao presentes em "communication"
		while(!communication.isConnected())
			System.out.print("");  //do nothing
		PlayerToServer playerToServer = new PlayerToServer(communication, protocol);
		playerToServer.start();
    }
}
