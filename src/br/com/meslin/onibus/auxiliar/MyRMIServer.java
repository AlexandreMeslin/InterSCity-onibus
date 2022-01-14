package br.com.meslin.onibus.auxiliar;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

@SuppressWarnings("serial")
public class MyRMIServer extends UnicastRemoteObject implements RMIServerIntf {
	/** number of client process (not number of client threads, as a process may contain some threads) */
	public volatile static Integer nClients;

	
	
	public MyRMIServer(Integer nClients) throws RemoteException {
		MyRMIServer.nClients = nClients;

		try { //special exception handler for registry creation
//			System.setProperty("java.rmi.server.hostname","172.16.0.211");
			LocateRegistry.createRegistry(1099); 
			Naming.rebind("Onibus", this);
            System.out.println("java RMI registry created.");
        } catch (RemoteException | MalformedURLException e) {
            //do nothing, error means registry already exists
            System.out.println("java RMI registry already exists.");
		}
	}
	
	
	
	@Override
	public int incClients() throws RemoteException {
		synchronized (this) {
			nClients++;
			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] nClients = " + nClients);
		}
		return nClients;
	}



	@Override
	public int clientReady() throws RemoteException {
		synchronized (this) {
			nClients--;
			System.err.println("[" + this.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "] nClients = " + nClients);
		}
		return nClients;
	}



	@Override
	public boolean allReady() throws RemoteException {
		return nClients == 0;
	}
}
