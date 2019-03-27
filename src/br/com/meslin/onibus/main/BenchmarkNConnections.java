package br.com.meslin.onibus.main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import lac.cnclib.net.NodeConnection;
import lac.cnclib.net.NodeConnectionListener;
import lac.cnclib.net.groups.Group;
import lac.cnclib.net.groups.GroupMembershipListener;
import lac.cnclib.net.mrudp.MrUdpNodeConnection;
import lac.cnclib.sddl.message.Message;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import prefecture.Prefecture;
import br.com.meslin.onibus.aux.Debug;
import br.com.meslin.onibus.aux.StaticLibrary;
import br.com.meslin.onibus.aux.connection.Constants;
import br.com.meslin.onibus.aux.model.Bus;

/**
 * @author Meslin
 * Creates lots of bus connections to ContextNet Gateway but does not send data<br>
 */

public class BenchmarkNConnections implements NodeConnectionListener, GroupMembershipListener {
	// properties
	private static String gatewayIP;
	private static int gatewayPort;
	/** number of buses, for simulation purpose */
	private static int nBuses;					
	/** ContextNet connection */
	private MrUdpNodeConnection connection;

	public BenchmarkNConnections() {
		System.out.println("[" + this.getClass().getName() + "." + "Benchmark] ContextNet address: " + gatewayIP + ":" + gatewayPort + "\n\n");
	}

	public static void main(String[] args) {
		final Date buildDate = StaticLibrary.getClassBuildTime();
		System.out.println("BenchmarkNConnections builded at " + buildDate);
		
		// get command line options
		Options options = new Options();
		Option option;

		option = new Option("a", "address", true, "ContextNet Gateway IP address");
		option.setRequired(false);
		options.addOption(option);

		option = new Option("i", "interval", true, "Interval between connection creation in milliseconds");
		option.setRequired(false);
		options.addOption(option);

		option = new Option("p", "port", true, "ContextNet Gateway IP port number");
		option.setRequired(false);
		options.addOption(option);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e1) {
			System.err.println("Date = " + new Date());
			formatter.printHelp("BenchmarkNConnections", options);
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
			StaticLibrary.intervalBetweenThreads = Integer.parseInt(cmd.getOptionValue("interval"));
		} catch(Exception e) {
			// no default parameters setted here (it is setted at StaticLibrary
		}

		BenchmarkNConnections bench = new BenchmarkNConnections();
		System.out.println("[BenchmarkNConnections.main] Starting connections every " + StaticLibrary.interval + " milliseconds");
		bench.doAll();
	}

	private void doAll() {
		Bus bus;
		boolean error = false;
		Prefecture prefecture = new Prefecture();
		nBuses = 0;
		
		while (!error) {
			bus = prefecture.getABus(584);
			bus.setOrdem(bus.getOrdem()+nBuses);	// apenas para não haver ordem repetida durante o benchmark (esse comando deve ser retirado na versão de produção)
			nBuses++;
			bus.addGroup(1);
			bus.setUUID(UUID.randomUUID());
			Prefecture.buses.put(bus.getOrdem(), bus);
			
			InetSocketAddress address = new InetSocketAddress(BenchmarkNConnections.gatewayIP, BenchmarkNConnections.gatewayPort);
			try {
				this.connection = new MrUdpNodeConnection();
				this.connection.addNodeConnectionListener(this);
				this.connection.connect(address);
			} catch (IOException e) {
				Debug.warning("Error at bus #" + nBuses);
				System.err.println("Date = " + new Date());
				e.printStackTrace();
				return;
			}

			// sleep for a while
			try {
				Thread.sleep(StaticLibrary.intervalBetweenThreads);
			} catch (InterruptedException e) {
				System.err.println("Date = " + new Date());
				e.printStackTrace();
			}
		}
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
	public void connected(NodeConnection arg0) {
		// TODO Auto-generated method stub
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
	public void newMessageReceived(NodeConnection arg0, Message arg1) {
		// TODO Auto-generated method stub
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
