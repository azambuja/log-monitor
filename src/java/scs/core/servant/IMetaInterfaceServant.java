package scs.core.servant;

import java.util.ArrayList;
import java.util.Iterator;

import scs.core.FacetDescription;
import scs.core.ReceptacleDescription;


/**
 * Classe abstrata que contém a implementação genérica de IMetaInterface 
 * para os demais componentes do SCS 
 * 
 * @author Eduardo Fonseca/Luiz Marques
 *
 */
public abstract class IMetaInterfaceServant extends scs.core.IMetaInterfacePOA  {

	/** 
	 * Metod abstrado que deve ser implementado nas classes derivadas para retornar seu proprio IComponent
	 * @return IComponentServant servant que representa o componente
	 */
	public abstract IComponentServant getIComponentServant();

	/**
	 * Template method a ser definido pelas classes concretas para retornar 
	 * seu proprio IReceptaclesServants
	 * @return ArrayList<IReceptaclesServant> Array contendo os IReceptaclesServant do Componente
	 */
	public abstract ArrayList<IReceptaclesServant> getIReceptaclesServants();

	/** 
	 * Retornas todas as facetas do componente
	 * @return ArrayList<IReceptaclesServant> Array contendo a descricao das facetas
	 */
	public FacetDescription[] getFacets() {
		IComponentServant servant = getIComponentServant();
		FacetDescription[] fd;
		
		if( servant != null )
			fd = servant.getFacets();
		else
			fd = new FacetDescription[1];
		
		return fd;
		
	}
	
	/** 
	 * Retorna as facetas cujos nomes foram especificados em names
	 * @param names Array de String contendo o nomes das facetas
	 * @return FacetDescription[] Array contendo a descricao das facetas
	 */
	public FacetDescription[] getFacetsByName(String[] names) throws scs.core.InvalidName {

		FacetDescription[] facets = this.getFacets();
		ArrayList<FacetDescription> f = new ArrayList<FacetDescription>();

		for (int i = 0; i < facets.length; i++) {
			FacetDescription description = facets[i];
			for (int j = 0; j < names.length; j++) {
				if(names[i].equals(description.name))
					f.add(description);
			}
		}

		return (FacetDescription[]) f.toArray();
	}

	/**
	 * Retorna os Receptaculos do componente
	 * @return ReceptacleDescription[] Array contendo descricao dos receptaculos 
	 */
	public ReceptacleDescription[] getReceptacles() {
		ArrayList<IReceptaclesServant> servants = getIReceptaclesServants();
		ArrayList<ReceptacleDescription> recDescs = new ArrayList<ReceptacleDescription>();

		if( servants != null )
		{
			for (Iterator<IReceptaclesServant> iter = servants.iterator(); iter.hasNext();) {
				IReceptaclesServant element = iter.next();
				ArrayList<ReceptacleDescription> aux = element.getReceptacles();
				recDescs.addAll(aux);
			}
		}
		
		return recDescs.toArray(new ReceptacleDescription[0]);
	}

	/** 
	 * Retorna os receptaculos cujos nomes foram especificados em names
	 * @param names Array de String contendo o nomes dos receptaculos
	 * @return ReceptacleDescription[] Array contendo a descricao dos receptaculos
	 */
	public ReceptacleDescription[] getReceptaclesByName(String[] names) throws scs.core.InvalidName {
		
		ReceptacleDescription[] Receptacle = this.getReceptacles();
		ArrayList<ReceptacleDescription> f = new ArrayList<ReceptacleDescription>();

		for (int i = 0; i < Receptacle.length; i++) {
			ReceptacleDescription description = Receptacle[i];
			for (int j = 0; j < names.length; j++) {
				if(names[i].equals(description.name))
					f.add(description);
			}
		}

		return (ReceptacleDescription[]) f.toArray();
	}
}
