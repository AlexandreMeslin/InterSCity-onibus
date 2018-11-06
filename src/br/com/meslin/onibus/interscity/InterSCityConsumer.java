package br.com.meslin.onibus.interscity;

import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import br.com.meslin.onibus.aux.model.Bus;

public class InterSCityConsumer implements Runnable {
	/** stores a queue of bus data to be sent to the InterSCity */
	private ConcurrentLinkedQueue<Bus> busQueue = new ConcurrentLinkedQueue<Bus>();
	private InterSCity interSCity;

	/**
	 * Constructor<br>
	 * Constructs a consumer to consume BusQueue data and send it to InterSCity<br>
	 * 
	 * @param interSCity
	 * @param busQueue
	 */
	public InterSCityConsumer(InterSCity interSCity, ConcurrentLinkedQueue<Bus> busQueue) {
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