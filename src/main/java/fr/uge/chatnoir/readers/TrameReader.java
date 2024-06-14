package fr.uge.chatnoir.readers;


import fr.uge.chatnoir.protocol.*;
import fr.uge.chatnoir.protocol.auth.AuthReqTrame;
import fr.uge.chatnoir.protocol.auth.AuthResTrame;
import fr.uge.chatnoir.protocol.file.*;
import fr.uge.chatnoir.protocol.proxy.ProxyReq;
import fr.uge.chatnoir.protocol.message.PrivateMessage;
import fr.uge.chatnoir.protocol.message.PublicMessage;

import java.nio.ByteBuffer;

public class TrameReader implements Reader<Trame> {
    private final Reader<PublicMessage> publicMessageReader = new PublicMessageReader();
    private final Reader<PrivateMessage> privateMessageReader = new PrivateMessageReader();
    private final Reader<AuthReqTrame> authRequestReader = new AuthRequestReader();
    private final Reader<AuthResTrame> authResponseReader = new AuthReponseReader();
    private final Reader<FileShare> fileShareReader = new FileShareReader();
    private final Reader<FileUnShare> fileUnshareReader = new FileUnshareReader();

    private final Reader<GetAllFileRes> getAllFileReader = new GetAllFileReader();
    private final Reader<FileDownloadInfoReq> fileDownloadInfoReqReader = new FileDownloadInfoRequestReader();
    private final Reader<FileDownloadInfoRes> fileDownloadInfoResReader = new FileDownloadInfoResponseReader();
    private final Reader<FileDownloadReq> fileDownloadRequestReader = new FileDownloadRequestReader();
    private final Reader<FileDownloadRes> fileDownloadResponseReader = new FileDownloadResponseReader();

    private final Reader<ProxyReq> proxyRequestReader = new ProxyReqReader();

    private boolean refillDownloadRes = false;
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

                    case ChatMessageProtocol.FILE_UNSHARE:
                        var fileUnshareReaderState = fileUnshareReader.process(buffer);

                        if (fileUnshareReaderState == Reader.ProcessStatus.DONE) {
                            value = fileUnshareReader.get();
                        } else if (fileUnshareReaderState == Reader.ProcessStatus.ERROR) {
                            buffer.clear();
                            return fileUnshareReaderState;
                        }
                        break;



                    case ChatMessageProtocol.FILE_LIST_REQUEST:
                        value = new GetAllFileReq();
                        break;


                    case ChatMessageProtocol.FILE_LIST_RESPONSE:
                        var getAllFileReaderState = getAllFileReader.process(buffer);

                        if (getAllFileReaderState == Reader.ProcessStatus.DONE) {
                            value = getAllFileReader.get();
                        } else if (getAllFileReaderState == Reader.ProcessStatus.ERROR) {
                            buffer.clear();
                            return getAllFileReaderState;
                        }
                        break;


                    case ChatMessageProtocol.FILE_DOWNLOAD_INFO_REQUEST:
                        var fileDownloadInfoReqReaderState = fileDownloadInfoReqReader.process(buffer);

                        if (fileDownloadInfoReqReaderState == Reader.ProcessStatus.DONE) {
                            value = fileDownloadInfoReqReader.get();
                        } else if (fileDownloadInfoReqReaderState == Reader.ProcessStatus.ERROR) {
                            buffer.clear();
                            return fileDownloadInfoReqReaderState;
                        }
                        break;

                    case ChatMessageProtocol.FILE_DOWNLOAD_INFO_RESPONSE:
                        var fileDownloadInfoResReaderState = fileDownloadInfoResReader.process(buffer);

                        if (fileDownloadInfoResReaderState == Reader.ProcessStatus.DONE) {

                            value = fileDownloadInfoResReader.get();
                        } else if (fileDownloadInfoResReaderState == Reader.ProcessStatus.ERROR) {
                            buffer.clear();
                            return fileDownloadInfoResReaderState;
                        }
                        break;
                    case ChatMessageProtocol.FILE_DOWNLOAD_REQUEST:
                        var fileDownloadRequestReaderState = fileDownloadRequestReader.process(buffer);

                        if (fileDownloadRequestReaderState == Reader.ProcessStatus.DONE) {
                            value = fileDownloadRequestReader.get();
                        } else if (fileDownloadRequestReaderState == Reader.ProcessStatus.ERROR) {
                            buffer.clear();
                            return fileDownloadRequestReaderState;
                        }
                        break;

                    case ChatMessageProtocol.FILE_DOWNLOAD_RESPONSE:
                        System.out.println("file download response");
                        var fileDownloadResponseReaderState = fileDownloadResponseReader.process(buffer);
                        System.out.println(fileDownloadResponseReaderState);
                        if (fileDownloadResponseReaderState == Reader.ProcessStatus.DONE) {
                            value = fileDownloadResponseReader.get();
                        } else if (fileDownloadResponseReaderState == Reader.ProcessStatus.ERROR) {
                            System.out.println("error");
                            buffer.clear();
                            return fileDownloadResponseReaderState;
                        }else if (fileDownloadResponseReaderState == Reader.ProcessStatus.REFILL) {
                            refillDownloadRes = true;
                            return fileDownloadResponseReaderState;
                        }
                        break;

                    case ChatMessageProtocol.PROXY_REQUEST:
                        System.out.println("proxy request");
                        var proxyRequestReaderState = proxyRequestReader.process(buffer);

                        if (proxyRequestReaderState == Reader.ProcessStatus.DONE) {
                            value = proxyRequestReader.get();
                        } else if (proxyRequestReaderState == Reader.ProcessStatus.ERROR) {
                            buffer.clear();
                            return proxyRequestReaderState;
                        }
                        break;

                    default:
                        System.out.println("Unknown operation code");
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
        getAllFileReader.reset();
        fileDownloadInfoReqReader.reset();
        fileDownloadInfoResReader.reset();
        fileDownloadRequestReader.reset();
        fileDownloadResponseReader.reset();
        proxyRequestReader.reset();

    }



}
