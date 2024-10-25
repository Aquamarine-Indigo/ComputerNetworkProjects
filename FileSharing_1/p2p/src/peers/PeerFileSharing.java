package p2p.src.peers;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.concurrent.Executors;
// import p2p.src.peers.ServerStatusListener;

public class PeerFileSharing {

	private static final int BUFFER_SIZE = 4096;

	private int serverPort;
	private String sharedDirectory;
	private String localAddress;
	private ServerSocket serverSocket;

	private volatile boolean isRunning;

	// private String getFileSaveDir(String fileName) {
	// 	return sharedDirectory + "/" + fileName;
	// }

	public PeerFileSharing(int serverPort, String sharedDirectory) {
		this.serverPort = serverPort;
		this.sharedDirectory = sharedDirectory;
		this.isRunning = true;
	}

	public final String getServerAddress() {
		return localAddress;
	}

	public void setSharedDirectory(String dir) {
		sharedDirectory = dir;
	}

	private String getNetworkIpAddress() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				if (iface.isLoopback() || !iface.isUp())
					continue;
				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					// if (addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isAnyLocalAddress())
					// 	continue;
					// return addr.getHostAddress();if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
					if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
						String ipAddress = addr.getHostAddress();
						// Return the first found address (usually will be the right one)
						return ipAddress;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void startServer(ServerStatusListener ssl) {
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				serverSocket = new ServerSocket(serverPort, 50, InetAddress.getByName("0.0.0.0"));
				// localAddress = InetAddress.getLocalHost().getHostAddress();
				localAddress = getNetworkIpAddress();
				System.out.println("Peer Server: started on Port " + serverPort);
				ssl.onStatusUpdate("Server started with IP: " + localAddress + ", Port: " + Integer.toString(serverPort));
				while(isRunning) {
					try {
						Socket clientSocket = serverSocket.accept();
						if(!isRunning)
							break;
						Thread clientHandleThread = new Thread(new ClientHandler(clientSocket, sharedDirectory, ssl));
						clientHandleThread.start();
					} catch(IOException e) {
						if(isRunning)
							e.printStackTrace();
					}
				}

			} catch (IOException e) {
				ssl.onError("Server start error: " + e.getMessage());
				e.printStackTrace();
			} finally {
				stopServer(ssl);
			}
		});
	}

	public void stopServer(ServerStatusListener ssl) {
		isRunning = false;
		if(serverSocket != null && !serverSocket.isClosed()) {
			try {
				serverSocket.close();
				System.out.println("Server Stopped.");
				ssl.onStatusUpdate("Server stopped.");
			} catch (IOException e) {
				ssl.onError("Server stopping error: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	// Send a file
	public void requestFile(String peerAddr, int peerPort, String fileName, 
				ServerStatusListener ssl) {
		try {
			File fileStore = new File(fileName);
			if(!fileStore.exists()) {
				System.out.println("File not exist");
				ssl.onError("File not exist.");
				return;
			}
			Socket socket = new Socket(peerAddr, peerPort);
			InputStream dataInput = socket.getInputStream();
			OutputStream dataOutput = socket.getOutputStream();

			Path path = Paths.get(fileName);
			String fileSaveName = path.getFileName().toString();
			dataOutput.write(("request " + fileSaveName).getBytes());
			dataOutput.flush();

			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = dataInput.read(buffer, 0, BUFFER_SIZE);
			if(bytesRead <= 0) {
				System.out.println("Error request.");
				ssl.onError("Error request.");
				socket.close();
				return;
			}
			String response = new String(buffer, 0, bytesRead);
			if(response.startsWith("error")) {
				System.out.println("Error request: " + response);
				ssl.onError("Error request: " + response);
				socket.close();
				return;
			}
			ssl.onStatusUpdate("Request succeed.");

			try {
				FileInputStream fis = new FileInputStream(fileStore);
				// byte[] buffer = new byte[BUFFER_SIZE];
				bytesRead = fis.read(buffer);
				while(bytesRead > 0) {
					dataOutput.write(buffer, 0, bytesRead);
					dataOutput.flush();
					bytesRead = fis.read(buffer);
				}
				dataOutput.close();
				System.out.println("File sent.");
				ssl.onStatusUpdate("File sent.");
				fis.close();
			} catch (IOException e) {
				System.out.println("Error sending file");
				e.printStackTrace();
			}

			socket.close();
		} catch (IOException e) {
			System.out.println("Request socket error");
			ssl.onError("Request socket error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// Client handler receives a file
	private static class ClientHandler implements Runnable {
		private Socket clientSocket;
		private String sharedDirectory;
		private ServerStatusListener ssl;

		public ClientHandler(Socket clienSocket, String sharedDirectory, ServerStatusListener ssl) {
			this.clientSocket = clienSocket;
			this.sharedDirectory = sharedDirectory;
			this.ssl = ssl;
		}

		private String getFileSaveDir(String fileName) {
			return sharedDirectory + "/" + fileName;
		}

		@Override
		public void run() {
			try {
				InputStream dataInput = clientSocket.getInputStream();
				OutputStream dataOutput = clientSocket.getOutputStream();
				byte[] buffer = new byte[BUFFER_SIZE];
				int bytesRead = dataInput.read(buffer, 0, BUFFER_SIZE);
				String request = new String(buffer, 0, bytesRead);
				if(request.startsWith("request")) {
					try {
						dataOutput.write("accepted request".getBytes());
						dataOutput.flush();
						String fileName = request.substring(8);
						FileOutputStream fileOut = new FileOutputStream(getFileSaveDir(fileName));
						// buffer = new byte[BUFFER_SIZE];
						bytesRead = dataInput.read(buffer, 0, BUFFER_SIZE);
						int totalRead = bytesRead;
						while(bytesRead > 0) {
							fileOut.write(buffer, 0, bytesRead);
							bytesRead = dataInput.read(buffer, 0, BUFFER_SIZE);
							System.out.println(bytesRead);
							totalRead += bytesRead;
						}
						System.out.println("Total " + Integer.toString(totalRead) + " bytes read.");
						ssl.onStatusUpdate("Total " + Integer.toString(totalRead) + " bytes read.");;
						fileOut.close();
					} catch (IOException e) {
						System.out.println("Saving file error");
						ssl.onError("Saving file error: " + e.getMessage());
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
