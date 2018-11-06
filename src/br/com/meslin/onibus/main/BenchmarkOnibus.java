/**
 * 
 * http connection from https://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/<br>
 * Polygon boundaires from https://gis.stackexchange.com/questions/183248/how-to-get-polygon-boundaries-of-city-in-json-from-google-maps-api
 * This version sends data using ContextNet
 * 
 * To execute: $ java -classpath .:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/contextnet-2.5.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/udilib.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JMapViewer/JMapViewer.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JSON/JSON-Parser/json-20160810.jar br.com.meslin.onibus.main.DefineGroup ../RegiaoMetropolitana.txt
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

import org.apache.commons.lang3.SerializationUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.meslin.onibus.aux.connection.Constants;
import br.com.meslin.onibus.aux.model.Bus;

/**
 * @author Meslin
 *
 */
public class BenchmarkOnibus implements NodeConnectionListener, GroupMembershipListener {
	// constants
	private static final int N_BUSES = 10;
	private static final String USER_AGENT = "Mozilla/5.0";
	private static final int DATAHORA = 0;
	private static final int ORDEM = 1;
	private static final int LINHA = 2;
	private static final int LATITUDE = 3;
	private static final int LONGITUDE = 4;
	private static final int VELOCIDADE = 5;
	
	// properties
	private static String gatewayIP;
	private static int gatewayPort;
	private UUID uuid;				// UUID do usuário local
	private MrUdpNodeConnection	connection;
	private GroupCommunicationManager groupManager;
	
	// statistics
	private static long nMessages;
	private static long startTime;
	private static long stopTime;
	private static long elapsedTime;

	// to control the threads
	private static long maxIterations;
	private static BenchmarkOnibus[] onibus;

	/** buses indexed by ORDEM */
	private volatile static Map<String, Bus> buses;
	
	/** ContextNet address */
	private InetSocketAddress address;



	/**
	 * Construct and empty Onibus object
	 */
	public BenchmarkOnibus() {
		buses = new HashMap<String, Bus>();
		this.uuid = UUID.randomUUID();
		System.out.println("[" + this.getClass().getName() + "." + "Benchmark] ContextNet address: " + gatewayIP + ":" + gatewayPort + "\n\n");

		address = new InetSocketAddress(gatewayIP, gatewayPort);
		try {
			this.connection = new MrUdpNodeConnection(this.uuid);
			this.connection.addNodeConnectionListener(this);
			this.connection.connect(address);
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
/*
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	System.err.println("CTRL+C");
				stopTime = System.currentTimeMillis();
				elapsedTime = stopTime - startTime;
				System.err.println("\n[BenchmarkOnibus.MAIN] Stopped sending data after " + elapsedTime + " ms with " + nMessages + " sent");
		    }
		 });
*/	
		if(args.length != 0) {
			gatewayIP = args[0];
			gatewayPort = Integer.parseInt(args[1]);
			maxIterations = Integer.parseInt(args[2]);
		}
		else {
			gatewayIP = Constants.GATEWAY_IP;
			gatewayPort = Constants.GATEWAY_PORT;
			maxIterations = 10000;
		}
		
		System.out.println("\n\nContextNet address: " + gatewayIP + ":" + gatewayPort + "\n\n");
		
		onibus = new BenchmarkOnibus[N_BUSES];
		for(int i=0; i<N_BUSES; i++) {
			onibus[i]= new BenchmarkOnibus();
			System.out.println("Connecting bus #" + i);
			Thread.sleep(200);
		}
		buses = onibus[0].getABus(584);
		System.err.println("***** STARTING!!!! *****");

		nMessages = 0;
		startTime = System.currentTimeMillis();
		for(long j=0; j<maxIterations; j++) {
			for(int i=0; i<N_BUSES; i++) {
				System.out.print(".");
				onibus[i].updateDB(buses);
			}
			Thread.sleep(1000);
		}
		stopTime = System.currentTimeMillis();
		elapsedTime = stopTime - startTime;
		System.err.println("\n[BenchmarkOnibus.MAIN] Stopped sending data after " + elapsedTime + " ms with " + nMessages + " sent");

		while(true) {
			try {
				Thread.sleep(Long.MAX_VALUE);
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}



	/**
	 * Update InterSCity data base<br>
	 * @param buses 
	 */
	private void updateDB(Map<String, Bus> buses) {
		// for each bus...
		for(Map.Entry<String, Bus> bus: buses.entrySet()) {
			// sends coordinates to the selector
			try {
				ApplicationMessage message = new ApplicationMessage();
				message.setContentObject(new Bus(bus));
				byte[] data = SerializationUtils.serialize(new Bus(bus));
				System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Sending this message: " + data.length);
				this.connection.sendMessage(message);
				nMessages++;
			} 
			catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	

	/**
	 * Gets a single bus from city hall database using getAllPositions
	 * @param line
	 * @return
	 */
	protected Map<String, Bus> getABus() {
		return getABus("");
	}
	/**
	 * Gets a single bus from city hall database using getAllPositions
	 * @param line
	 * @return
	 */
	protected Map<String, Bus> getABus(int line) {
		return getABus(Integer.toString(line));
	}
	/**
	 * Gets a single bus from city hall database using getAllPositions
	 * @param line
	 * @return
	 */
	protected Map<String, Bus> getABus(String line) {
		Map<String, Bus> buses = getAllPositions(line);
		Map.Entry<String, Bus> newBus = null;
		
		// Get the first bus (only!)
		for(Map.Entry<String, Bus> bus: buses.entrySet()) {
			newBus = bus;
			break;
		}
		if(newBus==null) {
			return null;
		}
		buses = new HashMap<String, Bus>();
		buses.put(newBus.getKey(), newBus.getValue());
		return buses;
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
	 * @see BenchmarkOnibus#getAllPositions()
	 * {@link BenchmarkOnibus#getAllPositions()}
	 * @param line bus line
	 * @return 
	 */
	public Map<String, Bus> getAllPositions(int line) {
		return getAllPositions(Integer.toString(line));
	}
	/**
	 * Gets all bus positions from city hall<br>
	 * @see BenchmarkOnibus#getAllPositions()
	 * {@link BenchmarkOnibus#getAllPositions()}
	 * @param line bus route
	 * @return 
	 */
	@SuppressWarnings("deprecation")
	public Map<String, Bus> getAllPositions(String line) {
//		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] with line = |" + line + "|");
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
			System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Fatal error while getting bus positions: " + e.getMessage());
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
				System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Fatal error while reading bus positions: " + e.getMessage());
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
//				System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] DATA = " + jsonData);
//				System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] " + jsonData.length() + " buses");
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
//						System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] ordem = " + bus.getOrdem());
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
//						System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Selecionado: " + bus);
					}
				}
//				System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Total de ônibus: " + buses.size());
//			} catch (Exception e) {
//				System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Fatal error while creating JSON object: " + e.getMessage());
//			}
		}
		else {
//			System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Error code #" + responseCode);
		}
		return buses;
	}

	@Override
	public void connected(NodeConnection remoteCon) {
		this.groupManager = new GroupCommunicationManager(remoteCon);
		this.groupManager.addMembershipListener(this);
//		this.remoteCon = remoteCon;		
	}

	@Override
	public void disconnected(NodeConnection remoteCon) {
		System.err.println("\n\n\n***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] " + new Date());
		try {
			remoteCon.connect(address);
		} catch (IOException e) {
			e.printStackTrace();
		}
//		System.exit(0);
	}

	@Override
	public void newMessageReceived(NodeConnection remoteCon, Message message) {
		if(message.getContentObject() instanceof Bus) {
			Bus bus = (Bus) message.getContentObject();
			
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
