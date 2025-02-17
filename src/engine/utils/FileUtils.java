package engine.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * The {@code FileUtils} class provides utility methods for file operations,
 * including loading file content as a string and locating resource files.
 */
public class FileUtils {
    /** The root directory for resources. */
    public static String resourcesPath = "res";
    
    /**
     * Searches for a file with the given name in the resources directory.
     *
     * @param fileName the name of the file to search for.
     * @return the absolute path to the file if found; otherwise, {@code null}.
     */
    public static String load(String fileName) {
        File root = new File(resourcesPath);
        return searchFile(root, fileName);
    }
    
    /**
     * Returns the absolute path to the resources directory.
     *
     * @return the absolute resources path.
     */
    public static String getResPath() {
        return new File(resourcesPath).getAbsolutePath();
    }
    
    /**
     * Recursively searches for a file with the given name within a directory.
     *
     * @param dir      the directory to search in.
     * @param fileName the name of the file to search for.
     * @return the absolute path to the file if found; otherwise, {@code null}.
     */
    private static String searchFile(File dir, String fileName) {
        if (!dir.exists() || !dir.isDirectory()) return null;
        
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                String found = searchFile(file, fileName);
                if (found != null) return found; // Return the first match found.
            } else if (file.getName().equals(fileName)) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }
    
    /**
     * Reads the entire content of the file at the given path and returns it as a string.
     *
     * @param path the file path.
     * @return the file content as a string, or an empty string if an error occurs.
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
