package scs.execution_node.servant;

import java.util.ArrayList;
import java.util.Properties;

import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.core.ComponentId;
import scs.core.FacetDescription;
import scs.core.IComponent;
import scs.core.IComponentHelper;
import scs.core.servant.IComponentServant;
import scs.execution_node.ExecutionNode;
import scs.execution_node.ExecutionNodeHelper;

/**
 * Servant do IComponent que oferece a faceta scs::execution_node::ExecutionNode
 * 
 * @author Eduardo Fonseca / Luiz Marques
*/
public class ExecutionNodeComponent extends IComponentServant {

	private ExecutionNodeServant execNodeServant = null;
	private ExecutionNode execNode = null;
	private Properties config = null;

	private final static String IFACE_EXECNODE = "scs::execution_node::ExecutionNode";
	private final static String FACET_EXECNODE = "ExecutionNode";
	
	public ExecutionNodeComponent( Properties configProp ) {
		this.config = configProp;
	}

	/* (non-Javadoc)
	 * @see SCS.servant.IComponentServant#createFacets()
	 */
	protected ComponentId createComponentId() {
		return new ComponentId("ExecutionNodeComponent", 1);
	}

	
	/* (non-Javadoc)
	 * @see SCS.servant.IComponentServant#createFacets()
	 */
	@Override
	protected ArrayList<FacetDescription> createFacets() {
		
		ArrayList<FacetDescription> facets = new ArrayList<FacetDescription>();
		FacetDescription fd = new FacetDescription();
		fd.interface_name = IFACE_EXECNODE;
		fd.name= FACET_EXECNODE;
		fd.facet_ref = getExecutionNode();
		facets.add(fd);
		
		return facets;
	}

	/**
	 * @return instancia unica do ExecutionNodeServant
	 */
	private ExecutionNode getExecutionNode() {
		if( this.execNodeServant == null ) {
			try {
				IComponent thisComponent = IComponentHelper.narrow(this._poa().servant_to_reference(this));
				this.execNodeServant = new ExecutionNodeServant(thisComponent, config);
				this.execNode = ExecutionNodeHelper.narrow(this._poa().servant_to_reference(this.execNodeServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return this.execNode;
	}

	@Override
	protected boolean doShutdown() {
		return true;
	}

	@Override
	protected boolean doStartup() {
		return true;
	}

}
