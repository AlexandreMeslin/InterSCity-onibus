package br.com.meslin.onibus.aux.contextnet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;
import java.util.List;

import lac.cnclib.net.NodeConnection;
import lac.cnclib.net.NodeConnectionListener;
import lac.cnclib.net.groups.Group;
import lac.cnclib.net.groups.GroupCommunicationManager;
import lac.cnclib.net.groups.GroupMembershipListener;
import lac.cnclib.net.mrudp.MrUdpNodeConnection;
import lac.cnclib.sddl.message.ApplicationMessage;
import lac.cnclib.sddl.message.Message;
import br.com.meslin.onibus.aux.StaticLibrary;
import br.com.meslin.onibus.aux.model.Bus;

public class BusThread implements Runnable, NodeConnectionListener, GroupMembershipListener{
	private int id;		// just a sequencial to index the onibus vectors (bus thread vector vs bus return value vector) 
	private long maxIterations; 
	private Object canStart;		// just a sincronize object
	private int[] threadReturnValue;
	private String gatewayIP;
	private int gatewayPort;
	private MrUdpNodeConnection connection;
	private Bus bus;
	private GroupCommunicationManager groupManager;
	private long backoffTime;	// backoff time between reconnections

	
	
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
	public BusThread(String gatewayIP, int gatewayPort, Bus bus, int id, long maxIterations, Object canStart, int[] threadReturnValue) {
		this.gatewayIP = gatewayIP;
		this.gatewayPort = gatewayPort;
		this.bus = bus;
		this.id = id;
		this.maxIterations = maxIterations;
		this.canStart = canStart;
		this.threadReturnValue = threadReturnValue;
		
		this.threadReturnValue[id] = 0;
		
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
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		synchronized (canStart) {}		// wait all threads to be ready to go

		// wait a while to begin to send packets
		try {
			Thread.sleep(StaticLibrary.interval);
		} catch (InterruptedException e1) {
			System.err.println("Date = " + new Date());
			e1.printStackTrace();
		}

		for(long i=0; i<maxIterations; i++) {
			if(threadReturnValue[id]%100==0) System.out.print(".");
			if(!sendBusToContextNet()) {
				System.err.println("\n[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Thread #" + id + " could not update database");
				threadReturnValue[id] = -1;	// error type: DB update error
				break;
			}
			threadReturnValue[id]++;
			try {
				Thread.sleep((long) (StaticLibrary.interval - StaticLibrary.interval / StaticLibrary.variance + 2 * (Math.random() * StaticLibrary.interval)/StaticLibrary.variance));
			} catch (InterruptedException e) {
				System.err.println("Date = " + new Date());
				e.printStackTrace();
			}
		}
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
