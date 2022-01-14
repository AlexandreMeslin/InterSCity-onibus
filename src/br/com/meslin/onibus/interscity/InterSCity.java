package br.com.meslin.onibus.interscity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.meslin.onibus.auxiliar.model.Bus;
import br.com.meslin.onibus.auxiliar.Debug;
import br.com.meslin.onibus.auxiliar.StaticLibrary;
import br.com.meslin.onibus.auxiliar.connection.HTTPConnection;
import br.com.meslin.onibus.auxiliar.connection.HTTPException;

public class InterSCity {
	private HTTPConnection connection;
	private Map<String, String> ordemUUIDMap;

	
	
	/**
	 * Constructor
	 * @param connection an HTTP connection to the InterSCity platform
	 */
	public InterSCity(HTTPConnection connection) {
		this.connection = connection;

		ordemUUIDMap = new HashMap<String, String>();
	}
	/**
	 * Constructor<br>
	 * Creates an HTTPConnection HTTP connction object with default parameters<br>
	 */
	public InterSCity() {
		this(new HTTPConnection());
	}
	
	public InterSCity(String interSCityIPAddress) {
		this(new HTTPConnection(interSCityIPAddress));
	}
	/**
	 * Update InterSCity database<br>
	 * InterSCity commands<br>
	 * <ul>
	 * <li>
	 * <li>curl -X GET  "http://localhost:8000/catalog/capabilities"
	 * <li>curl -X POST "http://localhost:8000/adaptor/resources/" + uuid + "/data" -H "Content-Type: application/json" -d jsonobject
	 * </ul>
	 * @param bus
	 */
	public void updateDB(Bus bus) {
		if(StaticLibrary.nMessages % 1000 == 0) System.out.print("I");
		String uuid = null;
		JSONObject jsonObject, data;
		//HTTPConnection connection;
		String response = null;

		String ordem = bus.getOrdem();		// remember: the key is the bus ordem
		
		// try to find the UUID
		try {
			uuid = getUUID(ordem);
		} catch (IOException e) {
			System.err.println("Date = " + new Date());
			System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Fatal error while seeking for UUID by 'ordem': " + e.getMessage());
			return;
		}

		// if UUID not found, create a new bus
		if(uuid == null) {
			/* ******************* uncomment this to connect to InterSCity *********************** */
			uuid = createNewResource(bus);
			if(uuid == null) {
				return;
			}
		}

		// update data: only linha and velocidade will be updated once ordem cannot change
		// POST /adaptor/resources/{uuid}/data
		/*
		 *	{
		 *		"data": {
 		 *			"bus_monitoring": [
  		 *				{
  		 *					"location": {
  		 *						"lat": -10.00032,
  		 *						"lon": -32.200223
  		 *					},
  		 *					"linha": "875",
  		 *					"velocidade": 54,
  		 *					"ordem": "abc",
    	 *					"timestamp": "2017-06-14T17:52:25.428Z"
  		 *				}
		 *			]
		 *		}
		 *	}
		 */
		// create the JSON string
		String linha = bus.getLinha();
		Double velocidade = bus.getVelocidade();

		JSONObject location = new JSONObject();
		location.put("lat", bus.getLatitude());
		location.put("lon", bus.getLongitude());
		
		JSONObject capabilities = new JSONObject();
		capabilities.put("location", location);
		capabilities.put("linha", linha);
		capabilities.put("velocidade", velocidade);
		capabilities.put("ordem", ordem);
		capabilities.put("timestamp", new Date());
		
		JSONArray bus_monitoring = new JSONArray();
		bus_monitoring.put(capabilities);
		
		data = new JSONObject();
		data.put("bus_monitoring", bus_monitoring);
		
		jsonObject = new JSONObject();
		jsonObject.put("data", data);
		String jsonString = jsonObject.toString().replace("[[", "[").replace("]]", "]");
		Debug.warning("Tamanho do payload ==> " + jsonString.length());
//		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Updating values to " + jsonObject.toString(2));

//		System.err.print("<");
		// make HTTP connection 
		//connection = new HTTPConnection();
		try {
//			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] trying to update values");
			response = connection.sendPost("adaptor/resources/" + uuid + "/data", jsonString);
//			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] response = " + response);
		} catch (IOException | HTTPException e) {
			System.err.println("Date = " + new Date());
			System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Could not update resource " + uuid + " because " + e.getMessage() + "\nResponse: " + response);
		}
//		System.err.print(">");
	}



	/**
	 * Creates a new resource based on bus data<br>
	 * Register new resources<br>
	 * /adaptor/resources<br>
	 * Method: POST<br>
	 *<br>
	 *  {<br>
	 *  	"data": {<br> 
	 *  		"description": "A public bus",<br> 
	 *  		"capabilities": [<br>
	 *  			"bus_monitoring"
	 *  		],<br> 
	 *  		"status": "active",<br> 
	 *  		"lat": -23.559616,<br> 
	 *  		"lon": -46.731386<br> 
	 *  	}<br> 
	 *  }<br>
	 *<br>
	 *  Answer:<br>
	 * 	{<br>
  	 *		"data": {<br>
     *			"uuid": "45b7d363-86fd-4f81-8681-663140b318d4",<br>
     *			"description": "A public bus",<br>
     *			"capabilities": [<br>
	 *  			"ordem",<br>
	 *  			"linha",<br> 
	 *  			"velocidade" <br>
     *			],<br>
     *  		"status": "active",<br>
     *  		"lat": -23.559616,<br>
     *  		"lon": -46.731386,<br>
     *  		"country": "Brazil",<br>
     *  		"state": "São Paulo",<br>
     *  		"city": "São Paulo",<br>
     *  		"neighborhood": "Butantã",<br>
     *  		"postal_code": null,<br>
     *  		"created_at": "2017-12-27T13:25:07.176Z",<br>
     *  		"updated_at": "2017-12-27T13:25:07.176Z",<br>
     *  		"id": 10<br>
     * 		}<br>
     * 	}<br>
     *<br>
	 * @param bus
	 * @return UUID String
	 */
	private String createNewResource(Bus bus) {
		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] criando ordem " + bus.getOrdem());
		System.out.print("N");
//		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "]");
		String uuid =null;
		String response = null;
		
		// create JSON string
		JSONObject data = new JSONObject();
		data.put("description", "A public bus");
		data.accumulate("capabilities", "bus_monitoring");
		data.accumulate("capabilities", "uv");	// workaround for the one postion JSON array bug - capability not used
		data.put("status", "active");
		data.put("lat", bus.getLatitude());
		data.put("lon", bus.getLongitude());

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("data", data);
		
//		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] JSON: " + jsonObject.toString(2));

		// HTTP connection
		//HTTPConnection connection = new HTTPConnection();
		try {
//			System.err.println("Sending data: " + jsonObject.toString(4));
			response = connection.sendPost("adaptor/resources", jsonObject.toString());
//			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Create resource response: " + response);
			jsonObject = new JSONObject(response);
			uuid = jsonObject.getJSONObject("data").getString("uuid");
		} catch (IOException | HTTPException e) {
			System.err.println("Date = " + new Date());
			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Resource not created: " + e.getMessage() + " Answer: " + response);
		}
		if(uuid==null) {
			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] UUID nulo para ordem = " + bus.getOrdem());
		}
		ordemUUIDMap.put(bus.getOrdem(), uuid);
		return uuid;
	}

	
	/**
	 * Get bus UUID by ordem (bus serial number)<br>
	 * Uses ordemUUIDMap object as a UUID vs ORDEM cache to avoid extra InterSCity commands<br>
	 * InterSCity commands:<br>
	 * <ul>
 	 * <li>curl -X GET  "http://localhost:8000/discovery/resources?capability=bus_monitoring&ordem.eq=" + ordem
	 * <ul>
	 * <br>
	 * Answer<br><pre>
	 *	{<br>
  	 *		"resources": [<br>
	 *	    	{<br>
	 *	    		"data": {<br>
	 *	    	    	"uuid": "45b7d363-86fd-4f81-8681-663140b318d4",<br>
	 *	    	    	"description": "A public bus",<br>
	 *	    	    	"bus_monitoring": [<br>
	 *  					"ordem",<br>
	 *  					"linha",<br> 
	 *  					"velocidade" <br>
	 *	    			],<br>
	 *	        		"status": "active",<br>
	 *	        		"lat": -23.559616,<br>
	 *	        		"lon": -46.731386,<br>
	 *	        		"country": "Brazil",<br>
	 *	        		"state": "São Paulo",<br>
	 *	        		"city": "São Paulo",<br>
	 *	        		"neighborhood": "Butantã",<br>
	 *	        		"postal_code": null,<br>
	 *	        		"created_at": "2017-12-27T13:25:07.146Z",<br>
	 *	        		"updated_at": "2017-12-27T13:25:07.146Z",<br>
	 *	        		"id": 10<br>
	 *	      		}<br>
	 *	    	}<br>
	 *	  	]<br>
	 *	}<br>
	 *
	 *	{
	 *		"resources":[
	 *			{
	 *				"id":29,
	 *				"uri":null,
	 *				"created_at":"2018-01-09T12:00:12.963Z",
	 *				"updated_at":"2018-01-09T12:00:12.963Z",
	 *				"lat":-22.89439,
	 *				"lon":-43.215542,
	 *				"status":"active",
	 *				"collect_interval":null,
	 *				"description":"A public bus",
	 *				"uuid":"b86613cb-7ae5-4e66-97ea-9d02cae0c200",
	 *				"city":"Rio de Janeiro",
	 *				"neighborhood":"S..o Crist..v..o",
	 *				"state":"Rio de Janeiro",
	 *				"postal_code":"20931-690",
	 *				"country":"Brazil",
	 *				"capabilities":[
	 *					"ordem",
	 *					"linha",
	 *					"velocidade"
	 *				]
	 *			}
	 *		]
	 *	}
	 *<br></pre>
	 * @param ordem (bus serial number)
	 * @return UUID String or null if not found
	 * @throws IOException 
	 */
	private String getUUID(String ordem) throws IOException {
		String response;
		String uuid =null;
		
		if(ordemUUIDMap.containsKey(ordem)) {
			return ordemUUIDMap.get(ordem);
		}
		System.out.print("X");

		try {
			response = connection.sendGet("discovery/resources" ,"capability=bus_monitoring&ordem.eq=" + ordem);
			try {
				uuid = ((JSONObject)((new JSONObject(response)).getJSONArray("resources").get(0))).getJSONObject("data").getString("uuid");
			} catch (JSONException e) {
				try {
					uuid = ((JSONObject)((new JSONObject(response)).getJSONArray("resources").get(0))).getString("uuid");
				} catch (JSONException e1) {
					// if you are here, probably there is no resource with this "ordem"
					uuid = null;
				}
			}
		} catch (IOException e) {
			System.err.println("Date = " + new Date());
			System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] deu erro " + e.getMessage());
			throw e;
		} catch (HTTPException e) {
			System.err.println("Date = " + new Date());
			System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Bus not found. Must be created first.");
		}
		if(uuid!=null) {
			ordemUUIDMap.put(ordem, uuid);
		}
		System.out.print("x");
		return uuid;
	}

	
	
	/**
	 * Checks the InterSCity<br>
	 * <ul>
	 * <li>Availability
	 * <li>Existence of the required capabilities (creates if not available)
	 * </ul>
	 * InterSCity commands:
	 * <ul>
	 * <li>curl -X GET  "http://localhost:8000/catalog/capabilities"
	 * <li>curl -X POST "http://localhost:8000/catalog/capabilities" -H "Content-Type: application/json" -d '{"name": "bus_monitoring", "description": "Bus monitoring", "capability_type": "sensor"}'
	 * </ul>
	 * @throws HTTPException 
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	public void checkInterSCity() throws MalformedURLException, IOException, HTTPException {
		//HTTPConnection connection;
		String response;
		Boolean found;
		
		// the availability is checked using the services
		
		// check for the existence of the bus_monitoring capability
		//connection = new HTTPConnection();
		response = connection.sendGet("catalog/capabilities", "");
		
		if(response == null) return;
		
		JSONArray capabilities = new JSONObject(response).getJSONArray("capabilities");
		found = false;
		for(int i=0; i<capabilities.length(); i++) {
			if(((String)((JSONObject)capabilities.get(i)).get("name")).equals("bus_monitoring")) {
				found = true;
			}
		}
		if(!found) {
			// bus_monitoring capability not found, need to be created now!
			System.err.println("*** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Capability not found, creating one");
			//connection = new HTTPConnection();
			String data = "{ \"name\": \"bus_monitoring\", \"description\": \"Bus monitoring\", \"capability_type\": \"sensor\" }";
			response = connection.sendPost("catalog/capabilities", data);
		}
		else {
			System.err.println("*** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Capability found!");
		}
	}



	/**
	 * Creates a generic bus just for startup speed up
	 */
	public void createABus() {
		Bus bus = new Bus();
		bus.setData(new Date());
		bus.setLatitude(1);
		bus.setLinha(584);
		bus.setLongitude(1);
		bus.setOrdem("12345");
		bus.setVelocidade(1);
		updateDB(bus);
	}
}
