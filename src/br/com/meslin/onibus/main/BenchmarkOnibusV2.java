/**
 * 
 * http connection from https://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/<br>
 * Polygon boundaires from https://gis.stackexchange.com/questions/183248/how-to-get-polygon-boundaries-of-city-in-json-from-google-maps-api
 * This version sends data using ContextNet
 * 
 * Version 2
 * uses one thread per bus 
 * 
 * To execute: $ java -classpath .:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/contextnet-2.5.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/udilib.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JMapViewer/JMapViewer.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JSON/JSON-Parser/json-20160810.jar br.com.meslin.onibus.main.DefineGroup ../RegiaoMetropolitana.txt
 */
package br.com.meslin.onibus.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.meslin.onibus.aux.MyRMIServer;
import br.com.meslin.onibus.aux.RMIServerIntf;
import br.com.meslin.onibus.aux.StaticLibrary;
import br.com.meslin.onibus.aux.connection.Constants;
import br.com.meslin.onibus.aux.contextnet.BusThread;
import br.com.meslin.onibus.aux.model.Bus;

/**
 * @author Meslin
 *
 */
public class BenchmarkOnibusV2 {
	// constants
	private static final int N_BUSES = 100;
	private static final int MAX_ITERATIONS = 100;
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
	
	// statistics
	private static long nMessages;
	private static long maxIterations;
	private static long startTime;
	private static long stopTime;
	private static long elapsedTime;

	// to control the threads
	private Thread[] busThread;
	// syncronization
	private static Object canStart;				// just a sincronize object
	private static int[] threadReturnValue;
	private static String rmiServerAddress;

	/** bus indexed by ORDEM<br>Map&lt;ORDEM, Bus&gt; */
	public volatile static Map<String, Bus> buses;
	private static int nBuses;					// number of buses, for simulation purpose



	/**
	 * Construct and empty Onibus object
	 */
	public BenchmarkOnibusV2() {
		canStart = new Object();
		nMessages = 0;
		threadReturnValue = new int[nBuses];
		busThread = new Thread[nBuses];
		
		buses = new HashMap<String, Bus>();
		System.out.println("[" + this.getClass().getName() + "." + "Benchmark] ContextNet address: " + gatewayIP + ":" + gatewayPort + "\n\n");
	}



	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		final Date buildDate = StaticLibrary.getClassBuildTime();
		System.out.println("BenchmarkOnibusV2 builded at " + buildDate);
		
		// get command line options
		Options options = new Options();
		Option option;

		option = new Option("a", "address", true, "ContextNet Gateway IP address");
		option.setRequired(false);
		options.addOption(option);

		option = new Option("b", "bus", true, "Number of buses");
		option.setRequired(false);
		options.addOption(option);

		option = new Option("i", "interval", true, "Interval between threads creation in milliseconds");
		option.setRequired(false);
		options.addOption(option);

		option = new Option("n", "iteration", true, "Number of iterations");
		option.setRequired(false);
		options.addOption(option);

		option = new Option("p", "port", true, "ContextNet Gateway IP port number");
		option.setRequired(false);
		options.addOption(option);
		
		option = new Option("m", "main", true, "Main RMI server address");
		option.setRequired(false);
		options.addOption(option);
		
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e1) {
			System.err.println("Date = " + new Date());
			formatter.printHelp("BenchmarkOnibusV2", options);
			e1.printStackTrace();
			return;
		}
		
		if((gatewayIP = cmd.getOptionValue("address")) == null) {
			gatewayIP = Constants.GATEWAY_IP;
		}
		try {
			gatewayPort = Integer.parseInt(cmd.getOptionValue("port"));
		} catch(Exception e) {
			gatewayPort = Constants.GATEWAY_PORT;
		}
		try {
			maxIterations = Integer.parseInt(cmd.getOptionValue("iteration"));
		} catch(Exception e) {
			maxIterations = MAX_ITERATIONS;
		}
		try {
			nBuses = Integer.parseInt(cmd.getOptionValue("bus"));
		} catch(Exception e) {
			nBuses = N_BUSES;
		}
		try {
			StaticLibrary.intervalBetweenThreads = Integer.parseInt(cmd.getOptionValue("interval"));
		} catch(Exception e) {
			// no default parameters setted here (it is setted at StaticLibrary
		}
		rmiServerAddress = cmd.getOptionValue("main");

		System.out.println("[BenchmarkOnibusV2.main] Starting sending " + maxIterations + " packets every " + StaticLibrary.interval + " milliseconds from " + nBuses + " buses");

		System.out.println("Ready, set...");

		// new RMI server
		MyRMIServer.nClients = new Integer(0);
		if(!cmd.hasOption("m")) {
			// binds this object instance to the name "Onibus"
			try {
				System.out.println("[BenchmarkOnibusV2.main] Creating RMI server");
				new MyRMIServer(MyRMIServer.nClients);
			} catch (RemoteException e) {
				System.err.println("Date = " + new Date());
				e.printStackTrace();
			}
		}

		BenchmarkOnibusV2 bench = new BenchmarkOnibusV2();
		
		// do all
		bench.doAll();

		elapsedTime = stopTime - startTime;
		System.err.println("\n[BenchmarkOnibus.MAIN] Stopped sending data after " + elapsedTime + " ms with " + nMessages + " sent");
		System.out.println("\n[BenchmarkOnibus.MAIN] Stopped sending data after " + elapsedTime + " ms with " + nMessages + " sent");

		for(int i=0; i<nBuses; i++) {
			if(threadReturnValue[i] < 0) {
				System.err.println("Thread #" + i + " returned status " + threadReturnValue[i]);
			}
		}

		System.out.println("FINISHED!!!");

/*		while(true) {
			try {
				Thread.sleep(Long.MAX_VALUE);
			} 
			catch (InterruptedException e) {
				System.err.println("Date = " + new Date());
				e.printStackTrace();
			}
		}
*/	}
	
	
	/**
	 * Do all the processing:<br>
	 * <ul>
	 * 	<li>Gets the a bus for each thread
	 * 	<li>Creates the bus threads
	 * 	<li>Start a thread
	 * 	<li>Synchronizes all bus threads
	 * 	<li>Waits for all thread to finish
	 * 	<li>Computes start and end time
	 * </ul>
	 */
	private void doAll() {
		Bus bus;
		RMIServerIntf rmiServerIntf = null;

		if(BenchmarkOnibusV2.rmiServerAddress != null) {
			try {
				System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Tring to connect to " + "rmi://" + BenchmarkOnibusV2.rmiServerAddress + ":1099/Onibus");
				rmiServerIntf = (RMIServerIntf) Naming.lookup("rmi://" + BenchmarkOnibusV2.rmiServerAddress + ":1099/Onibus");
				rmiServerIntf.incClients();
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.err.println("Date = " + new Date());
				e.printStackTrace();
				System.exit(-1);
			}
		}
		else {
			// this is the server
			MyRMIServer.nClients++;
			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] nClients = " + MyRMIServer.nClients);
		}

		// create each bus
		// create a thread for each bus
		synchronized(canStart) {
			for(int i=0; i<nBuses; i++) {
				bus = getABus(584);
				bus.setOrdem(bus.getOrdem()+i);	// apenas para não haver ordem repetida durante o benchmark (esse comando deve ser retirado na versão de produção)
				bus.addGroup(1);
				bus.setUUID(UUID.randomUUID());
				if(!buses.containsKey(bus.getOrdem())) {
					System.out.println("Connecting bus #" + i);
					buses.put(bus.getOrdem(), bus);
					busThread[i] = new Thread(new BusThread(gatewayIP, gatewayPort, bus, i, maxIterations, canStart, threadReturnValue));
					busThread[i].start();
					try {
						Thread.sleep(StaticLibrary.intervalBetweenThreads);
					} catch (InterruptedException e) {
						System.err.println("Date = " + new Date());
						e.printStackTrace();
					}
				}
			}
			// adds a delay corresponding to 10% of the time of the connection phase to wait for the other sets of clients (other machines) to terminate their connection phases
			try {
				Thread.sleep((long) (nBuses * StaticLibrary.intervalBetweenThreads * 0.10));
			} catch (InterruptedException e) {
				System.err.println("Date = " + new Date());
				e.printStackTrace();
			}
			if(BenchmarkOnibusV2.rmiServerAddress != null) {
				try {
					rmiServerIntf.clientReady();
					while(!rmiServerIntf.allReady()) {}
				} catch (RemoteException e) {
					System.err.println("Date = " + new Date());
					e.printStackTrace();
				}
			}
			else {
				System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] nClients = " + MyRMIServer.nClients);
				// this is the server (and also a client) and must wait for all clients be ready
				MyRMIServer.nClients--;
				while(MyRMIServer.nClients != 0) {}
			}
		}

		System.out.println("***** GO!!!! *****");

		startTime = System.currentTimeMillis();
		
		// wait for all bus threads to stop 
		for(int i=0; i<nBuses; i++) {
			try {
				busThread[i].join();
				if(threadReturnValue[i]>0) {
					nMessages += threadReturnValue[i];
				}
			} catch (InterruptedException e) {
				System.err.println("Date = " + new Date());
				e.printStackTrace();
			}
		}
		stopTime = System.currentTimeMillis();
	}


	
	/**
	 * Gets a single bus from city hall database using getAllPositions
	 * @param line
	 * @return
	 */
	protected Bus getABus() {
		return getABus("");
	}
	/**
	 * Gets a single bus from city hall database using getAllPositions
	 * @param line
	 * @return
	 */
	protected Bus getABus(int line) {
		return getABus(Integer.toString(line));
	}
	/**
	 * Gets a single bus from city hall database using getAllPositions
	 * @param line
	 * @return
	 */
	protected Bus getABus(String line) {
		getAllPositions(line);
		Bus newBus = null;
		
		// Get the first bus (only!)
		for(Map.Entry<String, Bus> busEntry: buses.entrySet()) {
			newBus = busEntry.getValue();
			newBus.setOrdem("12345");
			break;
		}
		if(newBus==null) {
			return null;
		}
		return newBus;
	}
	
	/**
	 * Gets all bus positions from city hall<br>
	 * gets all bus positions<br>
	 * Fills global object buses with new buses or update their positions<br>
	 * 
	 * City hall data format:<br>
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
	 */
	public void getAllPositions() {
		getAllPositions("");
	}
	/**
	 * Gets all bus positions from city hall<br>
	 * @see BenchmarkOnibusV2#getAllPositions()
	 * {@link BenchmarkOnibusV2#getAllPositions()}
	 * @param line bus line
	 *
	 * City hall data format:<br>
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
	 */
	public void getAllPositions(int line) {
		getAllPositions(Integer.toString(line));
	}
	/**
	 * Gets all bus positions from city hall<br>
	 * This method updates the List<Bus> buses class object.
	 * If the bus is already registered, only its position is updated.
	 * If the bus is new (not yet registered), a new bus entry is created<br>
	 * On error, while connecting to the city hall website, the buses object is not updated
	 * @see BenchmarkOnibusV2#getAllPositions()
	 * {@link BenchmarkOnibusV2#getAllPositions()}
	 * @param line bus route
	 * 
 	 * City hall data format:<br>
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
	 */
	@SuppressWarnings("deprecation")
	public void getAllPositions(String line) {
//		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] with line = |" + line + "|");
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
			System.err.println("Date = " + new Date());
			System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Fatal error while getting bus positions from the city hall website");
			e.printStackTrace();
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
				System.err.println("Date = " + new Date());
				System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Fatal error while reading bus positions: " + e.getMessage());
				e.printStackTrace();
				return;
			}
		}
		else {
			return;
		}

		// create a JSON object based on bus data
		if(buffer != null) {
			JSONObject jsonObject = new JSONObject(buffer.toString());
			JSONArray jsonData = jsonObject.getJSONArray("DATA");
//			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] DATA = " + jsonData);
//			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] " + jsonData.length() + " buses");
//			buses = new HashMap<String, Bus>();
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
//					System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] ordem = " + bus.getOrdem());
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
//					System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Selecionado: " + bus);
				}
			}
//			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Total de ônibus: " + buses.size());
		}
//		else {
//			System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Error code #" + responseCode);
//		}
	}
}
