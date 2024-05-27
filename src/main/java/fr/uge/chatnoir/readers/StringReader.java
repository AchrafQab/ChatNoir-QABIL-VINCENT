package fr.uge.progres;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringReader implements Reader<String> {

    private enum State {
        DONE, WAITING_SIZE, WAITING_CONTENT, ERROR
    };

    private State state = State.WAITING_SIZE;
    private final ByteBuffer internalBuffer = ByteBuffer.allocate(Integer.BYTES);
    private ByteBuffer contentBuffer;
    private final Charset charset = StandardCharsets.UTF_8;
    private String value;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        buffer.flip();
        try {
            if (state == State.WAITING_SIZE) {
                while (buffer.hasRemaining() && internalBuffer.hasRemaining()) {
                    internalBuffer.put(buffer.get());
                }
                if (internalBuffer.hasRemaining()) {
                    return ProcessStatus.REFILL;
                }
                internalBuffer.flip();
                int size = internalBuffer.getInt();
                if (size > 1024 || size < 0) {
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
                }
                contentBuffer = ByteBuffer.allocate(size);
                state = State.WAITING_CONTENT;
            }

            if (state == State.WAITING_CONTENT) {
                while (buffer.hasRemaining() && contentBuffer.hasRemaining()) {
                    contentBuffer.put(buffer.get());
                }
                if (contentBuffer.hasRemaining()) {
                    return ProcessStatus.REFILL;
                }
                contentBuffer.flip();
                value = charset.decode(contentBuffer).toString();
                state = State.DONE;
                return ProcessStatus.DONE;
            }

            return ProcessStatus.ERROR;
        } finally {
            buffer.compact();
        }
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
        state = State.WAITING_SIZE;
        internalBuffer.clear();
        if (contentBuffer != null) {
            contentBuffer.clear();
        }
        value = null;
    }
}
