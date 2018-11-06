/**
 * This program uploads all buses from a date to a InterSCity
 * 
 * References:
 * https://stackoverflow.com/questions/5694385/getting-the-filenames-of-all-files-in-a-folder
 * http://www.vogella.com/tutorials/JavaRegularExpressions/article.html#pattern-and-matcher
 * https://study.com/academy/lesson/how-to-sort-an-array-in-java.html
 */
package br.com.meslin.onibus.main;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.meslin.onibus.aux.Debug;
import br.com.meslin.onibus.aux.StaticLibrary;
import br.com.meslin.onibus.aux.connection.HTTPException;
import br.com.meslin.onibus.aux.model.Bus;
import br.com.meslin.onibus.interscity.InterSCity;

/**
 * @author meslin
 *
 */
public class UploadBuses {
	// constants
	private static final int DATAHORA = 0;
	private static final int ORDEM = 1;
	private static final int LINHA = 2;
	private static final int LATITUDE = 3;
	private static final int LONGITUDE = 4;
	private static final int VELOCIDADE = 5;

	private static String interSCityIPaddress;
	private static String directory;
	private static String patternAsString;
	private static Map<String, Bus> buses;
	
	/**
	 * Construct an UploadBuses<br>
	 * creates global variable buses<br>
	 */
	public UploadBuses() {
		buses = new HashMap<String, Bus>();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// get command line options
		Options options = new Options();
		Option option;

		option = new Option("d", "directory", true, "JSON file directory");
		option.setRequired(false);
		options.addOption(option);
		
		option = new Option("i", "InterSCity", true,	"InterSCity IP address");
		option.setRequired(true);
		options.addOption(option);

		option = new Option("p", "pattern", true, "Upload file pattern");
		option.setRequired(false);
		options.addOption(option);
		
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e1) {
			System.err.println("Date = " + new Date());
			formatter.printHelp("UploadBuses", options);
			e1.printStackTrace();
			return;
		}
		
		interSCityIPaddress = cmd.getOptionValue("InterSCity");
		
		if((directory = cmd.getOptionValue("directory")) == null) {
			directory = "/home/meslin/Google Drive/workspace-desktop-ubuntu/InterSCity-onibus/dados/RioOnibus/2018-10-03/";
		}
		if((patternAsString = cmd.getOptionValue("pattern")) == null) {
			patternAsString = "^2018\\d{4}_\\d{6}.txt$";
			patternAsString = "^20181003_10[0-2]\\d{3}.txt$";	// just for testing...
		}
		
		UploadBuses buses = new UploadBuses();
		buses.doAll(args);
	}

	@SuppressWarnings("deprecation")
	private void doAll(String[] args) {
		Pattern pattern = Pattern.compile(patternAsString);
		String buffer;
		InterSCity interSCity = new InterSCity(interSCityIPaddress);
		
		try {
			interSCity.checkInterSCity();
		} catch (IOException | HTTPException e1) {
			e1.printStackTrace();
			return;	// if InterSCity is not onlne, nothing can be done, just return
		}
		
		Calendar today;
		Calendar startDay;		// the day the upload will begin
		startDay = Calendar.getInstance();
		startDay.add(Calendar.DATE, 1);		// the upload will start tomorrow
		Debug.println("Waiting until tomorrow");
		// loop awaint tomorrow
		do {
			today = Calendar.getInstance();
		} while(today.get(Calendar.DATE) < startDay.get(Calendar.DATE));
		
		// Get a sorted directory list with all files and folders 
		File folder = new File(directory);
		File[] listOfFiles = folder.listFiles();
		Arrays.sort(listOfFiles);

		Debug.println("Uploading " + listOfFiles.length + " files");
		for (int fileIndex = 0; fileIndex < listOfFiles.length; fileIndex++) {
			Matcher matcher = pattern.matcher(listOfFiles[fileIndex].getName());
			if (listOfFiles[fileIndex].isFile() && matcher.find()) {
				int hours = Integer.parseInt(listOfFiles[fileIndex].getName().substring(9, 11));
				int minutes = Integer.parseInt(listOfFiles[fileIndex].getName().substring(11, 13));
				int seconds = Integer.parseInt(listOfFiles[fileIndex].getName().substring(13, 15));

				// Get the time corresponding to that file, based on this filename
				Calendar fileTime = Calendar.getInstance();
				fileTime.set(Calendar.HOUR_OF_DAY, hours);
				fileTime.set(Calendar.MINUTE, minutes);
				fileTime.set(Calendar.SECOND, seconds);

				// wait until now is more than file time
				Calendar now;
				do {
					now = Calendar.getInstance();
				} while(now.before(fileTime));

				System.out.println("Uploading file " + listOfFiles[fileIndex].getName());
				buffer = StaticLibrary.readFile(directory + "/" + listOfFiles[fileIndex].getName());
				JSONObject jsonObject = new JSONObject(buffer);
				JSONArray jsonData = jsonObject.getJSONArray("DATA");

				// upload each bus reported by the file
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
					// LINHA may be a string or an integer, so let's try...
					try {
						bus.setLinha(jsonBus.getString(LINHA));
					} catch (JSONException e) {
						bus.setLinha(jsonBus.getInt(LINHA));
					}
					bus.setLatitude(jsonBus.getDouble(LATITUDE));
					bus.setLongitude(jsonBus.getDouble(LONGITUDE));
					bus.setVelocidade(jsonBus.getDouble(VELOCIDADE));

					// if we already know this bus, just update its info. Do not change group info nor ordem (ordem is the primary key)
					if(buses.containsKey(bus.getOrdem())) {
						buses.get(bus.getOrdem()).setData(bus.getData());
						buses.get(bus.getOrdem()).setLatitude(bus.getLatitude());
						buses.get(bus.getOrdem()).setLongitude(bus.getLongitude());
						buses.get(bus.getOrdem()).setLinha(bus.getLinha());
						buses.get(bus.getOrdem()).setVelocidade(bus.getVelocidade());
					}
					// otherwise, creates a new entry without group information
					else {
						buses.put(jsonBus.getString(ORDEM), bus);
					}
					interSCity.updateDB(buses.get(bus.getOrdem()));
				}
			}
		}
	}
}
