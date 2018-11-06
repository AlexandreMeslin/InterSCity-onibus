/**
 * 
 */
package br.com.meslin.onibus.aux.connection;

/**
 * @author meslin
 *
 */
@SuppressWarnings("serial")
public class HTTPException extends Exception {
	private int httpResponseCode;

	/**
	 * Constructor
	 */
	public HTTPException() {
		this.httpResponseCode = -1;
	}

	/**
	 * Constructor
	 * @param message
	 */
	public HTTPException(String message) {
		super(message);
		this.httpResponseCode = -1;
	}

	/**
	 * Constructor
	 * @param cause
	 */
	public HTTPException(Throwable cause) {
		super(cause);
		this.httpResponseCode = -1;
	}

	/**
	 * Constructor
	 * @param message
	 * @param cause
	 */
	public HTTPException(String message, Throwable cause) {
		super(message, cause);
		this.httpResponseCode = -1;
	}

	/**
	 * Constructor
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public HTTPException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.httpResponseCode = -1;
	}
	/**
	 * Constructor
	 * @param httpResponseCode
	 */
	public HTTPException(int httpResponseCode) {
		this.httpResponseCode = httpResponseCode;
	}

	public int getHttpResponseCode() {
		return httpResponseCode;
	}

	public void setHttpResponseCode(int httpResponseCode) {
		this.httpResponseCode = httpResponseCode;
	}

	public String getMessage() {
		return "Code " + this.httpResponseCode;
	}
}
