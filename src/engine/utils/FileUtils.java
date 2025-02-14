package engine.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class FileUtils
{
    public static String resourcesPath = "res";
    
    
    public static String load(String fileName) {
        File root = new File(resourcesPath);
        return searchFile(root, fileName);
    }

    public static String getResPath() {
        return new File(resourcesPath).getAbsolutePath();
    }


    private static String searchFile(File dir, String fileName) {
        if (!dir.exists() || !dir.isDirectory()) return null;
        
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                String found = searchFile(file, fileName);
                if (found != null) return found; // Return the first match found
            } else if (file.getName().equals(fileName)) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }
    
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
