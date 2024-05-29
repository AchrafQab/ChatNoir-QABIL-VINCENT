package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.message.PrivateMessage;

import java.nio.ByteBuffer;

public class PrivateMessageReader implements Reader<PrivateMessage> {
    private enum State { DONE, WAITING_NICKNAME, WAITING_MESSAGE, ERROR }

    private State state = State.WAITING_NICKNAME;
    private final StringReader stringReader = new StringReader();
    private String nickname;
    private String message;
    private PrivateMessage value;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if(state == State.WAITING_NICKNAME){
            var read = stringReader.process(buffer);
            if(read == ProcessStatus.ERROR){
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }
            if(read == ProcessStatus.REFILL){
                return ProcessStatus.REFILL;
            }

            nickname = stringReader.get();

            stringReader.reset();
            state = State.WAITING_MESSAGE;
        }
        if(state == State.WAITING_MESSAGE){
            var read = stringReader.process(buffer);
            if(read == ProcessStatus.ERROR){
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }
            if(read == ProcessStatus.REFILL){
                return ProcessStatus.REFILL;
            }

            message = stringReader.get();
            stringReader.reset();


        }
        value = new PrivateMessage(message, nickname);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public PrivateMessage get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_NICKNAME;
        stringReader.reset();
        nickname = null;
        message = null;
    }
}
