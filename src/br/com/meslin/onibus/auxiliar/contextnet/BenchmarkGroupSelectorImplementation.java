package br.com.meslin.onibus.auxiliar.contextnet;

import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lac.cnclib.net.NodeConnection;
import lac.cnclib.net.NodeConnectionListener;
import lac.cnet.groupdefiner.components.groupselector.GroupSelector;
import lac.cnet.sddl.objects.GroupRegion;
import lac.cnet.sddl.objects.Message;
import br.com.meslin.onibus.auxiliar.GeographicMap;
import br.com.meslin.onibus.auxiliar.StaticLibrary;
import br.com.meslin.onibus.auxiliar.connection.HTTPException;
import br.com.meslin.onibus.auxiliar.model.Bus;
import br.com.meslin.onibus.auxiliar.model.Region;
import br.com.meslin.onibus.auxiliar.model.SamplePredicate;
import br.com.meslin.onibus.main.BenchmarkDefineGroup;

public class BenchmarkGroupSelectorImplementation implements GroupSelector, NodeConnectionListener {
	// properties
	private GeographicMap map;
	private List<Region> regionList;
	private List<Bus> busList;
//	private String contextNetIPAddress;
//	private int contextNetPortNumber;
	
	/**
	 * <p>Builds regions based on region description files</p>
	 * <p>File of filenames: a filename per line (path, if included, may be relative ou full)</p>
	 * <p>File of regions: each X, Y representing coordinates per line separeted by a blank</p>
	 * 
	 * @param name filename with region filenames, one per line
	 * @param filename2 
	 * @param contextNetPortNumber2 
	 * @throws HTTPException 
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	public BenchmarkGroupSelectorImplementation(String contextNetIPAddress, int contextNetPortNumber, String name) throws MalformedURLException, IOException, HTTPException
	{
//		this.contextNetIPAddress = contextNetIPAddress;
//		this.contextNetPortNumber = contextNetPortNumber;
		List<String> filenames = StaticLibrary.readFilenamesFile(name);
		
		// reads each region file
		this.regionList = new ArrayList<Region>();	// region list
		int regionNumber = 1;	// region number. Each region has a number assigned sequentially
		for(String filename : filenames) {
			Region region = StaticLibrary.readRegion(filename, regionNumber);
			regionList.add(region);
			regionNumber++;
		}

		// checks if there is an graphic environment available (true if not, otherwise, false)
		if(!GraphicsEnvironment.isHeadless() && !StaticLibrary.forceHeadless) {
			map = new GeographicMap(regionList);
			map.setVisible(true);
		}
		
		busList = new ArrayList<Bus>();	// the bus list starts empty
	}



	/**
	 * Returns the group type (always 3)
	 */
	@Override
	public int getGroupType() {
		return 3;
	}

	
	
	/**
	 * Seleciona o grupo do ônibus de acordo com a sua região (longitude e latitude)
	 * <p>This version also creates/updates the bus on the InterSCity 
	 */
	@Override
	public Set<Integer> processGroups(Message nodeMessage) {
		if(StaticLibrary.nMessages % 10000 == 0) System.out.print("C");
		// set the start time, if not setted yet
		if(StaticLibrary.startTime < 0) {
			StaticLibrary.startTime = System.currentTimeMillis();
			System.err.println("\n\n\n***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Starting at " + StaticLibrary.startTime);
		}
		// update the number of messages
		StaticLibrary.nMessages++;
		
		// gets bus position
		Bus bus = null;
		try {
			bus = (Bus) new ObjectInputStream(new ByteArrayInputStream(nodeMessage.getContent())).readObject();
		}
		catch (IOException | ClassNotFoundException e) {
			System.err.println("Date = " + new Date());
			e.printStackTrace();
		}

		// updates bus position on the map
		busList.removeIf(new SamplePredicate(bus.getOrdem()));
		busList.add(bus);
		if(!GraphicsEnvironment.isHeadless() && !StaticLibrary.forceHeadless) {
			map.remove(bus);
			map.addBus(bus);
		}
		
		HashSet<Integer> groups = new HashSet<Integer>(2, 1);
//		UUID uuid = nodeMessage.getSenderId();
		// procura as regiões onde o ônibus pode estar
		for(Region region : regionList)
		{
			if (region.contains(bus)) {
				groups.add(region.getNumero());
			}
		}
		bus.setGroups(groups);
		// sends information about current group to the bus
/* ******************* uncomment those line to response to the mobile object ****************************************
		InetSocketAddress address = new InetSocketAddress(this.contextNetIPAddress, this.contextNetPortNumber);
		UUID senderUUID = UUID.randomUUID();
		try {
			MrUdpNodeConnection connection = new MrUdpNodeConnection(senderUUID);
			connection.addNodeConnectionListener(this);
			connection.connect(address);
			ApplicationMessage message = new ApplicationMessage();
			message.setContentObject(bus);
			message.setRecipientID(uuid);
			connection.sendMessage(message);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
//*/
//		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Adding bus " + bus.getLinha() + "@" + bus.getOrdem());
		// update InterSCity DB
		// only updates the DB if this bus has a group (i.e., this bus is inside at least one region)
		if(bus.getGroups().size() > 0) {
			// TODO remover o comentário para transmitir dados para o InterSCity
//			BenchmarkDefineGroup.busQueue.add(bus);
			synchronized (BenchmarkDefineGroup.busQueue) {
				BenchmarkDefineGroup.busQueue.notify();
			}
		}

		StaticLibrary.stopTime = System.currentTimeMillis();
		return groups;
	}

	
	



	/**
	 * <p>Formats the UUID</p>
	 * <ul>
	 *  <li>XXXXXXXX: bits 127-96 (32 bits)</li>
	 *  <li>XXXX: bits 95-80 (16 bits)</li>
	 *  <li>XXXX: bits 79-64 (16 bits)</li>
	 *  <li>XXXX: bits 63-48 (16 bits)</li>
	 *  <li>XXXXXXXXXXXX: bits 47-0 (48 bits)</li>
	 * </ul>
	 * 
	 * @param mostSignificantBits
	 * @param leastSignificantBits
	 * @return UUID int the format XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
	 */
	/*
	private String formatUUID(long mostSignificantBits, long leastSignificantBits)
	{
		String result = "";
		result += Integer.toHexString((int)(mostSignificantBits>>(96-64)));				// 127-96
		result += "-";
		result += Integer.toHexString((int)((mostSignificantBits>>(80-64)) & 0xFFFF));	// 95-80
		result += "-";
		result += Integer.toHexString((int)(mostSignificantBits & 0xFFFF));				// 95-80
		result += "-";
		result += Integer.toHexString((int)((leastSignificantBits>>48) & 0xFFFF));		// 63-48
		result += "-";
		result += Integer.toHexString((int)((leastSignificantBits>>32) & 0xFFFF));		// 47-32
		result += Integer.toHexString((int)(leastSignificantBits & 0xFFFFFFFF));		// 31-0
		return result;
	}
	*/
	@Override
	public void createGroup(GroupRegion arg0) {}
	@Override
	public void connected(NodeConnection connection) {}
	@Override
	public void disconnected(NodeConnection connection) {
		System.err.println("\n\n\n***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] " + new Date());
	}
	@Override
	public void internalException(NodeConnection connection, Exception e) {
		System.err.println("\n\n\n***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "]");
	}
	@Override
	public void newMessageReceived(NodeConnection connection, lac.cnclib.sddl.message.Message arg1) {}
	@Override
	public void reconnected(NodeConnection connection, SocketAddress arg1, boolean arg2, boolean arg3) {}
	@Override
	public void unsentMessages(NodeConnection connection, List<lac.cnclib.sddl.message.Message> arg1) {}
}