package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.AuthTrame;

import java.nio.ByteBuffer;

public class AuthRequestReader implements Reader<String> {

    private enum State { DONE, WAITING_NICKNAME, ERROR }
    private State state = State.WAITING_NICKNAME;
    private final StringReader stringReader = new StringReader();
    private AuthTrame auth;
    private String login;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if(state == State.WAITING_NICKNAME){
            var read = stringReader.process(buffer);
            if(read == ProcessStatus.ERROR){
                state= State.ERROR;
                return ProcessStatus.ERROR;
            }
            if(read == ProcessStatus.REFILL){
                return ProcessStatus.REFILL;
            }

            login = stringReader.get();
            stringReader.reset();


        }


        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public String get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return stringReader.get();
    }

    @Override
    public void reset() {
        state = State.WAITING_NICKNAME;
        stringReader.reset();

    }
}
