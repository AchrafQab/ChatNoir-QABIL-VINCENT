package fr.uge.chatnoir.readers;

public class FileShare {
    private final String fileId;
    private final int fileSize;

    public FileShare(String fileId, int fileSize) {
        this.fileId = fileId;
        this.fileSize = fileSize;
    }

    public String getFileId() {
        return fileId;
    }

    public int getFileSize() {
        return fileSize;
    }
}
