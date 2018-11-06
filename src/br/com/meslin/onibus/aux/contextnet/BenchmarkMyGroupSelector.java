/**
 * 
 */
package br.com.meslin.onibus.aux.contextnet;

import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import lac.cnet.groupdefiner.components.groupselector.GroupSelector;
import lac.cnet.sddl.objects.GroupRegion;
import lac.cnet.sddl.objects.Message;
import br.com.meslin.onibus.aux.Debug;
import br.com.meslin.onibus.aux.GeographicMap;
import br.com.meslin.onibus.aux.StaticLibrary;
import br.com.meslin.onibus.aux.event.EventRegion;
import br.com.meslin.onibus.aux.event.EventBusArrivingListener;
import br.com.meslin.onibus.aux.model.Bus;
import br.com.meslin.onibus.aux.model.MobileObject;
import br.com.meslin.onibus.aux.model.Passenger;
import br.com.meslin.onibus.aux.model.Region;
import br.com.meslin.onibus.aux.model.SamplePredicate;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;

/**
 * @author meslin
 *
 */
public class BenchmarkMyGroupSelector implements GroupSelector {
	private GeographicMap map;
	private List<Region> regionList;
	private List<Bus> busList;
//	private String contextNetIPAddress;
//	private int contextNetPortNumber;

	// CEP	
	private EPServiceProvider epService;


	/**
	 * Creates a Group Definer based on ContextNet IP address, ContextNet port number and region description filename 
	 * @param filename
	 */
	public BenchmarkMyGroupSelector(String name) {
		List<String> lines = StaticLibrary.readFilenamesFile(name);
		
		/*
		 * reads each region file
		 */
		this.regionList = new ArrayList<Region>();	// region list
		for(String line : lines) {
			int regionNumber = Integer.parseInt(line.substring(0, line.indexOf(" ")).trim());
			String filename = line.substring(line.indexOf(" ")).trim();
			Region region = StaticLibrary.readRegion(filename, regionNumber);
			regionList.add(region);
		}

		/*
		 * checks if there is an graphic environment available (true if not, otherwise, false)
		 */
		if(!GraphicsEnvironment.isHeadless() && !StaticLibrary.forceHeadless) {
			map = new GeographicMap(regionList);
			map.setVisible(true);
		}
		
		busList = new ArrayList<Bus>();	// the bus list starts empty
		
		/*
		 * Create an CEP
		 */
		// configuration
		Configuration config = new Configuration();
		config.addEventTypeAutoName("br.com.meslin.onibus.aux.event");
		config.addEventType("EventRegion", EventRegion.class.getName());
		// creating a statement
		epService = EPServiceProviderManager.getProvider("myCEPEngine", config);
//		String expression = "select * from EventRegion having EventRegion.bus.linha = \"117\" and EventRegion.region = 1";
		String expression = "select * from EventRegion having EventRegion.region = 1";
//		String expression = "select * from EventRegion";
		EPStatement statement = epService.getEPAdministrator().createEPL(expression);
		statement.addListener(new EventBusArrivingListener());
	}

	/* (non-Javadoc)
	 * @see lac.cnet.groupdefiner.components.groupselector.GroupSelector#createGroup(lac.cnet.sddl.objects.GroupRegion)
	 */
	@Override
	public void createGroup(GroupRegion arg0) {}

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
		Passenger passenger = null;
		MobileObject mobileObject = null;
		
		// TODO: how to handle message from a real Mobile-Hub? Details at http://wiki.lac.inf.puc-rio.br/doku.php?id=m_hub
		try {
			mobileObject = (MobileObject) new ObjectInputStream(new ByteArrayInputStream(nodeMessage.getContent())).readObject();
		} catch (ClassNotFoundException | IOException e1) {
			try {
				// if a nodeMessage cames from M-Hub, its is a JSON string coded as byte[]
				mobileObject = new Passenger(new JSONObject(new String(nodeMessage.getContent())));
			} catch (Exception e) {
				System.err.println("Date = " + new Date());
				e.printStackTrace();
			}
		}
		
		if(mobileObject instanceof Bus) {
			bus = (Bus) mobileObject;			
			// update bus position on the map
			busList.removeIf(new SamplePredicate(bus.getOrdem()));
			busList.add(bus);
			if(!GraphicsEnvironment.isHeadless() && !StaticLibrary.forceHeadless) {
				// TODO remover esse comentário para o mapa mostrar somente a última posição de cada ônibus
				map.remove(bus);
				map.addBus(bus);
			}
		}
		else if(mobileObject instanceof Passenger) {
			passenger = (Passenger) mobileObject;
			Debug.println("Passenger ==> " + passenger);
			map.addPassenger(passenger);
		}

		HashSet<Integer> newGroups = new HashSet<Integer>(2, 1);
		// search the regions where the bus may be
		for(Region region : regionList) {
			if (region.contains(mobileObject)) {
				newGroups.add(region.getNumero());
			}
		}
		
		if(mobileObject instanceof Bus) {
			// check all new groups to generate bus "entering a new region" event
			for(int group : newGroups) {
				if(!bus.getGroups().contains(group)) {
					EventRegion eventRegion = new EventRegion(bus, group, System.currentTimeMillis());
					epService.getEPRuntime().sendEvent(eventRegion);
					if(bus.getLinha().equals("117") && group == 1) Debug.println("Evento sendo gerado");
				}
			}
			bus.setGroups(newGroups);

			// sends information about current group to the bus
	/* ******************* uncomment those lines to response to the mobile object ****************************************
			UUID uuid = nodeMessage.getSenderId();
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
		}
		
		return newGroups;
	}
}
