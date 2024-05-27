package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.server.Message;

import java.nio.ByteBuffer;

public class PublicMessageReader implements Reader<Message> {
    private enum State { DONE, WAITING_LOGIN, WAITING_MESSAGE, ERROR }

    private State state = State.WAITING_LOGIN;
    private final IntReader intReader = new IntReader();
    private final StringReader stringReader = new StringReader();
    private int length;
    private String login;
    private String message;
    private Message value;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if(state == State.WAITING_LOGIN){
            var read = stringReader.process(buffer);
            if(read == ProcessStatus.ERROR){
                state=State.ERROR;
                return ProcessStatus.ERROR;
            }
            if(read == ProcessStatus.REFILL){
                return ProcessStatus.REFILL;
            }

            login = stringReader.get();
            stringReader.reset();

            state = State.WAITING_MESSAGE;
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
        System.out.println("login ==> "+login);
        System.out.println("message ==> "+message);

        value = new Message(login, message);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public Message get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_LOGIN;
        intReader.reset();
        stringReader.reset();
        length = 0;
    }
}
