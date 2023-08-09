package server;

import java.util.Map;
import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {
        startServer();
    }

    private static void startServer() {
        ExecutorService executorService = Executors.newSingleThreadExecutor(); // Create an executor service
        System.out.println("Server started!");
        Map<Object, Object> database = new ConcurrentHashMap<>();
        executorService.submit(new RequestHandler(database));
        executorService.shutdown(); // Shutdown the executor when the server stops
    }
}