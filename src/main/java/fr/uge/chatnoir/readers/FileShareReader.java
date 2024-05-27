package fr.uge.chatnoir.readers;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FileShareReader implements Reader<List<FileShare>> {
    private enum State { DONE, WAITING_FILE_COUNT, WAITING_FILE_ID_LENGTH, WAITING_FILE_ID, WAITING_FILE_SIZE, ERROR }

    private State state = State.WAITING_FILE_COUNT;
    private final IntReader intReader = new IntReader();
    private final StringReader stringReader = new StringReader();
    private int fileCount;
    private String fileId;
    private int fileSize;
    private final List<FileShare> fileShares = new ArrayList<>();

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        switch (state) {
            case WAITING_FILE_COUNT:
                if (intReader.process(buffer) == ProcessStatus.DONE) {
                    fileCount = intReader.get();
                    if (fileCount <= 0) {
                        state = State.ERROR;
                        return ProcessStatus.ERROR;
                    }
                    intReader.reset();
                    state = State.WAITING_FILE_ID_LENGTH;
                } else {
                    return ProcessStatus.REFILL;
                }
                break;

            case WAITING_FILE_ID_LENGTH:
                if (intReader.process(buffer) == ProcessStatus.DONE) {
                    int fileIdLength = intReader.get();
                    if (fileIdLength <= 0 || fileIdLength > 1024) {
                        state = State.ERROR;
                        return ProcessStatus.ERROR;
                    }
                    stringReader.reset();
                    state = State.WAITING_FILE_ID;
                } else {
                    return ProcessStatus.REFILL;
                }
                break;

            case WAITING_FILE_ID:
                if (stringReader.process(buffer) == ProcessStatus.DONE) {
                    fileId = stringReader.get();
                    intReader.reset();
                    state = State.WAITING_FILE_SIZE;
                } else {
                    return ProcessStatus.REFILL;
                }
                break;

            case WAITING_FILE_SIZE:
                if (intReader.process(buffer) == ProcessStatus.DONE) {
                    fileSize = intReader.get();
                    if (fileSize < 0) {
                        state = State.ERROR;
                        return ProcessStatus.ERROR;
                    }
                    fileShares.add(new FileShare(fileId, fileSize));
                    fileCount--;
                    if (fileCount > 0) {
                        state = State.WAITING_FILE_ID_LENGTH;
                    } else {
                        state = State.DONE;
                    }
                    intReader.reset();
                } else {
                    return ProcessStatus.REFILL;
                }
                break;

            default:
                throw new IllegalStateException();
        }

        return ProcessStatus.DONE;
    }

    @Override
    public List<FileShare> get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return fileShares;
    }

    @Override
    public void reset() {
        state = State.WAITING_FILE_COUNT;
        intReader.reset();
        stringReader.reset();
        fileShares.clear();
    }
}

