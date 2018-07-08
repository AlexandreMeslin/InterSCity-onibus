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
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import lac.cnet.sddl.objects.ApplicationObject;
import lac.cnet.sddl.objects.Message;
import lac.cnet.sddl.udi.core.listener.UDIDataReaderListener;
import br.com.meslin.onibus.aux.GeographicMap;
import br.com.meslin.onibus.aux.StaticLibrary;
import br.com.meslin.onibus.aux.model.Bus;
import br.com.meslin.onibus.aux.model.Region;
import br.com.meslin.onibus.aux.model.SamplePredicate;

/**
 * @author meslin
 *
 */
public class BenchmarkMyProcessingNode implements UDIDataReaderListener<ApplicationObject> {
	private GeographicMap map;
	private List<Region> regionList;
	private List<Bus> busList;
	private String contextNetIPAddress;
	private int contextNetPortNumber;
	private ConcurrentLinkedQueue<Bus> busQueue;
	/**
	 * Constructor
	 * @param busQueue
	 */
	public BenchmarkMyProcessingNode(String contextNetIPAddress, int contextNetPortNumber, String name, ConcurrentLinkedQueue<Bus> busQueue) {
		this.contextNetIPAddress = contextNetIPAddress;
		this.contextNetPortNumber = contextNetPortNumber;

		// checks if there is an graphic environment available (true if not, otherwise, false)
		if(!GraphicsEnvironment.isHeadless()) {
			List<String> filenames = StaticLibrary.readFilenamesFile(name);
			// reads each region file
			this.regionList = new ArrayList<Region>();	// region list
			int regionNumber = 1;	// region number. Each region has a number assigned sequentially
			for(String filename : filenames) {
				Region region = StaticLibrary.readRegion(filename, regionNumber);
				regionList.add(region);
				regionNumber++;
			}
			this.map = new GeographicMap(regionList);
			this.map.setVisible(true);
		}

		this.busList = new ArrayList<Bus>();	// the bus list starts empty
	}

	@Override
	public void onNewData(ApplicationObject nodeMessage) {
		// gets bus position
		Bus bus = null;
		try {
			bus = (Bus) new ObjectInputStream(new ByteArrayInputStream(((Message)nodeMessage).getContent())).readObject();
		}
		catch (IOException | ClassNotFoundException e) {
			System.err.println("Date = " + new Date());
			e.printStackTrace();
		}

		// updates bus position on the map
		this.busList.removeIf(new SamplePredicate(bus.getOrdem()));
		this.busList.add(bus);
		if(!GraphicsEnvironment.isHeadless()) {
			this.map.remove(bus);
			this.map.addBus(bus);
		}

		if(bus.getGroups().size() > 0) {
			// TODO remover o comentário para transmitir dados para o InterSCity
//			BenchmarkDefineGroup.busQueue.add(bus);
			synchronized(busQueue) {
				this.busQueue.notify();
			}
		}
	}
}
