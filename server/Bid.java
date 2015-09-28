import java.util.Vector;
import java.io.Serializable;

public class Bid implements Serializable{

	private String _user;
	private int _value;

	/** Serial number. */
	private static final long serialVersionUID = 2008122586463412L;

	/** Construtor.*/
	public Bid() {
	}

	public int getAmount() {
		return _value;
	}

	public Bid(String user, int value) {
		_user = user;
		_value = value;
	}

}
