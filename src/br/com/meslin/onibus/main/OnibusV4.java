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
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lac.cnclib.net.mrudp.MrUdpNodeConnection;
import lac.cnclib.sddl.message.ApplicationMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.meslin.onibus.auxiliar.model.Bus;
import br.com.meslin.onibus.auxiliar.connection.Constants;

/**
 * @author Meslin
 *
 */
public class OnibusV4 {
	// constants
	private static final String USER_AGENT = "Mozilla/5.0";
	private static final int DATAHORA = 0;
	private static final int ORDEM = 1;
	private static final int LINHA = 2;
	private static final int LATITUDE = 3;
	private static final int LONGITUDE = 4;
	private static final int VELOCIDADE = 5;
	
	// properties
//	private UUID uuid;				// UUID do usuário local
//	private NodeConnection remoteCon =null;
//	private GroupCommunicationManager groupManager;
	
	/** all buses in the city indexed by ORDEM */
	private Map<String, Bus> buses;
	
	/** ContextNet address */
//	private InetSocketAddress endereco;
	/** ContextNet current group */
//	private int mainGroup;	// if there are more than one group, this is the main group



	/**
	 * Construct and empty Onibus objectSystem
	 */
	public OnibusV4() {
		this.buses = new HashMap<String, Bus>();
	}



	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		System.err.println("[OnibusV4." + new Object(){}.getClass().getEnclosingMethod().getName() + "]");

		if(args.length != 0)
		{
			System.err.println("Usage: OnibusV4");
			return;
		}

		OnibusV4 onibus = new OnibusV4();
		onibus.production();
	}



	/**
	 * Does the hard work...
	 * @throws InterruptedException 
	 */
	private void production() throws InterruptedException {
		System.err.println("[OnibusV4.main] Looping");
		while(true) {
//			buses = getAllPositions(485);
			buses = getAllPositions();
			updateDB(buses);
			System.err.println("[OnibusV4.production] indo dormir...");
			Thread.sleep(60 * 1000);		// sleep for a while
		}
	}

	
	
	/**
	 * Gets all bus positions from city hall database<br>
	 * gets all bus positions<br>
	 * creates/updates the hashset buses<br>
	 * each new bus receives a new UUID<br>
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
	 * Gets all bus positions from city hall database<br>
	 * gets all bus positions<br>
	 * creates/updates the hashset buses<br>
	 * each new bus receives a new UUID<br>
	 * @see OnibusV4#getAllPositions()
	 * {@link OnibusV4#getAllPositions()}
	 * @param line bus line
	 * @return 
	 */
	public Map<String, Bus> getAllPositions(int line) {
		return getAllPositions(Integer.toString(line));
	}
	/**
	 * Gets all bus positions from city hall database<br>
	 * gets all bus positions<br>
	 * creates/updates the hashset buses<br>
	 * each new bus receives a new UUID<br>
	 * @see OnibusV4#getAllPositions()
	 * {@link OnibusV4#getAllPositions()}
	 * @param line bus route
	 * @return 
	 */
	@SuppressWarnings("deprecation")
	public Map<String, Bus> getAllPositions(String line) {
		System.err.println("[getAllPositions]");

		URL url;
		HttpURLConnection connection = null;	// connection to the city hall
		int responseCode =-1;
		StringBuffer buffer = null;

		// Connect to bus database
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
		else {
			return null;
		}

		// create a JSON object based on bus data
		if(buffer != null) {
			JSONObject jsonObject = new JSONObject(buffer.toString());
			JSONArray jsonData = jsonObject.getJSONArray("DATA");
			System.err.println("[Onibus.getAllPositions] " + jsonData.length() + " buses found");

			for(int i=0; i<jsonData.length(); i++) {
				JSONArray jsonBus = jsonData.getJSONArray(i);
				Bus bus = new Bus();
				
				// date
				bus.setData(new Date(
						Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[2]),
						Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[0]),
						Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[1]),
						Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[3]),
						Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[4]),
						Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[5])
					));
				// ordem
				bus.setOrdem(jsonBus.getString(ORDEM));
				// line (sometimes string, but sometimes int...)
				try {
					bus.setLinha(jsonBus.getString(LINHA));
				} catch (JSONException e) {
					bus.setLinha(jsonBus.getInt(LINHA));
				}
				// latitude & longitude
				bus.setLatitude(jsonBus.getDouble(LATITUDE));
				bus.setLongitude(jsonBus.getDouble(LONGITUDE));
				// speed
				bus.setVelocidade(jsonBus.getDouble(VELOCIDADE));
				// UUID
				bus.setUUID(UUID.randomUUID());
				
				// Filter buses based on the bus line
				if(bus.getLinha().contains(line)) {
					// if we already know this bus, just update its info. Do not change group info nor ordem nor UUID 
					// (ordem is the primary key)
					if(buses.containsKey(bus.getOrdem())) {
						buses.get(bus.getOrdem()).setData(bus.getData());
						buses.get(bus.getOrdem()).setLatitude(bus.getLatitude());
						buses.get(bus.getOrdem()).setLongitude(bus.getLongitude());
						buses.get(bus.getOrdem()).setLinha(bus.getLinha());
						buses.get(bus.getOrdem()).setVelocidade(bus.getVelocidade());
					}
					// otherwise, (we do not know this bus, so) creates a new entry without group information and UUID
					else {
						bus.setUUID(UUID.randomUUID());		// new bus ==> new UUID
						//bus.setGroupManager(groupManager);
						//bus.setRemoteCon(remoteCon);
						System.err.println("\n\nContextNet address: " + Constants.GATEWAY_IP_LIST[0] + ":" + Constants.GATEWAY_PORT_LIST[0] + "\n\n");
						try {
							MrUdpNodeConnection conexao = new MrUdpNodeConnection(bus.getUUID());
							// *** inverti a ordem para ver se o pedido de conexão fica mais estável
							// TODO resolver o comentário abaixo
//							conexao.addNodeConnectionListener(bus);
							// as this is a new bus, and we do not know which group this bus belongs to, we will use the first gateway
							InetSocketAddress endereco = new InetSocketAddress(Constants.GATEWAY_IP_LIST[0], Constants.GATEWAY_PORT_LIST[0]);
							conexao.connect(endereco);	// TODO modificar o endereço para acompanhar o grupo
						}
						catch (IOException e) {
							e.printStackTrace();
						}
						System.err.println("[" + this.getClass().getName() + "." + "OnibusV4] UUID = " + bus.getUUID().toString());

						buses.put(jsonBus.getString(ORDEM), bus);
					}
					System.err.println("[Onibus.getAllPositions] Selecionado: " + bus);
				}
			}
			System.err.println("[Onibus.getAllPositions] Total de ônibus: " + buses.size());
		}
		else {
			System.err.println("*** [Onibus.getAllPositions] Error code #" + responseCode);
		}
		return buses;
	}

	
	
	/**
	 * Update InterSCity data base thru ContextNet<br>
	 * @param buses 
	 */
	private void updateDB(Map<String, Bus> buses) {
		System.err.println("\n[OnubusV4.updateDB] " + new Date());
		System.err.println(buses.size() + " buses located.");
		// for each bus...
		for(Map.Entry<String, Bus> bus: buses.entrySet()) {
			sendBusToContextNet(bus);
		}
	}

	
	
	/**
	 * send a message with a bus to ContextNet
	 * @param bus
	 */
	private void sendBusToContextNet(Map.Entry<String, Bus> bus) {
		// sends coordinates to the selector
		try {
			ApplicationMessage message = new ApplicationMessage();
			message.setContentObject(new Bus(bus));
			// TODO resolver esse comentário abaixo
//			bus.getValue().getConnection().sendMessage(message);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
