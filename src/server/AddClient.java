package server;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class AddClient extends Thread {

    final DataInputStream dis;
    final DataOutputStream dos;
    final Socket socket;

    public AddClient(Socket socket, DataInputStream dis, DataOutputStream dos) {
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;
    }

    @Override
    public void run() {
        try {
            System.out.println("Waiting for client to send login info");
            String received = dis.readUTF();
            System.out.println("received: " + received);
            JSONObject receivedJSON = new JSONObject(received);

            if (!receivedJSON.get("command").equals("login")) {
                System.out.println("Client " + socket + " sent "
                        + receivedJSON.get("command"));
                closeConnection();
                return;
            }

            String username = receivedJSON.get("username").toString();
            String password = receivedJSON.get("password").toString();
            boolean ok = Server.authenticate(username, password);

            JSONObject response = new JSONObject();
            response.put("username", "server");

            if (ok) {
                response.put("command", "ok");
            } else {
                response.put("command", "error");
                response.put("message", "Invalid username or password");
            }

            dos.writeUTF(response.toString());
            dos.flush();

            if (!ok) {
                closeConnection();
                return;
            }

            System.out.println("Creating client...");

            // This doesn't take into account the coordinates a client may have
            // if it moved before the other clients were created. Therefore, it 
            // is necessary to define how the initial coordinates will be 
            // assigned to a client, as well as whether the server will keep a 
            // record of each client's coordinates.
            
            JSONObject message = new JSONObject();
            message.put("username", username);
            message.put("command", "create");
            message.put("class", "RemotePlayer");

            ClientManager client = new ClientManager(
                    socket.getInetAddress(), socket.getPort(),
                    username, socket, dis, dos);

            for (ClientManager c : Server.connectedClients) {
                JSONObject cMsg = new JSONObject();
                cMsg.put("username", c.username);
                cMsg.put("command", "create");
                cMsg.put("class", "RemotePlayer");

                if (!c.username.equals(client.username)) {
                    client.queue.add(cMsg);
                }
            }

            Server.connectedClients.add(client);

            Server.queue.add(message);

            client.start();

            System.out.println("Client running...");

        } catch (Exception e) {
            System.out.println("Error: " + e);
            closeConnection();
        }
    }

    private void closeConnection() {
        try {
            System.out.println("Closing the connection.");

            dis.close();
            dos.close();
            socket.close();

            System.out.println("Connection closed.");
        } catch (IOException e) {
            System.err.println("Could not close connection.");
        }
    }
}
