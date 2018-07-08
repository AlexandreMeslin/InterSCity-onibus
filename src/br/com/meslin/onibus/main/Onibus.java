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
 * @author meslin
 *
 */
public class Onibus {
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
	public Onibus() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.err.println("Onibus v1");
		Map<String, Bus> buses;
		
		Onibus onibus = new Onibus();
		buses = onibus.getAllPositions(485);
		onibus.updateDB(buses);
	}
	
	/**
	 * Update InterSCity data base<br>
	 * @param buses 
	 */
	private void updateDB(Map<String, Bus> buses) {
		System.err.println("\n[Onibus.updateDB]");
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
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
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

			// Update an existing resource with its new longitude and latitude
			// PUT /adaptor/resources/{uuid}
			data = new JSONObject();
			data.put("lat", bus.getValue().getLatitude());
			data.put("lon", bus.getValue().getLongitude());
			jsonObject = new JSONObject();
			jsonObject.put("data", data);
			System.err.println("[Onibus.updateDB] JSON (update resource): " + jsonObject.toString(2));
			
			connection = new HTTPConnection();
			try {
				response = connection.sendPut("adaptor/resources/" + uuid, jsonObject.toString());
				System.err.println("[Onibus.updateDB] Update resource response: " + response);
			} catch (IOException | HTTPException e) {
				System.err.println("[Onibus.updateDB] Resource not created: " + e.getMessage());
			}
			
			// update data: only linha and velocidade will be updated once ordem cannot change
			// POST /adaptor/resources/{uuid}/data
			/*
			 *	{
  			 *		"data": {
     		 *			"bus_monitoring": [
      		 *				{
      		 *					"linha": "875",
      		 *					"velocidade: 54,
      		 *					"ordem": "abc"
        	 *					"timestamp": "2017-06-14T17:52:25.428Z"
      		 *				}
    		 *			]
  			 *		}
			 *	}
			 */
			String linha = bus.getValue().getLinha();
			Double velocidade = bus.getValue().getVelocidade();
			JSONArray bus_monitoring = new JSONArray();
			JSONObject capabilities = new JSONObject();
			capabilities.put("linha", linha);
			capabilities.put("velocidade", velocidade);
			capabilities.put("ordem", ordem);
			capabilities.put("timestamp", new Date());
			bus_monitoring.put(capabilities);
			data = new JSONObject();
			data.accumulate("bus_monitoring", bus_monitoring);
//			data.accumulate("bus_monitoring", bus_monitoring);
			jsonObject = new JSONObject();
			jsonObject.put("data", data);
			System.err.println("[Onibus.updateDB] Updating values to " + jsonObject.toString(2));
			connection = new HTTPConnection();
			String jsonString = jsonObject.toString().replace("[[", "[").replace("]]", "]");
			try {
				System.err.println("[Onibus.updateDB] trying to update values");
				response = connection.sendPost("adaptor/resources/" + uuid + "/data", jsonString);
			} catch (IOException | HTTPException e) {
				System.err.println("*** [Onibus.updateDB] Could not update resource " + uuid + " because " + e.getMessage());
			}
			
//if(new Date().getHours()>=0) return;
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
			response = connection.sendGet("discovery/resources" ,"ordem.eq=" + ordem);
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
	 * Method: POST<br>
	 *<br>
	 *  {<br>
	 *  	"data": {<br> 
	 *  		"description": "A public bus",<br> 
	 *  		"capabilities": [<br>
	 *  			"ordem",<br>
	 *  			"linha",<br> 
	 *  			"velocidade" <br>
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
		
		JSONObject data = new JSONObject();
		data.put("description", "A public bus");
		data.accumulate("capabilities", "ordem");
		data.accumulate("capabilities", "linha");
		data.accumulate("capabilities", "velocidade");
		data.put("status", "active");
		data.put("lat", bus.getValue().getLatitude());
		data.put("lon", bus.getValue().getLongitude());

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("data", data);
		
		System.err.println("[Onibus.createNewResource] JSON: " + jsonObject.toString(2));
		
		HTTPConnection connection = new HTTPConnection();
		try {
			String response = connection.sendPost("adaptor/resources", jsonObject.toString());
			System.err.println("[Onibus.createNewResource] Create resource response: " + response);
			jsonObject = new JSONObject(response);
			uuid = jsonObject.getJSONObject("data").getString("uuid");
			//postDataByCapability(uuid, "ordem", "{\"data\": [{\"ordem\": \"" + bus.getValue().getOrdem() + "\"}]}");
		} catch (IOException | HTTPException e) {
			System.err.println("[Onibus.createNewResource] Resource not created: " + e.getMessage());
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
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	 * @see Onibus#getAllPositions()
	 * {@link Onibus#getAllPositions()}
	 * @param line bus line
	 * @return 
	 */
	public Map<String, Bus> getAllPositions(int line) {
		return getAllPositions("" + line);
	}
	/**
	 * @see Onibus#getAllPositions()
	 * {@link Onibus#getAllPositions()}
	 * @param line bus route
	 * @return 
	 */
	@SuppressWarnings("deprecation")
	public Map<String, Bus> getAllPositions(String line) {
		System.err.println("[getAllPositions]");
		Map<String, Bus> buses = null;
		URL url;
		
		try {
			url = new URL("http://dadosabertos.rio.rj.gov.br/apiTransporte/apresentacao/rest/index.cfm/obterTodasPosicoes");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", USER_AGENT);
			int responseCode = connection.getResponseCode();
			if(responseCode == 200) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String inputLine;
				StringBuffer buffer = new StringBuffer();
				while((inputLine = reader.readLine()) != null) {
					buffer.append(inputLine);
				}
				reader.close();
//				System.err.println(buffer);
				JSONObject jsonObject = new JSONObject(buffer.toString());
//				System.err.println(jsonObject.toString(4));
				JSONArray jsonData = jsonObject.getJSONArray("DATA");
				System.err.println("[Onibus.getAllPositions] DATA = " + jsonData);
				System.err.println("[Onibus.getAllPositions] " + jsonData.length() + " buses");
				buses = new HashMap<String, Bus>();
				for(int i=0; i<jsonData.length(); i++) {
					JSONArray jsonBus = jsonData.getJSONArray(i);
//					System.err.println();
					Bus bus = new Bus();
//					System.err.println("Data: " + jsonBus.getString(DATAHORA));
					bus.setData(new Date(
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[2]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[0]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[1]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[3]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[4]),
							Integer.parseInt(jsonBus.getString(DATAHORA).split("[\\- :]")[5])
						));
//					System.err.println("Ordem: " + jsonBus.getString(ORDEM));
					bus.setOrdem(jsonBus.getString(ORDEM));
					try {
//						System.err.println("Linha: " + jsonBus.getString(LINHA));
						bus.setLinha(jsonBus.getString(LINHA));
					} catch (JSONException e) {
//						System.err.println("Linha: " + jsonBus.getInt(LINHA));
						bus.setLinha(jsonBus.getInt(LINHA));
					}
//					System.err.println("Latitude: " + jsonBus.getDouble(LATITUDE));
					bus.setLatitude(jsonBus.getDouble(LATITUDE));
//					System.err.println("Longitude: " + jsonBus.getDouble(LONGITUDE));
					bus.setLongitude(jsonBus.getDouble(LONGITUDE));
//					System.err.println("Velocidade: " + jsonBus.getDouble(VELOCIDADE));
					bus.setVelocidade(jsonBus.getDouble(VELOCIDADE));
					if(bus.getLinha().contains(line)) {
						buses.put(jsonBus.getString(ORDEM), bus);
						System.err.println("[Onibus.getAllPositions] Selecionado: " + bus);
					}
				}
				System.err.println("[Onibus.getAllPositions] Total de ônibus: " + buses.size());
			}
			else {
				System.err.println("*** [Onibus.getAllPositions] Error code #" + responseCode);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buses;
	}
}
