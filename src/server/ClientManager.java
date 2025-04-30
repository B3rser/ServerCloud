package server;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.net.InetAddress;

public class ClientManager extends Thread {

    // Maximum number of messages in the queue before the client is
    // considered unresponsive and the connection is closed
    private static final int MAX_QUEUED_MESSAGES = 1000;

    private int CLIENT_STATUS;

    // IP address and port of the client
    // these are not being used right now, but might be used in the future.
    public InetAddress ip;
    public int port;
    public String username;
    // queue that stores the messages
    public ConcurrentLinkedQueue<JSONObject> queue
            = new ConcurrentLinkedQueue<>();
    // variables required for the communication
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    // threads for communication
    private Thread senderThread;
    private Thread receiverThread;

    public ClientManager(InetAddress ip, int port, String username,
            Socket socket, DataInputStream dis,
            DataOutputStream dos) throws IOException {
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;
    }

    /**
     * Client manager main method.
     *
     * Creates two threads, one for sending and one for receiving. If either of
     * the two threads are interrupted, the client is completely disconnected.
     *
     */
    @Override
    public void run() {
        this.senderThread = new Thread(() -> {
            try {
                sender();
            } catch (IOException e) {
                closeConnection();
            }
        });
        this.receiverThread = new Thread(() -> {
            try {
                receiver();
            } catch (IOException e) {
                closeConnection();
            }
        });
        this.senderThread.start();
        this.receiverThread.start();
    }

    /**
     * Receiver threads Receives messages and puts them in the the server
     * messages queue.
     *
     */
    public void receiver() throws IOException {
        for (;;) {
            try {
                JSONObject received = new JSONObject(this.dis.readUTF());
                Server.queue.add(received);
                System.out.println("Server received:\n" + received);
            } catch (JSONException e) {
                System.err.println("Ignoring message because it has invalid "
                        + "JSON: " + e);
            }
        }
    }

    /**
     * Sender threads Picks messages from the client queue and sends them
     * through the socket If the client queue gets too long, it disconnects the
     * client.
     *
     */
    public void sender() throws IOException {
        for (;;) {
            /*try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                System.err.println(e);
            }*/

            JSONObject message = this.queue.poll();

            if (message == null) {
                continue;
            }

            if (this.queue.size() >= MAX_QUEUED_MESSAGES) {
                throw new IOException("Client queue became too big");
            }

            System.out.println("Sending message\n" + message);
            this.dos.writeUTF(message.toString());
        }
    }

    /**
     * Closes the data streams and the socket causing exceptions in all other
     * threads which indicates them to stop.
     */
    public void closeConnection() {
        try {
            this.dis.close();
        } catch (IOException e) {
            System.err.println("Cannot close DataInputStream gracefully");
        }
        try {
            this.dos.close();
        } catch (IOException e) {
            System.err.println("Cannot close DataOutputStream gracefully");
        }
        try {
            this.socket.close();
        } catch (IOException e) {
            System.err.println("Cannot close socket gracefully");
        }

        Server.connectedClients.remove(this);

        this.senderThread.interrupt();
        this.receiverThread.interrupt();
        this.interrupt();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClientManager that)) {
            return false;
        }
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }
}
