package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.file.FileDownloadReq;
import fr.uge.chatnoir.protocol.file.FileInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FileDownloadRequestReader implements Reader<FileDownloadReq> {
    private enum State { DONE, WAITING_MODE, WAITING_FILE, ERROR }

    private State state = State.WAITING_MODE;
    private final IntReader intReader = new IntReader();
    private final FileReader fileReader = new FileReader();
    private int download_mode;
    private FileInfo fileInfo;
    private FileDownloadReq value;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if (state == State.WAITING_MODE) {
            var read = intReader.process(buffer);
            if (read == ProcessStatus.ERROR) {
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }
            if (read == ProcessStatus.REFILL) {
                return ProcessStatus.REFILL;
            }

            download_mode = intReader.get();
            intReader.reset();
            state = State.WAITING_FILE;
        }

        if (state == State.WAITING_FILE) {
            var read = fileReader.process(buffer);
            if (read == ProcessStatus.ERROR) {
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }
            if (read == ProcessStatus.REFILL) {
                return ProcessStatus.REFILL;
            }

            fileInfo = fileReader.get();
            fileReader.reset();

        }
        value = new FileDownloadReq(fileInfo, download_mode);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public FileDownloadReq get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }

        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_MODE;
        intReader.reset();
        fileReader.reset();

    }
}
