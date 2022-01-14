package br.com.meslin.onibus.auxiliar.contextnet;

import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lac.cnclib.net.NodeConnection;
import lac.cnclib.net.NodeConnectionListener;
import lac.cnclib.net.mrudp.MrUdpNodeConnection;
import lac.cnclib.sddl.message.ApplicationMessage;
import lac.cnet.groupdefiner.components.groupselector.GroupSelector;
import lac.cnet.sddl.objects.GroupRegion;
import lac.cnet.sddl.objects.Message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.meslin.onibus.auxiliar.GeographicMap;
import br.com.meslin.onibus.auxiliar.StaticLibrary;
import br.com.meslin.onibus.auxiliar.connection.HTTPConnection;
import br.com.meslin.onibus.auxiliar.connection.HTTPException;
import br.com.meslin.onibus.auxiliar.model.Bus;
import br.com.meslin.onibus.auxiliar.model.Region;
import br.com.meslin.onibus.auxiliar.model.SamplePredicate;

public class GroupSelectorImplementation implements GroupSelector, NodeConnectionListener {
	// properties
	private GeographicMap map;
	private List<Region> regionList;
	private List<Bus> busList;
	private String contextNetIPAddress;
	private int contextNetPortNumber;
	
	/**
	 * <p>Builds regions based on region description files</p>
	 * <p>File of filenames: a filename per line (path, if included, may be relative ou full)</p>
	 * <p>File of regions: each X, Y representing coordinates per line separeted by a blank</p>
	 * 
	 * @param name filename with region filenames, one per line
	 * @param filename2 
	 * @param contextNetPortNumber2 
	 * @throws HTTPException 
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	public GroupSelectorImplementation(String contextNetIPAddress, int contextNetPortNumber, String name) throws MalformedURLException, IOException, HTTPException
	{
		this.contextNetIPAddress = contextNetIPAddress;
		this.contextNetPortNumber = contextNetPortNumber;
		List<String> filenames = StaticLibrary.readFilenamesFile(name);
		
		// reads each region file
		this.regionList = new ArrayList<Region>();	// region list
		int regionNumber = 1;	// region number. Each region has a number assigned sequentially
		for(String filename : filenames) {
			Region region = StaticLibrary.readRegion(filename, regionNumber);
			regionList.add(region);
			regionNumber++;
		}
		
		// checks if there is an graphic environment available (true if not, otherwise, false)
		if(!GraphicsEnvironment.isHeadless() && !StaticLibrary.forceHeadless) {
			map = new GeographicMap(regionList);
			map.setVisible(true);
		}
		
		busList = new ArrayList<Bus>();	// the bus list starts empty
		checkInterSCity();
	}

	
	
	/**
	 * Checks the InterSCity<br>
	 * <ul>
	 * <li>Availability
	 * <li>Existence of the required capabilities (creates if not available)
	 * </ul>
	 * @throws HTTPException 
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	private void checkInterSCity() throws MalformedURLException, IOException, HTTPException {
		HTTPConnection connection;
		String response;
		Boolean found;
		
		// the availability is checked using the services
		
		// check for the existence of the bus_monitoring capability
		connection = new HTTPConnection();
		response = connection.sendGet("catalog/capabilities", "");
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
			connection = new HTTPConnection();
			String data = "{ \"name\": \"bus_monitoring\", \"description\": \"Bus monitoring\", \"capability_type\": \"sensor\" }";
			response = connection.sendPost("catalog/capabilities", data);
		}
		else {
			System.err.println("*** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Capability found!");
		}
	}



	/**
	 * Returns the group type (always 3)
	 */
	@Override
	public int getGroupType() {
		return 3;
	}

	
	
	/**
	 * Seleciona o grupo do usuário de acordo com a sua região (longitude e latitude)
	 * <p>Sempre que o usuário trocar de região, o cliente do usuário deve enviar uma mensagem com as coordenadas da nova região
	 * <p>Responsável pela atualização do mapa (remove usuário da posição antiga e coloca na nova posição)
	 * <p>This version also creates/updates the bus on the InterSCity 
	 */
	@Override
	public Set<Integer> processGroups(Message nodeMessage) {
		// gets bus position
		Bus bus = null;
		try {
			bus = (Bus) new ObjectInputStream(new ByteArrayInputStream(nodeMessage.getContent())).readObject();
		}
		catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		// updates bus position on the map
		busList.removeIf(new SamplePredicate(bus.getOrdem()));
		busList.add(bus);
		if(!GraphicsEnvironment.isHeadless() && !StaticLibrary.forceHeadless) {
			map.remove(bus);
			map.addBus(bus);
		}
		
//		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Longitude = " + bus.getLongitude() + " Latitude = " + bus.getLatitude());
		
		HashSet<Integer> groups = new HashSet<Integer>(2, 1);
		UUID uuid = nodeMessage.getSenderId();
		// procura as regiões onde o ônibus pode estar
		for(Region region : regionList)
		{
			if (region.contains(bus)) {
				groups.add(region.getNumero());
			}
		}
		bus.setGroups(groups);
		// sends information about current group to the bus
		InetSocketAddress address = new InetSocketAddress(this.contextNetIPAddress, this.contextNetPortNumber);
		UUID senderUUID = UUID.randomUUID();
		try
		{
			MrUdpNodeConnection connection = new MrUdpNodeConnection(senderUUID);
			connection.addNodeConnectionListener(this);
			connection.connect(address);
			ApplicationMessage message = new ApplicationMessage();
			message.setContentObject(bus);
			message.setRecipientID(uuid);
			connection.sendMessage(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	
//		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Adding bus " + bus.getLinha() + "@" + bus.getOrdem());
		// update InterSCity DB
		// only updates the DB if this bus has a group (i.e., this bus is inside at least one region)
		if(bus.getGroups().size() > 0) updateDB(bus);
		
		return groups;
	}

	
	
	/**
	 * Update InterSCity database<br>
	 * @param bus
	 */
	private void updateDB(Bus bus) {
		System.out.print("-");
		String uuid = null;
		JSONObject jsonObject, data;
		HTTPConnection connection;
		String response = null;
		
		String ordem = bus.getOrdem();		// remember: the key is the bus ordem
//		System.err.println("[GroupSelectorImplementation.updateDB] Storing " + ordem);
		
		// try to find the UUID
		try {
//			System.err.println("[GroupSelectorImplementation.updateDB] trying to get UUID");
			uuid = getUUID(ordem);
		} catch (IOException e) {
			System.err.println("***** [GroupSelectorImplementation.updateDB] Fatal error while seeking for UUID by 'ordem': " + e.getMessage());
			return;
		}
		
		// if UUID not found, create a new bus
		if(uuid == null) {
//			System.err.println("[GroupSelectorImplementation.updateDB] UUID not found, trying to create new UUID");
			uuid = createNewResource(bus);
			if(uuid == null) {
//				System.err.println("*** Fatal error: could not create new bus");
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
//		System.err.println("[GroupSelectorImplementation.updateDB] Updating values to " + jsonObject.toString(2));


		// make HTTP connection 
		connection = new HTTPConnection();
		try {
//			System.err.println("[GroupSelectorImplementation.updateDB] trying to update values");
			response = connection.sendPost("adaptor/resources/" + uuid + "/data", jsonString);
//			System.err.println("[GroupSelectorImplementation.updateDB] response = " + response);
		} catch (IOException | HTTPException e) {
			System.err.println("*** [GroupSelectorImplementation.updateDB] Could not update resource " + uuid + " because " + e.getMessage() + "\nResponse: " + response);
		}

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
//		System.err.println("[createNewResource]");
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
		
//		System.err.println("[GroupSelectorImplementation.createNewResource] JSON: " + jsonObject.toString(2));

		// HTTP connection
		HTTPConnection connection = new HTTPConnection();
		try {
//			System.err.println("Sending data: " + jsonObject.toString(4));
			response = connection.sendPost("adaptor/resources", jsonObject.toString());
//			System.err.println("[GroupSelectorImplementation.createNewResource] Create resource response: " + response);
			jsonObject = new JSONObject(response);
			uuid = jsonObject.getJSONObject("data").getString("uuid");
		} catch (IOException | HTTPException e) {
			System.err.println("[GroupSelectorImplementation.createNewResource] Resource not created: " + e.getMessage() + " Answer: " + response);
		}
		return uuid;
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
//		System.err.println("[GroupSelectorImplementation.getUUID]");
		String response;
		String uuid =null;
		
		HTTPConnection connection = new HTTPConnection();
		try {
			response = connection.sendGet("discovery/resources" ,"capability=bus_monitoring&ordem.eq=" + ordem);
//			System.err.println("[GroupSelectorImplementation.getUUID] HTTP response = " + response);
			try {
				uuid = ((JSONObject)((new JSONObject(response)).getJSONArray("resources").get(0))).getJSONObject("data").getString("uuid");
//				System.err.println("***** UUID " + uuid + " for " + ordem + " found");
			} catch (JSONException e) {
				try {
					uuid = ((JSONObject)((new JSONObject(response)).getJSONArray("resources").get(0))).getString("uuid");
//					System.err.println("***** UUID " + uuid + " for " + ordem + " found at 2nd strike");
				} catch (JSONException e1) {
					// if you are here, probably there is no resource with this "ordem"
					uuid = null;
//					System.err.println("***** UUID not found for ordem " + ordem);
				}
			}
		} catch (IOException e) {
			System.err.println("*** [GroupSelectorImplementation.getUUID] deu erro " + e.getMessage());
			throw e;
		} catch (HTTPException e) {
			System.err.println("[GroupSelectorImplementation.getUUID] Bus not found. Must be created first.");
		}
		return uuid;
	}



	/**
	 * <p>Formats the UUID</p>
	 * <ul>
	 *  <li>XXXXXXXX: bits 127-96 (32 bits)</li>
	 *  <li>XXXX: bits 95-80 (16 bits)</li>
	 *  <li>XXXX: bits 79-64 (16 bits)</li>
	 *  <li>XXXX: bits 63-48 (16 bits)</li>
	 *  <li>XXXXXXXXXXXX: bits 47-0 (48 bits)</li>
	 * </ul>
	 * 
	 * @param mostSignificantBits
	 * @param leastSignificantBits
	 * @return UUID int the format XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
	 */
	/*
	private String formatUUID(long mostSignificantBits, long leastSignificantBits)
	{
		String result = "";
		result += Integer.toHexString((int)(mostSignificantBits>>(96-64)));				// 127-96
		result += "-";
		result += Integer.toHexString((int)((mostSignificantBits>>(80-64)) & 0xFFFF));	// 95-80
		result += "-";
		result += Integer.toHexString((int)(mostSignificantBits & 0xFFFF));				// 95-80
		result += "-";
		result += Integer.toHexString((int)((leastSignificantBits>>48) & 0xFFFF));		// 63-48
		result += "-";
		result += Integer.toHexString((int)((leastSignificantBits>>32) & 0xFFFF));		// 47-32
		result += Integer.toHexString((int)(leastSignificantBits & 0xFFFFFFFF));		// 31-0
		return result;
	}
	*/
	@Override
	public void createGroup(GroupRegion arg0) {}
	@Override
	public void connected(NodeConnection arg0) {}
	@Override
	public void disconnected(NodeConnection arg0) {}
	@Override
	public void internalException(NodeConnection arg0, Exception arg1) {}
	@Override
	public void newMessageReceived(NodeConnection arg0, lac.cnclib.sddl.message.Message arg1) {}
	@Override
	public void reconnected(NodeConnection arg0, SocketAddress arg1, boolean arg2, boolean arg3) {}
	@Override
	public void unsentMessages(NodeConnection arg0, List<lac.cnclib.sddl.message.Message> arg1) {}
}