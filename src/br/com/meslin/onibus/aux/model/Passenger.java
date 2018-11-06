package br.com.meslin.onibus.aux.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.UUID;

import org.json.JSONObject;

public class Passenger implements Serializable, MobileObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private double latitude;
	private double longitude;
	private HashSet<Integer> groups;
	private UUID uuid;

	/**
	 * Constructor<br>
	 */
	public Passenger() {
		// TODO Auto-generated constructor stub
	}

	public Passenger(String name, double latitude, double longitude) {
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * Constructs a Passeger based on Mobile-Hub data:<br>
	 * {<br>
	 * 	"latitude" : -22.9384629,<br>
	 * 	"longitude" : -43.192745,<br>
   	 * 	"tag" : "LocationData",<br>
	 * 	"connection" : "java.lang.InfoConnectivityWifi",<br>
   	 * 	"altitude" : 39.5,<br>
   	 * 	"speed" : 0,<br>
   	 * 	"provider" : "network",<br>
   	 * 	"bearing" : 0,<br>
   	 * 	"date" : "Tue Oct 23 18:05:37 GMT-03:00 2018",<br>
   	 * 	"battery" : 58,<br>
   	 * 	"timestamp" : 1540328737,<br>
   	 * 	"uuid" : "9509494b-b270-4cd7-a5a2-08cc6bb998d1",<br>
   	 * 	"accuracy" : 19.5060005187988,<br>
   	 * 	"charging" : false<br>
	 * }<br>
	 * 
	 * @param jsonMHub
	 */
	public Passenger(JSONObject jsonMHub) {
		this.name = "Mobile-Hub";
		this.latitude = jsonMHub.getDouble("latitude");
		this.longitude = jsonMHub.getDouble("longitude");
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	/**
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	/**
	 * @return the groups
	 */
	public HashSet<Integer> getGroups() {
		return groups;
	}

	/**
	 * @param groups the groups to set
	 */
	public void setGroups(HashSet<Integer> groups) {
		this.groups = groups;
	}

	/**
	 * @return the uuid
	 */
	public UUID getUuid() {
		return uuid;
	}

	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	/**
	 * @return the serialversionuid
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Passenger [name=" + name + ", latitude=" + latitude
				+ ", longitude=" + longitude + ", groups=" + groups + ", uuid="
				+ uuid + "]";
	}
}
