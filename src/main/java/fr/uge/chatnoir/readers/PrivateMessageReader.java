package fr.uge.chatnoir.readers;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PrivateMessageReader implements Reader<PrivateMessage> {
    private enum State { DONE, WAITING_RECIPIENT_LENGTH, WAITING_RECIPIENT, WAITING_MESSAGE_LENGTH, WAITING_MESSAGE, ERROR }

    private State state = State.WAITING_RECIPIENT_LENGTH;
    private final IntReader intReader = new IntReader();
    private final StringReader stringReader = new StringReader();
    private String recipient;
    private String message;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        switch (state) {
            case WAITING_RECIPIENT_LENGTH:
                if (intReader.process(buffer) == ProcessStatus.DONE) {
                    int recipientLength = intReader.get();
                    if (recipientLength <= 0 || recipientLength > 1024) {
                        state = State.ERROR;
                        return ProcessStatus.ERROR;
                    }
                    stringReader.reset();
                    state = State.WAITING_RECIPIENT;
                } else {
                    return ProcessStatus.REFILL;
                }
                break;

            case WAITING_RECIPIENT:
                if (stringReader.process(buffer) == ProcessStatus.DONE) {
                    recipient = stringReader.get();
                    intReader.reset();
                    state = State.WAITING_MESSAGE_LENGTH;
                } else {
                    return ProcessStatus.REFILL;
                }
                break;

            case WAITING_MESSAGE_LENGTH:
                if (intReader.process(buffer) == ProcessStatus.DONE) {
                    int messageLength = intReader.get();
                    if (messageLength <= 0 || messageLength > 1024) {
                        state = State.ERROR;
                        return ProcessStatus.ERROR;
                    }
                    stringReader.reset();
                    state = State.WAITING_MESSAGE;
                } else {
                    return ProcessStatus.REFILL;
                }
                break;

            case WAITING_MESSAGE:
                if (stringReader.process(buffer) == ProcessStatus.DONE) {
                    message = stringReader.get();
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
    public PrivateMessage get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return new PrivateMessage(recipient, message);
    }

    @Override
    public void reset() {
        state = State.WAITING_RECIPIENT_LENGTH;
        intReader.reset();
        stringReader.reset();
        recipient = null;
        message = null;
    }
}
