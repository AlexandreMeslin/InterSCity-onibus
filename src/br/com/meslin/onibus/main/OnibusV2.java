/**
 * 
 * http connection from https://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/
 */
package br.com.meslin.onibus.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.meslin.onibus.aux.connection.HTTPConnection;
import br.com.meslin.onibus.aux.connection.HTTPException;
import br.com.meslin.onibus.aux.model.Bus;

/**
 * @author Meslin
 *
 */
public class OnibusV2 {
	// constants
	private static final String USER_AGENT = "Mozilla/5.0";
	private static final int DATAHORA = 0;
	private static final int ORDEM = 1;
	private static final int LINHA = 2;
	private static final int LATITUDE = 3;
	private static final int LONGITUDE = 4;
	private static final int VELOCIDADE = 5;

	/**
	 * Construct and empty Onibus objectSystem
	 */
	public OnibusV2() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.err.println("Onibus v2");
		Map<String, Bus> buses;
		
		OnibusV2 onibus = new OnibusV2();
		buses = onibus.getAllPositions(584);
		onibus.updateDB(buses);
	}
	
	/**
	 * Update InterSCity data base<br>
	 * @param buses 
	 */
	private void updateDB(Map<String, Bus> buses) {
		System.err.println("\n[Onubus.updateDB]");
		String response =null;
		String uuid =null;
		JSONObject jsonObject, data;
		HTTPConnection connection;

		// for each bus...
		for(Map.Entry<String, Bus> bus: buses.entrySet()) {
			String ordem = bus.getKey();		// remember: the key is the bus ordem
			System.err.println("[Onibus.updateDB] Storing " + ordem);
			
			// try to find the UUID
			try {
				System.err.println("[Onibus.updateDB] trying to get UUID");
				uuid = getUUID(ordem);
			} catch (IOException e) {
				System.err.println("***** [Onibus.updateDB] Fatal error while seeking for UUID by 'ordem': " + e.getMessage());
				return;
			}
			
			// if UUID not found, create a new bus
			if(uuid == null) {
				System.err.println("[Onibus.updateDB] UUID not found, trying to create new UUID");
				uuid = createNewResource(bus);
				if(uuid == null) {
					System.err.println("*** Fatal error: could not create new bus");
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
      		 *					"velocidade: 54,
      		 *					"ordem": "abc"
        	 *					"timestamp": "2017-06-14T17:52:25.428Z"
      		 *				}
    		 *			]
  			 *		}
			 *	}
			 */
			// create the JSON string
			String linha = bus.getValue().getLinha();
			Double velocidade = bus.getValue().getVelocidade();

			JSONObject location = new JSONObject();
			location.put("lat", bus.getValue().getLatitude());
			location.put("lon", bus.getValue().getLongitude());
			
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
			System.err.println("[Onibus.updateDB] Updating values to " + jsonObject.toString(2));

			// make HTTP connection 
			connection = new HTTPConnection();
			try {
				System.err.println("[Onibus.updateDB] trying to update values");
				response = connection.sendPost("adaptor/resources/" + uuid + "/data", jsonString);
				System.err.println("[Oninus.updateDB] response = " + response);
			} catch (IOException | HTTPException e) {
				System.err.println("*** [Onibus.updateDB] Could not update resource " + uuid + " because " + e.getMessage());
			}
		}
	}

	
	
	/**
	 * Get bus UUID by ordem (bus serial number)<br>
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
	 * @return UUID String
	 * @throws IOException 
	 */
	private String getUUID(String ordem) throws IOException {
		System.err.println("[Onibus.getUUID]");
		String response;
		String uuid =null;
		
		HTTPConnection connection = new HTTPConnection();
		try {
			response = connection.sendGet("discovery/resources" ,"capability=bus_monitoring&ordem.eq=" + ordem);
			System.err.println("[Onibus.getUUID] HTTP response = " + response);
			try {
				uuid = ((JSONObject)((new JSONObject(response)).getJSONArray("resources").get(0))).getJSONObject("data").getString("uuid");
				System.err.println("***** UUID " + uuid + " for " + ordem + " found");
			} catch (JSONException e) {
				try {
					uuid = ((JSONObject)((new JSONObject(response)).getJSONArray("resources").get(0))).getString("uuid");
					System.err.println("***** UUID " + uuid + " for " + ordem + " found at 2nd strike");
				} catch (JSONException e1) {
					// if you are here, probably there is no resource with this "ordem"
					uuid = null;
					System.err.println("***** UUID not found for ordem " + ordem);
				}
			}
		} catch (IOException e) {
			System.err.println("*** [Onibus.getUUID] deu erro " + e.getMessage());
			throw e;
		} catch (HTTPException e) {
			System.err.println("[Onibus.getUUID] Bus not found. Must be created first.");
		}
		return uuid;
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
	private String createNewResource(Entry<String, Bus> bus) {
		System.err.println("[createNewResource]");
		String uuid =null;
		String response = null;
		
		// create JSON string
		JSONObject data = new JSONObject();
		data.put("description", "A public bus");
		data.accumulate("capabilities", "bus_monitoring");
		data.accumulate("capabilities", "uv");	// workaround for the one postion JSON array bug - capability not used
		data.put("status", "active");
		data.put("lat", bus.getValue().getLatitude());
		data.put("lon", bus.getValue().getLongitude());

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("data", data);
		
		System.err.println("[Onibus.createNewResource] JSON: " + jsonObject.toString(2));

		// HTTP connection
		HTTPConnection connection = new HTTPConnection();
		try {
			response = connection.sendPost("adaptor/resources", jsonObject.toString());
			System.err.println("[Onibus.createNewResource] Create resource response: " + response);
			jsonObject = new JSONObject(response);
			uuid = jsonObject.getJSONObject("data").getString("uuid");
		} catch (IOException | HTTPException e) {
			System.err.println("[Onibus.createNewResource] Resource not created: " + e.getMessage() + " Answer: " + response);
		}
		return uuid;
	}

	
	
	/**
	 * 
	 * @param uuid
	 * @param capability
	 * @param data
	 * @return connection answer (raw format - probably JSON)
	 */
	@SuppressWarnings("unused")
	private String postDataByCapability(String uuid, String capability, JSONObject data) {
		return postDataByCapability(uuid, capability, data.toString());
	}
	private String postDataByCapability(String uuid, String capability, String data) {
		System.err.println("[postDataByCapability]");
		String response =null;
		HTTPConnection connection = new HTTPConnection();
		try {
			response = connection.sendPost("adaptor/resources/" + uuid + "/data/" + capability, data);
			System.err.println("[Onibus.postDataByCapability] Post resource response: " + response);
		} catch (IOException | HTTPException e) {
			System.err.println("***** [Onibus.postDataByCapability] Fatal error while posting data by capability: " + e.getMessage());
		}
		return response;
	}
	
	
	
	/**
	 * gets all bus positions<br>
	 * 
     *{<br>
     *	"COLUMNS":[<br>
     * 		"DATAHORA",<br>
     *		"ORDEM",<br>
     *		"LINHA",<br>
     *		"LATITUDE",<br>
     *		"LONGITUDE",<br>
     *		"VELOCIDADE"<br>
  	 *	],<br>
  	 *	"DATA":[<br>
     *		[<br>
     *	 		"MM-DD-YYY HH:MM:SS",<br>
     *			"ORDEM",<br>
     *			"LINHA",<br>
     *			LATITUDE,<br>
     *			LONGITUDE,<br>
     *			VELOCIDADE<br>
     *		],<br>
     *		[], []<br>
	 * @return 
	 */
	public Map<String, Bus> getAllPositions() {
		return getAllPositions("");
	}
	/**
	 * @see OnibusV2#getAllPositions()
	 * {@link OnibusV2#getAllPositions()}
	 * @param line bus line
	 * @return 
	 */
	public Map<String, Bus> getAllPositions(int line) {
		return getAllPositions("" + line);
	}
	/**
	 * @see OnibusV2#getAllPositions()
	 * {@link OnibusV2#getAllPositions()}
	 * @param line bus route
	 * @return 
	 */
	@SuppressWarnings("deprecation")
	public Map<String, Bus> getAllPositions(String line) {
		System.err.println("[getAllPositions]\n\n\n");
		Map<String, Bus> buses = null;
		URL url;
		HttpURLConnection connection = null;
		int responseCode =-1;
		StringBuffer buffer = null;

		// Connect to bus data base
		try {
			url = new URL("http://dadosabertos.rio.rj.gov.br/apiTransporte/apresentacao/rest/index.cfm/obterTodasPosicoes");
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", USER_AGENT);
			responseCode = connection.getResponseCode();
		} catch (IOException e) {
			System.err.println("***** [Onibus.getAllPositions] Fatal error while getting bus positions: " + e.getMessage());
		}

		// read bus data
		if(responseCode == 200) {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String inputLine;
				buffer = new StringBuffer();
				while((inputLine = reader.readLine()) != null) {
					buffer.append(inputLine);
				}
				reader.close();
			} catch (IOException e) {
				System.err.println("***** [Onibus.getAllPositions] Fatal error while reading bus positions: " + e.getMessage());
			}
		}
		else
		{
			return null;
		}

		// create a JSON object based on bus data
		if(buffer != null) {
			try {
				JSONObject jsonObject = new JSONObject(buffer.toString());
				JSONArray jsonData = jsonObject.getJSONArray("DATA");
				System.err.println("[Onibus.getAllPositions] DATA = " + jsonData);
				System.err.println("[Onibus.getAllPositions] " + jsonData.length() + " buses");
				buses = new HashMap<String, Bus>();
				for(int i=0; i<jsonData.length(); i++) {
					JSONArray jsonBus = jsonData.getJSONArray(i);
					Bus bus = new Bus();
					bus.setData(new Date(
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[2]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[0]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[1]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[3]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[4]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[5])
						));
					bus.setOrdem(jsonBus.getString(ORDEM));
					try {
						bus.setLinha(jsonBus.getString(LINHA));
					} catch (JSONException e) {
						bus.setLinha(jsonBus.getInt(LINHA));
					}
					bus.setLatitude(jsonBus.getDouble(LATITUDE));
					bus.setLongitude(jsonBus.getDouble(LONGITUDE));
					bus.setVelocidade(jsonBus.getDouble(VELOCIDADE));
					
					// Filter buses based on bus line
					if(bus.getLinha().contains(line)) {
						buses.put(jsonBus.getString(ORDEM), bus);
						System.err.println("[Onibus.getAllPositions] Selecionado: " + bus);
					}
				}
				System.err.println("[Onibus.getAllPositions] Total de ônibus: " + buses.size());
			} catch (Exception e) {
				System.err.println("***** [Onibus.getAllPositions] Fatal error while creating JSON object: " + e.getMessage());
			}
		}
		else {
			System.err.println("*** [Onibus.getAllPositions] Error code #" + responseCode);
		}
		return buses;
	}
}
