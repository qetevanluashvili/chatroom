import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ChatUser {

    private String username = "Anonymous";
    private NameChangeHandler nameChangeHandler;
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private ChatRoom chatRoom;

    public ChatUser(String address, int port) {
        this.chatRoom = new ChatRoom();
        this.nameChangeHandler = new NameChangeHandler(chatRoom);
        connectToServer(address, port);
    }

    private void connectToServer(String address, int port) {
        try {
            socket = new Socket(address, port);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            new Thread(this::receiveMessages).start();


            outputStream.writeUTF(username);

            sendMessages();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendMessages() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("Enter your message: ");
                String message = scanner.nextLine();

                if ("/exit".equals(message)) {
                    sendSpecialMessage("/exit");
                    closeSocket();
                    break;
                } else if (message.startsWith("/private ")) {
                    sendPrivateMessage(message);
                } else if (message.startsWith("/name ")) {
                    sendSpecialMessage(message);
                } else {
                    sendMessageToChat(message);
                }
            }
        }
    }

    private void closeSocket() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendPrivateMessage(String message) {
        String[] parts = message.split(" ", 3);
        String targetUsername = parts[1];
        String privateMessage = parts[2];

        ObjectOutputStream targetOutputStream = chatRoom.getConnectedClient(targetUsername);

        if (targetOutputStream != null) {
            try {
                targetOutputStream.writeObject("(Private from " + username + "): " + privateMessage);
                targetOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class NameChangeHandler {
        private ChatRoom chatRoom;

        public NameChangeHandler(ChatRoom chatRoom) {
            this.chatRoom = chatRoom;
        }

        public void handleNameChange(String oldName, String newName) {
            chatRoom.broadcast(oldName + " changed their name to " + newName);
        }
    }


    private void sendMessageToChat(String message) {
        try {
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendSpecialMessage(String message) {
        try {
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (message.startsWith("/name ")) {
            String newName = message.substring(6);

            System.out.println("nameChangeHandler: " + nameChangeHandler);
            System.out.println("this.username: " + this.username);

            this.username = newName;

            if (nameChangeHandler != null) {
                nameChangeHandler.handleNameChange(this.username, newName);
            }
        }
    }



    private void receiveMessages() {
        try {
            while (true) {
                Object receivedMessage = inputStream.readObject();
                if (receivedMessage instanceof String) {
                    System.out.println((String) receivedMessage);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Connection lost. Reconnecting...");
            connectToServer(socket.getInetAddress().getHostAddress(), socket.getPort());
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the server address: ");
        String address = scanner.nextLine();

        System.out.print("Enter the server port: ");
        int port = scanner.nextInt();
        if (port < 1024 || port > 65535) {
            System.out.println("Invalid port number. Please choose a port between 1024 and 65535.");
            return;
        }

        new ChatUser(address, port);

    }
}


