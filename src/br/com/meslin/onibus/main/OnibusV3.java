/**
 * 
 * http connection from https://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/<br>
 * Polygon boundaires from https://gis.stackexchange.com/questions/183248/how-to-get-polygon-boundaries-of-city-in-json-from-google-maps-api<br>
 * This version sends data using ContextNet<br>
 * <br>
 * To execute: $ java -classpath .:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/contextnet-2.5.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/udilib.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JMapViewer/JMapViewer.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JSON/JSON-Parser/json-20160810.jar br.com.meslin.onibus.main.DefineGroup ../RegiaoMetropolitana.txt<br>
 */
package br.com.meslin.onibus.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lac.cnclib.net.NodeConnection;
import lac.cnclib.net.NodeConnectionListener;
import lac.cnclib.net.groups.Group;
import lac.cnclib.net.groups.GroupCommunicationManager;
import lac.cnclib.net.groups.GroupMembershipListener;
import lac.cnclib.net.mrudp.MrUdpNodeConnection;
import lac.cnclib.sddl.message.ApplicationMessage;
import lac.cnclib.sddl.message.Message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.meslin.onibus.auxiliar.model.Bus;
import br.com.meslin.onibus.auxiliar.connection.Constants;

/**
 * @author Meslin
 *
 */
public class OnibusV3 implements NodeConnectionListener, GroupMembershipListener {
	// constants
	private static final String USER_AGENT = "Mozilla/5.0";
	private static final int DATAHORA = 0;
	private static final int ORDEM = 1;
	private static final int LINHA = 2;
	private static final int LATITUDE = 3;
	private static final int LONGITUDE = 4;
	private static final int VELOCIDADE = 5;
	
	// properties
	private UUID uuid;				// UUID do usuário local
	private NodeConnection remoteCon =null;
	private GroupCommunicationManager groupManager;
	
	/** buses indexed by ORDEM */
	private Map<String, Bus> buses;
	
	/** ContextNet address */
	private InetSocketAddress endereco;

	/**
	 * Construct and empty Onibus object
	 */
	public OnibusV3() {
		buses = new HashMap<String, Bus>();
		this.uuid = UUID.randomUUID();
		System.err.println("[" + this.getClass().getName() + "." + "OnibusV3] UUID = " + this.uuid.toString());

		endereco = new InetSocketAddress(Constants.GATEWAY_IP, Constants.GATEWAY_PORT);
		try {
			MrUdpNodeConnection conexao = new MrUdpNodeConnection(this.uuid);
			// *** inverti a ordem para ver se o pedido de conexão fica mais estável
			conexao.addNodeConnectionListener(this);
			conexao.connect(endereco);	// TODO modificar o endereço para acompanhar o grupo
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		System.out.println("\n\nContextNet address: " + Constants.GATEWAY_IP + ":" + Constants.GATEWAY_PORT + "\n\n");
		
		OnibusV3 onibus = new OnibusV3();
		onibus.production();
	}
	
	
	/**
	 * Does the hard work...
	 * @throws InterruptedException 
	 */
	private void production() throws InterruptedException {
		System.err.println("[OnibusV3.main] Looping");
		while(true) {
			buses = getAllPositions(584);
//			buses = getAllPositions();
			updateDB(buses);
			System.err.println("[OnibusV3.production] indo dormir...");
			Thread.sleep(10 * 60 * 1000);
		}
	}
	
	/**
	 * Update InterSCity data base<br>
	 * @param buses 
	 */
	private void updateDB(Map<String, Bus> buses) {
//		Scanner scanner = new Scanner(System.in);
		
		System.err.println("\n[Onubusv3.updateDB] " + new Date());
		System.err.println(buses.size() + " buses located.");
		// for each bus...
		for(Map.Entry<String, Bus> bus: buses.entrySet()) {
			// sends coordinates to the selector
			try {
				ApplicationMessage message = new ApplicationMessage();
				message.setContentObject(new Bus(bus));
				this.remoteCon.sendMessage(message);
//				System.err.println("[OnibusV3.updateDB] Bus sent... wait for operator to continue");
//				scanner.nextLine();
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	
	/**
	 * Gets all bus positions from city hall<br>
	 * gets all bus positions<br>
	 * 
     *{<br>
     *	"COLUMNS":[<br>
     * 		"DATAHORA",<br>
     *		"ORDEM",<br>
     *		"LINHA",<br>
     *		"LATITUDE",<br>
     *		"LONGITUDE",<br>
     *		"VELOCIDADE"<br>
  	 *	],<br>
  	 *	"DATA":[<br>
     *		[<br>
     *	 		"MM-DD-YYY HH:MM:SS",<br>
     *			"ORDEM",<br>
     *			"LINHA",<br>
     *			LATITUDE,<br>
     *			LONGITUDE,<br>
     *			VELOCIDADE<br>
     *		],<br>
     *		[], []<br>
	 * @return 
	 */
	public Map<String, Bus> getAllPositions() {
		return getAllPositions("");
	}
	/**
	 * Gets all bus positions from city hall<br>
	 * @see OnibusV3#getAllPositions()
	 * {@link OnibusV3#getAllPositions()}
	 * @param line bus line
	 * @return 
	 */
	public Map<String, Bus> getAllPositions(int line) {
		return getAllPositions(Integer.toString(line));
	}
	/**
	 * Gets all bus positions from city hall<br>
	 * @see OnibusV3#getAllPositions()
	 * {@link OnibusV3#getAllPositions()}
	 * @param line bus route
	 * @return 
	 */
	@SuppressWarnings("deprecation")
	public Map<String, Bus> getAllPositions(String line) {
		System.err.println("[getAllPositions]\n\n\n");
//		Map<String, Bus> buses = null;
		URL url;
		HttpURLConnection connection = null;
		int responseCode =-1;
		StringBuffer buffer = null;

		// Connect to bus data base
		try {
			url = new URL("http://dadosabertos.rio.rj.gov.br/apiTransporte/apresentacao/rest/index.cfm/obterTodasPosicoes");
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", USER_AGENT);
			responseCode = connection.getResponseCode();
		} catch (IOException e) {
			System.err.println("***** [Onibus.getAllPositions] Fatal error while getting bus positions: " + e.getMessage());
		}

		// read bus data
		if(responseCode == 200) {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String inputLine;
				buffer = new StringBuffer();
				while((inputLine = reader.readLine()) != null) {
					buffer.append(inputLine);
				}
				reader.close();
			} catch (IOException e) {
				System.err.println("***** [Onibus.getAllPositions] Fatal error while reading bus positions: " + e.getMessage());
			}
		}
		else
		{
			return null;
		}

		// create a JSON object based on bus data
		if(buffer != null) {
//			try {
				JSONObject jsonObject = new JSONObject(buffer.toString());
				JSONArray jsonData = jsonObject.getJSONArray("DATA");
//				System.err.println("[Onibus.getAllPositions] DATA = " + jsonData);
				System.err.println("[Onibus.getAllPositions] " + jsonData.length() + " buses");
//				buses = new HashMap<String, Bus>();
				for(int i=0; i<jsonData.length(); i++) {
					JSONArray jsonBus = jsonData.getJSONArray(i);
					Bus bus = new Bus();
					bus.setData(new Date(
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[2]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[0]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[1]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[3]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[4]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[5])
						));
					bus.setOrdem(jsonBus.getString(ORDEM));
					try {
						bus.setLinha(jsonBus.getString(LINHA));
					} catch (JSONException e) {
						bus.setLinha(jsonBus.getInt(LINHA));
					}
					bus.setLatitude(jsonBus.getDouble(LATITUDE));
					bus.setLongitude(jsonBus.getDouble(LONGITUDE));
					bus.setVelocidade(jsonBus.getDouble(VELOCIDADE));
					
					// Filter buses based on bus line
					if(bus.getLinha().contains(line)) {
						// if we already know this bus, just update its info. Do not change group info nor ordem (ordem is the primary key)
						if(buses.containsKey(bus.getOrdem())) {
							buses.get(bus.getOrdem()).setData(bus.getData());
							buses.get(bus.getOrdem()).setLatitude(bus.getLatitude());
							buses.get(bus.getOrdem()).setLongitude(bus.getLongitude());
							buses.get(bus.getOrdem()).setLinha(bus.getLinha());
							buses.get(bus.getOrdem()).setVelocidade(bus.getVelocidade());
						}
						// otherwise, creates a new entry without group information
						else {
							buses.put(jsonBus.getString(ORDEM), bus);
						}
						System.err.println("[Onibus.getAllPositions] Selecionado: " + bus);
					}
				}
				System.err.println("[Onibus.getAllPositions] Total de ônibus: " + buses.size());
//			} catch (Exception e) {
//				System.err.println("***** [Onibus.getAllPositions] Fatal error while creating JSON object: " + e.getMessage());
//			}
		}
		else {
			System.err.println("*** [Onibus.getAllPositions] Error code #" + responseCode);
		}
		return buses;
	}

	@Override
	public void connected(NodeConnection remoteCon) {
		this.groupManager = new GroupCommunicationManager(remoteCon);
		this.groupManager.addMembershipListener(this);
		this.remoteCon = remoteCon;		
	}

	@Override
	public void disconnected(NodeConnection remoteCon) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void newMessageReceived(NodeConnection remoteCon, Message message) {
		// TODO Auto-generated method stub
		if(message.getContentObject() instanceof Bus) {
			Bus bus = (Bus) message.getContentObject();
			System.err.print("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "]\n" + message.getSenderID() + "\n" + bus.getLinha() + "@" + bus.getOrdem() + " Grupos: ");
			for(int i : bus.getGroups()) System.err.print(i + " ");
			System.err.println();
			
			// if the bus already (or yet) exists, just set the groups, otherwise, ignore the message
			if(buses.containsKey(bus.getOrdem())) {
				buses.get(bus.getOrdem()).setGroups(bus.getGroups());
			}
		}
	}

	@Override
	public void internalException(NodeConnection remoteCon, Exception e) {}
	@Override
	public void reconnected(NodeConnection remoteCon, SocketAddress endPoint, boolean wasHandover, boolean wasMandatory) {}
	@Override
	public void unsentMessages(NodeConnection remoteCon, List<Message> unsentMessages) {}
	@Override
	public void enteringGroups(List<Group> arg0) {}
	@Override
	public void leavingGroups(List<Group> arg0) {}
}