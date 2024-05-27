package fr.uge.chatnoir.readers;


import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;
import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.Message;

import java.nio.ByteBuffer;

public class FrameReader implements Reader<Trame> {

    private final Reader<Message> publicMessageReader = new PublicMessageReader();
    private enum State {
        DONE, WAITING_FRAME, ERROR, REFILL
    };

    private static final int BUFFER_SIZE = Integer.BYTES;
    private State state = State.WAITING_FRAME;
    //private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private final ByteBuffer internalBuffer = ByteBuffer.allocate(BUFFER_SIZE);// write-mode
    private Trame value;
    private  final Reader<Integer> opReader = new opReader();

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        // Process the operation reader state
        var opReaderState = opReader.process(buffer);
        switch (opReaderState) {
            case DONE:
                var op = opReader.get();
                opReader.reset();

                System.out.println("op ==> " + op);
                switch (op) {





                    case ChatMessageProtocol.PUBLIC_MESSAGE:
                        var publicMessageReaderState = publicMessageReader.process(buffer);

                        if (publicMessageReaderState == Reader.ProcessStatus.DONE) {
                            value = publicMessageReader.get();
                            publicMessageReader.reset();
                        } else if (publicMessageReaderState == Reader.ProcessStatus.ERROR) {
                            buffer.clear();
                            return publicMessageReaderState;
                        }
                        break;
                    case ChatMessageProtocol.AUTH_REQUEST:
                       /*var publicMessageReaderState = publicMessageReader.process(buffer);

                        if (publicMessageReaderState == Reader.ProcessStatus.DONE) {
                            value = publicMessageReader.get();
                            publicMessageReader.reset();
                        } else if (publicMessageReaderState == Reader.ProcessStatus.ERROR) {
                            buffer.clear();
                            return publicMessageReaderState;
                        }*/
                        value = new Message("auth client");

                        break;
                    default:
                        state = State.ERROR;
                        return ProcessStatus.ERROR;
                }
                break;
            case ERROR:
                state = State.ERROR;
                return ProcessStatus.ERROR;
            default:
                return opReaderState;
        }

        state = State.DONE;
        return ProcessStatus.DONE;
    }


    @Override
    public Trame get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_FRAME;
        internalBuffer.clear();
    }



}
