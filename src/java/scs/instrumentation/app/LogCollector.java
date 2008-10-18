package scs.instrumentation.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Aplicacao GUI que recebe os logs do LogInterceptor via porta UDP 514 (syslog) 
 */
public class LogCollector implements ActionListener {

	public class LogReceiver extends Thread {
		private LogCollector parent;

		public LogReceiver(LogCollector parent) {
			this.parent = parent;
		}

		@Override
		public void run() {
			DatagramSocket socket = null;
			try {
				socket = new DatagramSocket(514);
			} catch (SocketException e) {
				e.printStackTrace();
			}

			DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);

			while (true) {

				try {
					socket.receive(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}

				String data = new SimpleDateFormat("yyyy-MM-dd H:m:s").format(new Date());				
				
				String msg = new String(packet.getData(), 0, packet.getLength());
				String msgsyslog = "" + data + " " + packet.getAddress().getHostName() + " " + msg;
				parent.addMessage(msgsyslog);
			}
		}

	}

	private static String labelPrefix = "Number of button clicks: ";

	private int numClicks = 0;

	final JLabel label = new JLabel(labelPrefix + "0    ");

	final static String LOOKANDFEEL = null;

	protected JTextField textField;

	protected JTextArea textArea;
	
	protected JCheckBox scrllCheck; 

	private final static String newline = "\n";

	private LogReceiver receiver;

	public Component createComponents() {
		JButton button = new JButton("Close");
		button.setMnemonic(KeyEvent.VK_C);
		button.addActionListener(this);
		label.setLabelFor(button);

		JPanel pane = new JPanel(new BorderLayout());
		JPanel pane2 = new JPanel(new BorderLayout());

		textField = new JTextField(80);
		textField.addActionListener(this);

		textArea = new JTextArea(10, 80);
		textArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textArea,
				 JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		scrllCheck = new JCheckBox("Scroll automatically");
		scrllCheck.setSelected(true);		
		
		pane2.add(button, BorderLayout.LINE_START);
		pane2.add(scrllCheck, BorderLayout.LINE_END);
		pane.add(pane2, BorderLayout.NORTH);

        
		pane.add(scrollPane, BorderLayout.CENTER);
		
		pane.setBorder(BorderFactory.createEmptyBorder(30, // top
				30, // left
				10, // bottom
				30) // right
				);

		receiver = new LogReceiver(this);
		receiver.start();

		return pane;
	}

	public void actionPerformed(ActionEvent e) {
		numClicks++;
		label.setText(labelPrefix + numClicks);
		System.exit(0);
	}

	private static void initLookAndFeel() {
		String lookAndFeel = null;

		if (LOOKANDFEEL != null) {
			if (LOOKANDFEEL.equals("Metal")) {
				lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
			} else if (LOOKANDFEEL.equals("System")) {
				lookAndFeel = UIManager.getSystemLookAndFeelClassName();
			} else if (LOOKANDFEEL.equals("Motif")) {
				lookAndFeel = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
			} else if (LOOKANDFEEL.equals("GTK+")) { // new in 1.4.2
				lookAndFeel = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
			} else {
				System.err
						.println("Unexpected value of LOOKANDFEEL specified: "
								+ LOOKANDFEEL);
				lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
			}

			try {
				UIManager.setLookAndFeel(lookAndFeel);
			} catch (ClassNotFoundException e) {
				System.err
						.println("Couldn't find class for specified look and feel:"
								+ lookAndFeel);
				System.err
						.println("Did you include the L&F library in the class path?");
				System.err.println("Using the default look and feel.");
			} catch (UnsupportedLookAndFeelException e) {
				System.err.println("Can't use the specified look and feel ("
						+ lookAndFeel + ") on this platform.");
				System.err.println("Using the default look and feel.");
			} catch (Exception e) {
				System.err.println("Couldn't get specified look and feel ("
						+ lookAndFeel + "), for some reason.");
				System.err.println("Using the default look and feel.");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	private static void createAndShowGUI() {
		// Set the look and feel.
		initLookAndFeel();

		// Make sure we have nice window decorations.
		JFrame.setDefaultLookAndFeelDecorated(true);

		// Create and set up the window.
		JFrame frame = new JFrame("LogCollector");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		LogCollector app = new LogCollector();
		Component contents = app.createComponents();
		frame.getContentPane().add(contents, BorderLayout.CENTER);

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	public void addMessage(String msg) {
		//System.out.println(msg);
		String text = textField.getText();
		textArea.append(text + newline + msg);
		
		if( this.scrllCheck.isSelected() )
			textArea.setCaretPosition(textArea.getText().length());
	}

	public static void main(String[] args) {

		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}