import java.net.*;
import java.io.*;
import java.util.*;

public class UdpCommThread extends Thread {
	private int _port = 20000;
	private Vector<AuctionInfo> _auctions;
	private Vector<ServerInfo> _serversInfo;
	private UdpComm _udpComm;
	private ServerInfo _thisServerInfo;
	private Vector<ServerProtocol> _protocols;
	private Vector<AuctionProtocol> _auctionProtocols;
	
    public UdpCommThread(int port, Vector<AuctionInfo> auctions, Vector<AuctionProtocol> auctionProtocols, Vector<ServerProtocol> protocols ,Vector<ServerInfo> serversInfo, String serverName) {
		super("UdpCommThread");
		_port = port;
		_auctions = auctions;
		_serversInfo = serversInfo;
		for(ServerInfo s : serversInfo) {
			if(s.getServerName().equals(serverName)) {
				_thisServerInfo = s;
				break;
			}
		}
		_udpComm = new UdpComm(_serversInfo, _thisServerInfo);
		_protocols = protocols;
		_auctionProtocols = auctionProtocols;
    }

  public UdpComm getUdpComm() {
    return _udpComm;
  }

	public AuctionProtocol findAuction(int id, String serverName){
		for(AuctionProtocol a : _auctionProtocols)
			if((a.getId() == id)  && (a.getSName().equals(serverName)))
				return a;
			return null;
	}
	
	
  // Notificar os clientes de que um leilao acabou
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

	// processa as pdus recebidas de outros servidores
	private void processPdu(String pdu, InetAddress pduAddress, int pduPort) {
		System.out.println("Host address " + pduAddress.getHostName());
		
		String answer = "";
		int pduId = Pdu.pduId(pdu);
		switch(pduId) {
			case 5:
			String[] splitMessage = pdu.split(Pdu.getSpacer());
        for(AuctionInfo a : _auctions) {
            if(splitMessage[1].equals(a.getCity()) && splitMessage[2].equals(a.getHouse())) {
                auctionEndNotify(a);
                break;
            }
        }
        
        break;
			case 14:				//BidPdu
				String[] splitBid = pdu.split(Pdu.getSpacer());
				System.out.println(pdu);
				for(AuctionInfo a : _auctions) {
					if(a.getCity().equals(splitBid[2]) && a.getHouse().equals(splitBid[3])) {
						a.setValue(splitBid[5], Integer.parseInt(splitBid[4]));
						//16#Id#T2#Braga#casa#value#type#user#fileManagerAddress#fileManagerPort
						_udpComm.broadcastPdu(16 + Pdu.getSpacer() + a.getId() + Pdu.getSpacer() + a.getHouseClass() + Pdu.getSpacer() + 
											a.getCity() + Pdu.getSpacer() + a.getHouse() + Pdu.getSpacer() + a.getValue() + Pdu.getSpacer() 
											+ 2 + Pdu.getSpacer() + splitBid[5] + Pdu.getSpacer() + a.getFileAddress() + Pdu.getSpacer() + a.getFilePort());
            Timer newTimer = new Timer();
            a.scheduleTimer(newTimer, new ServerTask(40,  5 + Pdu.getSpacer() + a.getCity() + Pdu.getSpacer() + a.getHouse() + Pdu.getSpacer() +
                                                                 a.getValue() + Pdu.getSpacer() + 0, this, a, newTimer), 6000);
            
            AuctionProtocol ap = findAuction(a.getId(), _thisServerInfo.getServerName());
            for(ServerProtocol p : ap.getProtocolVector()) {
              TcpComm tempComm = p.getCommunication();
              try{
                tempComm.send(Pdu.stringToPdu("15" + Pdu.getSpacer() + "3" + Pdu.getSpacer() + a.getCity() +
                              Pdu.getSpacer()+ a.getHouse() + Pdu.getSpacer() + a.getValue()));
              } catch(IOException ioe) {
              }

						}
						break;
					}
				}
				break;
			case 16:				//AuctionInfo
				//16#Id#T2#Braga#casa#value#type#user#fileManagerAddress#fileManagerPort
				String[] splitPdu = pdu.split(Pdu.getSpacer());
				int id;
				if(_auctions.isEmpty())
					id = 0;
				else
					id = _auctions.lastElement().getId() + 1;
				
				if(Integer.parseInt(splitPdu[6]) == 1){			// nova auction
					// adicionar a auction da qual sou manager e avisar os outros servidores
					_auctions.add(new AuctionInfo(id, splitPdu[2], splitPdu[3], splitPdu[4], Integer.parseInt(splitPdu[5]), splitPdu[7], splitPdu[8], Integer.parseInt(splitPdu[9]), true));
					_auctionProtocols.add(new AuctionProtocol(id, _thisServerInfo.getServerName(), new Vector<ServerProtocol>(3 , 1)));
					_udpComm.broadcastPdu(16 + Pdu.getSpacer() + id + Pdu.getSpacer() + splitPdu[2] + Pdu.getSpacer() + 
											splitPdu[3] + Pdu.getSpacer() + splitPdu[4] + Pdu.getSpacer() + splitPdu[5] + Pdu.getSpacer() 
											+ 0 + Pdu.getSpacer() + splitPdu[7] + Pdu.getSpacer() + splitPdu[8] + Pdu.getSpacer() + splitPdu[9]);
				} else if(Integer.parseInt(splitPdu[6]) == 0){
					// Adicionar uma auction da qual nao sou manager
					_auctions.add(new AuctionInfo(id, splitPdu[2], splitPdu[3], splitPdu[4], Integer.parseInt(splitPdu[5]), splitPdu[7], splitPdu[8], Integer.parseInt(splitPdu[9]), false));
					for(ServerInfo s : _serversInfo) {
						if(s.managesCity(splitPdu[3])) {
							_auctionProtocols.add(new AuctionProtocol(id, s.getServerName(), new Vector<ServerProtocol>(3 , 1)));
							break;
						}
					}
					for(ServerProtocol p : _protocols) {
						if(p.isWaiting()) {
							String[] lastLaunch = p.getLastLaunch();
							System.out.println("Equals " + lastLaunch[0] + " " + splitPdu[2]);
							if(lastLaunch[0].equals(splitPdu[2]) && lastLaunch[1].equals(splitPdu[3]) && lastLaunch[2].equals(splitPdu[4])) {
								TcpComm tempComm = p.getCommunication();
								try{
									tempComm.send(Pdu.stringToPdu("4" + Pdu.getSpacer() + "1"));
								} catch(IOException ioe) {
								}
							}
						}
					}
				} else {
					// Update de uma auction da qual nao sou manager
					for(AuctionInfo a : _auctions) {
						if(a.getCity().equals(splitPdu[3]) && a.getHouse().equals(splitPdu[4])) {
							a.setValue(splitPdu[7], Integer.parseInt(splitPdu[5]));
              //auctionUpdateNotify(a);
              AuctionProtocol auctionProtocol = null;
              for(ServerInfo s : _serversInfo) {
                if(s.managesCity(a.getCity())){
                  auctionProtocol = findAuction(a.getId(), s.getServerName());
                  break;
                }
              }
							for(ServerProtocol p : auctionProtocol.getProtocolVector()) {
								String[] lastBid = p.getLastBid();
								if(p.isWaiting()) {
									System.out.println("Equals " + lastBid[0] + " " + splitPdu[3]);
									if(lastBid[0].equals(splitPdu[3]) && lastBid[1].equals(splitPdu[4]) && lastBid[2].equals(splitPdu[5])) {
										TcpComm tempComm = p.getCommunication();
										try{
											tempComm.send(Pdu.stringToPdu("15" + Pdu.getSpacer() + "1" + Pdu.getSpacer() + lastBid[0]
                                    + Pdu.getSpacer()+ lastBid[1] + Pdu.getSpacer() + a.getValue()));
										} catch(IOException ioe) {
										}
									} else {
										TcpComm tempComm = p.getCommunication();
										try{
											tempComm.send(Pdu.stringToPdu("15" + Pdu.getSpacer() + "3" + Pdu.getSpacer() + lastBid[0] +
                                    Pdu.getSpacer()+ lastBid[1] + Pdu.getSpacer() + a.getValue()));
										} catch(IOException ioe) {
										}
									}
								} else {
									TcpComm tempComm = p.getCommunication();
									try{
										tempComm.send(Pdu.stringToPdu("15" + Pdu.getSpacer() + "3" + Pdu.getSpacer() + lastBid[0] +
                                  Pdu.getSpacer()+ lastBid[1] + Pdu.getSpacer() + a.getValue()));
									} catch(IOException ioe) {
									}
								}	
							}
							break;
						}
					}
				}
				break;
			default:
				break;
		}
		System.out.println("Processado");
	}
	
    public void run(){
		try{
			MulticastSocket socket = new MulticastSocket(_port);
			InetAddress address = InetAddress.getByName("230.0.0.1");
			socket.joinGroup(address);
			boolean listening = true;
			DatagramPacket packet;
			int packetPort;
			InetAddress packetAddress;
			while(listening) {
				byte[] buf = new byte[256];
				packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				packetPort = packet.getPort();
				packetAddress = packet.getAddress();
				String received = new String(packet.getData(), 0, packet.getLength());
				System.out.println("Recebido pelo servidor" + received);
				processPdu(received, packetAddress, packetPort);
			}
			socket.leaveGroup(address);
			socket.close();
		} catch (IOException e) {
          e.printStackTrace();
		}
	}
}

