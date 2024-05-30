package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.file.FileInfo;
import fr.uge.chatnoir.protocol.file.FileShare;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FileShareReader implements Reader<FileShare> {
    private enum State { DONE, WAITING_FILE_COUNT, WAITING_FILE, ERROR }

    private State state = State.WAITING_FILE_COUNT;
    private final IntReader intReader = new IntReader();
    private final FileReader fileReader = new FileReader();
    private int fileCount;

    private FileShare value;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if (state == State.WAITING_FILE_COUNT) {
            var read = intReader.process(buffer);
            if (read == ProcessStatus.ERROR) {
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }
            if (read == ProcessStatus.REFILL) {
                return ProcessStatus.REFILL;
            }

            fileCount = intReader.get();
            intReader.reset();
            state = State.WAITING_FILE;
        }

        if (state == State.WAITING_FILE) {
            List<FileInfo> fileInfos = new ArrayList<>();
            for (int i = 0; i < fileCount; i++) {
                var read = fileReader.process(buffer);
                if (read == ProcessStatus.ERROR) {
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
                }
                if (read == ProcessStatus.REFILL) {
                    return ProcessStatus.REFILL;
                }

                fileInfos.add(fileReader.get());
                fileReader.reset();
            }

            value = new FileShare(fileInfos);
            state = State.DONE;
            return ProcessStatus.DONE;
        }





        return ProcessStatus.DONE;
    }

    @Override
    public FileShare get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_FILE_COUNT;
        intReader.reset();
        fileReader.reset();
    }
}

