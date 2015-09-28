import java.util.*;
import java.io.*;
import java.net.*;


public class ServerTask extends TimerTask {
	private int _type;
	private String _pdu;
	private int _state = 40;
	private String _address;
	private int _port;
	private ServerProtocol _serverProtocol;
	private Timer _timer;
	private AuctionInfo _auction;
	private UdpCommThread _udpCommThread;
	
	public ServerTask(int state, ServerProtocol serverProtocol, Timer timer) {
		super();
		_type = 0;
		_state = state;
		_serverProtocol = serverProtocol;
		_timer = timer;
	}
	
	public ServerTask(int state, String pdu, ServerProtocol serverProtocol, AuctionInfo auction, Timer timer) {
		super();
		_type = 1;
		_state = state;
		_pdu = pdu;
		_serverProtocol = serverProtocol;
		_timer = timer;
		_auction = auction;
	}
	
	public ServerTask(int state, String pdu, String address, int port, ServerProtocol serverProtocol, AuctionInfo auction, Timer timer) {
		super();
		_type = 2;
		_state = state;
		_pdu = pdu;
		_address = address;
		_serverProtocol = serverProtocol;
		_timer = timer;
		_port = port;
		_auction = auction;
	}
	
	public ServerTask(int state, String pdu, UdpCommThread udpCommThread, AuctionInfo auction, Timer timer) {
		super();
		_type = 1;
		_state = state;
		_pdu = pdu;
		_timer = timer;
		_auction = auction;
		_udpCommThread = udpCommThread; 
	}
    
    public void run() {
    if(_udpCommThread == null) {
      UdpComm udpComm = _serverProtocol.getUdpComm();
      switch(_type) {
        case 1 : udpComm.broadcastPdu(_pdu); break;			
        case 2 : udpComm.transmitPdu(_pdu, _address, _port); break;
      }
      if (_state < 40){
        _serverProtocol.setState(_state);
      }
      _serverProtocol.auctionEndNotify(_auction);
		} else {
        UdpComm udpComm = _udpCommThread.getUdpComm();
      switch(_type) {
        case 1 : udpComm.broadcastPdu(_pdu); break;			
        case 2 : udpComm.transmitPdu(_pdu, _address, _port); break;
      }
      _udpCommThread.auctionEndNotify(_auction);
		}
		_timer.cancel();
	}	
}