/**
 * 
 */
package br.com.meslin.onibus.main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import br.com.meslin.onibus.aux.StaticLibrary;
import br.com.meslin.onibus.aux.connection.Constants;
import br.com.meslin.onibus.aux.model.Passenger;
import lac.cnclib.net.NodeConnection;
import lac.cnclib.net.NodeConnectionListener;
import lac.cnclib.net.groups.Group;
import lac.cnclib.net.groups.GroupCommunicationManager;
import lac.cnclib.net.groups.GroupMembershipListener;
import lac.cnclib.net.mrudp.MrUdpNodeConnection;
import lac.cnclib.sddl.message.ApplicationMessage;
import lac.cnclib.sddl.message.Message;

/**
 * @author meslin
 *
 */
public class PassengerAtBusStop implements NodeConnectionListener, GroupMembershipListener {

	/*
	 * Command line parameters
	 */
	private static double passengerLatitude;
	private static double passengerLongitude;
	private static String passengerName;

	
	/*
	 * Attributes
	 */
	private MrUdpNodeConnection connection;
	private GroupCommunicationManager groupManager;


	/**
	 * Constructor<br>
	 */
	public PassengerAtBusStop() {
		/*
		 * Connect to ContextNet
		 */
		StaticLibrary.uuidLocal = UUID.randomUUID();
		InetSocketAddress address = new InetSocketAddress(StaticLibrary.contextNetIPAddress, StaticLibrary.contextNetPortNumber);
		try {
			connection = new MrUdpNodeConnection(StaticLibrary.uuidLocal);
			connection.connect(address);
			connection.addNodeConnectionListener(this);
		} catch (IOException e) {
			System.err.println(new Date());
			e.printStackTrace();
		}
		
		/*
		 * Send passenger position (latitude and longitude)
		 */
		ApplicationMessage message = new ApplicationMessage();
		message.setContentObject(new Passenger(passengerName, passengerLatitude, passengerLongitude));
		try {
			connection.sendMessage(message);
		} catch (IOException e) {
			System.err.println(new Date());
			e.printStackTrace();
		}
	}

	/**
	 * Main<br>
	 * @param args
	 */
	public static void main(String[] args) {
		final Date buildDate = StaticLibrary.getClassBuildTime();
		System.out.println("PassengerAtBusStop builded at " + buildDate);

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

		new PassengerAtBusStop();
	}

	@Override
	public void enteringGroups(List<Group> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void leavingGroups(List<Group> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void connected(NodeConnection remoteCon) {
		groupManager = new GroupCommunicationManager(remoteCon);
		groupManager.addMembershipListener(this);		
	}

	@Override
	public void disconnected(NodeConnection arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void internalException(NodeConnection arg0, Exception arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void newMessageReceived(NodeConnection remoteCon, Message message) {
		System.out.println("Message received at " + (new Date()).getTime());
		System.out.println("Message received: " + message.getContentObject());
	}

	@Override
	public void reconnected(NodeConnection arg0, SocketAddress arg1,
			boolean arg2, boolean arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unsentMessages(NodeConnection arg0, List<Message> arg1) {
		// TODO Auto-generated method stub
		
	}
}
