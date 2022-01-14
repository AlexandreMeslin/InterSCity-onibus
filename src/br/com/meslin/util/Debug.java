/**
 * @author: Alexandre Meslin
 * 
 * https://stackoverflow.com/questions/11306811/how-to-get-the-caller-class-in-java
 */
package br.com.meslin.util;

public class Debug {
	public static boolean enable = true;
	public Debug() {
		enable = true;
	}
	
	/**
	 * Outputs text to stardard output<br>
	 * This is not for debuging<br>
	 * @param s text to output
	 */
	public final static void message(String s) {
		System.out.println("[" + new Exception().getStackTrace()[1].getClassName() + "."
				+ new Exception().getStackTrace()[1].getMethodName() + " (line #" 
				+ new Exception().getStackTrace()[1].getLineNumber() + ")] " + s);
	}

	/**
	 * Outputs text to standard error<br>
	 * This is not for debugging<br>
	 * @param s text to output
	 */
	public final static void error(String s) {
		System.err.println("[" + new Exception().getStackTrace()[1].getClassName() + "."
				+ new Exception().getStackTrace()[1].getMethodName() + " (line #" 
				+ new Exception().getStackTrace()[1].getLineNumber() + ")] " + s);
	}

	/**
	 * Outputs text to stardard output<br>
	 * This is not for debuging<br>
	 * @param s text to output
	 * @param e error object
	 */
	public static void error(String s, Exception e) {
		System.err.println("[" + new Exception().getStackTrace()[1].getClassName() + "." 
				+ new Exception().getStackTrace()[1].getMethodName() + " (line #" 
				+ new Exception().getStackTrace()[1].getLineNumber() + ")] " + s + " :: " + e.getLocalizedMessage());
	}
	
	/**
	 * Outputs text to stardard output<br>
	 * This is for debuging purpose<br>
	 * @param s text to output
	 */
	public final static void info(String s) {
		if(Debug.enable) System.out.println("[" + new Exception().getStackTrace()[1].getClassName() + "." 
				+ new Exception().getStackTrace()[1].getMethodName() + " (line #" 
				+ new Exception().getStackTrace()[1].getLineNumber() + ")] " + s);
	}

	/**
	 * Outputs text to standard error<br>
	 * This is for debuging purpose<br>
	 * @param s text to output
	 */
	public final static void warning(String s) {
		if(Debug.enable) 
			System.err.println("[" + new Exception().getStackTrace()[1].getClassName() + "." 
				+ new Exception().getStackTrace()[1].getMethodName() + " (line #" 
				+ new Exception().getStackTrace()[1].getLineNumber() + ")] " + s);
	}

	/**
	 * Outputs text to stardard error<br>
	 * This is for debuging purpose<br>
	 * @param s text to output
	 * @param e error object
	 */
	public static void warning(String s, Exception e) {
		if(Debug.enable) System.err.println("[" + new Exception().getStackTrace()[1].getClassName() + "." 
				+ new Exception().getStackTrace()[1].getMethodName() + " (line #" 
				+ new Exception().getStackTrace()[1].getLineNumber() + ")] " + s + ":: " + e.getLocalizedMessage());
	}
}
