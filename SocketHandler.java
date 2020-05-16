import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import org.json.*;

public class SocketHandler extends Thread {
    private String name;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private HashSet<String> names;
    private HashSet<PrintWriter> writers;
    public static ArrayList<MessageBlock> messageBlockchain = new ArrayList<MessageBlock>();
    private Integer counterWrongMessages = 0;
    private static int DEFAULT_DIFFICULTY = 1;

    public SocketHandler(Socket socket, HashSet<String> names, HashSet<PrintWriter> writers) {
        this.socket = socket;
        this.names = names;
        this.writers = writers;
    }

    public void run() {
        try {

            initSocketStream();
            requestNameToClient();
            broadcastReceivedMessage();
        } catch (IOException | JSONException e) {
            System.out.println(e);
        } finally {

            if (name != null) {
                names.remove(name);
            }
            if (out != null) {
                writers.remove(out);
            }
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    public void initSocketStream() throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void requestNameToClient() throws JSONException, IOException {
        while (true) {
            if (this.counterWrongMessages == 3) {
                writeToLogFile(logData("wrong message limit exceeded", ""));
                socket.close();
            }
            writeToLogFile(logData("SUBMIT_NAME", ""));

            // set difficulty of proof work method
            out.println(req("SET_DIFFICULTY", Integer.toString((DEFAULT_DIFFICULTY + this.counterWrongMessages))));
            // request client a name
            out.println(req("SUBMIT_NAME", ""));

            writeToLogFile(logData("PENDING_NAME", ""));

            String input = in.readLine();
            JSONTokener tokener = new JSONTokener(input);
            JSONObject reqObj = new JSONObject(tokener);

            if (isMessageValid(reqObj)) {
                name = (String) reqObj.get("message");
                writeToLogFile(logData("MessageBlock", name + " -> " + input));

                if (name == null || name.isEmpty()) {
                    return;
                }

                writeToLogFile(logData("NAME_RECEIVED ", name));
                synchronized (names) {
                    if (!names.contains(name)) {
                        names.add(name);
                        break;
                    }
                }
            } else {
                writeToLogFile(logData("INVALID_MESSAGE_RECEIVED", input));
                this.counterWrongMessages++;
            }
        }

        writeToLogFile(logData("NAME_ACCEPTED", ""));
        // send confirmation to client
        out.println(req("NAME_ACCEPTED", ""));
        // add new client into clients array
        writers.add(out);
    }

    public void broadcastReceivedMessage() throws JSONException, IOException {
        while (true) {
            if (this.counterWrongMessages == 3) {
                writeToLogFile(logData("wrong message limit exceeded", ""));
                socket.close();
            }
            String input = in.readLine();
            JSONTokener tokener = new JSONTokener(input);
            JSONObject reqObj = new JSONObject(tokener);
            if (isMessageValid(reqObj)) {

                String message = (String) reqObj.get("message");
                if (input == null) {
                    return;
                }
                // messageArea.append("Message from " + name + " --- "+ input + "\n");
                writeToLogFile(logData("MessageBlock", name + " -> " + input));
                // writeToLogFile(logData("INFO", "Message from " + name + " " + message));

                for (PrintWriter writer : writers) {
                    writer.println(req("BROADCAST_MESSAGE", name + ": " + message));
                }
            } else {
                writeToLogFile(logData("INVALID_MESSAGE_RECEIVED", input));
                this.counterWrongMessages++;
            }
        }
    }

    String logData(String action, String data) {
        String str = "";
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
        String strDate = dateFormat.format(date);
        str += strDate + ", ";
        str += "Action: " + action + ", ";
        str += "Data: " + data + ", ";
        str += "ThreadId: " + Long.toString(this.getId()) + ", ";
        str += "Socket: " + socket.hashCode() + "\n";
        return str;
    }

    JSONObject req(String action, String data) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("action", action);
        obj.put("data", data);
        return obj;
    }

    public void writeToLogFile(String data) throws IOException {
        FileWriter fr = new FileWriter(new File("./logs/log.log"), true);
        fr.write(data);
        fr.close();
    }

    public Boolean isMessageValid(JSONObject req) throws JSONException {

        String hash = (String) req.get("hash");
        String previousHash = (String) req.get("previousHash");
        String message = (String) req.get("message");
        Long timeStamp = (Long) req.get("timeStamp");
        Integer nonce = (Integer) req.get("nonce");
        Integer difficulty = (Integer) req.get("difficulty");
        System.out.println(hash);
        System.out.println(StringUtil.applySha256(previousHash + Long.toString(timeStamp) + Integer.toString(nonce)
                + message + Integer.toString(difficulty)));
        return hash.equals(StringUtil.applySha256(previousHash + Long.toString(timeStamp) + Integer.toString(nonce)
                + message + Integer.toString(difficulty)));
    }

    public static Boolean isChainValid() {
        MessageBlock currentBlock;
        MessageBlock previousBlock;

        // loop through blockchain to check hashes:
        for (int i = 1; i < messageBlockchain.size(); i++) {
            currentBlock = messageBlockchain.get(i);
            previousBlock = messageBlockchain.get(i - 1);
            // compare registered hash and calculated hash:
            if (!currentBlock.hash.equals(currentBlock.calculateHash())) {
                System.out.println("Current Hashes not equal");
                return false;
            }
            // compare previous hash and registered previous hash
            if (!previousBlock.hash.equals(currentBlock.previousHash)) {
                System.out.println("Previous Hashes not equal");
                return false;
            }
        }
        return true;
    }
}