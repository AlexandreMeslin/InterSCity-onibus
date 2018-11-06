/**
 * 
 * http connection from https://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/<br>
 * Polygon boundaires from https://gis.stackexchange.com/questions/183248/how-to-get-polygon-boundaries-of-city-in-json-from-google-maps-api
 * This version sends data using ContextNet
 * 
 * Onibus V5
 * use a thread to collect data from the city hall website and another thread to send data to the MUSANet
 * 
 * To execute: $ java -classpath .:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/contextnet-2.5.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/udilib.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JMapViewer/JMapViewer.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JSON/JSON-Parser/json-20160810.jar br.com.meslin.onibus.main.DefineGroup ../RegiaoMetropolitana.txt
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
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import lac.cnclib.net.mrudp.MrUdpNodeConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.meslin.onibus.aux.connection.Constants;
import br.com.meslin.onibus.aux.model.Bus;

/**
 * @author Meslin
 *
 */
public class OnibusV5 {
	// constants
	private static final String USER_AGENT = "Mozilla/5.0";
	private static final int DATAHORA = 0;
	private static final int ORDEM = 1;
	private static final int LINHA = 2;
	private static final int LATITUDE = 3;
	private static final int LONGITUDE = 4;
	private static final int VELOCIDADE = 5;
	
	/** all buses in the city indexed by ORDEM */
	private Map<String, Bus> buses;

	/**
	 * Construct and empty Onibus object
	 */
	public OnibusV5() {
		this.buses = new HashMap<String, Bus>();
	}



	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		System.err.println("[OnibusV5." + new Object(){}.getClass().getEnclosingMethod().getName() + "] ContextNet address: " + Constants.GATEWAY_IP_LIST[0] + ":" + Constants.GATEWAY_PORT_LIST[0] + "\n\n");

		if(args.length != 0)
		{
			System.err.println("Usage: OnibusV5");
			return;
		}

		OnibusV5 onibus = new OnibusV5();
		onibus.production();
	}



	/**
	 * Does the hard work...
	 * @throws InterruptedException 
	 */
	private void production() {
		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Looping");
		
		// Thread to collect data from city hall website
		new Thread() {
			public void run() {
				while(true) {
					buses = getAllPositions(584);
//					buses = getAllPositions();
					System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] collect going to sleep...");
					try {
						Thread.sleep(600 * 1000);		// sleep for a while
					} catch (InterruptedException e) {
						e.printStackTrace();
					}		
				}
			}
		}.start();
		
		// Thread to send data to MUSANet infrastructure
		new Thread() {
			public void run() {
				while(true) {
					updateDB(buses);
					System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] send going to sleep...");
					try {
						Thread.sleep(30*1000);		// sleep for a while
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
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
	 * @see OnibusV5#getAllPositions()
	 * {@link OnibusV5#getAllPositions()}
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
	 * @see OnibusV5#getAllPositions()
	 * {@link OnibusV5#getAllPositions()}
	 * @param line bus route
	 * @return 
	 */
	@SuppressWarnings("deprecation")
	public Map<String, Bus> getAllPositions(String line) {
		URL url;
		HttpURLConnection connection = null;	// connection to the city hall
		int responseCode =-1;
		StringBuffer buffer = null;

		/*
		 * First step: get data from city hall website
		 */
		
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

		/*
		 * Second step: create/update the buses structure
		 * This include create the bus connetion to the ContextNet
		 */
		
		// create a JSON object based on bus data
		if(buffer != null) {
			JSONObject jsonObject = new JSONObject(buffer.toString());
			JSONArray jsonData = jsonObject.getJSONArray("DATA");
			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] " + jsonData.length() + " buses found");

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
						// update an existing bus in buses
						buses.get(bus.getOrdem()).setData(bus.getData());
						buses.get(bus.getOrdem()).setLatitude(bus.getLatitude());
						buses.get(bus.getOrdem()).setLongitude(bus.getLongitude());
						buses.get(bus.getOrdem()).setLinha(bus.getLinha());
						buses.get(bus.getOrdem()).setVelocidade(bus.getVelocidade());
					}
					// otherwise, (we do not know this bus, so) creates a new entry without group information and UUID
					else {
						// new bus
						// update some fields
						bus.setUUID(UUID.randomUUID());		// new bus => new UUID
						HashSet<Integer> group = new HashSet<Integer>();
						group.add(0);
						bus.setGroups(group);
						// Connect bus to ContextNet
						try {
							// as this is a new bus, and we do not know which group this bus belongs to, we will use the first gateway
							InetSocketAddress endereco = new InetSocketAddress(Constants.GATEWAY_IP_LIST[0], Constants.GATEWAY_PORT_LIST[0]);
							MrUdpNodeConnection conexao = new MrUdpNodeConnection(bus.getUUID());
							// TODO resolver o comentário abaixo
//							conexao.addNodeConnectionListener(bus);
							conexao.connect(endereco);	// TODO modificar o endereço para acompanhar o grupo
						}
						catch (IOException e) {
							e.printStackTrace();
						}
						System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] UUID = " + bus.getUUID().toString());

						buses.put(jsonBus.getString(ORDEM), bus);
					}
					System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Selecionado: " + bus);
				}
			}
			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Total de ônibus: " + buses.size());
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
	@SuppressWarnings("unused")
	private void updateDB(Map<String, Bus> buses) {
		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] " + buses.size() + " buses located.");
		// for each bus...
		if(buses != null) for(Map.Entry<String, Bus> bus: buses.entrySet()) {
			// TODO resolver esse comentários abaixo
//			bus.getValue().sendBusToContextNet();
		}
	}
}