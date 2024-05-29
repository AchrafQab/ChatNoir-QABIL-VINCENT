package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.message.PublicMessage;

import java.nio.ByteBuffer;

public class PublicMessageReader implements Reader<PublicMessage> {
    private enum State { DONE, WAITING_MESSAGE, ERROR }

    private State state = State.WAITING_MESSAGE;
    private final StringReader stringReader = new StringReader();
    private String message;
    private PublicMessage value;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }


        if(state == State.WAITING_MESSAGE){
            var read = stringReader.process(buffer);
            if(read == ProcessStatus.ERROR){
                state=State.ERROR;
                return ProcessStatus.ERROR;
            }
            if(read == ProcessStatus.REFILL){
                return ProcessStatus.REFILL;
            }

            message = stringReader.get();
            stringReader.reset();

        }
       // System.out.println("login ==> "+login);
       // System.out.println("message ==> "+message);

        value = new PublicMessage(message);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public PublicMessage get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_MESSAGE;
        stringReader.reset();
    }
}
