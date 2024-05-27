package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FileUnshareReader implements Reader<List<String>> {
    private enum State { DONE, WAITING_FILE_COUNT, WAITING_FILE_ID_LENGTH, WAITING_FILE_ID, ERROR }

    private State state = State.WAITING_FILE_COUNT;
    private final IntReader intReader = new IntReader();
    private final StringReader stringReader = new StringReader();
    private int fileCount;
    private final List<String> fileIds = new ArrayList<>();

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
                    fileIds.add(stringReader.get());
                    intReader.reset();
                    fileCount--;
                    if (fileCount > 0) {
                        state = State.WAITING_FILE_ID_LENGTH;
                    } else {
                        state = State.DONE;
                    }
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
    public List<String> get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return fileIds;
    }

    @Override
    public void reset() {
        state = State.WAITING_FILE_COUNT;
        intReader.reset();
        stringReader.reset();
        fileIds.clear();
    }
}
