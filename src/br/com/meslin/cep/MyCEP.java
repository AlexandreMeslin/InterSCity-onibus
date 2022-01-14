package br.com.meslin.cep;

import java.util.HashSet;

import br.com.meslin.onibus.auxiliar.model.Bus;
import br.com.meslin.onibus.auxiliar.event.EventBusArrivingListener;
import br.com.meslin.onibus.auxiliar.event.EventRegion;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;

public class MyCEP {
	// CEP	
	private EPServiceProvider epService;

	public MyCEP() {
		// configuration
		Configuration config = new Configuration();
		config.addEventTypeAutoName("br.com.meslin.onibus.aux.event");
		config.addEventType("EventRegion", EventRegion.class.getName());
		// creating a statement
		epService = EPServiceProviderManager.getProvider("myCEPEngine", config);
		String expression = "select * from EventRegion having EventRegion.region = 1";
		EPStatement statement = epService.getEPAdministrator().createEPL(expression);
		statement.addListener(new EventBusArrivingListener());
	}

	public void createEvent(Bus bus, HashSet<Integer> newGroups) {
		for(int group : newGroups) {
			if(!bus.getGroups().contains(group)) {
				EventRegion eventRegion = new EventRegion(bus, group, System.currentTimeMillis());
				epService.getEPRuntime().sendEvent(eventRegion);
			}
		}
	}
}
