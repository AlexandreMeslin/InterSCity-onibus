/**
 * 
 */
package br.com.meslin.onibus.main;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import lac.cnet.groupdefiner.components.GroupDefiner;
import lac.cnet.groupdefiner.components.groupselector.GroupSelector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import br.com.meslin.onibus.aux.Debug;
import br.com.meslin.onibus.aux.StaticLibrary;
import br.com.meslin.onibus.aux.connection.Constants;
import br.com.meslin.onibus.aux.connection.HTTPException;
import br.com.meslin.onibus.aux.contextnet.BenchmarkMyGroupSelector;
import br.com.meslin.onibus.aux.contextnet.BenchmarkMyProcessingNode;
import br.com.meslin.onibus.aux.model.Bus;
import br.com.meslin.onibus.interscity.InterSCity;
import br.com.meslin.onibus.interscity.InterSCityConsumer;

/**
 * @author meslin
 * 
 * This application creates a core environment with a Processing Node thread and
 * It also creates a consumer to send data to a InterSCity 
 *
 */
public class BenchmarkMyCore {
	/*
	 * Configuration parameters
	 */
	/** InterSCity IP address */
	private static String interSCityIPAddress;
	/** group description file name */
	private static String filename; 

	/** An interface to InterSCity */
	private static InterSCity interSCity;
	public static Logger log = Logger.getLogger(BenchmarkMyCore.class);
	/** stores a queue of bus data to be sent to the InterSCity */
	public static ConcurrentLinkedQueue<Bus> busQueue = new ConcurrentLinkedQueue<Bus>(); 

	/**
	 * 
	 */
	public BenchmarkMyCore() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 * @throws HTTPException 
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	public static void main(String[] args) throws MalformedURLException, IOException, HTTPException {
		// Build date
		final Date buildDate = StaticLibrary.getClassBuildTime();
		System.out.println("BenchmarMyCore builed at " + buildDate);
		
		// get command line options
		Options options = new Options();
		Option option;
		
		option = new Option("a", "address", true, "ContextNet Gateway IP address");
		option.setRequired(false);
		options.addOption(option);

		option = new Option("f", "groupfilename", true, "Group description filename");
		option.setRequired(true);
		options.addOption(option);
		
		option = new Option("h", "force-headless", false, "Run as in a headless environment");
		option.setRequired(false);
		options.addOption(option);

		option = new Option("i", "InterSCity", true, "InterSCity IP address");
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
			System.err.println("Date = " +  new Date());
			formatter.printHelp("BenchmarkMyCore", options);
			e.printStackTrace();
			return;
		}
		
		// getting command line options
		// ContextNet IP address
		if((StaticLibrary.contextNetIPAddress = cmd.getOptionValue("address")) == null) {
			StaticLibrary.contextNetIPAddress = Constants.GATEWAY_IP;
		}
		// group description filename
		filename = cmd.getOptionValue("groupfilename");
		// ContextNet TCP port number
		try {
			StaticLibrary.contextNetPortNumber = Integer.parseInt(cmd.getOptionValue("port"));
		} catch(Exception e) {
			StaticLibrary.contextNetPortNumber = Constants.GATEWAY_PORT;
		}
		// InterSCity IP address
		interSCityIPAddress = cmd.getOptionValue("InterSCity");	// null if not available
		
		StaticLibrary.forceHeadless = cmd.hasOption("force-headless");
		Debug.warning("Running as headless: " + StaticLibrary.forceHeadless);

		StaticLibrary.nMessages = 0;		// for statistics

		/*
		 * Catch Ctrl+C
		 */
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	long elapsedTime = StaticLibrary.stopTime - StaticLibrary.startTime;
		    	System.err.println("CTRL+C");
		    	System.err.println("Time: " +  elapsedTime + " (" + StaticLibrary.stopTime + " - " + StaticLibrary.startTime + ") with " + StaticLibrary.nMessages + " messages");
		    }
		});

		System.out.println("\n\nStarting ContextNet Core using gateway at " + StaticLibrary.contextNetIPAddress + ":" + StaticLibrary.contextNetPortNumber + "\n\n");
		System.out.println("Ready, set...");

		// check and set InterSCity capabilities
		interSCity = new InterSCity(interSCityIPAddress);
		interSCity.checkInterSCity();

		/*
		 * Creating GroupSelector
		 */
		GroupSelector groupSelector = new BenchmarkMyGroupSelector(filename);
		new GroupDefiner(groupSelector);
		
		/*
		 * Create Processing Node
		 */
		new BenchmarkMyProcessingNode(busQueue);

		/*
		 * Create a thread to send bus data to the InterSCity
		 */
		Thread consumer = new Thread(new InterSCityConsumer(interSCity, busQueue));
		consumer.start();
		
		System.out.println("\nGO!");
		while(true) {}
	}
}
