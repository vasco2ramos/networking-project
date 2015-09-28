import java.util.*;
import java.io.Serializable;

public class AuctionInfo implements Serializable{

	private int _id, _value, _filePort;
	private Vector<Bid> _bids;
	private String _class, _city, _house, _seller, _fileAddress;
	private boolean _manager;
	private Timer _auctionEndTimer;
	String _highestBidUser;
	
	/** Serial number. */
	private static final long serialVersionUID = 2008122585763412L;

	/** Adiciona uma bid a AuctionInfo caso esta seja maior que a maior bid
		Devolve true se a bid foi adicionada, false se nao for.
		Nota: A bid de maior valor estara sempre no final do vector _bids*/
	public void addBid(Bid newBid) {
		_bids.add(newBid);
	}
	
	/** Construtor.*/
	public AuctionInfo(int id, String houseClass, String city, String house, int value, String seller, String fileAddress, int filePort, boolean manager) {
		_id = id;
		_class = houseClass;
		_city = city;
		_house = house;
		_value = value;
		_bids = new Vector<Bid>(5,1);
		_seller = seller;
		//_managerAddress = managerAddress;
		//_managerPort = managerPort;
		_fileAddress = fileAddress;
		_filePort = filePort;
		_manager = manager;
	}

	/*public String getManagerAddress(){
		return _managerAddress;
	}*/
	
	public String getHighestBidUser() {
    return _highestBidUser;
	}
	
	public String getFileAddress(){
		return _fileAddress;
	}
	
	public boolean getIsManager(){
		return _manager;
	}
	
	public int getFilePort(){
		return _filePort;
	}
	
	/*public int getManagerPort(){
		return _managerPort;
	}*/
	
	public String getCity() {
		return _city;
	}
	
	public String getSeller() {
		return _seller;
	}
	
	public int getId() {
		return _id;
	}
	
	public int getValue() {
		return _value;
	}
	
	public void scheduleTimer(Timer timer, ServerTask serverTask, int time) {
    if(_auctionEndTimer != null)
      _auctionEndTimer.cancel();
    _auctionEndTimer = timer;				// armar timer
		_auctionEndTimer.schedule(serverTask, time);
	}
	
	public void setValue(String user, int value) {
		_value = value;
		_highestBidUser = user;
		addBid(new Bid(user, value));
	}
	
	public String getHouse() {
		return _house;
	}
	
	public String getHouseClass() {
		return _class;
	}
	
	public AuctionInfo() {
		_id = 0;
		_bids = new Vector<Bid>(5,1);
	}
	
}
