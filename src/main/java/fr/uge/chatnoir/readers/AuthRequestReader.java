package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.auth.AuthReqTrame;

import java.nio.ByteBuffer;

public class AuthRequestReader implements Reader<AuthReqTrame> {

    private enum State { DONE, WAITING_NICKNAME, WAITING_PORT, ERROR }
    private State state = State.WAITING_NICKNAME;
    private final StringReader stringReader = new StringReader();

    private final IntReader intReader = new IntReader();

    private AuthReqTrame value;
    private String login;

    private int port;

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
            state = State.WAITING_PORT;

        }

        if(state == State.WAITING_PORT){
            var read = intReader.process(buffer);
            if(read == ProcessStatus.ERROR){
                state= State.ERROR;
                return ProcessStatus.ERROR;
            }
            if(read == ProcessStatus.REFILL) {
                return ProcessStatus.REFILL;
            }
            state = AuthRequestReader.State.DONE;
            port = intReader.get();
        }

        value = new AuthReqTrame(login, port);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public AuthReqTrame get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_NICKNAME;
        stringReader.reset();
        intReader.reset();
    }
}
