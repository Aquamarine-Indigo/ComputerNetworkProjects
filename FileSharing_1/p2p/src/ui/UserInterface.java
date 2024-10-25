package p2p.src.ui;
import javax.swing.*;

import java.awt.*;

import p2p.src.peers.*;

public class UserInterface extends JFrame implements ServerStatusListener{
	private PeerFileSharing fileShare;
	private boolean serverStarted;

	private JLabel fileNameLabel;
	private JTextField fileNameField;
	private JLabel serverPortLabel;
	private JTextField serverPortField;
	private JLabel peerAddrLabel;
	private JTextField peerAddrField;
	private JLabel peerPortLabel;
	private JTextField peerPortField;
	private JLabel saveDirLabel;
	private JTextField saveDirField;

	private JLabel ipAddressLabel;
	private JTextField ipAddressField;

	private JButton requestButton;
	private JButton startServerButton;

	private JTextArea statusArea;

	public UserInterface() {
		serverStarted = false;
		setTitle("P2P File Sharing");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setSize(500, 400);
		setVisible(true);

		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(7, 1));

		fileNameLabel = new JLabel("File Name");
		fileNameField = new JTextField();
		serverPortLabel = new JLabel("Server Port");
		serverPortField = new JTextField();
		peerAddrLabel = new JLabel("Peer Address");
		peerAddrField = new JTextField();
		peerPortLabel = new JLabel("Peer Port");
		peerPortField = new JTextField();
		saveDirLabel = new JLabel("Save File Directory");
		saveDirField = new JTextField();
		requestButton = new JButton("Send File");
		startServerButton = new JButton("Start Server");

		ipAddressLabel = new JLabel("IP Address");
		ipAddressField = new JTextField();
		ipAddressField.setEditable(false);


		panel.add(serverPortLabel);
		panel.add(serverPortField);
		panel.add(saveDirLabel);
		panel.add(saveDirField);
		panel.add(fileNameLabel);
		panel.add(fileNameField);
		panel.add(peerAddrLabel);
		panel.add(peerAddrField);
		panel.add(peerPortLabel);
		panel.add(peerPortField);

		panel.add(ipAddressLabel);
		panel.add(ipAddressField);

		panel.add(startServerButton);
		panel.add(requestButton);
		add(panel, BorderLayout.NORTH);

		statusArea = new JTextArea();
		statusArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(statusArea);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		add(scrollPane, BorderLayout.CENTER);

		Timer timer = new Timer(2000, e -> updateIpAddress());
		timer.start();

		startServerButton.addActionListener(e -> {
			if(serverStarted == false) {
				int serverPort = Integer.parseInt(serverPortField.getText());
				String saveDirectory = saveDirField.getText();
				fileShare = new PeerFileSharing(serverPort, saveDirectory);
				fileShare.startServer(this);
				serverStarted = true;
				startServerButton.setText("Stop Server");
			} else {
				fileShare.stopServer(this);
				serverStarted = false;
				startServerButton.setText("Start Server");
			}
		});
		requestButton.addActionListener(e -> {
			if(serverStarted == true) {
				String peerAddress = peerAddrField.getText();
				String fileName = fileNameField.getText();
				int peerPort = Integer.parseInt(peerPortField.getText());
				fileShare.requestFile(peerAddress, peerPort, fileName, this);
			} else {
				System.out.println("Server not started. Please start the server first.");
				appendStatus("Please start the server before request.");
			}
		});
	}

	@Override
	public void onStatusUpdate(String message) {
		SwingUtilities.invokeLater(() -> appendStatus("INFO: " + message));
	}

	@Override
	public void onError(String errorMessage) {
		SwingUtilities.invokeLater(() -> appendStatus("ERROR: " + errorMessage));
	}

	private void appendStatus(String message) {
		statusArea.append(message + "\n");
		statusArea.setCaretPosition(statusArea.getDocument().getLength());
	    }

	private void updateIpAddress() {
		if(serverStarted == true) {
			ipAddressField.setText(fileShare.getServerAddress());
			fileShare.setSharedDirectory(saveDirField.getText());
		} else {
			ipAddressField.setText("Server not started.");
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			UserInterface p2pClient = new UserInterface();
			p2pClient.setVisible(true);
		    });
	}
}

// /Users/aquamarineindigo/Desktop/bin/好看的照片2/IMG_20240116_170951.jpg