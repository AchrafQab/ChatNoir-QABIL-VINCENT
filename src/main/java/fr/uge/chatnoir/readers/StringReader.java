package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringReader implements Reader<String> {

    private enum State {
        DONE, WAITING_INT, WAITING_STRING, ERROR, REFILL
    };

    private static final int BUFFER_SIZE = 1024;
    private State state = State.WAITING_INT;
    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private final ByteBuffer internalBuffer = ByteBuffer.allocate(BUFFER_SIZE);// write-mode
    private String value;
    private IntReader intReader = new IntReader();
    private int size;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if(state == State.WAITING_INT){
            if(intReader.process(buffer) == ProcessStatus.REFILL){
                return ProcessStatus.REFILL;
            }

            size = intReader.get();
            if(size < 0 || size > BUFFER_SIZE){
                return ProcessStatus.ERROR;
            }

            state = State.WAITING_STRING;
        }

        if(state == State.WAITING_STRING){
            buffer.flip();
            var missing = size - internalBuffer.position();
            try {
                if (buffer.remaining() <= missing) {
                    internalBuffer.put(buffer);
                } else {
                    var tmpLimit = buffer.limit();
                    buffer.limit(buffer.position() + missing);
                    internalBuffer.put(buffer);
                    buffer.limit(tmpLimit);
                }
            } finally {
                buffer.compact();
            }
            if (internalBuffer.position() < size) {
                return ProcessStatus.REFILL;
            }
            state = State.DONE;
            internalBuffer.flip();
            value = UTF_8.decode(internalBuffer).toString();
            //value = internalBuffer.getInt();
        }
        return ProcessStatus.DONE;
    }

    @Override
    public String get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_INT;
        intReader.reset();
        internalBuffer.clear();
    }
}