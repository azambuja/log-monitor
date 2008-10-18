package scs.instrumentation.app;

import system.SystemInformation.CPUUsageSnapshot;

/**
 * Classe que contem as informacoes de contexto de uma chamada de metodo
 * Usada pelo interceptor para salvar as informacoes em receive_request
 * e depois pegar novamente em send_reply
 */
public class CallData {
	private CPUUsageSnapshot start;
	private String interfaceName;
	
	
	public CallData(CPUUsageSnapshot start,String interfaceName) {
		this.start = start;
		this.interfaceName = interfaceName;
	}
	public CPUUsageSnapshot getStart() {
		return start;
	}
	public void setStart(CPUUsageSnapshot start) {
		this.start = start;
	}
	public String getInterfaceName() {
		return interfaceName;
	}
	public void setInterfaceName(String interfaceName) {
	}
	
}

