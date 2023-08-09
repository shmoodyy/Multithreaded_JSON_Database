package client;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jsonDbUtil.ReadWriteUtil;

public class Main {

//    final String REQUEST_PATH = System.getProperty("user.dir") + "/JSON Database (Java)/task/src/client/data/"; for local
    final String REQUEST_PATH = System.getProperty("user.dir") + "/src/client/data/"; // for testing
    final ReadWriteLock LOCK = new ReentrantReadWriteLock();
    private final String SERVER_ADDRESS = "127.0.0.1";
    private final int SERVER_PORT = 58543;
    final Gson GSON = new GsonBuilder().create();

    @Parameter(names={"-t"}, description = "Type of the request")
    String type;
    @Parameter(names={"-k"}, description = "Index of the cell")
    String key;
    @Parameter(names="-v", description = "Value to save in the database")
    String value;
    @Parameter(names = "-in", description = "File to read a request from")
    String filename;

    public static void main(String... argv) {
        Main main = new Main();
        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(argv);
        main.run();
    }

    public void run() {
        startClient(type, key, value, filename);
    }

    private void startClient(String type, String key, String value, String filename) {
        System.out.println("Client started!");
        try (
                Socket clientSocket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                DataInputStream input = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
        ) {
            ClientRequest clientRequest = new ClientRequest(type, key, value);
            String sentMsg = filename == null ? "\n" +
                    GSON.toJson(clientRequest) : ReadWriteUtil.readFromFile(REQUEST_PATH + filename);
            output.writeUTF(sentMsg);
            System.out.println("Sent: " + sentMsg);
            String receivedMsg = input.readUTF();
            System.out.println("Received: " + receivedMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}