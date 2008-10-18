package scs.container.servant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import scs.container.ComponentCollectionPOA;
import scs.container.ComponentHandle;
import scs.core.ComponentId;

/**
 * Servant da interface scs::container::ComponentCollection
 * @author Eduardo Fonseca / Luiz Marques
 */
public class ComponentCollectionServant extends ComponentCollectionPOA {

	/**
	 * Container de componentes instanciados
	 * Map indexado pela chave criada pelo metodo makeKey
	 * @see makeKey 
	 */
	HashMap<String, ArrayList<ComponentHandle> > hashCompList = null;

	/**
	 * ctor default
	 */
	public ComponentCollectionServant() {
		hashCompList = new HashMap<String, ArrayList<ComponentHandle> >();		 
	}

	/**
	 * Cria a chave para o map de componentes
	 * @return string concatenando o nome do componente com a sua versao
	 */
	private String makeKey(ComponentId compid) {
		return compid.name + String.valueOf(compid.version);
	}
	
	/** 
	 * Retorna o conjunto de instancias de um componente representado por ComponentId
	 * @param id ComponentId representando o componente cujas intancias devem ser retornadas
	 */
	public ComponentHandle[] getComponent(ComponentId id) {


		System.out.println("ComponentCollectionServant.getComponent() parameters:");
		System.out.println("\tid.name ==> " + id.name);
		System.out.println("\tid.version ==> " + id.version);

		this.dumpComponents();
		
		ArrayList<ComponentHandle> compList = hashCompList.get(makeKey(id));
		
		if( compList == null ){
			System.err.println("ComponentCollection: Component not found !");
			return new ComponentHandle[0];
		}
		
		return compList.toArray(new ComponentHandle[compList.size()]);
	}
	
	
	/** 
	 * Retorna todas as instancias de componentes que executam no container
	 * @return ComponentHandle[] representando as instancias de componentes
	 */
	public ComponentHandle[] getComponents() {
		
		//System.out.println("getComponents()");
		//this.dumpComponents();
		
		ArrayList<ComponentHandle> components = new ArrayList<ComponentHandle>();
		
		for (Iterator<String> iter = hashCompList.keySet().iterator(); iter.hasNext();) {
			ArrayList<ComponentHandle> element = hashCompList.get( iter.next() );
			
			components.addAll(element);
		}
	
		return components.toArray(new ComponentHandle[components.size()]);
	}

	/**
	 * Metodo para adicionar um componente a colecao existente
	 * @param compHandle ComponentHandle contendo as informacoes do novo componente carregado
	 */
	public void addComponent(ComponentHandle compHandle) {
		
		ArrayList<ComponentHandle> compList = this.hashCompList.get(makeKey(compHandle.id));
		
		if( compList == null ) { 
			compList = new ArrayList<ComponentHandle>();
			this.hashCompList.put(makeKey(compHandle.id), compList);
		}
		
		compList.add(compHandle);

		//System.out.println("addComponent()");
		//this.dumpComponents();
	}
	
	/**
	 * Remove um componente da colecao 
	 * @param compHandle ComponentHandle descrevendo o componente a ser removido
	 * @return booleano indicando o status da operacao de remocao
	 */
	public boolean removeComponent(ComponentHandle compHandle) {
		boolean ret = false;
		ArrayList<ComponentHandle> compList = this.hashCompList.get(makeKey(compHandle.id));
		for (Iterator<ComponentHandle> iterator = compList.iterator(); iterator.hasNext();) {
			ComponentHandle handle = iterator.next();
			if (handle.instance_id == compHandle.instance_id) {
				iterator.remove();
				ret = true;
			}
		}
		return ret;
		
//		boolean ret = ( this.hashCompList.remove(makeKey(compHandle.id)) != null );
		//System.out.println("removeComponent()");
		//this.dumpComponents();
	}

	/**
	 * Metodo privado para debug do estado da colecao de componentes.
	 * Habilitar quando necessario para monitorar os componentes da colecao
	 */
	private void dumpComponents() {

		System.out.println("Tamanho do hashCompList: " + this.hashCompList.size());
		
		for (Iterator<String> iter = hashCompList.keySet().iterator(); iter.hasNext();) {
			ArrayList<ComponentHandle> element = hashCompList.get( iter.next() );
			
			for (Iterator<ComponentHandle> iterator = element.iterator(); iterator.hasNext();) {
				ComponentHandle handle = iterator.next();
				System.out.println("Componente: ----------------------------------");
				System.out.println("ComponentId ==> ("+handle.id.name+", "+handle.id.version+")");
				System.out.println("instance_id ==> "+handle.instance_id);
				System.out.println("IComponent ==> "+handle.cmp);
				System.out.println("----------------------------------------------");
			}
		}
	}
}
