import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.sun.net.ssl.internal.ssl.Provider;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ClientApp {

    private int SERVER_PORT = 9001;
    private String SERVER_ADDRESS = "192.168.100.8";
    private String TRUST_STORE_PATH = "./keys/myTrustStore_4096.jts";
    private String TRUST_STORE_PASSWORD = "1234567";
    private BufferedReader in;
    private PrintWriter out;
    private SSLSocket sslSocket;
    public static ArrayList<MessageBlock> messageBlockchain = new ArrayList<MessageBlock>();
    JFrame frame;
    JTextField textField;
    JTextArea messageArea;
    public int currentDifficulty = 0;

    public static void main(String[] args) throws IOException, JSONException {

        ClientApp client = new ClientApp();
        client.initChatUI();
        client.initSSLSocket();
        client.initSocketStream();
        client.startRequestHandler();

    }

    public void initSSLSocket() throws UnknownHostException, IOException {
        Security.addProvider(new Provider());
        System.setProperty("javax.net.ssl.trustStore", TRUST_STORE_PATH);
        System.setProperty("javax.net.ssl.trustStorePassword", TRUST_STORE_PASSWORD);
        SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        sslSocket = (SSLSocket) sslsocketfactory.createSocket(SERVER_ADDRESS, SERVER_PORT);
    }

    public void initSocketStream() throws IOException {
        in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
        out = new PrintWriter(sslSocket.getOutputStream(), true);
    }

    public void initChatUI() {
        frame = new JFrame("TASI SSL Chat");
        textField = new JTextField(40);
        messageArea = new JTextArea(20, 40);
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        textField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                MessageBlock messageBlock = new MessageBlock(textField.getText(),
                        messageBlockchain.get(messageBlockchain.size() - 1).hash, currentDifficulty);
                messageBlock.doProofOfWork();
                messageBlockchain.add(messageBlock);
                try {
                    out.println(messageBlock.toJSON());
                    textField.setText("");

                } catch (JSONException err) {
                    err.printStackTrace();
                }
            }
        });
    }

    private String getName() {
        return JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    public void startRequestHandler() throws IOException, JSONException {
        while (true) {
            String line = in.readLine();
            System.out.println(line);
            JSONTokener tokener = new JSONTokener(line);
            JSONObject req = new JSONObject(tokener);
            String data = (String) req.get("data");
            String action = (String) req.get("action");
            if (action.equals("SET_DIFFICULTY")) {
                currentDifficulty = Integer.parseInt(data);
                System.out.println("currentDifficulty: " + currentDifficulty);
            } else if (action.equals("SUBMIT_NAME")) {
                String name = getName();
                MessageBlock genesisBlock = new MessageBlock(name,
                        StringUtil.applySha256(Long.toString(new Date().getTime()) + name), currentDifficulty);
                genesisBlock.doProofOfWork();
                messageBlockchain.add(genesisBlock);
                out.println(genesisBlock.toJSON());
            } else if (action.equals("NAME_ACCEPTED")) {
                textField.setEditable(true);
            } else if (action.equals("BROADCAST_MESSAGE")) {
                messageArea.append(data + "\n");
            }
        }
    }

}