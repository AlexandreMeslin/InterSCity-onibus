/**
 * @author: Alexandre Meslin
 * 
 * https://stackoverflow.com/questions/11306811/how-to-get-the-caller-class-in-java
 */
package br.com.meslin.onibus.aux;

public class Debug {

	public static boolean enable = true;
	public Debug() {
		enable = true;
	}

	public final static void println(String s) {
		if(Debug.enable) System.err.println("\r[" + new Exception().getStackTrace()[1].getClassName() + "." 
												+ new Exception().getStackTrace()[1].getMethodName() + " (line #" 
												+ new Exception().getStackTrace()[1].getLineNumber() + ")] " + s);
	}
}
