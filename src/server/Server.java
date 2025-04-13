/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package server;

import java.io.*;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue; 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
/**
 *
 * @author uriel
 */
public class Server {
    // Messages queue that stores the messages from all clients.
    public static ConcurrentLinkedQueue<JSONObject> queue
            = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<ClientManager> connectedClients
            = new ConcurrentLinkedQueue<>(); //cambiar a contraseña de la base
    public static JSONObject config = null;
    private static final String configFileName = "env/config.json";

    public static void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC Driver loaded!");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el driver de MySQL." + e);
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Server starting...");

        System.out.println("Reading config file...");

        try (FileReader reader = new FileReader(configFileName)) {
            JSONTokener tokenizer = new JSONTokener(reader);
            config = new JSONObject(tokenizer);

            String mapPath = config.getString("map");

            System.out.println("Reading map...");
            FileReader mapReader = new FileReader(mapPath);
            BufferedReader br = new BufferedReader(mapReader);
            StringBuilder map = new StringBuilder();
            while (br.readLine() != null) {
                map.append(br.readLine());
            }
            config.put("map", map.toString());
        } catch (IOException e) {
            System.err.println("Cannot read config file. " + e.getMessage());
            return;
        }
        System.out.println("Server ready!");

        Thread multicastThread = new Thread(() -> {
            try {
                multicast();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        multicastThread.start();

        try {
            ServerSocket serverSocket = new ServerSocket(2555);
            System.out.println("ServerSocket: " + serverSocket);
            while (true) {
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                    
                    System.out.println("A new Client is connected: " + socket);

                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    System.out.println("Assigning new Thread for this Client.");

                    Thread t = new AddClient(socket, dis, dos);
                    t.start();
                } catch (Exception e) {
                    if (socket != null)
                        socket.close();
                    System.out.println("Server Error: " + e);
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Server Error: " + e);
        }

    }

    public static String authenticate(String username, String password) {
        loadDriver();
        String msg;

        try (Connection conn = DriverManager.getConnection(
                config.getString("dbUrl"),
                config.getString("dbUser"),
                config.getString("dbPassword"))) {
            String query = "SELECT * FROM usuario WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password); 
            System.out.println("Trying: " + username + " - " + password);  // DEBUG
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                msg = "ok";
            } else {
                msg = "No user found: Wrong username or password.";
            }
        } catch (Exception e) {
            msg = "Database Error: " + e;
        }

        System.out.println(msg);
        return msg;
    }

    public static void multicast() throws InterruptedException {
        for (;;) {
            Thread.sleep(50);

            JSONObject message = queue.poll();

            if (message == null) {
                continue;
            }

            String sender = message.getString("username");

            for (ClientManager client : connectedClients) {
                if (!client.username.equals(sender)) {
                    client.queue.add(message);
                }
            }
        }
    }
}

