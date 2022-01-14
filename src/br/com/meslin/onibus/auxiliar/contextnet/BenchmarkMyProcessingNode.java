/**
 * 
 */
package br.com.meslin.onibus.auxiliar.contextnet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONObject;

import lac.cnet.sddl.objects.ApplicationObject;
import lac.cnet.sddl.objects.Message;
import lac.cnet.sddl.objects.PrivateMessage;
import lac.cnet.sddl.udi.core.SddlLayer;
import lac.cnet.sddl.udi.core.UniversalDDSLayerFactory;
import lac.cnet.sddl.udi.core.listener.UDIDataReaderListener;
import br.com.meslin.onibus.auxiliar.Debug;
import br.com.meslin.onibus.auxiliar.StaticLibrary;
import br.com.meslin.onibus.auxiliar.model.Bus;
import br.com.meslin.onibus.auxiliar.model.Passenger;
import br.com.meslin.onibus.main.BenchmarkDefineGroup;

/**
 * @author meslin
 */
public class BenchmarkMyProcessingNode implements UDIDataReaderListener<ApplicationObject> {
//	private GeographicMap map;
//	private List<Region> regionList;
//	private List<Bus> busList;
//	private String contextNetIPAddress;
//	private int contextNetPortNumber;
	private ConcurrentLinkedQueue<Bus> busQueue;
	private SddlLayer processingNode;
	/**
	 * Constructor
	 * @param busQueue
	 */
	public BenchmarkMyProcessingNode(ConcurrentLinkedQueue<Bus> busQueue) {
		this.busQueue = busQueue;

		// checks if there is an graphic environment available (true if not, otherwise, false)
/*
		*** This is commented because graphics is shown by GroupDefiner! ***
		if(!GraphicsEnvironment.isHeadless() && !StaticLibrary.forceHeadless) {
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
*/
		
		this.processingNode = UniversalDDSLayerFactory.getInstance();
		this.processingNode.createParticipant(UniversalDDSLayerFactory.CNET_DOMAIN);
		
		this.processingNode.createPublisher();
		this.processingNode.createSubscriber();
		
		Object receiveMessageTopic = this.processingNode.createTopic(Message.class, Message.class.getSimpleName());
		this.processingNode.createDataReader(this, receiveMessageTopic);
		
		Object toMobileNodeTopic = this.processingNode.createTopic(PrivateMessage.class, PrivateMessage.class.getSimpleName());
		this.processingNode.createDataWriter(toMobileNodeTopic);
	}

	@Override
	public void onNewData(ApplicationObject nodeMessage) {
		Bus bus = null;
		// TODO remover esse suppresswarnings
		@SuppressWarnings("unused")
		Passenger passenger = null;
		Object mobileObject = null;

		try {
			// try to receive a Bus or a PassengerAtBusStop
			mobileObject = new ObjectInputStream(new ByteArrayInputStream(((Message)nodeMessage).getContent())).readObject();
		} catch (ClassNotFoundException | IOException e1) {
			try {
				// if a nodeMessage comes from M-Hub, its is a JSON string coded as byte[]
				mobileObject = new Passenger(new JSONObject(new String(((Message)nodeMessage).getContent())));
			} catch (Exception e) {
				System.err.println("Date = " + new Date());
				e.printStackTrace();
			}
		}

		if(mobileObject instanceof Bus) {
			if(StaticLibrary.startTime < 0) {
				StaticLibrary.startTime = System.currentTimeMillis();
				Debug.warning("Starting at " + StaticLibrary.startTime);
			}
			StaticLibrary.nMessages++;

			bus = (Bus) mobileObject;
			if(bus.getGroups().size() > 0) {
				// TODO remover o comentário para transmitir dados para o InterSCity
				BenchmarkDefineGroup.busQueue.add(bus);
				synchronized(busQueue) {
					this.busQueue.notify();
				}
			}			
			// updates bus position on the map
			/*		this.busList.removeIf(new SamplePredicate(bus.getOrdem()));
					this.busList.add(bus);
					if(!GraphicsEnvironment.isHeadless() && !StaticLibrary.forceHeadless) {
						this.map.remove(bus);
						// TODO: remove next line comment and comment the other addBus command
						this.map.addBus(bus);
						//this.map.addBus("", new Coordinate(bus.getLatitude(), bus.getLongitude()));
					}
			*/
					StaticLibrary.stopTime = System.currentTimeMillis();
		}
	}
}
