package br.com.meslin.onibus.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONObject;

public class GatherBuses {
	private static final String USER_AGENT = "Mozilla/5.0";

	public static void main(String[] args) throws InterruptedException {
		GatherBuses gatherBuses = new GatherBuses();
		gatherBuses.doAll();
	}

	@SuppressWarnings("deprecation")
	private void doAll() throws InterruptedException {
		String buffer;
		
		// will end at the day after tomorrow, 00:00 o'clock 
		Date endingDate = new Date(new Date().getTime() + 2 * 24 * 60 * 60 * 1000);
		endingDate.setHours(0);
		endingDate.setMinutes(0);
		endingDate.setSeconds(0);

		System.out.println("Will end on " + endingDate);
		System.out.println("Starting...");

		while((new Date()).getTime() < endingDate.getTime())
		for(int i=0; i<30 * 60 * 2; i++) {	// 30 horas de 60 minutos a cada 30 segundos
			buffer = getAllPositions("");
	        BufferedWriter writer = null;
	        try {
	            //create a temporary file
	            String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
	            File logFile = new File(timeLog + ".txt");
	
	            // This will output the full path where the file will be written to...
	            System.out.println(logFile.getCanonicalPath());
	
	            writer = new BufferedWriter(new FileWriter(logFile));
	            writer.write(buffer);
	        } catch (Exception e) {
	            e.printStackTrace();
	        } finally {
	            try {
	                // Close the writer regardless of what happens...
	                writer.close();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
			Thread.sleep(30 * 1000);
		}
	}
	/**
	 * Gets all bus positions from city hall<br>
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
	public String getAllPositions(String line) {
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
			connection.setRequestProperty("User-Agent", USER_AGENT);
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
				return null;
			}
		}
		else {
			return null;
		}
		// Convertion just to format the JSON string
		return new JSONObject(buffer.toString()).toString(4);
	}
}
