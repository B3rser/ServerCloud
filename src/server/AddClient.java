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
            String loginMsg = Server.authenticate(username, password);

            JSONObject response = new JSONObject();
            response.put("username", "server");

            if (loginMsg.equals("ok")) {
                response.put("command", "ok");
                String [] keys = {"mapWidth", "mapHeight", "screenWidth",
                    "screenHeight", "playerSpeed", "projectileSpeed", "map"};
                for (String key : keys) {
                    response.put(key, Server.config.getString(key));
                }

                // TODO hacer una mejor manera de decidir los spawns
                response.put("spawnX",
                        (int) (Math.random() * Server.config.getDouble("mapWidthPx")));
                response.put("spawnY",
                        (int) (Math.random() * Server.config.getDouble("mapWidthPY")));

            } else {
                response.put("command", "error");
                response.put("message", loginMsg);
            }

            dos.writeUTF(response.toString());
            dos.flush();

            if (!loginMsg.equals("ok")) {
                closeConnection();
                return;
            }

            System.out.println("Creating client...");
            ClientManager client = new ClientManager(
                    socket.getInetAddress(),socket.getPort(),
                    username,socket, dis, dos);
            Server.connectedClients.add(client);
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
