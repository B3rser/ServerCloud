package server;

import org.json.JSONObject;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.net.InetAddress;


public class ClientManager {
    public InetAddress ip;
    public int port;
    public String username;
    public ConcurrentLinkedQueue<JSONObject> queue = new ConcurrentLinkedQueue<>();

    public ClientManager(InetAddress ip, int port, String username) {
        this.ip = ip;
        this.port = port;
        this.username = username;
    }

    public void sender() throws InterruptedException {
        for (;;) {
            Thread.sleep(50);

            JSONObject message = this.queue.poll();

            if (message == null)
                continue;

            // mandar al socket 👍
        }
    }
}
