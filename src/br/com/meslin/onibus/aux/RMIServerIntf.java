package br.com.meslin.onibus.aux;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIServerIntf extends Remote {
	public int incClients() throws RemoteException;
	public int clientReady() throws RemoteException;
	public boolean allReady() throws RemoteException;
}
