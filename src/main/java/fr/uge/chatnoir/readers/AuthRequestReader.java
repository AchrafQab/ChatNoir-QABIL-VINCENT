package fr.uge.chatnoir.readers;

import java.nio.ByteBuffer;

public class AuthRequestReader implements Reader<String> {

    private enum State { DONE, WAITING_LENGTH, WAITING_NICKNAME, WAITING_RESPONSE, ERROR }
    private State state = State.WAITING_LENGTH;
    private final IntReader intReader = new IntReader();
    private final StringReader stringReader = new StringReader();
    private int length;
    private int responseCode;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        switch (state) {
            case WAITING_LENGTH:
                if (intReader.process(buffer) == ProcessStatus.DONE) {
                    length = intReader.get();
                    if (length <= 0 || length > 1024) {
                        state = State.ERROR;
                        return ProcessStatus.ERROR;
                    }
                    stringReader.reset();
                    state = State.WAITING_NICKNAME;
                } else {
                    return ProcessStatus.REFILL;
                }
                break;

            case WAITING_NICKNAME:
                if (stringReader.process(buffer) == ProcessStatus.DONE) {
                    state = State.WAITING_RESPONSE;
                    intReader.reset();
                } else {
                    return ProcessStatus.REFILL;
                }
                break;

            case WAITING_RESPONSE:
                if (intReader.process(buffer) == ProcessStatus.DONE) {
                    responseCode = intReader.get();
                    state = State.DONE;
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
    public String get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return stringReader.get() + " Response Code: " + responseCode;
    }

    @Override
    public void reset() {
        state = State.WAITING_LENGTH;
        intReader.reset();
        stringReader.reset();
        length = 0;
        responseCode = 0;
    }
}
