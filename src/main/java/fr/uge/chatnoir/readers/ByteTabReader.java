package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;
import java.nio.ByteBuffer;

public class ByteTabReader implements Reader<byte[]> {
    private enum State {
        DONE, WAITING_INT, WAITING_BYTETAB, ERROR, REFILL
    };

    private static final int INITIAL_BUFFER_SIZE = 1024; // Taille initiale du buffer
    private State state = State.WAITING_INT;
    private ByteBuffer internalBuffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE); // Mode écriture
    private byte[] value;
    private final IntReader intReader = new IntReader();
    private int size;

    @Override
    public Reader.ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if (state == State.WAITING_INT) {
            if (intReader.process(buffer) == Reader.ProcessStatus.REFILL) {
                return Reader.ProcessStatus.REFILL;
            }

            size = intReader.get();
            if (size < 0 ) {
                state = State.ERROR;
                return Reader.ProcessStatus.ERROR;
            }

            if(size > internalBuffer.remaining()){
                var tmp = ByteBuffer.allocate(size);
                internalBuffer = tmp;
            }

            state = State.WAITING_BYTETAB;
        }

        if (state == State.WAITING_BYTETAB) {

            if (buffer.remaining() < size) {
                return Reader.ProcessStatus.REFILL;
            }

            System.out.println("size => "+size);

            state = State.DONE;
            value = new byte[size];
            buffer.get(value, 0, size);


        }
        return Reader.ProcessStatus.DONE;
    }

    @Override
    public byte[] get() {
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
