package br.com.meslin.onibus.aux.event;

import java.util.Date;

import br.com.meslin.onibus.aux.model.Bus;

public class EventRegion {

	private Bus bus;
	private int region;
	private Date timeStamp;

	public EventRegion(Bus bus, int region, long time) {
		this.bus = bus;
		this.region = region;
		this.timeStamp = new Date(time);
	}

	/**
	 * @return the bus
	 */
	public Bus getBus() {
		return bus;
	}

	/**
	 * @param bus the bus to set
	 */
	public void setBus(Bus bus) {
		this.bus = bus;
	}

	/**
	 * @return the group
	 */
	public int getRegion() {
		return region;
	}

	/**
	 * @param group the group to set
	 */
	public void setRegion(int region) {
		this.region = region;
	}

	/**
	 * @return the timeStamp
	 */
	public Date getTimeStamp() {
		return timeStamp;
	}

	/**
	 * @param timeStamp the timeStamp to set
	 */
	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
}
