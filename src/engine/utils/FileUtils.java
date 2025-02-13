package engine.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileUtils {
    /**
     * Reads the entire content of the file at the given path and returns it as a String.
     *
     * @param path the file path to read from
     * @return the file content as a String, or an empty string if an error occurs
     */
    public static String loadFileAsString(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            System.err.println("Error reading file: " + path);
            e.printStackTrace();
            return "";
        }
    }
}
