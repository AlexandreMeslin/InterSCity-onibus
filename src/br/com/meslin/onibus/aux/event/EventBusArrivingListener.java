/**
 * 
 */
package br.com.meslin.onibus.aux.event;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import br.com.meslin.onibus.aux.Debug;
import br.com.meslin.onibus.aux.StaticLibrary;
import br.com.meslin.onibus.aux.contextnet.MessageSender;
import br.com.meslin.onibus.aux.model.Bus;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.client.UpdateListener;

/**
 * @author meslin
 *
 */
public class EventBusArrivingListener implements UpdateListener {
	/** Map of the next region (region to be adviced that a bus is arriving) */
	private Map<Integer, Integer> nextRegion;

	/**
	 * Constructor
	 */
	public EventBusArrivingListener() {
		nextRegion = new HashMap<Integer, Integer>();
		nextRegion.put(1, 10002);
		nextRegion.put(2, 10001);
	}

	/**
	 * @see com.espertech.esper.client.UpdateListener#update(com.espertech.esper.client.EventBean[], com.espertech.esper.client.EventBean[])
	 */
	@Override
	public void update(EventBean[] newEvents, EventBean[] oldEvents) {
		try {
			MessageSender messageSender = MessageSender.getInstance();	// must be one of the first commands to create UUID when constructing a MessageSender object
			Debug.warning("***** EVENT: bus arriving! ***** bus " + newEvents[0].get("bus") + " at region " + newEvents[0].get("region") + " at " + newEvents[0].get("timeStamp"));
			String linha = ((Bus)newEvents[0].get("bus")).getLinha();
			String ordem = ((Bus)newEvents[0].get("bus")).getOrdem();
			double lat = ((Bus)newEvents[0].get("bus")).getLatitude();
			double lon = ((Bus)newEvents[0].get("bus")).getLongitude();
			
			int sourceRegion = (int) newEvents[0].get("region");
			int targetRegion = nextRegion.get(sourceRegion);
			
			// cria o objeto JSON
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("uuid", StaticLibrary.coreUUID.toString());
			jsonObject.put("username", "MUSANetCore");
			jsonObject.put("date", (new Date()).toString());
			jsonObject.put("accuracy", 19.5310001373291);
			jsonObject.put("provider", "network");
			jsonObject.put("speed", 0);
			jsonObject.put("bearing", 0);
			jsonObject.put("altitude", 0);
			jsonObject.put("connection", "java.lang.InfoConnectivityWifi");
			jsonObject.put("battery", 96);
			jsonObject.put("charging", false);
			jsonObject.put("tag", "SoundAction");
			jsonObject.put("SoundAction", "ATENÇÃO (SoundAction)!!! Seu ônibus " + linha + " (" + ordem + ") está chegando");
			jsonObject.put("actuation", "SoundAction");
			jsonObject.put("latitude", lat);
			jsonObject.put("longitude", lon);
			jsonObject.put("timestamp", System.currentTimeMillis() /1000);	// divide por 1000 para compatibilizar com o M-Hub			
			jsonObject.put("sequencial", StaticLibrary.sequencial++);
			jsonObject.put("type", "act");
			jsonObject.put("message", "ATENÇÃO (message)!!! Seu ônibus " + linha + "(" + ordem + ") está chegando");

			// Send a message to the passenger at the next bus stop
			messageSender.sendMessageToGroup(targetRegion, jsonObject);
		} catch (PropertyAccessException | JSONException e) {
			System.err.println("Date = " + new Date());
			e.printStackTrace();
		}
	}
}
