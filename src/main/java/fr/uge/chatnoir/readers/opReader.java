package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.server.Message;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class opReader implements Reader<Integer> {

    private enum State {
        DONE, WAITING_OP, ERROR, REFILL
    };

    private static final int BUFFER_SIZE = Integer.BYTES;
    private State state = State.WAITING_OP;
    //private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private final ByteBuffer internalBuffer = ByteBuffer.allocate(BUFFER_SIZE);// write-mode
    private int value;
    private IntReader intReader = new IntReader();

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if(state == State.WAITING_OP){
            var read = intReader.process(buffer);
            if(read == ProcessStatus.ERROR){
                state=State.ERROR;
                return ProcessStatus.ERROR;
            }
            if(read == ProcessStatus.REFILL){
                return ProcessStatus.REFILL;
            }

            value = intReader.get();
            intReader.reset();

            state = State.DONE;
        }
        return ProcessStatus.DONE;
    }

    @Override
    public Integer get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_OP;
        intReader.reset();
        internalBuffer.clear();
    }
}