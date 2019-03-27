/**
 * Implements start and end date and time for benchmark<br>
 * There are 2 flags for empty date and time<br>
 */
package br.com.meslin.onibus.aux.model;

public class BenchmarkDateTime {
	private static final BenchmarkDateTime INSTANCE = new BenchmarkDateTime();
	
	private long startTime;
	private long endTime;
	private boolean emptyStartTime;
	private boolean emptyEndTime;

	private BenchmarkDateTime() {
		emptyStartTime = true;
		emptyEndTime = true;
	}
	
	public static BenchmarkDateTime getInstance() {
		return INSTANCE;
	}

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * @param endTime the endTime to set
	 */
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	/**
	 * @return the emptyStartTime
	 */
	public boolean isEmptyStartTime() {
		return emptyStartTime;
	}

	/**
	 * @param emptyStartTime the emptyStartTime to set
	 */
	public void setEmptyStartTime(boolean emptyStartTime) {
		this.emptyStartTime = emptyStartTime;
	}

	/**
	 * @return the emptyEndTime
	 */
	public boolean isEmptyEndTime() {
		return emptyEndTime;
	}

	/**
	 * @param emptyEndTime the emptyEndTime to set
	 */
	public void setEmptyEndTime(boolean emptyEndTime) {
		this.emptyEndTime = emptyEndTime;
	}

	/**
	 * @return start time, end time and elapsed time
	 */
	public String toString() {
		return "Started at " + startTime + ", ended at " + endTime + ", time elapsed " + (endTime - startTime);
	}
}
