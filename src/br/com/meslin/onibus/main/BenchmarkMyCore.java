/**
 * 
 */
package br.com.meslin.onibus.main;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import lac.cnet.groupdefiner.components.GroupDefiner;
import lac.cnet.groupdefiner.components.groupselector.GroupSelector;
import br.com.meslin.onibus.aux.StaticLibrary;
import br.com.meslin.onibus.aux.connection.HTTPException;
import br.com.meslin.onibus.aux.contextnet.BenchmarkMyGroupSelector;
import br.com.meslin.onibus.aux.contextnet.BenchmarkMyProcessingNode;
import br.com.meslin.onibus.aux.model.Bus;
import br.com.meslin.onibus.interscity.InterSCity;

/**
 * @author meslin
 *
 */
public class BenchmarkMyCore {
	// statistics
	public static long startTime = -1;	// negative value means that there is no start time setted yet
	public static long stopTime;		// when last message was received
	public static long nMessages;

	private static InterSCity interSCity;

	/** stores a queue of bus data to be sent to the InterSCity */
	public static ConcurrentLinkedQueue<Bus> busQueue = new ConcurrentLinkedQueue<Bus>(); 

	/**
	 * 
	 */
	public BenchmarkMyCore() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 * @throws HTTPException 
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	public static void main(String[] args) throws MalformedURLException, IOException, HTTPException {
		// Build date
		final Date buildDate = StaticLibrary.getClassBuildTime();
		System.out.println("BenchmarMyCore builed at " + buildDate);

		// Catch Ctrl+C
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	long elapsedTime = stopTime - startTime;
		    	System.err.println("CTRL+C");
		    	System.err.println("Time: " +  elapsedTime + " (" + stopTime + " - " + startTime + ") with " + nMessages + " messages");
		    }
		});

		// Command line parameters
		if(args.length != 3) {
			System.err.println("Usage: BenchmarkMyCore <gateway ip address> <gateway port number> <group filename>");
			return;
		}
		String contextNetIPAddress = args[0];
		int contextNetPortNumber = Integer.parseInt(args[1]);
		String filename = args[2];
		System.out.println("\n\nStarting ContextNet Core using gateway at " + contextNetIPAddress + ":" + contextNetPortNumber + "\n\n");
		
		System.out.println("Ready, set...");

		/**
		 * Creating GroupSelector
		 */
		GroupSelector groupSelector = new BenchmarkMyGroupSelector(contextNetIPAddress, contextNetPortNumber, filename);
		new GroupDefiner(groupSelector);

		// check and set InterSCity capabilities and a fake bus
		interSCity = new InterSCity();
		interSCity.checkInterSCity();
		interSCity.createABus();
		
		/**
		 * Create the Processing Node thread
		 */
		BenchmarkMyProcessingNode processingNode = new BenchmarkMyProcessingNode(filename, contextNetPortNumber, filename, busQueue);

		/**
		 * Create a thread to send bus data to the InterSCity
		 */
		Thread consumer = new Thread(new BusQueueConsumer(interSCity, busQueue));
		consumer.start();
		
		System.out.println("\nGO!");
		while(true) {}
	}
}


class BusQueueConsumer implements Runnable {
	/** stores a queue of bus data to be sent to the InterSCity */
	private ConcurrentLinkedQueue<Bus> busQueue = new ConcurrentLinkedQueue<Bus>();
	private InterSCity interSCity;

	public BusQueueConsumer(InterSCity interSCity, ConcurrentLinkedQueue<Bus> busQueue) {
		this.interSCity = interSCity;
		this.busQueue = busQueue;
	}

	@Override
	public void run() {
		Bus bus;
		while(true) {
			while(busQueue.isEmpty()) {
				synchronized (busQueue) {
					try {
						busQueue.wait();
					} catch (InterruptedException e) {
						System.err.println("Date = " + new Date());
						e.printStackTrace();
					}
				}
			}
			// busQueue is ConcurrentLinkedQueue thread safe linked queue, so, does NOT need to be synchronized
			while ((bus = busQueue.poll()) != null) {
				interSCity.updateDB(bus);
			}
		}
	}	
}
