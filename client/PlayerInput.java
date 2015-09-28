import java.io.*;
import java.net.*;

public class PlayerInput extends Thread {
	private BufferedReader _input;
	private String _localServer;
	private PlayerProtocol _protocol;
	private TcpComm _communication;
	
	public PlayerInput(TcpComm communication, PlayerProtocol protocol) {
		super();
		_communication = communication;
		_protocol = protocol;
	}
	public PlayerInput() {
		super();
		
	}
	
	public void run(){
		// Obter buffer para ler a partir da consola
        try {
			_input = new BufferedReader(new InputStreamReader(System.in));		
			String fromUser;
			while ((fromUser = _input.readLine()) != null) {
				if(fromUser.startsWith("Launch") || 
				   fromUser.startsWith("List") ||
				   fromUser.startsWith("Bid") ||
				   fromUser.startsWith("View") ||
				   fromUser.startsWith("Quit")) {
		
				    if(!_communication.isConnected())
						_protocol.setBusy(false);
					if(_protocol.isBusy())
						_protocol.pushCommand(fromUser);
					else{
						if(_protocol.getState() == 0) {
							_communication.send(_protocol.processToServer("1 " + _protocol.getUserName() + " " + _protocol.getUserPass()));
							_protocol.setBusy(true);
							_protocol.pushCommand(fromUser);
						} else {
							_communication.send(_protocol.processToServer(fromUser));
							_protocol.setBusy(true);
						}
					}
				}
				else 
					System.out.println("Comando nao reconhecido");

				System.out.println("\t\t\t### Aguardando comando ###");
			}
			_input.close(); 
		 } catch (IOException e) {
			System.err.println("Couldn't get input from console");
			System.exit(1);
		}
	}
	
}