import java.io.IOException;

import org.json.JSONException;

public class MainLoop {
    
    public static void main(String[] args) throws IOException, JSONException {
        for (int i = 0; i < 1000; i++) {
            ClientAppLoop client = new ClientAppLoop();
            client.clientIndex = i;
            client.start();
        }
    }
}