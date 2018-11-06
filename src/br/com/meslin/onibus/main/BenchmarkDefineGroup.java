package br.com.meslin.onibus.main;

/**
 * Compiling:
 * $ javac -classpath .:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/contextnet-2.5.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/udilib.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JMapViewer/JMapViewer.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JSON/JSON-Parser/json-20160810.jar -d . br/com/meslin/onibus/main/DefineGroup.java
 * 
 * Executing:
 * in meslin@meslin-notebook:~/Google Drive/workspace-desktop-ubuntu/InterSCity-onibus/bin
 * $ java -classpath .:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/contextnet-2.5.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/udilib.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JMapViewer/JMapViewer.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JSON/JSON-Parser/json-20160810.jar br.com.meslin.onibus.main.DefineGroup ../names-relative.txt
 */

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import lac.cnet.groupdefiner.components.GroupDefiner;
import lac.cnet.groupdefiner.components.groupselector.GroupSelector;
import br.com.meslin.onibus.aux.StaticLibrary;
import br.com.meslin.onibus.aux.connection.HTTPException;
import br.com.meslin.onibus.aux.contextnet.BenchmarkGroupSelectorImplementation;
import br.com.meslin.onibus.aux.model.Bus;
import br.com.meslin.onibus.interscity.InterSCity;
import br.com.meslin.onibus.interscity.InterSCityConsumer;

/**
 * Implements group definition and selection based on regions
 * <p>
 * Usage: DefineGroup <group filename>
 * 
 * @author meslin
 *
 */
public class BenchmarkDefineGroup {
	private static InterSCity interSCity;
	
	/** stores a queue of bus data to be sent to the InterSCity */
	public static ConcurrentLinkedQueue<Bus> busQueue = new ConcurrentLinkedQueue<Bus>(); 

	public static void main(String[] args) throws MalformedURLException, IOException, HTTPException {
		final Date buildDate = StaticLibrary.getClassBuildTime();

		// Catch Ctrl+C
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	long elapsedTime = StaticLibrary.stopTime - StaticLibrary.startTime;
		    	System.err.println("CTRL+C");
		    	System.err.println("Time: " +  elapsedTime + " (" + StaticLibrary.stopTime + " - " + StaticLibrary.startTime + ") with " + StaticLibrary.nMessages + " messages");
		    }
		 });

		if(args.length != 3) {
			System.err.println("Usage: BenchmarkDefineGroup <gateway ip address> <gateway port number> <group filename>");
			return;
		}
		
		String contextNetIPAddress = args[0];
		int contextNetPortNumber = Integer.parseInt(args[1]);
		String filename = args[2];
		
		System.out.println("BenchmarkDefineGroup builed at " + buildDate);
		System.out.println("\n\nStarting Group Define using gateway at " + contextNetIPAddress + ":" + contextNetPortNumber + "\n\n");

		StaticLibrary.nMessages = 0;		// for statistics
		
		System.out.println("Ready, set...");

		GroupSelector groupSelector = new BenchmarkGroupSelectorImplementation(contextNetIPAddress, contextNetPortNumber, filename);
		new GroupDefiner(groupSelector);

		// check and set InterSCity capabilities and a fake bus
		interSCity = new InterSCity();
		interSCity.checkInterSCity();
		interSCity.createABus();

		/**
		 * Thread to send bus data to the InterSCity
		 */
		Thread consumer = new Thread(new InterSCityConsumer(interSCity, busQueue));
		consumer.start();
		
		System.out.println("\nGO!");
		try {
			Thread.sleep(Long.MAX_VALUE);
		} 
		catch (InterruptedException e) {
			System.err.println("Date = " + new Date());
			e.printStackTrace();
		}
	}
}
