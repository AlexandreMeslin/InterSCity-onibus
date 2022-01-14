package br.com.meslin.onibus.auxiliar.connection;

public class Constants {
	// constants
	/** ContextNet addresses */
//	public static final String GATEWAY_IP = "127.0.0.1";
//	public static final String GATEWAY_IP = "172.16.0.202";
	public static final String GATEWAY_IP = "scp.inf.puc-rio.br";
	public static final int GATEWAY_PORT = 5500;
	public static final String GATEWAY_IP_LIST[] = {
		"scp.inf.puc-rio.br",	// group 1
		"scp.inf.puc-rio.br"	// group 2
	};
	public static final int GATEWAY_PORT_LIST[] = {
		5501,					// group 1
		5502					// group 2
	};

	/** InterSCity addresses */
//	public static final String INTERSCITY_URL = "http://0.0.0.0:8000";
//	public static final String INTERSCITY_URL = "http://localhost:8000";
	public static final String INTERSCITY_URL = "http://172.16.10.251:8000";
//	public static final String INTERSCITY_URL = "http://172.16.0.201:8000";
//	public static final String INTERSCITY_URL = "http://scp.inf.puc-rio.br:8000";
}
