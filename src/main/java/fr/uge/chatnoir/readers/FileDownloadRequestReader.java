package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.file.FileDownloadInfoReq;
import fr.uge.chatnoir.protocol.file.FileDownloadReq;
import fr.uge.chatnoir.protocol.file.FileInfo;

import java.nio.ByteBuffer;

public class FileDownloadRequestReader implements Reader<FileDownloadReq> {
    private enum State { DONE, WAITING_FILE, WAITING_START, WAITING_END, ERROR }

    private State state = State.WAITING_FILE;
    private final IntReader intReader = new IntReader();
    private final FileReader fileReader = new FileReader();
    private int start;
    private int end;
    private FileInfo fileInfo;
    private FileDownloadReq value;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
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
            state = State.WAITING_START;
        }

        if (state == State.WAITING_START) {
            var read = intReader.process(buffer);
            if (read == ProcessStatus.ERROR) {
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }
            if (read == ProcessStatus.REFILL) {
                return ProcessStatus.REFILL;
            }

            start = intReader.get();
            intReader.reset();
            state = State.WAITING_END;
        }

        if (state == State.WAITING_END) {
            var read = intReader.process(buffer);
            if (read == ProcessStatus.ERROR) {
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }
            if (read == ProcessStatus.REFILL) {
                return ProcessStatus.REFILL;
            }

            end = intReader.get();
            intReader.reset();
            state = State.DONE;
        }

        value = new FileDownloadReq(fileInfo, start, end);
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
        state = State.WAITING_FILE;
        intReader.reset();
        fileReader.reset();

    }
}
