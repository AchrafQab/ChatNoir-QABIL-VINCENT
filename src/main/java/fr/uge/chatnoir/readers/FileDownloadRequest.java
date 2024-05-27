package fr.uge.chatnoir.readers;

import java.util.Objects;

public class FileDownloadRequest {
    private final int downloadMode;
    private final String fileId;

    public FileDownloadRequest(int downloadMode, String fileId) {
        this.downloadMode = downloadMode;
        this.fileId = Objects.requireNonNull(fileId);
    }

    public int getDownloadMode() {
        return downloadMode;
    }

    public String getFileId() {
        return fileId;
    }
}
