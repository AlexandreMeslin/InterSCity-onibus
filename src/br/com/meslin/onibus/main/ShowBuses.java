package br.com.meslin.onibus.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import br.com.meslin.onibus.auxiliar.model.Bus;
import br.com.meslin.onibus.auxiliar.model.Region;
import br.com.meslin.onibus.auxiliar.GeographicMap;
import br.com.meslin.onibus.auxiliar.StaticLibrary;
import br.com.meslin.onibus.auxiliar.connection.Constants;
import br.com.meslin.onibus.auxiliar.connection.HTTPConnection;
import br.com.meslin.onibus.auxiliar.connection.HTTPException;

public class ShowBuses {
	private GeographicMap map;
	private List<Region> regionList;
	private List<Bus> busList;

	public ShowBuses(String name) {
		List<String> filenames = StaticLibrary.readFilenamesFile(name);
		
		// reads each region file
		this.regionList = new ArrayList<Region>();	// region list
		int regionNumber = 1;						// region number. Each region has a number assigned sequentially
		for(String filename : filenames) {
			Region region = StaticLibrary.readRegion(filename, regionNumber);
			regionList.add(region);
			regionNumber++;
		}

		map = new GeographicMap(regionList);
		map.setVisible(true);
		
		busList = new ArrayList<Bus>();	// the bus list starts empty
		int ciclo =0;
		while(true) {
			System.err.println("[ShowBuses] ciclo = " + (ciclo++));
			callDoInBackground();
			try {
				Thread.sleep(1 * 60 * 1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		System.err.println("[ShowBuses." + new Object(){}.getClass().getEnclosingMethod().getName() + "]");
		if(args.length > 1)
		{
			System.err.println("Usage: ShowBuses [<group filename>]");
			return;
		}
		
		System.out.println("\n\nInterSCity address: " + Constants.INTERSCITY_URL + "\n\n");
		
		String filename = null;
		if(args.length == 0) filename = "names.txt";
		else filename = args[0];

		new ShowBuses(filename);

		try {
			Thread.sleep(Long.MAX_VALUE);
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
    /**
    *
    * @param view
    */
   public void callDoInBackground() {
      /*
       * 1st: discover the resources (all buses near user)
       * 2nd: for each resource, get its data (position) and plot on map
       */

       HTTPConnection connection;
       String response;
       List<String> uuids;
       JSONArray resources;

        /*
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
         */

       // Get a list of UUID resources located near the user
       connection = new HTTPConnection();
       uuids = new ArrayList<String>();
       try {
//           response = connection.sendGet("discovery/resources" ,"capability=bus_monitoring&linha.eq=" + busLine);
           response = connection.sendGet("discovery/resources" ,"capability=bus_monitoring");
           resources = (new JSONObject(response)).getJSONArray("resources");
           for(int i=0; i<resources.length(); i++) {
               uuids.add(((JSONObject)resources.get(i)).getString("uuid"));
           }
       }
       catch (IOException e) {
           System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] IOException");
           e.printStackTrace();
       }
       catch (HTTPException e) {
           System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] HTTPException");
           e.printStackTrace();
       }
       catch (JSONException e) {
           System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] JSONException");
           e.printStackTrace();
       }
       catch (Exception e) {
           System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Exception: " + e.toString() + " " + e.getMessage() + " " + e.getLocalizedMessage() + " " + e.getCause());
           e.printStackTrace();
       }

       // for each bus, get its location and plot at the map
       String data ="";
       if(uuids.size() == 1) {
           data = "{\"uuids\":[\"" + uuids.get(0) + "\"]}";
       }
       else {
           for(String uuid: uuids) {
               if(data.length() > 0) data += ",";
               data += "\"" + uuid + "\"";
           }
           data = "{\"uuids\":[" + data + "]}";
       }

         /*
          * POST:
          *  {
          *      "uuids":[
          *          "5ad20589-a3db-4521-b1bc-a21dde00a25c",
          *          "b5d170b5-aaf3-42bc-9e47-58e3fe2a4846"
          *      ]
          *  }
          *
          * Response:
          *  {
          *      "resources" : [
          *          {
          *              "uuid" : "d7925d4d-174b-4474-97b3-b6dd48807271",
          *              "capabilities" : {
          *                  "bus_monitoring" : [
          *                      {
          *                          "date" : "2018-01-12T19:28:04.000Z",
          *                          "ordem" : "B31135",
          *                          "linha" : 485,
          *                          "velocidade" : 0,
          *                          "location" : {
          *                              "lon" : -43.187328,
          *                              "lat" : -22.96629
          *                          }
          *                      }
          *                  ]
          *              }
          *          },
          *          ...
          *      ]
          *  }
          */
       // get a list of bus positions from InterSCity
       busList = new ArrayList<Bus>();
       connection = new HTTPConnection();
       try {
           response = connection.sendPost("collector/resources/data/last", data);
           resources = (new JSONObject(response)).getJSONArray("resources");
           System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] received " + resources.length() + " buses");
           map.removeAll();
           for(int i=0; i<resources.length(); i++) {
        	   Bus bus = new Bus();
               Double busLat = ((JSONObject)(((JSONArray)(
                       ((JSONObject)resources.get(i))  // each resource in resources
                               .getJSONObject("capabilities")
                               .getJSONArray("bus_monitoring"))).get(0)))  // The first (and single) dataset
                       .getJSONObject("location").getDouble("lat");
               Double busLon = ((JSONObject)(((JSONArray)(
                       ((JSONObject)resources.get(i))  // each resource in resources
                               .getJSONObject("capabilities")
                               .getJSONArray("bus_monitoring"))).get(0)))  // The first (and single) dataset
                       .getJSONObject("location").getDouble("lon");
               bus.setLatitude(busLat);
               bus.setLongitude(busLon);
               bus.setOrdem(((JSONObject)(((JSONObject)(resources.get(i))).getJSONObject("capabilities").getJSONArray("bus_monitoring").get(0))).getString("ordem"));
               bus.setLinha(((JSONObject)(((JSONObject)(resources.get(i))).getJSONObject("capabilities").getJSONArray("bus_monitoring").get(0))).getInt("linha"));
               bus.setData(((JSONObject)(((JSONObject)(resources.get(i))).getJSONObject("capabilities").getJSONArray("bus_monitoring").get(0))).getString("date"));
               busList.add(bus);
//               map.remove(bus);
               // only plots updated buses (less than 15 minutes)
               
               if(new Date().getTime() - bus.getData().getTime() < 0) {
            	   System.err.println("***** Horário negativo no onibus " + bus);
               }
               
               
               if(new Date().getTime() - bus.getData().getTime() < 15 * 60 * 1000) {
            	   map.addBus(bus);
               }
           }
       } catch (IOException e) {
           System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] IOException2");
           e.printStackTrace();
       } catch (HTTPException e) {
           System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] HTTPException2");
           e.printStackTrace();
       } catch (JSONException e) {
           System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] JSONException2");
           e.printStackTrace();
       } catch (Exception e) {
           System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] Exception2 " + e.toString());
           e.printStackTrace();
       }
       return;
   }
}
