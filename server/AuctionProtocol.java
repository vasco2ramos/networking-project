import java.util.Vector;

public class AuctionProtocol {
	private Vector<ServerProtocol> _protocolVector;
    private int _auctionId;
    private String _sName;
    
    public AuctionProtocol(int auctionId,String sName, Vector<ServerProtocol> protocolVector){
		_auctionId = auctionId;
		_sName = sName;
		_protocolVector = protocolVector;
	}
	
	public int getId() {
		return _auctionId;
	}
	
	public String getSName() {
		return _sName;
	}
	
	public Vector<ServerProtocol> getProtocolVector(){
		return _protocolVector;
	}
	
	public void addProtocol(ServerProtocol protocol){
		_protocolVector.add(protocol);
	}
	
}
