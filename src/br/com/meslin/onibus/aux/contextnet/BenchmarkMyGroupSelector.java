/**
 * 
 */
package br.com.meslin.onibus.aux.contextnet;

import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import br.com.meslin.onibus.aux.GeographicMap;
import br.com.meslin.onibus.aux.StaticLibrary;
import br.com.meslin.onibus.aux.model.Bus;
import br.com.meslin.onibus.aux.model.Region;
import lac.cnclib.net.mrudp.MrUdpNodeConnection;
import lac.cnclib.sddl.message.ApplicationMessage;
import lac.cnet.groupdefiner.components.groupselector.GroupSelector;
import lac.cnet.sddl.objects.GroupRegion;
import lac.cnet.sddl.objects.Message;

/**
 * @author meslin
 *
 */
public class BenchmarkMyGroupSelector implements GroupSelector {
	private GeographicMap map;
	private List<Region> regionList;
	private List<Bus> busList;
	private String contextNetIPAddress;
	private int contextNetPortNumber;

	/**
	 * 
	 * @param contextNetIPAddress
	 * @param contextNetPortNumber
	 * @param filename
	 */
	public BenchmarkMyGroupSelector(String contextNetIPAddress, int contextNetPortNumber, String name) {
		this.contextNetIPAddress = contextNetIPAddress;
		this.contextNetPortNumber = contextNetPortNumber;
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
		if(!GraphicsEnvironment.isHeadless()) {
			map = new GeographicMap(regionList);
			map.setVisible(true);
		}
		
		busList = new ArrayList<Bus>();	// the bus list starts empty
	}

	/* (non-Javadoc)
	 * @see lac.cnet.groupdefiner.components.groupselector.GroupSelector#createGroup(lac.cnet.sddl.objects.GroupRegion)
	 */
	@Override
	public void createGroup(GroupRegion arg0) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see lac.cnet.groupdefiner.components.groupselector.GroupSelector#getGroupType()
	 */
	@Override
	public int getGroupType() {
		return 3;
	}

	/* (non-Javadoc)
	 * @see lac.cnet.groupdefiner.components.groupselector.GroupSelector#processGroups(lac.cnet.sddl.objects.Message)
	 */
	@Override
	public Set<Integer> processGroups(Message nodeMessage) {
		Bus bus = null;

		// get bus position
		try {
			bus = (Bus) new ObjectInputStream(new ByteArrayInputStream(nodeMessage.getContent())).readObject();
		}
		catch (IOException | ClassNotFoundException e) {
			System.err.println("Date = " + new Date());
			e.printStackTrace();
		}
		HashSet<Integer> groups = new HashSet<Integer>(2, 1);
		UUID uuid = nodeMessage.getSenderId();
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

		return groups;
	}
}
