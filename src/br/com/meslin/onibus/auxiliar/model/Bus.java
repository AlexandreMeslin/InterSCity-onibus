package br.com.meslin.onibus.auxiliar.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.UUID;

public class Bus implements Serializable, MobileObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Date data;
	private String ordem;
	private String linha;
	private double latitude;
	private double longitude;
	private double velocidade;
	private HashSet<Integer> groups;
	private UUID uuid;
	
	
	
	/**
	 * Constructor<br>
	 */
	public Bus() {
		this.groups = new HashSet<Integer>();
		this.data = null;
		this.ordem = null;
		this.linha = null;
		this.latitude = 0;
		this.longitude = 0;
		this.velocidade = 0;
		this.uuid = null;
	}
	/**
	 * Constructor<br>
	 * @param data
	 * @param ordem
	 * @param linha
	 * @param latitude
	 * @param longitude
	 * @param velocidade
	 * @param groups
	 * @param uuid
	 */
	public Bus(Date data, String ordem, String linha, double latitude, double longitude, double velocidade, HashSet<Integer> groups, UUID uuid) {
		this();
		this.data = data;
		this.ordem = ordem;
		this.linha = linha;
		this.latitude = latitude;
		this.longitude = longitude;
		this.velocidade = velocidade;
		this.groups = groups;
		this.uuid = uuid;
	}
	/**
	 * Constructor
	 * @param bus
	 */
	public Bus(Entry<String, Bus> bus) {
		this();
		this.data       = bus.getValue().getData();
		this.ordem      = bus.getValue().getOrdem();
		this.linha      = bus.getValue().getLinha();
		this.latitude   = bus.getValue().getLatitude();
		this.longitude  = bus.getValue().getLongitude();
		this.velocidade = bus.getValue().getVelocidade();
		this.groups     = bus.getValue().getGroups();
		this.uuid		= bus.getValue().getUUID();
	}

	/**
	 * toString a bus<br>
	 * 	private Date data;<br>
	 *	private String ordem;<br>
	 *	private String linha;<br>
	 *	private double latitude;<br>
	 *	private double longitude;<br>
	 *	private double velocidade;<br>
	 *	private HashSet<Integer> groups;<br>
	 *	private UUID uuid;<br>
	 */
	@Override
	public String toString() {
		String string;
		string = "Bus [data=" + data + ", ordem=" + ordem + ", linha=" + linha
				+ ", latitude=" + latitude + ", longitude=" + longitude
				+ ", velocidade=" + velocidade + ", grupos = ( ";
		for(int grupo: getGroups()) {
			string += grupo + " ";
		}
		string += "), UUID = " + getUUID() + "]";
		return string;
	}

	public Date getData() {
		return data;
	}
	public void setData(Date data) {
		this.data = data;
	}
	/**
	 * set date in format yyyy-mm-ddThh:mm:ss.sssz
	 * @param string
	 */
	@SuppressWarnings("deprecation")
	public void setData(String string) {
		if(string.matches(".*T.*Z")) {
			// this date is in the format yyyy-mm-ddThh:mm:ss.sssZ
			String[] slice = string.split("[\\-T\\:\\.Z]");
			this.data = new Date();
			this.data.setYear(Integer.parseInt(slice[0]) -1900);
			this.data.setMonth(Integer.parseInt(slice[1]) -1);
			this.data.setDate(Integer.parseInt(slice[2]));
			this.data.setHours(Integer.parseInt(slice[3]));
			this.data.setMinutes(Integer.parseInt(slice[4]));
			this.data.setSeconds(Integer.parseInt(slice[5]));
			this.data.setTime(data.getTime() + Integer.parseInt(slice[6]));
		}
		else {
			// unknown format, try something...
			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] date format: " + string);
			this.data = new Date(string);
		}
	}
	public String getOrdem() {
		return ordem;
	}
	public void setOrdem(String ordem) {
		this.ordem = ordem;
	}
	public String getLinha() {
		return linha;
	}
	public void setLinha(String linha) {
		this.linha = linha;
	}
	public void setLinha(int linha) {
		this.linha = "" + linha;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public double getVelocidade() {
		return velocidade;
	}
	public void setVelocidade(double velocidade) {
		this.velocidade = velocidade;
	}
	public HashSet<Integer> getGroups() {
		return groups;
	}
	public void setGroups(HashSet<Integer> groups) {
		this.groups = groups;
	}
	public void addGroup(int group) {
		if(this.groups==null) {
			this.groups = new HashSet<Integer>();
		}
		this.groups.add(group);
	}
	public UUID getUUID() {
		return uuid;
	}
	public void setUUID(UUID uuid) {
		this.uuid = uuid;
	}
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
}
