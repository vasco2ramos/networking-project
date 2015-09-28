import java.util.*;
import java.io.*;
import java.net.*;


public class PlayerTask extends TimerTask {
		private final int CONNECTED_IDLE = 1;
		private int _state = -1;
		private PlayerProtocol _playerProtocol;
		private Timer _timer;
		private String _message;
		private boolean _busy;
	    
	    
	    public PlayerTask(int state, PlayerProtocol playerProtocol, Timer timer, boolean busy) {
			super();
			_state = state;
			_playerProtocol = playerProtocol;
			_timer = timer;
			_busy = busy;
		}
		
		public PlayerTask(int state, PlayerProtocol playerProtocol, Timer timer, String message, boolean busy) {
			super();
			_state = state;
			_playerProtocol = playerProtocol;
			_timer = timer;
			_message = message;
			_busy = busy;
		}	    
		
	    public PlayerTask(PlayerProtocol playerProtocol, Timer timer, boolean busy) {
			super();
			_playerProtocol = playerProtocol;
			_timer = timer;
			_busy = busy;
		}
		
		public PlayerTask(PlayerProtocol playerProtocol, Timer timer, String message, boolean busy) {
			super();
			_playerProtocol = playerProtocol;
			_timer = timer;
			_message = message;
			_busy = busy;
		}
    
    public void run() {
		if (_state != -1)
			_playerProtocol.setState(_state);
		if(_message != null)
			System.out.println(_message);
		
		_playerProtocol.setBusy(_busy);

		_timer.cancel();		
	}
			

			
			
	}
