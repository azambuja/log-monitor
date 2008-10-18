
package scs.instrumentation.app;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import scs.core.StartupFailed;
import scs.execution_node.ContainerDescription;
import scs.execution_node.ExecutionNode;
import scs.execution_node.ExecutionNodeHelper;
import scs.instrumentation.InterfaceStats;
import scs.instrumentation.ContainerStats;
import scs.instrumentation.MethodStats;
import scs.instrumentation.StatsCollection;
import scs.instrumentation.ComponentStatsNotAvailable;
import scs.instrumentation.servant.MethodNotification;

/**
 * Aplicacao GUI para coletar as informacoes de instrumentacao do SCS
 *
 */
public class Monitor extends JPanel
                      implements TreeSelectionListener, MouseListener {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JEditorPane htmlPane;
    private JTree tree;
    
	static ORB orb;
	static ExecutionNode execNode;
	static StatsCollection statsCollection;
	
	static HashMap<String,ExecutionNode> execNodeArr = new HashMap<String,ExecutionNode>();
	
	private static final String EXEC_NODE_NAME = "ExecutionNode";
	
    private static boolean playWithLineStyle = false;
    private static String lineStyle = "Horizontal";
    
    private static boolean useSystemLookAndFeel = false;
    
    private static JTable table;
    private static SharedDataModel dataModel;
    private DefaultMutableTreeNode top; 
    private static JFrame frame;
    MethodNotification methodNotification = null;
    
    public Monitor() {
        super(new GridLayout(1,0));

        top =
            new DefaultMutableTreeNode("Máquinas");
        createNodes(top);

        tree = new JTree(top);
        tree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);

        tree.addTreeSelectionListener(this);

        if (playWithLineStyle) {
            System.out.println("line style = " + lineStyle);
            tree.putClientProperty("JTree.lineStyle", lineStyle);
        }

        JScrollPane treeView = new JScrollPane(tree);

        htmlPane = new JEditorPane();
        htmlPane.setEditable(false);
        JScrollPane htmlView = new JScrollPane(htmlPane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(treeView);
        splitPane.setBottomComponent(htmlView);

        Dimension minimumSize = new Dimension(100, 50);
        htmlView.setMinimumSize(minimumSize);
        treeView.setMinimumSize(minimumSize);
        splitPane.setDividerLocation(300);
                                      
        JButton b = new JButton("Monitorar Método");
        JButton rb = new JButton("Remover");
        JButton ab = new JButton("Atualizar");
        JButton cb = new JButton("Adicionar IP");
        
        
        
        b.addMouseListener(this);
        rb.addMouseListener(this);
        ab.addMouseListener(this);
        cb.addMouseListener(this);
        
        String[] columnNames = { "IP","Container","Interface","Método","Ultima Chamada"};

        dataModel = new SharedDataModel(columnNames);
        
        
        
        table = new JTable(dataModel);
        
        JScrollPane tablePane = new JScrollPane(table);

        
        tablePane.setPreferredSize(new Dimension(500, 200));

        JPanel splitPane1 = new JPanel();

        splitPane1.add(b);
        splitPane1.add(rb);
        splitPane1.add(ab);
        splitPane1.add(cb);
        splitPane1.add(tablePane);
        

        splitPane.setDividerLocation(100); 

        splitPane.setPreferredSize(new Dimension(500, 600));

        
        splitPane1.setPreferredSize(new Dimension(10, 300));

        add(splitPane);
        add(splitPane1);

        
        MethodObserver methodObserver = new MethodObserver();
		methodNotification = new MethodNotification(methodObserver);

    }

    /** Required by TreeSelectionListener interface. */
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                           tree.getLastSelectedPathComponent();

        if (node == null ) return;

        Object nodeInfo = node.getUserObject();
        if (nodeInfo instanceof  String) return;
        if (node.isLeaf()) {
            MethodStats ms = ((MsWrapper)nodeInfo).ms;
            displayProperties(ms);
        } else if (nodeInfo instanceof ContWrapper) {
        	ContWrapper nodeWrapper = (ContWrapper) nodeInfo;
        	displayProperties(nodeWrapper.cs);
			
		}
    }
	class MethodObserver implements Observer {
		public void update(Observable o, Object arg) {
			System.err.println("MethodObserver chamado: " + (String)arg);
			String [] resp = ((String)arg).split("\\|");
			int linha = searchMethodInterface(resp[0],resp[1],resp[2],resp[3]);
			updateLine(linha); 
			
		}
	}
	
    class MsWrapper {
    	
    	MethodStats ms;
    	
        public String toString()
        {
        	return ms.methodName ;
        }
        
    	public MsWrapper(MethodStats ms)
        {
        	this.ms = ms;
        }
    }
    class ContWrapper {
    	
    	ContainerStats cs;
    	
        public String toString()
        {
        	return cs.containerName;
        }
        
    	public ContWrapper(ContainerStats cs)
        {
        	this.cs = cs;
        }
    }
    class SharedDataModel extends DefaultListModel
    implements TableModel {
    	public String[] columnNames;

    	public SharedDataModel(String[] columnNames) {
    		super();
    		this.columnNames = columnNames;
    	}

    	public void rowChanged(int row) {
    		fireContentsChanged(this, row, row); 
    	}

    	private TableModel tableModel = new AbstractTableModel() {
    		public String getColumnName(int column) {
    			return columnNames[column];
    		}
    		public int getRowCount() { 
    			return size();
    		}
    		public int getColumnCount() {
    			return columnNames.length;
    		}
    		public Object getValueAt(int row, int column) {
    			String[] rowData = (String [])elementAt(row);
    			return rowData[column];
    		}
    		public boolean isCellEditable(int row, int column) {
    			return true;
    		}
    		public void setValueAt(Object value, int row, int column) {
    			String newValue = (String)value;
    			String[] rowData = (String [])elementAt(row);
    			rowData[column] = newValue;
    			fireTableCellUpdated(row, column); //table event
    			rowChanged(row);                   //list event
    		}
    	};


    	public int getRowCount() {
    		return tableModel.getRowCount();
    	}
    	public int getColumnCount() {
    		return tableModel.getColumnCount();
    	}
    	public String getColumnName(int columnIndex) {
    		return tableModel.getColumnName(columnIndex);
    	}
    	public Class getColumnClass(int columnIndex) {
    		return tableModel.getColumnClass(columnIndex);
    	}
    	public boolean isCellEditable(int rowIndex, int columnIndex) {
    		return tableModel.isCellEditable(rowIndex, columnIndex);
    	}
    	public Object getValueAt(int rowIndex, int columnIndex) {
    		return tableModel.getValueAt(rowIndex, columnIndex);
    	}
    	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    		tableModel.setValueAt(aValue, rowIndex, columnIndex);
    	}
    	public void addTableModelListener(TableModelListener l) {
    		tableModel.addTableModelListener(l);
    	}
    	public void removeTableModelListener(TableModelListener l) {
    		tableModel.removeTableModelListener(l);
    	}
    }



    private void displayProperties(MethodStats ms) {
        if (ms != null) {
        	String result = "";
        	result +=  "Número de Chamadas: " + ms.callsCount + "\n";
        	result +=  "Tempo de CPU: " + ms.cpuTime + " (ms)\n";
        	result +=  "Tempo de resposta: " + ms.elapsedTime + " (ms)\n";
        	
        	
        	htmlPane.setText(result);
        	//htmlPane.setText("Número de Chamadas: " );
        } 
        
    }
    private void displayProperties(ContainerStats cs) {
        if (cs != null) {
        	String result = "";
        	
        	DecimalFormat df = new DecimalFormat();
        	
        	result +=  "Resultados do Container " + cs.containerName + "\n";
        	result +=  "Tempo total do uso da CPU: " + cs.cpuTime + " (ms)\n";
        	result +=  "Tempo de execução do container: " + cs.elapsedTime + " (ms)\n";
        	
        	df.applyPattern("#0.00");
        	
        	result +=  "Uso de CPU (instante): " +  df.format(cs.cpuUsage * 100) + "%\n";
        	result +=  "Uso de CPU médio: " + df.format(cs.avgCpuUsage * 100) + "%\n";
        	result +=  "Uso de memória: " + cs.memoryUsage  + " KB\n";
        	
        	htmlPane.setText(result);
        	//htmlPane.setText("Número de Chamadas: " );
        } 
        
    }

    private void createNodes(DefaultMutableTreeNode top) {
    	DefaultMutableTreeNode nodeIP = null;
    	DefaultMutableTreeNode nodeCont = null;
        DefaultMutableTreeNode nodeInt = null;
        DefaultMutableTreeNode component = null;
			
        //ContainerDescription[] cd = execNode.getContainers();
        Set <String> Ips = execNodeArr.keySet();
        
        Iterator itIps = Ips.iterator();
        
        while (itIps.hasNext()) {
        	String Ip = (String) itIps.next();
        	nodeIP = new DefaultMutableTreeNode(Ip);
        	top.add(nodeIP);
        	execNode = execNodeArr.get(Ip);
        	ContainerDescription[] cd = null;
        	
        	try{
        		cd = execNode.getContainers();
        	}
        	catch(Exception ex)
        	{
        		execNodeArr.remove(Ip);
        		continue;
        	}
        	for (int k = 0; k < cd.length; k++) {

        		StatsCollection sc = (StatsCollection)cd[k].container.getFacet("scs::instrumentation::StatsCollection");
        		if(sc == null)
        		{
        			continue;
        		}
        		ContainerStats contS = sc.getContainerStats();
                
        		nodeCont = new DefaultMutableTreeNode(new ContWrapper(contS));
        		nodeIP.add(nodeCont);
                        
                        try{
        		        InterfaceStats[] cs = sc.getComponentsStats();
        		        for (int i = 0; i < cs.length; i++) {
        			      MethodStats[] ms = cs[i].methodStatsCollection;
        			      component = new DefaultMutableTreeNode(cs[i].interfaceName);
        			      nodeCont.add(component);

        			      for (int j = 0; j < ms.length; j++) {
        			        	MsWrapper wr = new MsWrapper(ms[j]); 
        				        nodeInt = new DefaultMutableTreeNode(wr);
        				        component.add(nodeInt);
        			      }

        		        }
                         } catch (ComponentStatsNotAvailable e) {
                                continue;
                         }
        	}
        }
    }
         
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        if (useSystemLookAndFeel) {
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Couldn't use system look and feel.");
            }
        }

        //Create and set up the window.
        frame = new JFrame("Monitor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        Monitor newContentPane = new Monitor();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
    	orb  = ORB.init(args, null);
    	/*try {
			initializeStats("localhost");
		} catch (StartupFailed e) {
			e.printStackTrace();
		} catch (InvalidName e) {
			e.printStackTrace();
		} catch (NotFound e) {
			e.printStackTrace();
		} catch (CannotProceed e) {
			e.printStackTrace();
		} catch (org.omg.CosNaming.NamingContextPackage.InvalidName e) {
			e.printStackTrace();
		}*/
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
    /**
	 * @param args
	 * @throws StartupFailed
	 * @throws org.omg.CORBA.ORBPackage.InvalidName 
	 * @throws org.omg.CosNaming.NamingContextPackage.InvalidName 
	 * @throws CannotProceed 
	 * @throws NotFound 
	 */
	static void initializeStats(String host) throws StartupFailed, org.omg.CORBA.ORBPackage.InvalidName, NotFound, CannotProceed, org.omg.CosNaming.NamingContextPackage.InvalidName{
		
		
		String port = "1050";
		String corbaname = "corbaname::10.0.8.169:"+port+"#"+EXEC_NODE_NAME + "-" + host;
		
		System.out.println("corbaname == "+corbaname);
		org.omg.CORBA.Object obj = orb.string_to_object( corbaname );
		IComponent execNodeComp = IComponentHelper.narrow(obj);
		
		if (execNodeComp== null) {
			System.err.println("stringfied object reference is of wrong type");
			throw(new NotFound());
		}
		
		execNodeComp.startup();
		
		//statsCollection = (StatsCollection)execNodeComp.getFacet("Instrumentation::StatsCollection");
		
		execNode = ExecutionNodeHelper.narrow(execNodeComp.getFacet("scs::execution_node::ExecutionNode"));
		execNodeArr.put(host, ExecutionNodeHelper.narrow(execNodeComp.getFacet("scs::execution_node::ExecutionNode")));
		
		if( execNode == null ) {
			System.err.println("Erro ao pegar faceta ExecutionNode do componente !");
			System.exit(-1);
		}
		
	}
    public void removeLine()
    {	
    	if(dataModel.size() > 0)
    		if(table.isRowSelected(table.getSelectedRow()))
    		{	
    			Object line = dataModel.remove(table.getSelectedRow());
    			System.out.println(((String[])line)[0]);
    			execNode = execNodeArr.get(((String[])line)[0]);
    			IComponent cont = execNode.getContainer(((String[])line)[1]);
    			statsCollection = (StatsCollection)cont.getFacet("scs::instrumentation::StatsCollection");
    			statsCollection.cancelMethodNotification("monitor",((String[])line)[0], ((String[])line)[1]);
    			
    		
    		}
    	table.updateUI();
    	
    }
    public void updateTreeLine()
    {	
    	
    	//TreePath th = tree.getSelectionPath();
    	
    	top.removeAllChildren();
    	
    	createNodes(top);   
    	
    	tree.updateUI();
    	

    	
    	
    }
    public void addNode()
    {
    	String s = (String)JOptionPane.showInputDialog(
    			frame,
    			"Digite o Ip da máquina:",
    			"Adcionar IP",
    			JOptionPane.PLAIN_MESSAGE,
    			null,
    			null,
    			"");
    	
    	try {
			initializeStats(s);
		} catch (StartupFailed e) {
			JOptionPane.showMessageDialog(frame,
                    "Este IP não possui um executionNode ativo!!!",
                    "Alerta",
                    JOptionPane.PLAIN_MESSAGE);
			e.printStackTrace();
			
		} catch (InvalidName e) {
			JOptionPane.showMessageDialog(frame,
                    "Este IP não possui um executionNode ativo!!!",
                    "Alerta",
                    JOptionPane.PLAIN_MESSAGE);
			e.printStackTrace();
		} catch (NotFound e) {
			JOptionPane.showMessageDialog(frame,
                    "Este IP não possui um executionNode ativo!!!",
                    "Alerta",
                    JOptionPane.PLAIN_MESSAGE);
			e.printStackTrace();
		} catch (CannotProceed e) {
			JOptionPane.showMessageDialog(frame,
                    "Este IP não possui um executionNode ativo!!!",
                    "Alerta",
                    JOptionPane.PLAIN_MESSAGE);
			
			e.printStackTrace();
		} catch (org.omg.CosNaming.NamingContextPackage.InvalidName e) {
			JOptionPane.showMessageDialog(frame,
                    "Este IP não possui um executionNode ativo!!!",
                    "Alerta",
                    JOptionPane.PLAIN_MESSAGE);
			e.printStackTrace();
		}
    	
    	updateTreeLine();
    	
    }
    public void mouseClicked(MouseEvent arg0) {
		
		if((((JButton)arg0.getSource()).getText()).equals("Remover"))
		{
			removeLine();
			return;
		}
		if((((JButton)arg0.getSource()).getText()).equals("Atualizar"))
		{
			updateTreeLine();
			return;
		}
		if((((JButton)arg0.getSource()).getText()).equals("Adicionar IP"))
		{
			addNode();
			return;
		}
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)
        tree.getLastSelectedPathComponent();

		if (node == null) return;
		
		
		String methodName = "";
		String interfaceName = "";
		String container = "";
		String IP = "";
		
		if (node.isLeaf()) {
			methodName = node.toString(); 
 			TreeNode eNode = node.getParent();
 			int i = 0;
			while(!eNode.toString().equals("Máquinas"))
			{
				
				switch (i) {
				
				case 0:
					interfaceName = eNode.toString();
					break;
				case 1:
					container = eNode.toString();
					break;		
				case 2:
					IP = eNode.toString();
					
				default:
					break;
				}
				eNode = eNode.getParent();
				i++;
			}
		} 
		else
			return;
		System.out.println("Método: " + methodName + " Interface " + interfaceName + " Container " + container);
		
		
		int linha = searchMethodInterface(IP,container,interfaceName,methodName);
		if(linha < 0)
		{   
			execNode = execNodeArr.get(IP);
			IComponent cont = execNode.getContainer(container);
			statsCollection = (StatsCollection)cont.getFacet("scs::instrumentation::StatsCollection");
			
			if( !statsCollection.subscribeMethodNotification("Monitor",interfaceName, methodName, methodNotification.getEventSink() ) ) {
				System.err.println("Erro ao chamar subscribeMethodNotification()");
			}
			dataModel.addElement(new String[]{IP,container,interfaceName,methodName,""});
		}
		else
		{
			//updateLine(linha);
		}
		table.updateUI();
	}
	public void updateLine(int index)
	{
		if(index < 0) return; 
		Object line = dataModel.elementAt(index);
		
		((String[])line)[4] = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());;
		table.updateUI();
	}
	public int searchMethodInterface(String IP,String containerName, String interfaceName,  String methodName)
	{
		Object [] linhas = dataModel.toArray();
		if(linhas.length > 0)
		{   
			
			for (int i = 0; i < linhas.length; i++) {
				
				if( ((String[])linhas[i])[0].equals(IP) &&
					((String[])linhas[i])[1].equals(containerName) &&
					((String[])linhas[i])[2].equals(interfaceName) &&
					((String[])linhas[i])[3].equals(methodName))
				{
					 
					return i;
				}	
				
			}
			
		}
		
		return -1;
	}
	public void mouseEntered(MouseEvent arg0) {
		
	}

	public void mouseExited(MouseEvent arg0) {
		
	}

	public void mousePressed(MouseEvent arg0) {
		
	}

	public void mouseReleased(MouseEvent arg0) {
		
	}

}
