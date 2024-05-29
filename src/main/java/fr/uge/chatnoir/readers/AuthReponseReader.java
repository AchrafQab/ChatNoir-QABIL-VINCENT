package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.auth.AuthReqTrame;
import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.auth.AuthResTrame;

import java.nio.ByteBuffer;

public class AuthReponseReader implements Reader<AuthResTrame> {

    private enum State { DONE, WAITING_NICKNAME, ERROR }
    private State state = State.WAITING_NICKNAME;
    private final IntReader intReader = new IntReader();
    private AuthResTrame value;
    private Integer code;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if(state == State.WAITING_NICKNAME){
            var read = intReader.process(buffer);
            if(read == ProcessStatus.ERROR){
                state= State.ERROR;
                return ProcessStatus.ERROR;
            }
            if(read == ProcessStatus.REFILL){
                return ProcessStatus.REFILL;
            }

            code = intReader.get();
            intReader.reset();


        }

        value = new AuthResTrame(code);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public AuthResTrame get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_NICKNAME;
        intReader.reset();

    }
}
