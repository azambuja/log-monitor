package scs.container.servant;

import java.util.ArrayList;
import java.util.Properties;

import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.container.ComponentCollection;
import scs.container.ComponentCollectionHelper;
import scs.container.ComponentLoader;
import scs.container.ComponentLoaderHelper;
import scs.container.ComponentHandle;
import scs.container.ComponentNotFound;
import scs.core.ComponentId;
import scs.core.FacetDescription;
import scs.core.IComponent;
import scs.core.IMetaInterface;
import scs.core.IMetaInterfaceHelper;
import scs.core.servant.IComponentServant;
import scs.core.servant.IMetaInterfaceServant;
import scs.core.servant.IReceptaclesServant;
import scs.instrumentation.StatsCollection;
import scs.instrumentation.StatsCollectionHelper;
import scs.instrumentation.app.LogSender;
import scs.instrumentation.servant.StatsCollectionServant;
import system.SystemInformation;
import system.SystemInformation.CPUUsageSnapshot;
import system.SystemInformation.NegativeCPUTime;


/**
 * Servant do IComponent que implementa o componente Container
 */
public class ContainerServant extends IComponentServant {

	/**
	 * Inner class contendo a descricao das interfaces de EventManager
	 */
	private class ContainerMetaInterface extends IMetaInterfaceServant {

		IComponentServant icompServant = null;
		
		public ContainerMetaInterface(IComponentServant ics) {
			this.icompServant = ics;
		}
		
		@Override
		public IComponentServant getIComponentServant() {
			return this.icompServant;
		}

		@Override
		public ArrayList<IReceptaclesServant> getIReceptaclesServants() {
			return null;
		}

	}
	private class StatsCollector extends Thread {

		long interval;
		CPUUsageSnapshot startCpuSnapshot = null;
		CPUUsageSnapshot lastCpuSnapshot = null;
		boolean finish = false;
		LogSender sender = null;
		
		public StatsCollector(long interval) {
			this.interval = interval;

			try {
				this.startCpuSnapshot = SystemInformation.makeCPUUsageSnapshot();
				this.lastCpuSnapshot = this.startCpuSnapshot;
			} catch (NegativeCPUTime e) {
				e.printStackTrace();
			}
			
			Properties props = System.getProperties();
			String logHost = props.getProperty("container.loghost");
			
			if( logHost != null ) {
				int pid = SystemInformation.getProcessID();
				String cname = ContainerServant.getInstance().getName()
					+ " - " + String.valueOf(pid);
				
				this.sender = new LogSender(cname, logHost, 514);
			}
		}
		
		public void finish() {
			this.finish = true;
		}
		
		private void log(String msg) {
			/*if( this.sender != null )
				this.sender.sendEvent(msg);*/
                     //System.out.println(msg);
		}
		
		/**
		 * atualiza estatisticas de utilizacao de recursos do container
		 */
		private void updateTimes() {
			SystemInformation.CPUUsageSnapshot end = null;

			try {
				end = SystemInformation.makeCPUUsageSnapshot();
			} catch (NegativeCPUTime e) {
				e.printStackTrace();
			}
			
			long cputime = end.m_CPUTime - startCpuSnapshot.m_CPUTime;
			this.log("Cpu time: " + cputime );
			
			long elapsedTime = end.m_time - startCpuSnapshot.m_time;
			this.log("Elapsed time: " + elapsedTime );

			double avgCpuUsage = system.SystemInformation.getProcessCPUUsage();
			this.log("Avg Cpu Usage: " + avgCpuUsage );

			long cpuTimeDelta = end.m_CPUTime - this.lastCpuSnapshot.m_CPUTime;
			long elapsedTimeDelta = end.m_time - this.lastCpuSnapshot.m_time;
			double cpuUsage2 = ((double)cpuTimeDelta) / elapsedTimeDelta;
			this.log("Cpu Usage: " + cpuUsage2 );
			
			long contMem = system.SystemInformation.getMemoryUsage();
			this.log("Memory Usage: " + contMem );
			
			statsCollectionServant.setContainerStatsCPUTime( cputime );
			statsCollectionServant.setContainerStatsAvgCPUUsage( avgCpuUsage );
			statsCollectionServant.setContainerStatsCPUUsage( cpuUsage2 );
			statsCollectionServant.setContainerStatsElapsedTime( elapsedTime );
			statsCollectionServant.setContainerStatsMemoryUsage( contMem );
						
			this.lastCpuSnapshot = end;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {

			while(!finish){
				try {
					sleep(this.interval);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				//System.out.println("free memory: " + Runtime.getRuntime().freeMemory());
				updateTimes();
			}
		}
	}
		
	
	private static final String FACET_COMPLOADER = "ComponentLoader";
	private static final String IFACE_COMPLOADER = "scs::container::ComponentLoader";
	
	private static final String FACET_COMPCOLLECTION = "ComponentCollection";
	private static final String IFACE_COMPCOLLECTION = "scs::container::ComponentCollection";
	
	private static final String FACET_STATSCOLLECTION = "StatsCollection";
	private static final String IFACE_STATSCOLLECTION = "scs::instrumentation::StatsCollection";
	
	private static final String FACET_IMETAIFACE 	= "IMetaInterface";
	private static final String IFACE_IMETAIFACE 	= "scs::core::IMetaInterface";
	
	private static final long DEFAULT_STATS_INTERVAL = 15000;

	private ComponentLoaderServant compLoaderServant = null;
	private ComponentLoader compLoader = null;

	private ComponentCollectionServant compCollectionServant = null;
	private ComponentCollection compCollection = null;

	private StatsCollectionServant statsCollectionServant = null;
	private StatsCollection statsCollection = null;
	
	private Properties config=null;
	
	private StatsCollector statsCollector=null;
	
	private static ContainerServant _instance = null;

        private boolean initialized = false;

	String name = "";
	private IComponent eventMgr;

	private ContainerServant() {
		/* construtor privado do Singleton */ 
	}

	ContainerMetaInterface containerInterfaceServant = null;
	IMetaInterface metaInterface = null;

	
	/**
	 * Inicializa o componente Container
	 * @param name nome do container
	 * @param configuration conjunto de propriedades de configuracao
	 * @param eventMgr EventManager criado no execution node
	 */
	public void initialize(String name, Properties configuration, IComponent eventMgr ) {

		this.name = name;
		this.config = configuration;
		this.initialized = true;
		this.eventMgr = eventMgr;
		System.out.println("ContainerServant.initialize() : Configuration = " +this.config);

	}
	
	/**
	 * Retorna o EventManager associado ao containter
	 * @return  EventManager
	 */
	public IComponent getEventMgr(){
		return eventMgr;
	}
	
	/**
	 * Retorna o servant que implementa o componente Container
	 * @return servant do container
	 */
	public static ContainerServant getInstance() {
		if( _instance == null ) {
			_instance = new ContainerServant();
		}
		return _instance;
	}
	
	/* (non-Javadoc)
	 * @see SCS.servant.IComponentServant#createFacets()
	 */
	@Override
	protected ArrayList<FacetDescription> createFacets() {
		ArrayList<FacetDescription> facets = new ArrayList<FacetDescription>();
		FacetDescription fd = new FacetDescription();
		fd.name = FACET_COMPLOADER;
		fd.interface_name = IFACE_COMPLOADER;
		fd.facet_ref = getComponentLoader();
		facets.add(fd);
		
		fd = new FacetDescription();
		fd.name = FACET_COMPCOLLECTION;
		fd.interface_name = IFACE_COMPCOLLECTION;
		fd.facet_ref = getComponentCollection();
		facets.add(fd);
		
		fd = new FacetDescription();
		fd.name = FACET_STATSCOLLECTION;
		fd.interface_name = IFACE_STATSCOLLECTION;
		fd.facet_ref = getStatsCollection();
		facets.add(fd);
		
		fd = new FacetDescription();
		fd.name = FACET_IMETAIFACE;
		fd.interface_name = IFACE_IMETAIFACE;
		fd.facet_ref = getMetaInterface();
		facets.add(fd);
		
		return facets;
	}

	/**
	 * @return StatsCollection: estrutura contendo estatisticas de uso do container
	 */
	private StatsCollection getStatsCollection() {
		if( this.statsCollectionServant == null ) {
			try {
				this.statsCollectionServant = StatsCollectionServant.getInstance();
				this.statsCollection = StatsCollectionHelper.narrow(this._poa().servant_to_reference(this.statsCollectionServant));
				this.statsCollectionServant.setContainerName(this.getName());
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return this.statsCollection;
	}

	/**
	 * @return
	 */
	private ComponentCollection getComponentCollection() {
		if( this.compCollectionServant == null ){
			this.compCollectionServant = new ComponentCollectionServant();
			try {
				this.compCollection = ComponentCollectionHelper.narrow(this._poa().servant_to_reference(this.compCollectionServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return this.compCollection;
	}

	/**
	 * @return
	 */
	private ComponentLoader getComponentLoader() {
		if( this.compLoaderServant == null ){
			this.getComponentCollection();
			this.compLoaderServant = new ComponentLoaderServant( this.config, this.compCollectionServant);
			try {
				this.compLoader = ComponentLoaderHelper.narrow(this._poa().servant_to_reference(this.compLoaderServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return this.compLoader;

	}
	/**
	 * @return Objeto que contém a descrição de todas as facetas desta classe
	 */
	private IMetaInterface getMetaInterface() {
		
		if( this.containerInterfaceServant == null )
		{
			this.containerInterfaceServant = new ContainerMetaInterface(this);
			try {
				this.metaInterface = IMetaInterfaceHelper.narrow( this._poa().servant_to_reference(this.containerInterfaceServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		
		return this.metaInterface;
	}
        
        private class ShutDownContainer extends Thread {
		public void run() {

			try {
				sleep(10000);
				Runtime.getRuntime().exit(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
				
		}
	}

	@Override
	protected boolean doShutdown() {
	    if (statsCollector != null) {
			this.statsCollector.finish();
		}	
        ComponentCollection cmpCollection = getComponentCollection();
		ComponentLoader cmpLoad = getComponentLoader();
		ComponentHandle [] cmpHandles = cmpCollection.getComponents();
                
        for (int i = 0; i < cmpHandles.length; i++) {
			ComponentHandle handle = cmpHandles[i];
			try {
				cmpLoad.unload(handle);
			} catch (ComponentNotFound e) {
				e.printStackTrace();
				return false;
			}
		}
		ShutDownContainer sdc = new ShutDownContainer();
		sdc.start();
		return true;
	}
 
	@Override
	protected boolean doStartup() {
		if(!(this.config.getProperty("enableStatsCollection","0")).equalsIgnoreCase("0"))
		{	
			this.statsCollector = new StatsCollector(DEFAULT_STATS_INTERVAL);
			this.statsCollector.start();
                        System.out.println("Running statsCollector");
		}	
		return true;
	}

    /**
	 * Indica se componente foi iniciado
	 * @return boolean indicando situacao do componente
	 */
	public boolean isInitialized() {
		return this.initialized;
	}

	/**
	 * Retorna nome do container
	 * @return String indicando nome do container
	 */
	public String getName() {
		return this.name;
	}

}
