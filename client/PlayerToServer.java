import java.io.*;
import java.net.*;

public class PlayerToServer extends Thread {

	private PlayerProtocol _protocol;
	private TcpComm _communication;

	public PlayerToServer(TcpComm communication, PlayerProtocol protocol) {
		super();
		_communication = communication;
		_protocol = protocol;
	}
	public PlayerToServer() {
		super();
	}

	public void run(){
		String fromServer;
		try{
		
			while((fromServer = _communication.receive()) != null) {
				_protocol.processFromServer(fromServer);
				if(!_protocol.isBusy()) {
					String toSend = _protocol.popCommand();
					if(toSend != null)	{
						_communication.send(_protocol.processToServer(toSend));
						_protocol.setBusy(true);
					}
				}
				/*try {
					this.sleep(10);
				} catch (InterruptedException e) {}*/
			}
		} catch (IOException e) {
				System.err.println("Couldn't get I/O for the connection to local server.");
				_protocol.setState(0);
				_communication.setConnected(false);
				//System.exit(1);
		}
	}
	
}