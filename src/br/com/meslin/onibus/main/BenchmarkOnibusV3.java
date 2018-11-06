/**
 * 
 * http connection from https://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/<br>
 * Polygon boundaires from https://gis.stackexchange.com/questions/183248/how-to-get-polygon-boundaries-of-city-in-json-from-google-maps-api<br>
 * This version sends data using ContextNet<br>
 * 
 * Version 2<br>
 * - uses one thread per bus<br>
 * 
 * Version 3<br>
 * - option to read data from City Hall website ou from JSON files<br>
 * - different from version 2, does not send the same bus, instead, sends actual bus to ContextNet<br>
 * - there is no nBus (number of buses) parameter in this version<br>
 * - there is no MAX_ITERATIONS (maximum number of iterations) in this version 
 * - there is no RMI server/client
 * 
 * To execute: $ java -classpath .:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/contextnet-2.5.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/udilib.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JMapViewer/JMapViewer.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JSON/JSON-Parser/json-20160810.jar br.com.meslin.onibus.main.DefineGroup ../RegiaoMetropolitana.txt
 */
package br.com.meslin.onibus.main;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

import br.com.meslin.onibus.aux.StaticLibrary;
import br.com.meslin.onibus.aux.connection.Constants;
import br.com.meslin.onibus.aux.contextnet.BusThread;
import br.com.meslin.onibus.aux.model.Bus;

/**
 * @author Meslin
 *
 */
public class BenchmarkOnibusV3 {
	/*
	 * Configuration parameters
	 */
	/** will filter bus line based on command line option. For a set of bus line, insert parameters inside cotes (") */ 
	private static String busLineFilter;

	// properties
	private static String gatewayIP;
	private static int gatewayPort;
	private File[] listOfFiles;
	private int listOfFileIndex;

	// statistics
	private static long nMessages;
	private static long startTime;
	private static long stopTime;
	private static long elapsedTime;

	// to control the threads
	private Map<String, Thread> busThread;
	// syncronization
	private static Map<String, Integer> threadReturnValue; 

	/** bus indexed by ORDEM<br>Map&lt;ORDEM, Bus&gt; */
	public volatile static Map<String, Bus> buses;

	// parameters
	private static String busDataFilename = null;



	/**
	 * Construct and empty Onibus object
	 */
	public BenchmarkOnibusV3() {
		nMessages = 0;
		threadReturnValue = new HashMap<String, Integer>();
		busThread = new HashMap<String, Thread>();
		
		getBusFileList();
		listOfFileIndex = 0;
		buses = new HashMap<String, Bus>();
		System.out.println("[" + this.getClass().getName() + "." + "BenchmarkOnibus V3] ContextNet address: " + gatewayIP + ":" + gatewayPort + "\n\n");
	}



	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		final Date buildDate = StaticLibrary.getClassBuildTime();
		System.out.println("BenchmarkOnibusV3 builded at " + buildDate);

		// get command line options
		Options options = new Options();
		Option option;

		option = new Option("a", "address", true, "ContextNet Gateway IP address");
		option.setRequired(false);
		options.addOption(option);
		
		option = new Option("f", "filename", true, "Filename or directory where bus data are stored");
		option.setRequired(false);
		options.addOption(option);

		option = new Option("i", "interval", true, "Interval between threads creation in milliseconds");
		option.setRequired(false);
		options.addOption(option);

		option = new Option("l", "busline", true, "bus(es) line(s)");
		option.setRequired(false);
		options.addOption(option);

		option = new Option("p", "port", true, "ContextNet Gateway TCP port number");
		option.setRequired(false);
		options.addOption(option);
		
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("Date = " + new Date());
			formatter.printHelp("BenchmarkOnibusV3", options);
			e.printStackTrace();
			return;
		}
		
		if((busDataFilename = cmd.getOptionValue("filename")) == null) {
			busDataFilename = ".";
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
			StaticLibrary.intervalBetweenThreads = Integer.parseInt(cmd.getOptionValue("interval"));
		} catch(Exception e) {
			// no default parameters setted here (it is setted at StaticLibrary
		}
		// bus line to filter
		if((busLineFilter = cmd.getOptionValue("busline")) == null) {
			busLineFilter = "";
		}

		System.out.println("[BenchmarkOnibusV3.main] Starting sending packets every " + StaticLibrary.interval + " milliseconds from buses");

		System.out.println("Ready, set...");

		BenchmarkOnibusV3 bench = new BenchmarkOnibusV3();
		
		// do all
		bench.doAll();

		elapsedTime = stopTime - startTime;
		System.err.println("\n[BenchmarkOnibus.MAIN] Stopped sending data after " + elapsedTime + " ms with " + nMessages + " sent");
		System.out.println("\n[BenchmarkOnibus.MAIN] Stopped sending data after " + elapsedTime + " ms with " + nMessages + " sent");

		for(Map.Entry<String, Integer> entry : threadReturnValue.entrySet()) {
			if(entry.getValue() < 0) {
				System.err.println("Thread #" + entry.getKey() + " returned status " + entry.getValue());
			}
		}

		System.out.println("FINISHED!!!");
	}



	/**
	 * Gets a bus file list using busFileList class variable
	 */
	private void getBusFileList() {
		File folder;

		/*
		 * if folder is a directory, get its file list
		 * if folder is a file, it is the list of file 
		 */
		folder = new File(busDataFilename);
		if(folder.isDirectory()) {
			listOfFiles = folder.listFiles();
			Arrays.sort(listOfFiles);
		}
		else listOfFiles = new File[] {folder};
	}



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

		System.out.println("***** GO!!!! *****");
		startTime = System.currentTimeMillis();

		// create each bus
		// create a thread for each bus
		while(getAllPositions(busLineFilter)) {
			for (Map.Entry<String, Bus> entry : buses.entrySet()) {
				bus = entry.getValue();
				
				if(!busThread.containsKey(bus.getOrdem())) {
					// a new bus ==> a new thread must be create for this new bus
					// nothing must be done for a bus that we already know
					busThread.put(bus.getOrdem(), new Thread(new BusThread(gatewayIP, gatewayPort, bus, threadReturnValue)));
					busThread.get(bus.getOrdem()).start();
				}
			}
			
			// sleeps for 30 seconds
			// TODO verificar se o tempo está correto
			try {
				Thread.sleep(30 * 1000);
			} catch (InterruptedException e) {
				System.err.println("Date = " + new Date());
				e.printStackTrace();
			}
		}

		// terminates all bus threads
		for (Map.Entry<String, Bus> entry : buses.entrySet()) {
			bus = entry.getValue();
			busThread.get(bus.getOrdem()).interrupt();		// interrupt the thread to stop it
		}
		
		// wait for all bus threads to stop 
		for (Map.Entry<String, Bus> entry : buses.entrySet()) {
			bus = entry.getValue();
			try {
				busThread.get(bus.getOrdem()).join();
				if(threadReturnValue.get(bus.getOrdem())>0) {
					nMessages += threadReturnValue.get(bus.getOrdem());
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
	public boolean getAllPositions() {
		return getAllPositions("");
	}
	/**
	 * Gets all bus positions from city hall<br>
	 * @see BenchmarkOnibusV3#getAllPositions()
	 * {@link BenchmarkOnibusV3#getAllPositions()}
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
	 * @return 
	 */
	public boolean getAllPositions(int line) {
		return getAllPositions(Integer.toString(line));
	}
	/**
	 * Gets all bus positions from city hall<br>
	 * This method updates the List<Bus> buses class object.
	 * If the bus is already registered, only its position is updated.
	 * If the bus is new (not yet registered), a new bus entry is created<br>
	 * On error, while connecting to the city hall website, the buses object is not updated
	 * @see BenchmarkOnibusV3#getAllPositions()
	 * {@link BenchmarkOnibusV3#getAllPositions()}
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
	public boolean getAllPositions(String line) {
//		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] with line = |" + line + "|");
		
		// will process a file (if there is a file to be processed)
		if(listOfFileIndex < listOfFiles.length) {
			String buffer;
			buffer = StaticLibrary.readFile(listOfFiles[listOfFileIndex].getAbsolutePath());

			// create a JSON object based on bus DATA
			if(buffer != null) {
				JSONObject jsonObject = new JSONObject(buffer);
				JSONArray jsonData = jsonObject.getJSONArray("DATA");

				for(int i=0; i<jsonData.length(); i++) {
					JSONArray jsonBus = jsonData.getJSONArray(i);
					Bus bus = new Bus();
					bus.setData(new Date(
							Integer.parseInt(jsonBus.getString(StaticLibrary.DATAHORA).split("[\\- :]")[2]),
							Integer.parseInt(jsonBus.getString(StaticLibrary.DATAHORA).split("[\\- :]")[0]),
							Integer.parseInt(jsonBus.getString(StaticLibrary.DATAHORA).split("[\\- :]")[1]),
							Integer.parseInt(jsonBus.getString(StaticLibrary.DATAHORA).split("[\\- :]")[3]),
							Integer.parseInt(jsonBus.getString(StaticLibrary.DATAHORA).split("[\\- :]")[4]),
							Integer.parseInt(jsonBus.getString(StaticLibrary.DATAHORA).split("[\\- :]")[5])
					));
					bus.setOrdem(jsonBus.getString(StaticLibrary.ORDEM));
					try {
						bus.setLinha(jsonBus.getString(StaticLibrary.LINHA));
					} catch (JSONException e) {
						bus.setLinha(jsonBus.getInt(StaticLibrary.LINHA));
					}
					bus.setLatitude(jsonBus.getDouble(StaticLibrary.LATITUDE));
					bus.setLongitude(jsonBus.getDouble(StaticLibrary.LONGITUDE));
					bus.setVelocidade(jsonBus.getDouble(StaticLibrary.VELOCIDADE));
					
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
							buses.put(jsonBus.getString(StaticLibrary.ORDEM), bus);
						}
//						System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Selecionado: " + bus);
					}
				}
//				System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Total de ônibus: " + buses.size());
			}
//			else {
//				System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Error code #" + responseCode);
//			}

			listOfFileIndex++;
			return true;
		}
		else return false;
	}
}
