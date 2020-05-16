
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Security;
import java.util.HashSet;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import com.sun.net.ssl.internal.ssl.Provider;

public class ServerApp {

	private final int PORT = 9001;
	private final String KEY_STORE_PATH = "./keys/myKeyStore_4096.jks";
	private final String KEY_STORE_PASSWORD = "1234567";
	private HashSet<String> names = new HashSet<String>();
	private HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
	private SSLServerSocket sslServerSocket;

	public static void main(String[] args) throws IOException {
		ServerApp serverApp = new ServerApp();
		serverApp.initSSLServer();
		serverApp.runSSLServer();
	}

	/**
	 * @description
	 * @throws IOException
	 */
	private void initSSLServer() throws IOException {
		Security.addProvider(new Provider());
		System.setProperty("javax.net.ssl.keyStore", KEY_STORE_PATH);
		System.setProperty("javax.net.ssl.keyStorePassword", KEY_STORE_PASSWORD);
		// System.setProperty("javax.net.debug", "all");
		SSLServerSocketFactory sslServerSocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
		sslServerSocket = (SSLServerSocket) sslServerSocketfactory.createServerSocket(PORT);
		System.out.println("Echo Server Started & Ready to accept Client Connection");
	}

	/**
	 * @description
	 * @throws IOException
	 */
	private void runSSLServer() throws IOException {
		try {
			while (true) {
				new SocketHandler(sslServerSocket.accept(), names, writers).start();
			}
		} finally {
			sslServerSocket.close();
		}
	}
}
