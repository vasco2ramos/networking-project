import java.util.BitSet;

public class Pdu {

	static private String spacer = "#";
	
	static public String getSpacer() {
		return spacer;
	}
	
	/* pensar em separadores... receber os componentes numa string e vantajoso
	   porque e assim que eles sao lidos no input */
	static public char[] stringToPdu(String data) {
		String temp = "";
		int i = 0;
		String[] splitData = data.split(" ");
		/* Campo ID */
		if(data.startsWith("Launch")){
			temp = "3";
		} else if(data.startsWith("List")){
			temp = "8";
		} else if(data.startsWith("Bid")){
			temp = "14";
		} else if(data.startsWith("View")){
			temp = "11";
		} else if(data.startsWith("Quit")){
			temp = "18";
		} else {
			if(i < splitData.length)
				temp += splitData[i++];
		}
		for(; i < splitData.length; i++) {
			temp += spacer + splitData[i];
		}
		return temp.toCharArray();
	}
	
	// Devolve uma launch pdu
	static public char[] launchPdu(String data, String userName) {
		String temp = "";
		int i = 0;
		String tempData = data + " " + userName;
		String[] splitData = tempData.split(" ");
		/* Campo ID */
		temp = "3";
		for(; i < splitData.length; i++) {
			temp += spacer + splitData[i];
		}
		return temp.toCharArray();
	}
	
	static public int pduId(String pdu){
		String[] temp = pdu.split(spacer); 
		return Integer.parseInt(temp[0]);
	}
	
	// Devolve username e password num array de strings
	static public String[] getUserPass(String pdu){
		String[] temp = pdu.split(spacer);
		String[] answer = new String[2];
		answer[0] = temp[1];
		answer[1] = temp[2];
		return answer;
	}
	
	static public char[] stringToPdu(int id, String data) {
		String temp = "" + id;
		for(String s : data.split(" "))
			temp += spacer + s;
		return temp.toCharArray();
	}	
	
	// Cria uma pdu para responder a pdu userPass. 
	// Devolve String para facilitar uso no Server Protocol
	static public String pduUserPassAck(boolean ack) {
		String temp = 2 + spacer;
		if(ack)
			temp += 1;
		else
			temp += 0;
		return temp;
	}
	
	static public boolean pduAckSuccess(String data) {
		return data.contains(spacer + "1");
	}
	
	//Devolve o tipe de erro na pdu de ack a view, list...
	static public int pduAckType(String data) {
		String[] temp = data.split(spacer);
		return Integer.parseInt(temp[1]);
	}
	
	// Devolve um array de strings com a informacao relevante da pdu
	static public String[] pduToArray(String pdu) {
		String[] temp = pdu.split(spacer);
		String[] info = new String[temp.length-2];
		for(int i=2; i < temp.length; i++) {
			info[i-2] = temp[i];
		}
		return info;
	}
	
	static public String listInfoToString(String data) {
		String info = "->List: \n";
		String[] temp = data.split(spacer);
		for(int i=1; i < temp.length-1; i +=4) {
			info += "->Auction\n";
			info += "-Class: " + temp[i] + "\n";
			info += "-City: " + temp[i+1] + "\n";
			info += "-House: " + temp[i+2] + "\n";
			info += "-Value " + temp[i+3] + "\n";
		}
		return info;
	}
	
	//Devolve uma string que e uma pdu com view error de ficheiro nao encontrado
	static public String viewErrorFile(String fileName) {
		return 12 + spacer + 3 + spacer + fileName;
	}	
	
	//Devolve uma string que e uma pdu com view error de auction nao encontrada
	static public String viewErrorAuction(String city, String house) {
		return 12 + spacer + 2 + spacer + city + spacer + house;
	}
	
	//Devolve uma string que e uma pdu com view error de pdu mal formada
	static public String viewMalformed() {
		return 12 + spacer + 0;
	}	
	
	//Devolve uma string que e uma pdu com view error de pedido mal formado
	static public String viewSuccess() {
		return 12 + spacer + 1;
	}	
	
	//Devolve uma string que e uma pdu com list error de pedido mal formado
	static public String listMalformed() {
		return 10 + spacer + 0;
	}
	
	//Devolve uma string que e uma pdu com list error onde a informacao nao foi encontrada
	static public String listNotFound() {
		return 10 + spacer + 1;
	}
	
	// Devolve o valor de uma bid Pdu
	static public int bidValue(String bidPdu) {
		String[] temp = bidPdu.split(spacer);
		return Integer.parseInt(temp[4]);
	}
	
	//Devolve uma string que e uma pdu com bid error
	static public String bidError(String house, String city, int value) {
		return 15 + spacer + 2 + spacer + house + spacer + city + spacer + value;
	}	
	static public String bidError(String house, String city, String value) {
		return 15 + spacer + 2 + spacer + house + spacer + city + spacer + value;
	}	
	
	//Devolve uma string que e uma pdu com bid malformed error
	static public String bidMalformed() {
		return 15 + spacer + 0;
	}
	
	//Devolve uma string que e uma pdu com bid success
	static public String bidSuccess(String house, String city, int value) {
		return 15 + spacer + 1 + spacer + house + spacer + city + spacer + value;
	}
	
	//Devolve uma string que e uma pdu de redireccionamento
	static public String redirectPdu(String serverName, int serverPort) {
		return 13 + spacer + serverName + spacer + serverPort;
	}
	
	//Devolve o hostname de uma pdu redirect
	static public String getHostnameFromRedirect(String pdu) {
		String[] temp = pdu.split(spacer);
		return temp[1];
		
		
	}	
	// Cria uma pdu com 
	static public String infoAuction(String infoLaunch, int auctionId, int type, String fileAddr, int filePort) {
		String[] temp = infoLaunch.split(spacer);
		return 16 + spacer + auctionId + spacer + temp[2] + spacer + temp[3] + spacer + temp[4]+ spacer + temp[5] + spacer+ type + spacer + temp[6] + spacer + fileAddr + spacer + filePort;
	}
	
/*
	public Pdu(int id, String components) {
		_id = id;
		_pduData = "" + id;
		for(String s : components.split(" "))
			_pduData += "\n" + s;
	}
	
	public Pdu(String components) {
		if(components.startsWith("Launch")){
			_id = 3;
		} else if(components.startsWith("List")){
			_id = 8;
		} else if(components.startsWith("Bid")){
			_id = 14;
		} else if(components.startsWith("View")){
			_id = 11;
		} else if(components.startsWith("Quit")){
			_id = 18;
		} else 
			System.out.println("PDU Recebeu comando invalido: " + components);
			
		_pduData = "" + _id;
		String[] splitComponents = components.split(" ");
		for(int i = 0; i < splitComponents.length; i++)
			_pduData += "\n" + splitComponents[i];

	}
*/
	
	
}
