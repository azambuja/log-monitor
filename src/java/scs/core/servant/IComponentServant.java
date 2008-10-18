package scs.core.servant;

import java.util.ArrayList;
import java.util.Iterator;

import org.omg.CORBA.Object;

import scs.core.FacetDescription;
import scs.core.IComponentPOA;
import scs.core.ShutdownFailed;
import scs.core.StartupFailed;
import scs.core.ComponentId;

/**
 * Servant generico de um IComponent, que serve de classe abstrata para 
 * os demais IComponents
 * 
 * Implementa as caracteristicas comuns a todos os IComponents.
 * 
 * Enquanto o startup nao for chamado, nao retorna nenhuma faceta para o cliente.
 */
public abstract class IComponentServant extends IComponentPOA {

	/**
	 * Flag para controlar a ativacao do componente apos o startup
	 */
	private	boolean isActive = false;
	
	/**
	 * Container de facetas do IComponent
	 */
	ArrayList<FacetDescription> facets = null;
	
	/**
	 * ComponentId do componente
	 */
	protected ComponentId cpId = null;
	
	/**
	 * Metodo abstrato para criar as facetas nas classes derivadas
	 * @return container de facetas 
	 */
	protected abstract ArrayList<FacetDescription> createFacets();

	/**
	 * Metodo abstrato para implementar o shutdown nas classes derivadas
	 * @return status da operacao
	 */
	protected abstract boolean doShutdown();

	/**
	 * Metodo abstrato para implementar o startup nas classes derivadas
	 * @return status da operacao
	 */
	protected abstract boolean doStartup();

	 

	/** 
	 * Metodo que retorna o servant que implementa uma interface associada a uma faceta do componente
	 * @param facet_interface String que representa o nome da interface associada a faceta
	 */
	public org.omg.CORBA.Object getFacet(String facet_interface) {
		
		if(!isActive) {
			return null;
		}

		for (Iterator<FacetDescription> iter = this.facets.iterator(); iter.hasNext();) {
			FacetDescription f = iter.next();
			if( f.interface_name.equals(facet_interface)) {
				return f.facet_ref;
			}
		}
		
		return null;
	}
 
	/**
	 * Metodo que retorna o servant que implementa uma faceta do componente
	 * @param facet String que representa o nome da faceta
	 */
	public org.omg.CORBA.Object getFacetByName(String facet) {
		if(!isActive)
			return null;

		for (Iterator<FacetDescription> iter = this.facets.iterator(); iter.hasNext();) {
			FacetDescription f = iter.next();
			if( f.name.equals(facet) )
				return f.facet_ref;
		}
		
		return null;
	}

	/**
	 * Chama o metodo doShutdown das classes derivadas para permitir que elas
	 * facam alguma coisa durante o termino do ICOmponent
	 */
	public void shutdown() throws ShutdownFailed {
		if( !this.doShutdown() )
			throw new ShutdownFailed();
		
		this.isActive = false;
	}

	/** 
	 * Chama o metodo doStartup das classes derivadas para permitir que elas
	 * facam alguma coisa durante a inicializacao do ICOmponent
	 */
	public void startup() throws StartupFailed {
		this.facets = this.createFacets();

		if( !this.doStartup() )
			throw new StartupFailed();
		
		this.isActive = true;
	}

	/**
	 * Retorna todas as facetas do componente 
	 * @return FacetDescription[] Array contendo a descricao das facetas 
	 */
	public FacetDescription[] getFacets() {
		return this.facets.toArray(new FacetDescription[this.facets.size()]);
	}
	
	/** Metodo para prover o ComponentId nas classes derivadas
	 * @param ComponentId contendo nome e versao do componente 
	 */
	public void createComponentId(ComponentId id) {
		this.cpId = id;
	}
		

	/**
	 * Retorna o ComponentId do componente 
	 * @return ComponentId Nome e versão do componente 
	 */
	public ComponentId getComponentId() {
		return this.cpId;
	}
}
