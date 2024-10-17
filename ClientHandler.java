import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;
    private HashMap<String, String> userDatabase;

    public ClientHandler(Socket socket, HashMap<String, String> userDatabase) {
        try {
            this.socket = socket;
            this.userDatabase = userDatabase;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Authenticate the user
            authenticateUser();

        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;
        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                broadcastMessage(messageFromClient);
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    public void authenticateUser() throws IOException {
        String option;
        do {
            bufferedWriter.write("Welcome! Do you want to (1) Login or (2) Sign up? Type 1 or 2:");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            option = bufferedReader.readLine();

            if (option.equals("1")) {
                loginUser();
            } else if (option.equals("2")) {
                signupUser();
            } else {
                bufferedWriter.write("Invalid option, please type 1 or 2.");
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        } while (!option.equals("1") && !option.equals("2"));
    }

    public void loginUser() throws IOException {
        bufferedWriter.write("Enter your username:");
        bufferedWriter.newLine();
        bufferedWriter.flush();
        String username = bufferedReader.readLine();

        if (userDatabase.containsKey(username)) {
            bufferedWriter.write("Enter your password:");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            String password = bufferedReader.readLine();

            if (userDatabase.get(username).equals(password)) {
                clientUsername = username;
                clientHandlers.add(this);
                broadcastMessage("SERVER: " + clientUsername + " has entered the chat!");
            } else {
                bufferedWriter.write("Incorrect password. Try again.");
                bufferedWriter.newLine();
                bufferedWriter.flush();
                loginUser(); // Recursive call to retry
            }
        } else {
            bufferedWriter.write("Username does not exist. Try again.");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            authenticateUser(); // Start over
        }
    }

    public void signupUser() throws IOException {
        bufferedWriter.write("Enter a new username:");
        bufferedWriter.newLine();
        bufferedWriter.flush();
        String username = bufferedReader.readLine();

        if (!userDatabase.containsKey(username)) {
            bufferedWriter.write("Enter a new password:");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            String password = bufferedReader.readLine();

            // Save new username and password in the database
            userDatabase.put(username, password);
            clientUsername = username;
            clientHandlers.add(this);
            broadcastMessage("SERVER: " + clientUsername + " has entered the chat!");

        } else {
            bufferedWriter.write("Username already exists. Please try logging in.");
            bufferedWriter.newLine();
            bufferedWriter.flush();
            authenticateUser(); // Ask user to log in or sign up again
        }
    }

    public void broadcastMessage(String messageToSend) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    clientHandler.bufferedWriter.write(clientUsername+": "+messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage("SERVER: " + clientUsername + " has left the chat!");
    }
}
