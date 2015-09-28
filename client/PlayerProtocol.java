import java.net.*;
import java.io.*;
import java.util.*;

public class PlayerProtocol {
    private static final int DISCONNECTED_IDLE = 0;
    private static final int CONNECTED_IDLE = 1;
    private static final int WAIT_LAUNCH_ACK = 2;
    private static final int WAIT_LOCALSERVER_RESPONSE = 3;
    private static final int WAIT_BID_ACK = 4;
    private static final int WAITING_QUIT = 5;
	private TcpComm _communication;
	
	private int _pduId;
	private String _localServerAddr, _userName, _userPass;
	private int _localServerPort;
    private int _state;
	private String _lastFilename;
	private Stack<String> _commands;
	private boolean _busy;			// Indica se o cliente esta ocupado a espera de resposta a um pedido
	private boolean _verbose;		// Indica se o cliente devera dar detalhes das suas accoes na consola
	private Timer _tConfirmMaxBid;
	private Timer _tConfirmMaxLaunch;
	private char[] _lastViewRequest;
	
	public PlayerProtocol() {
	}
	
	public PlayerProtocol(TcpComm communication, String localServerAddr, int localServerPort, String userName, String userPass, boolean verbose) {
		_communication = communication;
		_localServerAddr = localServerAddr;
		_localServerPort = localServerPort;
		_userName = userName;
		_userPass = userPass;
		_commands = new Stack<String>();
		_busy = false;
		_verbose = verbose;
		_state = DISCONNECTED_IDLE;
	}
	
	public boolean isBusy(){
		return _busy;
	}
	
	public void setBusy(boolean busy){
		_busy = busy;
	}
	
	public String getUserName() {
		return _userName;
	}	
	
	public String getUserPass() {
		return _userPass;
	}	
	
	public String popCommand(){
		if(!_commands.empty()) {
			String command = _commands.pop();
			if(_verbose)
				System.out.println("O cliente esta disponivel para executar o proximo comando: " + command);
			return command;
		}
		return null;
	}
	
	public void pushCommand(String command){
		_commands.push(command);
	}
	
	public int getState(){
		return _state;
	}
	
	public void setState(int state){
		_state = state;
	}
	
	// Obtem a partir do ficheiro o address e port do server indicado
	private String[] getHostAddrPort(String localServerName) {
		String localServerAddr;
		int localServerPort;
		String[] hostAddrPort = new String[2];
		try {
			FileReader fileReader = new FileReader("mDBs.dat");
			hostAddrPort = fileReader.getHostFromFile(localServerName);
			if(!hostAddrPort[1].equals("")) {
				localServerAddr = hostAddrPort[0];
				localServerPort = Integer.parseInt(hostAddrPort[1]);
			}
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the serverfile. Server: " + localServerName);
			System.exit(1);
		}
		return hostAddrPort;
	}
	
	// Inicializa conecccoes com um servidor
	private void initializeConnections(TcpComm communication, String localServerAddr, int localServerPort) {
		if(_verbose)
			System.out.println(_userName + ": inicializar comunicacoes com o servidor " + localServerAddr + " no porto " +  localServerPort);
		communication.initializeClientSocket(localServerAddr, localServerPort);
		if(_verbose)
			if(communication.connectedSocket())
				System.out.println("Conectou-se ao servidor " + localServerAddr + " no porto " +  localServerPort);
			else 
				System.out.println("Nao foi possivel conectar-se com o servidor " + localServerAddr + " no porto " +  localServerPort);
		if(communication.connectedSocket()) {
			if(_verbose)
				System.out.println(_userName + ": inicializar buffers");
			communication.initializeBuffers();
			if(_verbose)
				System.out.println(_userName + ": buffers inicializados");
		}
	}
	
	private void processViewAck(String input, TcpComm communication) {
		int ackType = Pdu.pduAckType(input);
		String[] viewData = Pdu.pduToArray(input);
		switch(ackType) {
			case 0:
				System.out.println("Insucesso na realizacao de View (comando malformado)");
				break;
			case 1:
        if(_verbose)
          System.out.println("A receber ficheiro...");
          communication.receiveAndOpenFile();
				break;
			case 2:
				System.out.println("Falha em View " + viewData[0] + " " + viewData[1]);
				break;
			case 3:
				System.out.println("Falha no acesso a " + viewData[0]);
				break;
			default:
				break;	 
		}
		_state = CONNECTED_IDLE;
		_busy = false;
	}
	
	public char[] processToServer(String output){
		char[] answer = null;
		int changeState = _state;
		switch(_state) {
			case DISCONNECTED_IDLE: 
				//Conectar ao server
				initializeConnections(_communication, _localServerAddr, _localServerPort);
				/* //Enviar Username+Password
				try {
					_communication.send(Pdu.stringToPdu(1, _userName + " " + _userPass));
				} catch (IOException e) {
					System.err.println("Couldn't get I/O for the connection sending userpass.");
					System.exit(1);
				}*/
				//_commands.push("1 " + _userName + " " + _userPass);
				
				answer = Pdu.stringToPdu(output);
				break;
			case CONNECTED_IDLE:
				String[] splitOutput = output.split(" ");
				if(output.startsWith("Launch") && splitOutput.length >= 4) {
					_lastFilename = splitOutput[3] + ".bmp";
					_tConfirmMaxLaunch = new Timer();				// armar timer
					_tConfirmMaxLaunch.schedule(new PlayerTask(1, this, _tConfirmMaxLaunch, "Insucesso na realizacao de Launch", false), 7000);			
					_state = WAIT_LAUNCH_ACK;
					return Pdu.launchPdu(output, _userName);
				} else if(output.startsWith("Bid")) {
					    _tConfirmMaxBid = new Timer();			// armar timer
						_tConfirmMaxBid.schedule(new PlayerTask(1, this, _tConfirmMaxBid, "Falha em " + output, false), 7000);
						changeState = WAIT_BID_ACK;
				} else if(output.startsWith("View")) {
						_lastViewRequest = Pdu.stringToPdu(output);
					    changeState = WAIT_LOCALSERVER_RESPONSE;
				} else if(output.startsWith("Quit")) {
					    changeState = WAITING_QUIT;
					    _busy = true;
				}
				// Os comandos do utilizador sao automaticamente convertidos para pdus
				// usando Pdu.stringToPdu
				answer = Pdu.stringToPdu(output);
				break;
			default:
				answer = Pdu.stringToPdu(output);
				break;
		}
		_state = changeState;
		return answer;
	}

    public String processFromServer(String input) {
		String output = "";
		_pduId = Pdu.pduId(input);
		if(_verbose) {
			System.out.println("SERVER -> " + input);
			System.out.println("ESTADO -> " + _state);
		}
		switch(_state) {
			case DISCONNECTED_IDLE: 
				if (_pduId == 18){
					System.out.println("End of Connection from local server.");
					_busy = false;
						//break; Dizer ao cliente para se desconectar
				} else if (_pduId == 2) {							// Resposta a UserPass
					if(Pdu.pduAckSuccess(input)) {
						if(_verbose)
							System.out.println(_userName + ": Par User+Password Aceites");
						_state = CONNECTED_IDLE;
						_busy = false;
					} else {
						if(_verbose)
							System.out.println(_userName + ": Par User+Password invalido");
						if(!_commands.empty())
							_commands.pop();
					}
				}
				break;
			case CONNECTED_IDLE:
				if (_pduId == 5) {							// AuctionEnd
          String[] splitPdu = input.split(Pdu.getSpacer());
          if(Integer.parseInt(splitPdu[4]) == 1) {
            System.out.println("Arrematou a casa " + splitPdu[1] + " " + splitPdu[2] + " pelo valor de " + splitPdu[3]);
          } else {
            System.out.println("A casa " + splitPdu[1] + " " + splitPdu[2] + " foi arrematada por outro comprador pelo valor de " + splitPdu[3]);
          }
    
          
				} else if (_pduId == 9) {					// Informacao de um List
					if(Pdu.pduAckSuccess(input))			// Utilizado aqui para ver se o campo de list e o ultimo (0) ou nao (1)
						if(_verbose)
							System.out.println(_userName + ": recebido bloco list.");
					else {
						_busy = false;
						if(_verbose)
							System.out.println(_userName + ": recebido ultimo bloco list.");
					}
						System.out.println(Pdu.listInfoToString(input));
					_busy = false;
				} else if (_pduId == 10) {							// ListErrorPdu
					if(Pdu.pduAckType(input) == 1) {
						System.out.println("Nao existem casas em leilao para essa tipologia.");
						_busy = false;
					} else {
						System.out.println("Insucesso na realizacao de List (comando malformado)");
						_busy = false;
					}
				} else if (_pduId == 15) {							// Bid Ack
					_tConfirmMaxBid.cancel();		//cancelar timer
					String[] bidInfo = Pdu.pduToArray(input);
					if (Pdu.pduAckType(input) == 3)
						System.out.println("O seu bid " + bidInfo[0] + ", " + bidInfo[1] + " foi ultrapassado por " + bidInfo[2]);
				} else {
					System.out.println("-> Comando por implementar");
					_busy = false;
				}
				break;
			case WAIT_LAUNCH_ACK:
				if (_pduId == 4) {							// Resposta a pedido de Launch
					_tConfirmMaxLaunch.cancel();					// Cancelar o timer
					if(Pdu.pduAckSuccess(input)) {
						System.out.println("Sucesso na realizacao de Launch"); 
						File f = new File(_lastFilename);
						if(_verbose){
							if(f.exists())
								System.out.println(_userName + ": A enviar " + _lastFilename + "...");
							else
								System.out.println("Nao ha ficheiro para enviar.");
						}
						_communication.sendFile(_lastFilename);
						_lastFilename = "";
						_state = CONNECTED_IDLE;
						_busy = false;
						if(_verbose)
							if(f.exists())
								System.out.println(_userName + ": " + _lastFilename + " enviado com sucesso");
					} else { 					 // se o timer expirar ha insucesso
						System.out.println("Insucesso na realizacao de Launch");
						_lastFilename = "";
						_state = CONNECTED_IDLE;
						_busy = false;
					}		
				} else 
					System.out.println("Nao pode receber pdu " + _pduId + " no estado WAITING LAUNCH ACK");
					_state = CONNECTED_IDLE;
					_busy = false;
				break;
			case WAIT_LOCALSERVER_RESPONSE:
				if (_pduId == 12) {							// ViewAckPdu
					processViewAck(input, _communication);
				} else if (_pduId == 13) {							//Redirect Pdu
					String serverName = Pdu.getHostnameFromRedirect(input);
					if(_verbose)
						System.out.println(_userName + ": estabelecendo conexao com " + serverName);
					TcpComm tempCommunication = new TcpComm();
					String[] hostAddrPort = input.split(Pdu.getSpacer());
					initializeConnections(tempCommunication, hostAddrPort[1], Integer.parseInt(hostAddrPort[2]));
					String fromServer;
					try{
						tempCommunication.send(_lastViewRequest);
						if(_verbose)
              System.out.println("Enviou pedido view ao servidor responsavel pelo ficheiro");
						while((fromServer = tempCommunication.receive()) != null) {
							System.out.println(fromServer);
							break;
						}
					} catch (IOException e) {
						System.err.println("Couldn't get I/O for the connection to other server.");
						_state = CONNECTED_IDLE;
						_busy = false;
						return output;
					}
					System.out.println("coiso : " + fromServer);
					processViewAck(fromServer, tempCommunication);
					tempCommunication.closeConnections();
				}
				break;
			case WAIT_BID_ACK:
				if (_pduId == 15) {							// Bid Ack
					_tConfirmMaxBid.cancel();		//cancelar timer
					String[] bidInfo = Pdu.pduToArray(input);
					if(Pdu.pduAckType(input) == 1)
						System.out.println("Bid " + bidInfo[0] + ", " + bidInfo[1] + ", "+ bidInfo[2] + " realizado com sucesso.");
					else if (Pdu.pduAckType(input) == 2)
						System.out.println("Erro: Falha em Bid " + bidInfo[0] + ", " + bidInfo[1] + ", " + bidInfo[2]);
					else if (Pdu.pduAckType(input) == 3)
						System.out.println("O seu bid " + bidInfo[0] + ", " + bidInfo[1] + " foi ultrapassado por " + bidInfo[2]);
					else 
						System.out.println("Insucesso na realizacao de Bid (comando malformado)");
					_busy = false;
					_state = CONNECTED_IDLE;
				}
				break;
			case WAITING_QUIT:
				if (_pduId == 19) {
					if(Pdu.pduAckType(input) == 1)
						System.exit(1);
					_busy = true;
				} else if (_pduId == 15) {							// Bid Ack
					_tConfirmMaxBid.cancel();		//cancelar timer
					String[] bidInfo = Pdu.pduToArray(input);
					if (Pdu.pduAckType(input) == 3)
						System.out.println("O seu bid " + bidInfo[0] + ", " + bidInfo[1] + " foi ultrapassado por " + bidInfo[2]);
					System.exit(1);
				} else if (_pduId == 5) {							// AuctionEnd
          String[] splitPdu = input.split(Pdu.getSpacer());
          if(Integer.parseInt(splitPdu[4]) == 1) {
            System.out.println("Arrematou a casa " + splitPdu[1] + " " + splitPdu[2] + " pelo valor de " + splitPdu[3]);
          } else {
            System.out.println("A casa " + splitPdu[1] + " " + splitPdu[2] + " foi arrematada por outro comprador pelo valor de " + splitPdu[3]);
          }
          try{
            _communication.send(Pdu.stringToPdu(18 + Pdu.getSpacer() + "Quit"));
          } catch(IOException ioe) {
          }
        }
				break;
			default:
				//_pduId = Pdu._pduId(input);
				break;
		}
			return output;
	}
}
