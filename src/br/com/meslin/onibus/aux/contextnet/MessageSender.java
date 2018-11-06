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

import lac.cnclib.net.NodeConnection;
import lac.cnclib.net.NodeConnectionListener;
import lac.cnclib.net.groups.Group;
import lac.cnclib.net.groups.GroupCommunicationManager;
import lac.cnclib.net.mrudp.MrUdpNodeConnection;
import lac.cnclib.sddl.message.ApplicationMessage;
import lac.cnclib.sddl.message.ClientLibProtocol.PayloadSerialization;
import lac.cnclib.sddl.message.Message;

import org.json.JSONObject;

import br.com.meslin.onibus.aux.StaticLibrary;

/**
 * Class to send message from the core to a group
 * @author meslin
 *
 */
public final class MessageSender implements NodeConnectionListener {
	private static final MessageSender INSTANCE = new MessageSender();
	private static GroupCommunicationManager groupManager;
	private MrUdpNodeConnection connection;

	/**
	 * 
	 */
	private MessageSender() {
		StaticLibrary.uuidLocal = UUID.randomUUID();
		StaticLibrary.sequencial = 0;

		/*
		 * Connect to the infrastruct
		 */
		InetSocketAddress endereco = new InetSocketAddress(StaticLibrary.contextNetIPAddress, StaticLibrary.contextNetPortNumber);
		try {
			connection = new MrUdpNodeConnection(StaticLibrary.uuidLocal);
			// *** inverti a ordem para ver se o pedido de conexão fica mais estável
			connection.addNodeConnectionListener(this);
			connection.connect(endereco);
		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static MessageSender getInstance() {
		return INSTANCE;
	}
	
	public boolean sendMessageToGroup(int targetGroup, JSONObject jsonObject) {
		Group group = new Group(StaticLibrary.PASSENGER_GROUP, targetGroup);
		ApplicationMessage message = new ApplicationMessage();
		message.setSenderID(StaticLibrary.uuidLocal);
		message.setPayloadType(PayloadSerialization.JSON);
		message.setContentObject(jsonObject.toString());
		try {
			groupManager.sendGroupcastMessage(message, group);
		} catch (IOException e) {
			System.err.println("Date = " + new Date());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public void connected(NodeConnection remoteCon) {
		groupManager  = new GroupCommunicationManager(remoteCon);
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
