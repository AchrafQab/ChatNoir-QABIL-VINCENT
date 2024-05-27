package fr.uge.chatnoir.client;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedFileRegistry {
    private final Map<String, File> availableFiles = new HashMap<>();

    public void addFile(String fileName, File file) {
        availableFiles.put(fileName, file);
    }

    public void removeFile(String fileName) {
        availableFiles.remove(fileName);
    }

    public List<String> getFileList() {
        return new ArrayList<>(availableFiles.keySet());
    }

    public File getFile(String fileName) {
        return availableFiles.get(fileName);
    }
}
