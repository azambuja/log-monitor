package scs.core.servant;

import org.omg.CORBA.ORB;

/**
 * Classe utilitária que cria uma thread para execução do ORB 
 * sem bloquear a thread principal da aplicação
 * 
 * @author Eduardo Fonseca e Luiz Marques
 *
 */
public class OrbRunner extends Thread {
	ORB orb;
	
	public OrbRunner(ORB orb) {
		this.orb = orb;
	}

	@Override
	public void run() {
		this.orb.run();
	}

}

