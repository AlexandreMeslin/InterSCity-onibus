/**
 * 
 * http connection from https://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/<br>
 * Polygon boundaires from https://gis.stackexchange.com/questions/183248/how-to-get-polygon-boundaries-of-city-in-json-from-google-maps-api<br>
 * This version sends data using ContextNet<br>
 * <br>
 * Version 2<br>
 * uses one thread per bus<br> 
 * <br>
 * To execute: $ java -classpath .:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/contextnet-2.5.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/udilib.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JMapViewer/JMapViewer.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JSON/JSON-Parser/json-20160810.jar br.com.meslin.onibus.main.DefineGroup ../RegiaoMetropolitana.txt<br>
 * 
 * Para simular a taxa de transferência e quantidade de pacotes perdidos, utilizar em conjunto com BenchmarkMyCore
 * Passos para a simulação:
 * 1) Exportar como JAR (BenchmarkOnibusV2 e BenchmarkMyCore)
 * 2) Transferir para VM005
 * 3) Copiar BenchmarkOnibusV2 para Cliente1-2 e BenchmarkMyCore para ContextNet1-4
 * 4) Iniciar o InterSCity em ContextNet1 com o comando: (cd /opt/InterSCity/dev-env ; ./project start ; curl -X GET "http://localhost:8000/catalog/capabilities")
 * 5) Iniciar o ContextNet em ContextNet1-4 com o comando: java -Xmx8G -jar /opt/ContextNet/contextnet-2.5.jar 172.16.1.202 5500 OpenSplice
 * 6) Criar a variável INTERSCITY com o endereço do InterSCity (contextnet1)
 * 7) Iniciar o servidor em ContextNet1-4 com o script server.sh
 * 8) Editar o arquivo client1-2.sh e informar o número de ônibus modificando a variável b
 * 9) Iniciar o cliente em Cliente1-2 com o script client1-2.sh
 * 10) Aguardar todos os ônibus de todos os clientes terminarem de se conectar para apertar <ENTER>
 * <br>
 * IMPOBS: The RMI is disable by now, please, synchronize by hand.<br>
 */
package br.com.meslin.onibus.main;

//import java.rmi.Naming;
//import java.rmi.NotBoundException;
//import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import prefecture.Prefecture;
import br.com.meslin.onibus.auxiliar.model.Bus;
//import java.net.MalformedURLException;
import br.com.meslin.onibus.auxiliar.Debug;
import br.com.meslin.onibus.auxiliar.StaticLibrary;
import br.com.meslin.onibus.auxiliar.connection.Constants;
import br.com.meslin.onibus.auxiliar.contextnet.BusThread;

/**
 * @author Meslin
 */
public class BenchmarkOnibusV2 {
	// constants
	private static final int N_BUSES = 100;
	private static final int MAX_ITERATIONS = 100;
	
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
	private static Object canStart;				// just a sync object
	private static Map<String, Integer> threadReturnValue;
//	private static String rmiServerAddress;

	/** number of buses, for simulation purpose */
	public volatile static int nBuses;					
	public volatile static int busNumber;


	/**
	 * Construct and empty Onibus object, including:
	 * <ul>
	 * 	<li>can start flag
	 *  <li>qtd message
	 *  <li>thread return value
	 *  <li>the thread
	 * </ul>
	 */
	public BenchmarkOnibusV2() {
		// canStart is an object used to synchronize the bus threads
		canStart = new Object();
		nMessages = 0;
		threadReturnValue = new HashMap<String, Integer>();
		busThread = new Thread[nBuses];
		
		Debug.message("ContextNet address: " + gatewayIP + ":" + gatewayPort + "\n\n");
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

/*		option = new Option("m", "main", true, "Main RMI server address");
		option.setRequired(false);
		options.addOption(option);
*/		
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
			// no default parameters set here (it is set at StaticLibrary)
		}

		Debug.message("Starting sending " + maxIterations + " packets every " + StaticLibrary.interval + " milliseconds from " + nBuses + " buses");
		System.out.println("Ready, set...");

		BenchmarkOnibusV2 bench = new BenchmarkOnibusV2();
		
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
//		RMIServerIntf rmiServerIntf = null;

/*		// creates or connects to a RMI server
		if(BenchmarkOnibusV2.rmiServerAddress != null) {
			// this instance is the client
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
			// this instance is the server
			MyRMIServer.nClients++;
			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] nClients = " + MyRMIServer.nClients);
		}
*/
		// create each bus
		// create a thread for each bus
		/*
		 * The bus threads use canStart to synchronize themselves with this procedure.
		 * The bus threads will begin production when this code exists canStart region.
		 */
		synchronized(canStart) {
			Prefecture prefecture = new Prefecture();
			for(busNumber=0; busNumber<nBuses; busNumber++) {
				bus = prefecture.getABus(584);
				bus.setOrdem(bus.getOrdem()+busNumber);	// apenas para não haver ordem repetida durante o benchmark (esse comando deve ser retirado na versão de produção)
				bus.addGroup(1);
				bus.setUUID(UUID.randomUUID());
				if(!Prefecture.buses.containsKey(bus.getOrdem())) {
					System.out.println("Connecting bus #" + busNumber);
					Prefecture.buses.put(bus.getOrdem(), bus);
					busThread[busNumber] = new Thread(new BusThread(gatewayIP, gatewayPort, bus, busNumber, maxIterations, canStart, threadReturnValue));
					busThread[busNumber].start();
					try {
						Thread.sleep(StaticLibrary.intervalBetweenThreads);
					} catch (InterruptedException e) {
						System.err.println("Date = " + new Date());
						e.printStackTrace();
					}
				}
			}
			// adds a delay corresponding to 10% of the time of the connection phase to wait for the other sets of clients (other machines) to terminate their connection phases
/*			try {
				Thread.sleep((long) (nBuses * StaticLibrary.intervalBetweenThreads * 0.10));
			} catch (InterruptedException e) {
				System.err.println("Date = " + new Date());
				e.printStackTrace();
			}
*/
/*			if(BenchmarkOnibusV2.rmiServerAddress != null) {
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
*/
			System.out.println("When ready, press <ENTER> to GO:");
			Scanner scanner = new Scanner(System.in);
			scanner.nextLine();
			scanner.close();
		} // end of synchronize(canStart) ==> now the bus threads will really begin to produce data

		System.out.println("***** GO!!!! *****");

		startTime = System.currentTimeMillis();
		
		// wait for all bus threads to stop 
		for(int i=0; i<nBuses; i++) {
			try {
				busThread[i].join();
			} catch (InterruptedException e) {
				System.err.println("\nDate = " + new Date());
				e.printStackTrace();
			}
		}
		for(Map.Entry<String, Integer> entry : threadReturnValue.entrySet()) {
			if(entry.getValue() > 0) {
				nMessages += entry.getValue();
			}
		}

		stopTime = System.currentTimeMillis();
	}
}
