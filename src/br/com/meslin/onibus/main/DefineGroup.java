package br.com.meslin.onibus.main;

/**
 * Compiling:
 * $ javac -classpath .:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/contextnet-2.5.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/udilib.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JMapViewer/JMapViewer.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JSON/JSON-Parser/json-20160810.jar -d . br/com/meslin/onibus/main/DefineGroup.java
 * 
 * Executing:
 * in meslin@meslin-notebook:~/Google Drive/workspace-desktop-ubuntu/InterSCity-onibus/bin
 * $ java -classpath .:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/contextnet-2.5.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/ContextNet/udilib.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JMapViewer/JMapViewer.jar:/media/meslin/643CA9553CA92352/Program\ Files/Java/JSON/JSON-Parser/json-20160810.jar br.com.meslin.onibus.main.DefineGroup ../names-relative.txt
 */

import java.io.IOException;
import java.net.MalformedURLException;

import lac.cnet.groupdefiner.components.GroupDefiner;
import lac.cnet.groupdefiner.components.groupselector.GroupSelector;
import br.com.meslin.onibus.aux.connection.HTTPException;
import br.com.meslin.onibus.aux.contextnet.GroupSelectorImplementation;

/**
 * Implements group definition and selection based on regions
 * <p>
 * Usage: DefineGroup <group filename>
 * 
 * @author meslin
 *
 */
public class DefineGroup {
	public static void main(String[] args) throws MalformedURLException, IOException, HTTPException {
		System.err.println("[DefineGroup." + new Object(){}.getClass().getEnclosingMethod().getName() + "]");

		if(args.length != 3)
		{
			System.err.println("Usage: DefineGroup <gateway ip address> <gateway port number> <group filename>");
			return;
		}
		
		String contextNetIPAddress = args[0];
		int contextNetPortNumber = Integer.parseInt(args[1]);
		String filename = args[2];
		
		System.out.println("\n\nStarting Group Define using gateway at " + contextNetIPAddress + ":" + contextNetPortNumber + "\n\n");

		GroupSelector selectGroup = new GroupSelectorImplementation(contextNetIPAddress, contextNetPortNumber, filename);
		new GroupDefiner(selectGroup);
		try {
			Thread.sleep(Long.MAX_VALUE);
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
