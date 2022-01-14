/**
 * 
 */
package prefecture;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.meslin.onibus.auxiliar.model.Bus;
import br.com.meslin.onibus.auxiliar.StaticLibrary;
import br.com.meslin.onibus.main.BenchmarkOnibusV2;
import br.com.meslin.util.Debug;

/**
 * @author meslin
 *
 */
public class Prefecture {
	/** bus indexed by ORDEM<br>Map&lt;ORDEM, Bus&gt; */
	public volatile static Map<String, Bus> buses;

	/**
	 * @param buses 
	 * 
	 */
	public Prefecture() {
		Prefecture.buses = new HashMap<String, Bus>();
	}
	/**
	 * Gets a single bus from city hall database using getAllPositions
	 * @param line
	 * @return
	 */
	public Bus getABus() {
		return getABus("");
	}
	/**
	 * Gets a single bus from city hall database using getAllPositions
	 * @param line
	 * @return
	 */
	public Bus getABus(int line) {
		return getABus(Integer.toString(line));
	}
	/**
	 * Gets a single bus from city hall database using getAllPositions
	 * @param line
	 * @return
	 */
	public Bus getABus(String line) {
		getAllPositions(line);
		Bus newBus = null;
		
		// Get the first bus (only!)
		for(Map.Entry<String, Bus> busEntry: Prefecture.buses.entrySet()) {
			newBus = busEntry.getValue();
			newBus.setOrdem("12345");
			break;
		}
		if(newBus==null) {
			Debug.warning("Could not create buses from prefecture");
			return null;
		}
		return newBus;
	}
	
	/**
	 * Gets all bus positions from city hall<br>
	 * gets all bus positions<br>
	 * Fills global object buses with new buses or update their positions<br>
	 * 
	 * City hall data format:<br>
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
	 */
	public void getAllPositions() {
		getAllPositions("");
	}
	/**
	 * Gets all bus positions from city hall<br>
	 * @see BenchmarkOnibusV2#getAllPositions()
	 * {@link BenchmarkOnibusV2#getAllPositions()}
	 * @param line bus line
	 *
	 * City hall data format:<br>
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
	 */
	public void getAllPositions(int line) {
		getAllPositions(Integer.toString(line));
	}
	/**
	 * Gets all bus positions from city hall<br>
	 * This method updates the List<Bus> buses class object.
	 * If the bus is already registered, only its position is updated.
	 * If the bus is new (not yet registered), a new bus entry is created<br>
	 * On error, while connecting to the city hall website, the buses object is not updated
	 * @see BenchmarkOnibusV2#getAllPositions()
	 * {@link BenchmarkOnibusV2#getAllPositions()}
	 * @param line bus route
	 * 
 	 * City hall data format:<br>
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
	 */
	@SuppressWarnings("deprecation")
	public void getAllPositions(String line) {
//		System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] with line = |" + line + "|");
		URL url;
		HttpURLConnection connection = null;
		int responseCode =-1;
		StringBuffer buffer = null;

		// Connect to bus data base
		try {
			url = new URL("http://dadosabertos.rio.rj.gov.br/apiTransporte/apresentacao/rest/index.cfm/obterTodasPosicoes");
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", StaticLibrary.USER_AGENT);
			responseCode = connection.getResponseCode();
		} catch (IOException e) {
			System.err.println("Date = " + new Date());
			System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Fatal error while getting bus positions from the city hall website");
			e.printStackTrace();
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
				System.err.println("Date = " + new Date());
				System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Fatal error while reading bus positions: " + e.getMessage());
				e.printStackTrace();
				return;
			}
		}
		else {
			return;
		}

		// create a JSON object based on bus data
		if(buffer != null) {
			JSONObject jsonObject = new JSONObject(buffer.toString());
			JSONArray jsonData = jsonObject.getJSONArray("DATA");
//			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] DATA = " + jsonData);
//			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] " + jsonData.length() + " buses");
//			buses = new HashMap<String, Bus>();
			if(jsonData.length() == 0) {
				Bus bus = new Bus();
				bus.setData(new Date());
				bus.setOrdem("123456");
				bus.setLinha(123);
				bus.setLatitude(StaticLibrary.LATITUDE);
				bus.setLongitude(StaticLibrary.LONGITUDE);
				bus.setVelocidade(123);
				buses.put(bus.getOrdem(), bus);
			}
			else for(int i=0; i<jsonData.length(); i++) {
				JSONArray jsonBus = jsonData.getJSONArray(i);
				Bus bus = new Bus();
				bus.setData(new Date(
						Integer.parseInt(jsonBus.getString(StaticLibrary.DATAHORA).split("[\\- :]")[2]),
						Integer.parseInt(jsonBus.getString(StaticLibrary.DATAHORA).split("[\\- :]")[0]),
						Integer.parseInt(jsonBus.getString(StaticLibrary.DATAHORA).split("[\\- :]")[1]),
						Integer.parseInt(jsonBus.getString(StaticLibrary.DATAHORA).split("[\\- :]")[3]),
						Integer.parseInt(jsonBus.getString(StaticLibrary.DATAHORA).split("[\\- :]")[4]),
						Integer.parseInt(jsonBus.getString(StaticLibrary.DATAHORA).split("[\\- :]")[5])
				));
				bus.setOrdem(jsonBus.getString(StaticLibrary.ORDEM));
				try {
					bus.setLinha(jsonBus.getString(StaticLibrary.LINHA));
				} catch (JSONException e) {
					bus.setLinha(jsonBus.getInt(StaticLibrary.LINHA));
				}
				bus.setLatitude(jsonBus.getDouble(StaticLibrary.LATITUDE));
				bus.setLongitude(jsonBus.getDouble(StaticLibrary.LONGITUDE));
				bus.setVelocidade(jsonBus.getDouble(StaticLibrary.VELOCIDADE));
				
				// Filter buses based on bus line
				if(bus.getLinha().contains(line)) {
					// if we already know this bus, just update its info. Do not change group info nor ordem (ordem is the primary key)
//					System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] ordem = " + bus.getOrdem());
					if(Prefecture.buses.containsKey(bus.getOrdem())) {
						Prefecture.buses.get(bus.getOrdem()).setData(bus.getData());
						Prefecture.buses.get(bus.getOrdem()).setLatitude(bus.getLatitude());
						Prefecture.buses.get(bus.getOrdem()).setLongitude(bus.getLongitude());
						Prefecture.buses.get(bus.getOrdem()).setLinha(bus.getLinha());
						Prefecture.buses.get(bus.getOrdem()).setVelocidade(bus.getVelocidade());
					}
					// otherwise, creates a new entry without group information
					else {
						Prefecture.buses.put(jsonBus.getString(StaticLibrary.ORDEM), bus);
					}
//					System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Selecionado: " + bus);
				}
			}
//			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Total de ônibus: " + buses.size());
		}
//		else {
//			System.err.println("***** [" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Error code #" + responseCode);
//		}
	}
}
