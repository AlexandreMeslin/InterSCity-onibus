/**
 * Implements several passengers at the same bus stop<br>
 * Each passenger is represented by a thread<br>
 * To be used with br.com.meslin.onibus.aux.contextnet.BenchmarkMyGroupSelector
 */
package br.com.meslin.onibus.main;

import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import br.com.meslin.onibus.auxiliar.model.BenchmarkDateTime;
import br.com.meslin.onibus.auxiliar.Debug;
import br.com.meslin.onibus.auxiliar.StaticLibrary;
import br.com.meslin.onibus.auxiliar.connection.Constants;
import br.com.meslin.onibus.auxiliar.contextnet.PassengerThread;

/**
 * @author meslin
 *
 */
public class PassengerAtBusStop {

	/*
	 * Command line parameters
	 */
	private static double passengerLatitude;
	private static double passengerLongitude;
	private static String passengerName;
	/** number of passenger at the same bus stop */
	private static int nPassenger;


	/*
	 * Attributes
	 */
	private static Thread[] passengerThread;

	/**
	 * Constructor<br>
	 */
	public PassengerAtBusStop() {
		passengerThread = new Thread[nPassenger];
		for(int i=0; i<nPassenger; i++) {
			Debug.info("Creating thread #" + (i+1));
			passengerThread[i] = new Thread(new PassengerThread(i, passengerLatitude, passengerLongitude, passengerName));
			passengerThread[i].start();
			try {
				Thread.sleep(StaticLibrary.intervalBetweenThreads);
			} catch (InterruptedException e) {
				System.err.println("\nDate = " + new Date());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Main<br>
	 * @param args
	 */
	public static void main(String[] args) {
		final Date buildDate = StaticLibrary.getClassBuildTime();
		System.out.println("PassengerAtBusStop builded at " + buildDate);
		
		// Catch Ctrl+C
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	BenchmarkDateTime dateTime = BenchmarkDateTime.getInstance();
		    	Debug.warning("CTRL+C");
		    	Debug.warning(dateTime.toString());
		    }
		 });


		// get command line options
		Options options = new Options();
		Option option;

		option = new Option("a", "address", true, "ContextNet Gateway IP address");
		option.setRequired(false);
		options.addOption(option);

		option = new Option("p", "port", true, "ContextNet Gateway TCP port number");
		option.setRequired(false);
		options.addOption(option);

		option = new Option("l", "latitude", true, "Passenger's latitude in degrees");
		option.setRequired(true);
		options.addOption(option);

		option = new Option("n", "name", true, "Passenger's name");
		option.setRequired(false);
		options.addOption(option);

		option = new Option("o", "longitude", true, "Passenger's longitude in degrees");
		option.setRequired(true);
		options.addOption(option);
		
		option = new Option("q", "number", true, "number of passenger at the same bus stop");
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
		
		if((StaticLibrary.contextNetIPAddress = cmd.getOptionValue("address")) == null) {
			StaticLibrary.contextNetIPAddress = Constants.GATEWAY_IP;
		}
		try {
			StaticLibrary.contextNetPortNumber = Integer.parseInt(cmd.getOptionValue("port"));
		} catch(Exception e) {
			StaticLibrary.contextNetPortNumber = Constants.GATEWAY_PORT;
		}
		try {
			passengerLatitude = Double.parseDouble(cmd.getOptionValue("latitude"));
		} catch(Exception e) {
			System.err.println(new Date());
			e.printStackTrace();
		}
		try {
			passengerLongitude = Double.parseDouble(cmd.getOptionValue("longitude"));
		} catch(Exception e) {
			System.err.println(new Date());
			e.printStackTrace();
		}
		if((passengerName = cmd.getOptionValue("name")) == null) {
			passengerName = "NoName" + ((int) (Math.random() * 100000));
		}
		try {
			nPassenger = Integer.parseInt(cmd.getOptionValue("number"));
		} catch(Exception e) {
			nPassenger = 1;
		}

		new PassengerAtBusStop();
		System.out.println(nPassenger + " passenger(s) ready!");
		
		BenchmarkDateTime dateTime = BenchmarkDateTime.getInstance();
		// wait for all bus threads to stop 
		for(int i=0; i<nPassenger; i++) {
			try {
				Debug.info(dateTime.toString());
				passengerThread[i].join();
				Debug.info((i+1) + " passenger notified");
			} catch (InterruptedException e) {
				System.err.println("\nDate = " + new Date());
				e.printStackTrace();
			}
		}
		Debug.warning("All passengers had been notified");
	}
}
