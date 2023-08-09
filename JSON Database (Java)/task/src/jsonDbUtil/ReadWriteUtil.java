package jsonDbUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteUtil {

    static final ReadWriteLock LOCK = new ReentrantReadWriteLock();

    public static String readFromFile(String filename) {
        Lock readLock = LOCK.readLock();
        readLock.lock();
        StringBuilder inputFromFile = new StringBuilder();
        try {
            File file = new File(filename);
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                inputFromFile.append("\n" + line);
            }

            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("The file was not found.");
            e.printStackTrace();
        } finally {
            readLock.unlock();
        }
        return inputFromFile.toString();
    }

    public static void writeToFile(String filename, String json) {
        Lock writeLock = LOCK.writeLock();
        writeLock.lock();
        try (FileWriter fileWriter = new FileWriter(filename)) {
            fileWriter.write(json);
            // Flush and close the FileWriter to ensure data is written to the file
            fileWriter.flush();
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file.");
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
    }
}