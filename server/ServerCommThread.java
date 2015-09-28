import java.net.*;
import java.io.*;
import java.util.Vector;

public class ServerCommThread extends Thread {
	private final int SENDING_BLOCKS = 4;
    private TcpComm _communication;
	private OutputStream _outputStream;
	private ServerProtocol _sp;
    private Vector<ServerProtocol> _protocols;
	
public ServerCommThread(Socket socket, Vector<AuctionInfo> auctions, Vector<AuctionProtocol> auctionProtocols, Vector<ServerProtocol> protocols, String serverName, Vector<ServerInfo> serversInfo, String usersFile) {		super("ServerCommThread");
		_communication = new TcpComm();
		_communication.setSocket(socket);
		_communication.initializeBuffers();
		_sp = new ServerProtocol(auctions, auctionProtocols, serverName, serversInfo, usersFile);
		_protocols = protocols;
		protocols.add(_sp);
		_sp.setCommunication(_communication);
    }

    public void run() {

      try {
          String inputLine, outputLine;

          while ((inputLine = _communication.receive()) != null) {
                outputLine = _sp.processInput(inputLine);
				System.out.println("###############################################");
				System.out.println("INPUT: " + inputLine);
				System.out.println("OUTPUT: " + outputLine);
				if(outputLine.equals("4" + Pdu.getSpacer() + "1")){     // se enviou launch ack com sucesso
					_communication.send(Pdu.stringToPdu(outputLine));
					_communication.receiveFile();
				} else if(outputLine.startsWith(Pdu.viewSuccess())){     // se enviou view ack sucesso
				    _communication.send(Pdu.stringToPdu(outputLine));
					System.out.println("Enviando ficheiro " + _sp.getLastFilename());
					_communication.sendFile(_sp.getLastFilename());
				} else if(outputLine.equals("insufficientBid")){		// Se o valor da bid foi insuficiente, nao fazer nada
				} else if(outputLine.equals("sentBid")){		// Redireccionar a Bid
				} else if(outputLine.startsWith("sentLaunch")){		// Redireccionar o cliente
					String[] temp = outputLine.split(" ");
					System.out.println("Enviar Launch para " + temp[1]);
					_communication.receiveFile();
				} else if(outputLine.equals("sendBlocks")){				// se tem de enviar blocos com listInfo
					Vector<AuctionInfo> listReply = _sp.getListReply(); 
					int count = 0;
					String listInfo = "9" + Pdu.getSpacer();
					while(_sp.getState() == SENDING_BLOCKS) {
						if(listReply.isEmpty()) {
							_communication.send(Pdu.stringToPdu(listInfo + 0));
							_sp.setState(1);
						}
						else {						
							AuctionInfo a = listReply.remove(0);
							listInfo += a.getHouseClass() + Pdu.getSpacer() + a.getCity() + Pdu.getSpacer() + a.getHouse() + Pdu.getSpacer() + a.getValue() + Pdu.getSpacer();
							if(++count == 3) {
								_communication.send(Pdu.stringToPdu(listInfo + 1));
								listInfo = "9" + Pdu.getSpacer();
								count = 0;
							}
						}
					}
				} else
					_communication.send(Pdu.stringToPdu(outputLine));
				// if (outputLine.equals("EOC"))
				//	  break;
            }
		_communication.closeConnections();
     } catch (IOException e) {
          //e.printStackTrace();
     }
   }
}
