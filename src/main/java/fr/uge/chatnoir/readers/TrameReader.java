package fr.uge.chatnoir.readers;


import fr.uge.chatnoir.protocol.*;
import fr.uge.chatnoir.protocol.auth.AuthReqTrame;
import fr.uge.chatnoir.protocol.auth.AuthResTrame;
import fr.uge.chatnoir.protocol.file.FileShare;
import fr.uge.chatnoir.protocol.message.PrivateMessage;
import fr.uge.chatnoir.protocol.message.PublicMessage;

import java.nio.ByteBuffer;

public class TrameReader implements Reader<Trame> {
    private final Reader<PublicMessage> publicMessageReader = new PublicMessageReader();
    private final Reader<PrivateMessage> privateMessageReader = new PrivateMessageReader();
    private final Reader<AuthReqTrame> authRequestReader = new AuthRequestReader();
    private final Reader<AuthResTrame> authResponseReader = new AuthReponseReader();
    private final Reader<FileShare> fileShareReader = new FileShareReader();


    private enum State {
        DONE, WAITING_FRAME, ERROR, REFILL
    };

    private static final int BUFFER_SIZE = Integer.BYTES;
    private State state = State.WAITING_FRAME;
    private final ByteBuffer internalBuffer = ByteBuffer.allocate(BUFFER_SIZE);// write-mode
    private Trame value;
    private  final Reader<Integer> opReader = new opReader();

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        // Process the operation reader state;


        var opReaderState = opReader.process(buffer);
        switch (opReaderState) {
            case DONE:
                var op = opReader.get();
                opReader.reset();
                switch (op) {

                    case ChatMessageProtocol.PRIVATE_MESSAGE:
                        var privateMessageReaderState = privateMessageReader.process(buffer);

                        if (privateMessageReaderState == Reader.ProcessStatus.DONE) {
                            value = privateMessageReader.get();

                        } else if (privateMessageReaderState == Reader.ProcessStatus.ERROR) {
                            buffer.clear();
                            return privateMessageReaderState;
                        }
                        break;



                    case ChatMessageProtocol.PUBLIC_MESSAGE:
                        var publicMessageReaderState = publicMessageReader.process(buffer);

                        if (publicMessageReaderState == Reader.ProcessStatus.DONE) {
                            value = publicMessageReader.get();
                        } else if (publicMessageReaderState == Reader.ProcessStatus.ERROR) {
                            buffer.clear();
                            return publicMessageReaderState;
                        }
                        break;


                    case ChatMessageProtocol.AUTH_REQUEST:
                       var authRequestReaderState = authRequestReader.process(buffer);

                        if (authRequestReaderState == Reader.ProcessStatus.DONE) {
                            value = authRequestReader.get();

                        } else if (authRequestReaderState == Reader.ProcessStatus.ERROR) {
                            buffer.clear();
                            return authRequestReaderState;
                        }
                        break;
                    case ChatMessageProtocol.AUTH_RESPONSE:
                       var authResponseReaderState = authResponseReader.process(buffer);

                        if (authResponseReaderState == Reader.ProcessStatus.DONE) {
                            value = authResponseReader.get();
                        } else if (authResponseReaderState == Reader.ProcessStatus.ERROR) {
                            buffer.clear();
                            return authResponseReaderState;
                        }


                        break;


                    case ChatMessageProtocol.FILE_SHARE:
                        var fileShareReaderState = fileShareReader.process(buffer);

                        if (fileShareReaderState == Reader.ProcessStatus.DONE) {
                            value = fileShareReader.get();

                        } else if (fileShareReaderState == Reader.ProcessStatus.ERROR) {
                            buffer.clear();
                            return fileShareReaderState;
                        }
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
        authResponseReader.reset();
        publicMessageReader.reset();
        privateMessageReader.reset();
        authRequestReader.reset();
        internalBuffer.clear();
        fileShareReader.reset();
    }



}
