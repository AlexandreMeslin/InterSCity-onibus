/**
 * 
 */
package br.com.meslin.onibus.aux.contextnet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import br.com.meslin.onibus.aux.Debug;
import br.com.meslin.onibus.aux.StaticLibrary;
import br.com.meslin.onibus.aux.model.BenchmarkDateTime;
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
public class PassengerThread implements Runnable, NodeConnectionListener, GroupMembershipListener {
	private int id;
	private double passengerLatitude;
	private double passengerLongitude;
	private String passengerName;
	private boolean mustFinish;
	
	
	/*
	 * Attributes
	 */
	private MrUdpNodeConnection connection;
	private GroupCommunicationManager groupManager;
	private UUID uuidLocal;

	
	/**
	 * Constructor<br>
	 * @param passengerLatitude
	 * @param passengerLongitude
	 * @param passengerName
	 */
	public PassengerThread(int id, double passengerLatitude, double passengerLongitude, String passengerName) {
		this.id = id;
		this.passengerLatitude = passengerLatitude;
		this.passengerLongitude = passengerLongitude;
		this.passengerName = passengerName + (this.id>0?this.id:"");
		
		this.mustFinish = false;

		/*
		 * Connect to ContextNet
		 */
		uuidLocal = UUID.randomUUID();
		InetSocketAddress address = new InetSocketAddress(StaticLibrary.contextNetIPAddress, StaticLibrary.contextNetPortNumber);
		try {
			connection = new MrUdpNodeConnection(uuidLocal);
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
		message.setContentObject(new Passenger(this.passengerName, this.passengerLatitude, this.passengerLongitude));
		try {
			connection.sendMessage(message);
		} catch (IOException e) {
			System.err.println(new Date());
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see lac.cnclib.net.groups.GroupMembershipListener#enteringGroups(java.util.List)
	 */
	@Override
	public void enteringGroups(List<Group> arg0) {}

	/* (non-Javadoc)
	 * @see lac.cnclib.net.groups.GroupMembershipListener#leavingGroups(java.util.List)
	 */
	@Override
	public void leavingGroups(List<Group> arg0) {}

	/* (non-Javadoc)
	 * @see lac.cnclib.net.NodeConnectionListener#connected(lac.cnclib.net.NodeConnection)
	 */
	@Override
	public void connected(NodeConnection remoteCon) {
		groupManager = new GroupCommunicationManager(remoteCon);
		groupManager.addMembershipListener(this);		
	}

	/* (non-Javadoc)
	 * @see lac.cnclib.net.NodeConnectionListener#disconnected(lac.cnclib.net.NodeConnection)
	 */
	@Override
	public void disconnected(NodeConnection arg0) {}

	/* (non-Javadoc)
	 * @see lac.cnclib.net.NodeConnectionListener#internalException(lac.cnclib.net.NodeConnection, java.lang.Exception)
	 */
	@Override
	public void internalException(NodeConnection arg0, Exception arg1) {}

	/* (non-Javadoc)
	 * @see lac.cnclib.net.NodeConnectionListener#newMessageReceived(lac.cnclib.net.NodeConnection, lac.cnclib.sddl.message.Message)
	 */
	@Override
	public void newMessageReceived(NodeConnection remoteCon, Message message) {
		System.out.println("Message received at " + (new Date()).getTime());
		System.out.println("Message received: " + message.getContentObject());
		
		BenchmarkDateTime dateTime = BenchmarkDateTime.getInstance();
		synchronized (dateTime) {
			if(dateTime.isEmptyStartTime())
				dateTime.setStartTime(System.currentTimeMillis());
			dateTime.setEndTime(System.currentTimeMillis());
		}
		this.mustFinish = true;
	}

	/* (non-Javadoc)
	 * @see lac.cnclib.net.NodeConnectionListener#reconnected(lac.cnclib.net.NodeConnection, java.net.SocketAddress, boolean, boolean)
	 */
	@Override
	public void reconnected(NodeConnection arg0, SocketAddress arg1, boolean arg2, boolean arg3) {}

	/* (non-Javadoc)
	 * @see lac.cnclib.net.NodeConnectionListener#unsentMessages(lac.cnclib.net.NodeConnection, java.util.List)
	 */
	@Override
	public void unsentMessages(NodeConnection arg0, List<Message> arg1) {}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 * Runs until mustFinish is setted true
	 */
	@Override
	public void run() {
		while(!this.mustFinish) {
			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException e) {
				System.err.println("\nDate = " + new Date());
				e.printStackTrace();
			}
		}
		Debug.warning("Thread " + this.id + " finishing");
	}
}
