package br.com.meslin.onibus.auxiliar.contextnet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;
import java.util.List;
import java.util.Map;

import lac.cnclib.net.NodeConnection;
import lac.cnclib.net.NodeConnectionListener;
import lac.cnclib.net.groups.Group;
import lac.cnclib.net.groups.GroupCommunicationManager;
import lac.cnclib.net.groups.GroupMembershipListener;
import lac.cnclib.net.mrudp.MrUdpNodeConnection;
import lac.cnclib.sddl.message.ApplicationMessage;
import lac.cnclib.sddl.message.Message;
import br.com.meslin.onibus.auxiliar.Debug;
import br.com.meslin.onibus.auxiliar.StaticLibrary;
import br.com.meslin.onibus.auxiliar.model.Bus;
import br.com.meslin.onibus.main.BenchmarkOnibusV2;

public class BusThread implements Runnable, NodeConnectionListener, GroupMembershipListener {
	private int id;		// just a sequential to index the onibus vectors (bus thread vector vs bus return value vector) 
	private long maxIterations; 
	private Object canStart;		// just a sync object
	private Map<String, Integer> threadReturnValue;
	private String gatewayIP;
	private int gatewayPort;
	private MrUdpNodeConnection connection;
	private Bus bus;
	private GroupCommunicationManager groupManager;
	private long backoffTime;		// backoff time between reconnections
	private volatile static Long lastConnectionTime = new Long(0);
	
	
	/**
	 * Constructor<br>
	 * @param gatewayIP
	 * @param gatewayPort
	 * @param bus
	 * @param id
	 * @param maxIterations
	 * @param canStart
	 * @param threadReturnValue
	 */
	public BusThread(String gatewayIP, int gatewayPort, Bus bus, int id, long maxIterations, Object canStart, Map<String, Integer> threadReturnValue) {
		this.gatewayIP = gatewayIP;
		this.gatewayPort = gatewayPort;
		this.bus = bus;
		this.id = id;
		this.maxIterations = maxIterations;
		this.canStart = canStart;
		this.threadReturnValue = threadReturnValue;
		
		this.threadReturnValue.put(bus.getOrdem(), 0);
		
		this.backoffTime = 1;
		InetSocketAddress address = new InetSocketAddress(this.gatewayIP, this.gatewayPort);
		try {
			this.connection = new MrUdpNodeConnection();
			this.connection.addNodeConnectionListener(this);
			this.connection.connect(address);
		} catch (IOException e) {
			System.err.println("Date = " + new Date());
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * BusThread constructor<br>
	 * @param gatewayIP
	 * @param gatewayPort
	 * @param bus
	 * @param threadReturnValue
	 */
	public BusThread(String gatewayIP, int gatewayPort, Bus bus, Map<String, Integer> threadReturnValue) {
		this.gatewayIP = gatewayIP;
		this.gatewayPort = gatewayPort;
		this.bus = bus;
		this.maxIterations = 0;
		this.threadReturnValue = threadReturnValue;
		this.threadReturnValue.put(bus.getOrdem(), 0);
		this.backoffTime = 1;
	}



	/**
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		double lastLat = 0;
		double lastLon = 0;
		/*
		 * canStart is null when there is no need for thread synchronization during connection phase.
		 * The only requirement is to not make a connection in less than intervalBetweenThreads milliseconds.
		 * The variable lastConnectionTime stores the time the last connection occurred.
		 * Otherwise (if canStart is not null), must wait for canStart synchronization to begin to send data.
		 */
		if(canStart == null) {
			synchronized (threadReturnValue) {	// instead of threadReturnValue, it should be lastConnectionTime, but it does not work with this last option
				while(lastConnectionTime + StaticLibrary.intervalBetweenThreads > (new Date()).getTime()) {}
				lastConnectionTime = (new Date()).getTime();
			}
			InetSocketAddress address = new InetSocketAddress(this.gatewayIP, this.gatewayPort);
			try {
				this.connection = new MrUdpNodeConnection();
				this.connection.addNodeConnectionListener(this);
				this.connection.connect(address);
			} catch (IOException e) {
				System.err.println("Date = " + new Date());
				e.printStackTrace();
			}
		}
		else {
			/*
			 * TODO
			 * This loop is (probabily) unecessary.
			 * The synchronized instruction should be sufficent, but it's not working
			 * for a huge amount of buses (more then 16000).
			 * This workaround **MUST** be removed after the tests were completed.
			 */
			while(BenchmarkOnibusV2.busNumber < BenchmarkOnibusV2.nBuses) {
				synchronized (canStart) {}		// wait all threads to be ready to go
				Debug.warning("Starting...");
			}
		}
		
		// wait a while to begin to send packets
		try {
			Thread.sleep((long) (StaticLibrary.interval - StaticLibrary.interval / StaticLibrary.variance + 2 * (Math.random() * StaticLibrary.interval)/StaticLibrary.variance));
		} catch (InterruptedException e1) {
			System.err.println("Date = " + new Date());
			e1.printStackTrace();
		}

		for(long i=0; (maxIterations!=0 && i<maxIterations) || (maxIterations==0 && !Thread.interrupted()); i++) {
			// will send a new position only if its a NEW position (will not send the same position twice)
			// but always will send position if in qtd iteration mode (load benchmark)
			if(maxIterations != 0 || (lastLat != bus.getLatitude() && lastLon != bus.getLongitude())) {
				if(threadReturnValue.get(bus.getOrdem())%1000==0) System.out.print(".");
				lastLat = bus.getLatitude();
				lastLon = bus.getLongitude();
				if(!sendBusToContextNet()) {
					System.err.println("\n[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Thread #" + id + " could not update database");
					threadReturnValue.put(bus.getOrdem(),  -1);	// error type: DB update error
					break;
				}
			}
			threadReturnValue.put(bus.getOrdem(), threadReturnValue.get(bus.getOrdem()) +1);
			try {
//				Thread.sleep(2000);
				Thread.sleep((long) (StaticLibrary.interval - StaticLibrary.interval / StaticLibrary.variance + 2 * (Math.random() * StaticLibrary.interval)/StaticLibrary.variance));
			} catch (InterruptedException e) {
				System.err.println("Date = " + new Date());
				e.printStackTrace();
			}
		}
		Debug.warning("Thread " + bus.getOrdem() + " finished");
	}

	
	
	/**
	 * send a message with a bus to ContextNet
	 * @return true if success
	 */
	public boolean sendBusToContextNet() {
		boolean success = true;
//		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] with group size of " + this.getGroups().size());
		// sends coordinates to the selector
		try {
			ApplicationMessage message = new ApplicationMessage();
			if(this.bus.getLinha().equals("117") && this.bus.getOrdem().equals("A72005")) Debug.warning("This bus " + this.bus + " at " + System.currentTimeMillis());
			message.setContentObject(this.bus);
			this.connection.sendMessage(message);
		}
		catch (IOException e) {
			System.err.println("Date = " + new Date());
			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Tryed to send bus #" + bus.getOrdem());
			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Sent bus #" + bus.getLinha());
			success = false;
			e.printStackTrace();
		}
		return success;
	}

	
	
	/**
	 * Do nothing until now, but nobody knows the future
	 * @param remoteCon (useless)
	 */
	@Override
	public void connected(NodeConnection remoteCon) {
		this.groupManager = new GroupCommunicationManager(remoteCon);
		this.groupManager.addMembershipListener(this);
//		this.remoteCon = remoteCon;
	}

	
	
	/**
	 * Callback for a new message<br>
	 * Just set the new bus group (if available)<br>
	 * @param remoteCon
	 * @param message
	 */
	@Override
	public void newMessageReceived(NodeConnection remoteCon, Message message) {
		if(message.getContentObject() instanceof Bus) {
			Bus bus = (Bus) message.getContentObject();
			System.err.print("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "]\n" + message.getSenderID() + "\n" + bus.getLinha() + "@" + bus.getOrdem() + " Grupos: ");
			for(int i : bus.getGroups()) System.err.print(i + " ");
			System.err.println();
			
			// TODO trocar o gateway
			bus.setGroups(bus.getGroups());
		}
	}

	@Override
	public void disconnected(NodeConnection remoteCon) {
		try {
			Thread.sleep((long) (backoffTime + Math.random() * backoffTime));
		} catch (InterruptedException e1) {
			System.err.println("Date = " + new Date());
			e1.printStackTrace();
		}
		this.backoffTime *= 2;	// double backoff time each disconnection
		
		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Disconnected at " + new Date());
		System.out.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Disconnected at " + new Date());
		InetSocketAddress address = new InetSocketAddress(this.gatewayIP, this.gatewayPort);
		try {
			this.connection = new MrUdpNodeConnection();
			this.connection.addNodeConnectionListener(this);
			this.connection.connect(address);
			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Reconnected at " + new Date());
			System.out.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Reconnected at " + new Date());
		} catch (IOException e) {
			System.err.println("Date = " + new Date());
			e.printStackTrace();
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