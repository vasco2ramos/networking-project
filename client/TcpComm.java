import java.io.*;
import java.net.*;
import java.lang.*;

public class TcpComm{

	private	Socket _socket = null;
	private PrintWriter _out;
	private BufferedReader _in;
	private boolean _connected;
	private boolean _connectedSocket;
	
	private OutputStream _outputStream;
	
	public TcpComm() {
		_connected = false;
	}

	// Inicializa o socket para um cliente
	public void initializeClientSocket(String localServerAddr, int localServerPort) {
		try {
			_socket = new Socket(InetAddress.getByName(localServerAddr), localServerPort);
			_connectedSocket = true;
		} catch (UnknownHostException e) {
			System.err.println("Servidor " + localServerAddr + " no porto " + localServerPort + " desconhecido.");
			_connectedSocket = false;
			//System.exit(1);
		} catch (IOException e) {
			System.err.println("Servidor " + localServerAddr +  " no porto " + localServerPort + " encontra-se offline.");
			_connectedSocket = false;
			//System.exit(1);
		}
	}
	
	public void setSocket(Socket socket) {
		_socket = socket;
		_connectedSocket = true;
	}
	
	public boolean connectedSocket() {
		if(_socket != null)
			return _connectedSocket;
		else
			return false;
	}
	
	// Inicializa os buffers para o socket
	public void initializeBuffers(){
		try {
			if(_socket == null) {
				_connectedSocket = false;
				throw(new IOException("Cannot initialize connections before socket has been initialized."));
			}
			// Obter printer e reader para o socket
			_out = new PrintWriter(_socket.getOutputStream(), true);
			_in = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
			_outputStream = _socket.getOutputStream();
			_connected = true;
		} catch (IOException e) {
			System.err.println("Nao se conseguiram inicializar os buffers para ler da conexao: " + e.toString());
			_connected = false;
			//System.exit(1);
		}
	

	}
	
	public void closeConnections() {
		try {
			_out.close();
			_in.close();
			_socket.close();
 		} catch (IOException e) {
			System.err.println("Couldn't close the connections");
			System.exit(1);
		}
		_connected = false;
	}
	
	public String receive() throws IOException{
		return _in.readLine();
	}	
	
	public void setConnected(boolean connected){
		_connected = connected;
		_connectedSocket = connected;
	}
	
	public void send(char[] message) throws IOException{
		if(isConnected())
			_out.println(message);
		else
			System.out.println("ERRO: impossivel enviar mensagem, a conexao nao foi estabelecida. A mensagem sera descartada.");
	}
	
	public void sendFile(String fileName) {
		try {
			File f = new File(fileName);
			if(f.exists()) {
				ByteStream.toStream(_outputStream, fileName);
				ByteStream.toStream(_outputStream, new File(fileName));
			} else {
				ByteStream.toStream(_outputStream, "404");
			}
		} catch (Exception ex) {
		  ex.printStackTrace();
		}
	}
	
	public void receiveFile() {
		try {
			System.out.println("RECEBER FICHEIRO");
			InputStream in = _socket.getInputStream();
			String fileName = ByteStream.toString(in);
			if(fileName.equals("404")) {
				System.out.println("Nao ha ficheiro a receber");
				return;
			}
			System.out.println("NOME DO FICHEIRO: " + fileName);
			File file=new File(fileName);
			ByteStream.toFile(in, file);
		} catch (Exception ex) {
		  ex.printStackTrace();
		}
	}	
	
	public void receiveAndOpenFile() {
		try {
			InputStream in = _socket.getInputStream();
			String fileName = ByteStream.toString(in);
			if(fileName.equals("404")) {
				System.out.println("Falha em View (ficheiro nao encontrado).");
				return;
			}
			File file=new File(fileName);
			ByteStream.toFile(in, file);
			Process process = new ProcessBuilder("\"C:\\Programs\\Mozilla Firefox\\firefox.exe\"", fileName).start();
		} catch (IOException ioe) {
			System.out.println("Erro ao abrir o ficheiro recebido.");
		} catch (Exception ex) {
		  ex.printStackTrace();
		}
	}
	
	public boolean isConnected() {
		return (_connected && _connectedSocket);
	}
	
}