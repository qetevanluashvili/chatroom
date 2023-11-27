import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.System.in;
import static java.lang.System.out;

public class ChatRoom {
    private ServerSocket serverSocket;
    private ConcurrentHashMap<String, ObjectOutputStream> connectedClients = new ConcurrentHashMap<>();
    private final int port = 11;


    public ChatRoom() {
        try {
            serverSocket = new ServerSocket(port);
            out.println("ChatRoom server listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void broadcast(String message) {
        for (ObjectOutputStream clientStream : connectedClients.values()) {
            try {
                clientStream.writeObject(message);
                clientStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleNameChange(String oldName, String newName) {
        broadcast(oldName + " changed their name to " + newName);
    }

    private void handleClient(Socket clientSocket) {
        try {
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            
            String username = in.readUTF();

            out.writeUTF("Welcome to the chat, " + username + "!");
            out.flush();

            connectedClients.put(username, out);

            broadcast(username + " joined the chat. Total members: " + connectedClients.size());

            new Thread(() -> handleClientCommunication(username, in)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void handleClientCommunication(String username, ObjectInputStream in) {
        try {
            while (true) {
                Object receivedObject = in.readObject();
                if (receivedObject instanceof String) {
                    String message = (String) receivedObject;
                    processMessage(username, message);
                }
            }
        } catch (IOException | ClassNotFoundException e) {

            handleClientDisconnect(username);
        }
    }
    public ObjectOutputStream getConnectedClient(String username) {
        return connectedClients.get(username);
    }
    private void processMessage(String sender, String message) {
        if (message.startsWith("/private ")) {
            sendPrivateMessage(sender, message);
        } else if (message.equals("/exit")) {
            handleClientDisconnect(sender);
        } else {
            broadcast(sender + ": " + message);
        }
    }

    private void sendPrivateMessage(String sender, String message) {
        String[] parts = message.split(" ", 3);
        String targetUsername = parts[1];
        String privateMessage = parts[2];

        ObjectOutputStream targetOutputStream = connectedClients.get(targetUsername);

        if (targetOutputStream != null) {
            try {
                targetOutputStream.writeObject("(Private from " + sender + "): " + privateMessage);
                targetOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void handleClientDisconnect(String username) {
        connectedClients.remove(username);
        broadcast(username + " left the chat. Total members: " + connectedClients.size());


    }

    public static void main(String[] args) {
        ChatRoom chatRoom = new ChatRoom();
    }


}
