import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class MessageBlock {

    public String hash;
    public String previousHash;
    private String message; // a simple message.
    private long timeStamp;
    private int nonce;
    private int difficulty;

    public MessageBlock(String message, String previousHash, int difficulty) {
        this.message = message;
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();
        this.difficulty = difficulty;
    }

    public String calculateHash() {
        String calculatedhash = StringUtil.applySha256(previousHash + Long.toString(timeStamp) + Integer.toString(nonce)
                + message + Integer.toString(difficulty));
        return calculatedhash;
    }

    public void doProofOfWork() {
        long startTime = System.currentTimeMillis();
        String target = new String(new char[difficulty]).replace('\0', '0'); // Create a string with difficulty * "0"
        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = calculateHash();
        }
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Elapsed time to do this work: " + elapsedTime + "ms , hash: " + hash);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("hash", hash);
        obj.put("previousHash", previousHash);
        obj.put("message", message);
        obj.put("timeStamp", timeStamp);
        obj.put("nonce", nonce);
        obj.put("difficulty", difficulty);
        return obj;
    }
}