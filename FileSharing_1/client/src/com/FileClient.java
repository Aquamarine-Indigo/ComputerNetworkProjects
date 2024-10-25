package com;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
// import java.util.Scanner;

public class FileClient extends JFrame{
	// private static final String SERVER_ADDR = "127.0.0.1";
	// private static final int SERVER_PORT = 2680;
	private static final int SERVER_BUFFER_SIZE = 4096;

	private JTextField targetNameField;
	private JTextField fileNameField;
	private JTextField serverIpField;
	private JTextField portField;
	private JTextArea statusArea;

	public FileClient() {
		// Set up the main frame
		setTitle("File Download Client");
		setSize(600, 300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null); // Center the window
	
		// 182 215 176
		Color customColor = new Color(182, 215, 176);
		// Create a panel for input fields and buttons
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(5, 1));
		panel.setBackground(customColor);

		// server ip address input
		panel.add(new JLabel("Server IP Address:"));
		serverIpField = new JTextField("127.0.0.1");
		panel.add(serverIpField);
	
		// Port number input
		panel.add(new JLabel("Port:"));
		portField = new JTextField("2680");
		panel.add(portField);
	
		// File name input
		panel.add(new JLabel("File Name:"));
		fileNameField = new JTextField();
		panel.add(fileNameField);

		// Store name input
		panel.add(new JLabel("Store Name:"));
		targetNameField = new JTextField();
		panel.add(targetNameField);
	
		// Download button
		JButton downloadButton = new JButton("Download");
		panel.add(downloadButton);
		// JPanel buttonPanel = new JPanel();
		// buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER)); // Center alignment
		// JButton downloadButton = new JButton("Download");
		// buttonPanel.add(downloadButton); // Add the button to the button panel
	
		// Status area
		statusArea = new JTextArea();
		statusArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(statusArea);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
	
		// Add panel and status area to the frame
		add(panel, BorderLayout.NORTH);
		// add(buttonPanel, BorderLayout.CENTER); // Add button panel in the center
		add(scrollPane, BorderLayout.CENTER);
	
		// Add action listener to the download button
		downloadButton.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String serverIp = serverIpField.getText();
			int port = Integer.parseInt(portField.getText());
			String fileName = fileNameField.getText();
			String targetName = targetNameField.getText();
			String targetDir = System.getProperty("user.dir") + "/files/" + targetName;
			System.out.println(targetDir);
	
			downloadFile(serverIp, port, fileName, targetDir);
		}
		});
	}

	private void downloadFile(String serverIp, int port, String fileName, String targetDir){
		new Thread(() -> {
			try {
				Socket socket = new Socket(serverIp, port);
				InputStream input = socket.getInputStream();
				OutputStream output = socket.getOutputStream();

				output.write(fileName.getBytes());
				output.flush();
				System.out.println("Send request to server: " + fileName);
				updateStatus("Send request to server: " + fileName);

				FileOutputStream fileWrite = new FileOutputStream(targetDir);

				byte[] buffer = new byte[SERVER_BUFFER_SIZE];
				int bytesRead = input.read(buffer, 0, SERVER_BUFFER_SIZE);
				boolean flag = true;
				while(bytesRead > 0) {
					String response = new String(buffer, 0, bytesRead);
					// System.out.println("... received bytes: " + bytesRead);
					// updateStatus("... received bytes: " + bytesRead);
					if(response.startsWith("Error")) {
						System.out.println("Server reported error: " + response);
						updateStatus("Server reported error: " + response);
						flag = false;
						break;
					}
					fileWrite.write(buffer, 0, bytesRead);
					bytesRead = input.read(buffer, 0, SERVER_BUFFER_SIZE);
				}
				if(flag == true)
					System.out.println("Received file from server: " + fileName + "\nSave to " + targetDir);
					updateStatus("Received file from server: " + fileName + "\nSave to " + targetDir);

				socket.close();
				fileWrite.close();
			} catch (IOException e) {
				updateStatus("Error: " + e.getMessage());
				e.printStackTrace();
			}
		}).start();
	}

	private void updateStatus(String message) {
		SwingUtilities.invokeLater(() -> statusArea.append(message + "\n"));
	}

	public static void main(String args[]) throws IOException {
		// if(args.length != 2) {
		// 	System.out.println("Error argument. Should be 'java com.FileClient <filename> <targetname>'");
		// 	return;
		// }

		// String fileName = args[0];
		// String targetName = args[1];
		// String targetDir = System.getProperty("user.dir") + "/files/" + targetName;
		// System.out.println(targetDir);

		// downloadFile(fileName, targetDir);
		SwingUtilities.invokeLater(() -> {
			FileClient client = new FileClient();
			client.setVisible(true);
		    });
	}
}