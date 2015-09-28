import java.util.*;
import java.net.*;
import java.io.*;

public class ServerProtocol {
    private static final int DISCONECTED_IDLE = 0;
    private static final int CONECTED_IDLE = 1;
    private static final int WAIT_LAUNCHINFO = 2;
    private static final int WAIT_LAUNCHFILE = 3;
    private static final int SENDING_BLOCKS = 4;
    private static final int WAIT_INFO_AUCTION = 5;

	private int pduId;
	private FileReader fileReader;
    private int state = DISCONECTED_IDLE;
	private Vector<AuctionInfo> _auctions;
	private int _auctionNum = 0;
	private String _lastFilename;
	private String[] _lastLaunch, _lastBid;
	private Vector<AuctionInfo> _listReply;
	private Vector<ServerInfo> _serversInfo;
	private ServerInfo _thisServerInfo;
	private UdpComm _udpComm;
	private Vector<AuctionProtocol> _auctionProtocols;
	private boolean _waiting;
	private TcpComm _communication;
	private String _user;
	
	public AuctionProtocol findAuction(int id, String serverName){
		for(AuctionProtocol a : _auctionProtocols)
			if((a.getId() == id)  && (a.getSName().equals(serverName)))
				return a;
			return null;
	}
	
	public boolean userMayQuit() {
		for(AuctionProtocol p : _auctionProtocols) 
			for(ServerProtocol s : p.getProtocolVector()) 
				if(s.getUser().equals(_user)) {
					return false;
				}
		return true;
	}
	
	public void auctionEndNotify(AuctionInfo auction){
    ServerInfo manager = null;
    for(ServerInfo s : _serversInfo) {
      if(s.managesCity(auction.getCity())){
        manager = s;
        break;
      }
       
    }
    
    AuctionProtocol auctionProtocol = findAuction(auction.getId(), manager.getServerName());
    int type;
    for(ServerProtocol sp : auctionProtocol.getProtocolVector()) {
      if(auction.getHighestBidUser().equals(sp.getUser()))
        type = 1;
      else
        type = 0;
      try{
        sp.getCommunication().send(Pdu.stringToPdu(5 + Pdu.getSpacer() + auction.getCity() + Pdu.getSpacer() +
                                    auction.getHouse() + Pdu.getSpacer() + auction.getValue() + Pdu.getSpacer() + type));
      } catch  (IOException ioe) {
        
      }
    }
    _auctions.removeElement(auction);
    _auctionProtocols.removeElement(auctionProtocol);
	}
	
	public String getUser(){
		return _user;
	}

	public String[] getLastLaunch(){
		return _lastLaunch;
	}
	
	public String[] getLastBid(){
		return _lastBid;
	}
	
	public void setCommunication(TcpComm communication){
		_communication = communication;
	} 
	
	public TcpComm getCommunication(){
		return _communication;
	}
	
	public boolean isWaiting() {
		return _waiting;
	}
	
	public void setWaiting(boolean waiting) {
		_waiting = waiting;
	}
	
	public String getLastFilename(){
		return _lastFilename;
	}
	
	public int getState(){
		return state;
	}	
	
	public UdpComm getUdpComm(){
		return _udpComm;
	}	
	
	public void setState(int s){
		state = s;
	}
	
	public Vector<AuctionInfo> getListReply() {
		return _listReply;
	}
	
	public ServerProtocol (Vector<AuctionInfo> auctions, Vector<AuctionProtocol> auctionProtocols, String serverName, Vector<ServerInfo> serversInfo, String usersFile) {
    fileReader = new FileReader(usersFile);
		_auctions = auctions;
		_auctionProtocols = auctionProtocols;
		_serversInfo = serversInfo;
		for(ServerInfo s : serversInfo) {
			if(s.getServerName().equals(serverName)) {
				_thisServerInfo = s;
				break;
			}
		}
		_udpComm = new UdpComm(_serversInfo, _thisServerInfo);
		_waiting = false;
	}
	
	private ServerProtocol () {
	}

	private String processView(String input, int changeState) {
		state = changeState;
		String[] viewInfo = Pdu.pduToArray(input);
		if(viewInfo.length == 2) {
			for(AuctionInfo a : _auctions) {
				if(a.getCity().equals(viewInfo[0]) && a.getHouse().equals(viewInfo[1])) {
					System.out.println("View Successful: " + a.getCity() + " " + a.getHouse());   //enviar ficheiro
					if(a.getFileAddress().equals(_thisServerInfo.getServerAddr())) {
						System.out.println("Sou o responsavel por " + a.getHouse() + ".bmp");
						_lastFilename = a.getHouse() + ".bmp";
						File f = new File(_lastFilename);
						if(f.exists()) {
							System.out.println("File " + _lastFilename + " found");	
							return Pdu.viewSuccess();
						} else {
							return Pdu.viewErrorFile(_lastFilename);
						}
					} else {
						System.out.println("Nao sou o responsavel por " + viewInfo[1]);
            System.out.println(a.getFileAddress());
            System.out.println("O responsavel e " + a.getFileAddress());
            return Pdu.redirectPdu(a.getFileAddress(), a.getFilePort());
          }
				}
			}
			System.out.println("View Not Found");   //enviar erro
			return Pdu.viewErrorAuction(viewInfo[0], viewInfo[1]);
		} else {
			System.out.println("Pdu View mal formada"); // enviar erro
			return Pdu.viewMalformed();
    }
 }
	
    public String processInput(String input) {
        String output = null;
		pduId = Pdu.pduId(input);
		System.out.println("ESTADO: " + state);
		/*if(input != null)
			if(pduId == 18) 
				output = "19" + Pdu.getSpacer() + "1"; //enviar ACK quit
 			else
				output= input;
		*/
        if (state == DISCONECTED_IDLE) {
            /* ler e processar PDU*/
            if(pduId == 1) {		/*verificar user/pass*/
				String [] userPass = Pdu.getUserPass(input);
				if(fileReader.userPassCorrect(userPass[0], userPass[1])) {
					System.out.println("UserPass " + userPass[0] + " / " + userPass[1] + " aceites" + Pdu.pduUserPassAck(true));
					_user = userPass[0];
					state = CONECTED_IDLE;
					output = Pdu.pduUserPassAck(true);		// enviar confirmacao
				} else {
					System.out.println("UserPass " + userPass[0] + " / " + userPass[1] + " rejeitados");
					output = Pdu.pduUserPassAck(false);		// enviar erro
				}	
			} else if(pduId == 11) {
				//processar view
				output = processView(input, 0);
			}
        } else if (state == CONECTED_IDLE) {
            if (pduId == 3) {
				/*processar launch*/
				String[] newAuction = Pdu.pduToArray(input);
				if(newAuction.length == 5) {
					if(_thisServerInfo.managesCity(newAuction[1])) {
						System.out.println("Sou o responsavel por " + newAuction[1]);
						_auctions.add(new AuctionInfo(_auctionNum, newAuction[0], newAuction[1], newAuction[2], Integer.parseInt(newAuction[3]), newAuction[4], _thisServerInfo.getServerAddr(), _thisServerInfo.getServerPort(), true));
						_auctionProtocols.add(new AuctionProtocol(_auctionNum, _thisServerInfo.getServerName(), new Vector<ServerProtocol>(3 , 1)));
						//16#Id#T2#Braga#casa#value#type#user#fileManagerAddress#fileManagerPort
						_udpComm.broadcastPdu(16 + Pdu.getSpacer() + _auctionNum++ + Pdu.getSpacer() + newAuction[0] + Pdu.getSpacer() + 
												newAuction[1] + Pdu.getSpacer() + newAuction[2] + Pdu.getSpacer() + newAuction[3] + Pdu.getSpacer() 
												+ 0 + Pdu.getSpacer() + newAuction[4] + Pdu.getSpacer() + _thisServerInfo.getServerAddr() + Pdu.getSpacer() + _thisServerInfo.getServerPort());
						output = "4" + Pdu.getSpacer() + "1";
					} else {
						System.out.println("Nao sou o responsavel por " + newAuction[1]);
						for(ServerInfo s : _serversInfo){
							if(s.managesCity(newAuction[1])) {
								System.out.println("O responsavel e " + s.getServerName());
								_waiting = true;
								_lastLaunch = newAuction;
								_udpComm.transmitPdu(Pdu.infoAuction(input, 0, 1, _thisServerInfo.getServerAddr(), _thisServerInfo.getServerPort()), s.getServerAddr(), s.getServerPort());
								return "sentLaunch " + s.getServerName();			// enviou launch para o servidor responsavel
							}
						}
						output = "4" + Pdu.getSpacer() + "0";
					}
				} else {
					System.out.println("Pdu launch mal formada"); // enviar erro
					output = "4" + Pdu.getSpacer() + "0";
				}
            } else if(pduId == 8) {
				//processar list
				String[] listInfo = Pdu.pduToArray(input);
				_listReply = new Vector<AuctionInfo>(1,1);
				if(listInfo.length == 1) {
					for(AuctionInfo a : _auctions) {
						if(a.getHouseClass().equals(listInfo[0])) {
							_listReply.add(a);
						}
					}
					if(_listReply.isEmpty())
						output = Pdu.listNotFound();
					else {	
						state = SENDING_BLOCKS;
						output = "sendBlocks";
					}
				} else 
					output = (Pdu.listMalformed());
            } else if(pduId == 11) {
				output = processView(input, CONECTED_IDLE);
			} else if(pduId == 14) {
				//processar bid
				String[] bidInfo = Pdu.pduToArray(input);
				if(bidInfo.length == 3) {
					int value;
					try {
						value = Pdu.bidValue(input);
					} catch(NumberFormatException nfe) {
						System.out.println("Valor da bid nao e um numero");
						return Pdu.bidError(bidInfo[0], bidInfo[1], bidInfo[2]);
					}
					if(value < 0) 
						return Pdu.bidError(bidInfo[0], bidInfo[1], value);
					System.out.println("Pedido BID: " + bidInfo[0] + " " + bidInfo[1]);
					for(AuctionInfo a : _auctions) {
						if(a.getCity().equals(bidInfo[0]) && a.getHouse().equals(bidInfo[1])) {
							System.out.println("Bid found: " + a.getCity() + " " + a.getHouse());
							if(value > a.getValue()) {
								if(_thisServerInfo.managesCity(bidInfo[0])) {
									System.out.println("Sou responsavel por " + bidInfo[0]);
									a.setValue(_user, value);
									Timer newTimer = new Timer();
									a.scheduleTimer(newTimer, new ServerTask(40,  5 + Pdu.getSpacer() + a.getCity() + Pdu.getSpacer() + a.getHouse() + Pdu.getSpacer() +
                                                                 a.getValue() + Pdu.getSpacer() + 0, this, a, newTimer), 6000);
               		//16#Id#T2#Braga#casa#value#type#user#fileManagerAddress#fileManagerPort
                  _udpComm.broadcastPdu(16 + Pdu.getSpacer() + a.getId() + Pdu.getSpacer() + a.getHouseClass() + Pdu.getSpacer() + 
                                        a.getCity() + Pdu.getSpacer() + a.getHouse() + Pdu.getSpacer() + value + Pdu.getSpacer() 
                                        + 2 + Pdu.getSpacer() + _user + Pdu.getSpacer() + a.getFileAddress() + Pdu.getSpacer() + a.getFilePort());
									boolean add = true;
									AuctionProtocol auctionProtocol = null;
									for(AuctionProtocol ap : _auctionProtocols){
										if(ap.getId() == a.getId() && ap.getSName().equals(_thisServerInfo.getServerName())){
											auctionProtocol = ap;
											for(ServerProtocol sp : ap.getProtocolVector()) {  // Notificar os clients outbid											
                        if(!sp.getUser().equals(_user)) {
                          try{
                            sp.getCommunication().send(Pdu.stringToPdu(15 + Pdu.getSpacer() + 3 + 
                                                      Pdu.getSpacer() + a.getCity() + Pdu.getSpacer()+ a.getHouse() + Pdu.getSpacer() + value));
                          } catch(IOException ioe){
                          }
                        } else
                          add = false;                          
											}
										}
									}
									if(add)
                    auctionProtocol.addProtocol(this);
									
									return Pdu.bidSuccess(bidInfo[0], bidInfo[1], value);
								} else {
									System.out.println("Nao sou responsavel por " + bidInfo[0]);
									boolean add = true;
                  AuctionProtocol ap = null;
									ServerInfo serverInfo = null;
									for(ServerInfo s : _serversInfo) {
										if(s.managesCity(bidInfo[0])) {
                          ap = findAuction(a.getId(), s.getServerName());
                          serverInfo = s;
                          break;
                    }
                  }
                  for(ServerProtocol sp : ap.getProtocolVector()) {  // Notificar os clients outbid											
                    if(sp.getUser().equals(_user)) {
                      add = false;                 
                      break;
                    }         
									}
                  if(add)
                    ap.addProtocol(this);
                  _lastBid = bidInfo;
                  _waiting = true;
                  _udpComm.transmitPdu(input + Pdu.getSpacer() + _user, serverInfo.getServerAddr(), serverInfo.getServerPort());
                  return "sentBid";
								}
							} else
								return "insufficientBid";
						}
					}
					System.out.println("BID not found: " + bidInfo[0] + " " + bidInfo[1]);
					return Pdu.bidError(bidInfo[0], bidInfo[1], value);
				}
				else
					output = Pdu.bidMalformed();
			} else if(pduId == 18) {				//quit
				if(userMayQuit())
					return 19 + Pdu.getSpacer() + 1;
				else
					return 19 + Pdu.getSpacer() + 0;
			} else
				System.out.println("A PDU " + pduId + " nao deveria estar no estado " + state);
        } else if (state == WAIT_LAUNCHINFO) {
			if (pduId == 5) {
                /*continuar a processar*/
            } else
				System.out.println("A PDU " + pduId + " nao deveria estar no estado " + state);
        } else if (state == WAIT_LAUNCHFILE) {
                //A planear
        } else if (state == SENDING_BLOCKS) {
				//fazer algo no list
		} else if (state == WAIT_INFO_AUCTION) {
			if(pduId == 5) {
				//processar bid
			} else
				System.out.println("A PDU " + pduId + " nao deveria estar no estado " + state);
        } 
        return output;
    }
}
