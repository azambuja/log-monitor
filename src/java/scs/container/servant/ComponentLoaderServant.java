package scs.container.servant;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import org.omg.PortableServer.Servant;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongAdapter;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.container.ComponentAlreadyLoaded;
import scs.container.ComponentHandle;
import scs.core.ComponentId;
import scs.container.ComponentLoaderPOA;
import scs.container.ComponentNotFound;
import scs.container.LoadFailure;
import scs.core.IComponentHelper;
import scs.core.ShutdownFailed;
import scs.core.servant.IComponentServant;

/**
 * Servant da interface scs::container::ComponentLoader
 * @author Eduardo Fonseca / Luiz Marques
 */
public class ComponentLoaderServant extends ComponentLoaderPOA {

	public class JarFileLoader extends URLClassLoader
	{
		public JarFileLoader (URL[] urls)
		{
			super (urls);
		}
		
		public boolean addFile (String path) 
		{
			String urlPath = "file://" + path;
			try {
				addURL (new URL (urlPath));
			} catch (MalformedURLException e) {
				return(false);
			}
			
			return(true);
		}
		
	}
	
	private ComponentCollectionServant collectionServant = null;
	private Properties config = null;
	private int instanceIdCounter = 0;
	JarFileLoader loader = null;
	
	/**
	 * @param configuration Properties contendo os parametros de configuracao
	 * @param compColl Referencia para o ComponentCollection do container
	 */
	public ComponentLoaderServant(Properties configuration, ComponentCollectionServant compColl) {
		this.config = configuration;
		this.collectionServant = compColl;

		String jarfiles = config.getProperty("jar-files");
		URL urls [] = {};
		loader = new JarFileLoader(urls);
		
		if( jarfiles.length() > 0 ) {
			String files[] = jarfiles.split(";");
			
			for (int i = 0; i < files.length; i++) {
				loader.addFile(files[i]);
			}
		}
	}
	
	/**
	 * Retorna os componentes que podem ser instanciados pelo container
	 * @return ComponentId[] representando componentes conhecidos pelo container e, portanto,
	 * passiveis de instanciacao
	 */
	public ComponentId[] getInstalledComponents() {

		HashSet<ComponentId> componentIds = new HashSet<ComponentId>();
		
		Enumeration keys = config.keys();
		
		while (keys.hasMoreElements()) {
			String element = (String) keys.nextElement();

			String [] line = element.split("-");
			
			if(line.length == 3 && line[0].equals("component"))
			{
				ComponentId c = new ComponentId();
				c.name = line[1];
				c.version = Integer.parseInt(line[2]);
				componentIds.add(c);
			}
		}
		
		if(componentIds.size() == 0)
			return null;
		
		ComponentId[] cmps = new ComponentId[componentIds.size()];
		int i = 0;
		for (Iterator iter = componentIds.iterator(); iter.hasNext();) {
			ComponentId element = (ComponentId) iter.next();
			cmps[i] = element;
			i++;
		}
		return cmps;
	}
	
	/** Cria uma instancia da classe cujo nome e passado como parametro
	 * @param classname String que representa o nome da classe a ser intanciada
	 */
	
	public Servant loadServant(String classname) throws LoadFailure, ComponentNotFound {
		boolean found = false;
		Servant toRun = null;
		
		try {
			toRun = (Servant)Class.forName(classname).newInstance();
			found = true;
		} catch (ClassNotFoundException e) {
			found = false;
		} catch (InstantiationException e) {
			throw new LoadFailure();
		} catch (IllegalAccessException e) {
			throw new LoadFailure();
		} catch (IllegalArgumentException e) {
			throw new LoadFailure();
		} catch (SecurityException e) {
			throw new LoadFailure();
		}

		if( found )
			return toRun;
		
		try {
			System.err.println("chamando this.loader.loadClass(" + classname + ")");
			toRun = (Servant) this.loader.loadClass(classname).newInstance();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ComponentNotFound();
		} catch (InstantiationException e) {
			throw new LoadFailure();
		} catch (IllegalAccessException e) {
			throw new LoadFailure();
		} catch (IllegalArgumentException e) {
			throw new LoadFailure();
		} catch (SecurityException e) {
			throw new LoadFailure();
		}

		return toRun;
	}
	
	/**
	 * Carrega no container componente representado por ComponentId
	 * @param id ComponentId que representa o componente a ser carregado
	 * @param args Conjuntos de argumentos passados para o construtor do componente
	 */
	public ComponentHandle load(ComponentId id, String[] args)
			throws ComponentNotFound, ComponentAlreadyLoaded, LoadFailure {

		Servant toRun = null;
		String className = config.getProperty("component-"+id.name+"-"+id.version);
		
		if(className == null)
		{
			throw new ComponentNotFound();
		}
		
		//System.err.println("carregando classe: " + className );
		toRun = this.loadServant(className);
		IComponentServant icomp= (IComponentServant) toRun;
		icomp.createComponentId(id);
		
		ComponentHandle handle = new ComponentHandle();
		/**
		 * Nao limita a quantidade de instancias por componente por default,
		 * pois esse parametro nao esta definido nas propriedades do componente
		 */
		/*
		ComponentHandle compHandles[] = this.collectionServant.getComponent(id);
		
		if( compHandles != null && compHandles.length > 0 ) {
			throw new ComponentAlreadyLoaded();
		}
		*/
		 
		try {
			//System.err.println("Tentativa de narrow da classe carregada para IComponent ... " );
			//System.err.println();
			handle.cmp = IComponentHelper.narrow(this._poa().servant_to_reference(toRun));
			handle.id = id;
			handle.instance_id = instanceIdCounter++;
			
		} catch (ServantNotActive e) {
			throw new LoadFailure();
		} catch (WrongPolicy e) {
			throw new LoadFailure();
		}

		this.collectionServant.addComponent(handle);
		
		return handle;
	}

	/** 
	 * descarrega a instancia ComponentHandle do container
	 * @param handle ComponentHandle representando a instancia a ser descarregada
	 */
	public void unload(ComponentHandle handle) throws ComponentNotFound {
		if( ! this.collectionServant.removeComponent(handle) )
			throw new ComponentNotFound();
		
		try {
			handle.cmp.shutdown();
		} catch (ShutdownFailed e) {
			e.printStackTrace();
		}
		
		try {
			this._poa().deactivate_object(this._poa().reference_to_id(handle.cmp));
		} catch (ObjectNotActive e) {
			e.printStackTrace();
		} catch (WrongPolicy e) {
			e.printStackTrace();
		} catch (WrongAdapter e) {
			e.printStackTrace();
		}
	
	}

}
