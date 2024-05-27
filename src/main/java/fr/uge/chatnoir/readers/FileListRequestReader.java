package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;

import java.nio.ByteBuffer;

public class FileListRequestReader implements Reader<Void> {
    private enum State { DONE, WAITING_REQUEST_NUMBER, ERROR }

    private State state = State.WAITING_REQUEST_NUMBER;
    private final IntReader intReader = new IntReader();

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if (state == State.WAITING_REQUEST_NUMBER) {
            if (intReader.process(buffer) == ProcessStatus.DONE) {
                int requestNumber = intReader.get();
                if (requestNumber != 6) { // 6 corresponds to file list request
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
                }
                state = State.DONE;
            } else {
                return ProcessStatus.REFILL;
            }
        }

        return ProcessStatus.DONE;
    }

    @Override
    public Void get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return null;
    }

    @Override
    public void reset() {
        state = State.WAITING_REQUEST_NUMBER;
        intReader.reset();
    }
}
