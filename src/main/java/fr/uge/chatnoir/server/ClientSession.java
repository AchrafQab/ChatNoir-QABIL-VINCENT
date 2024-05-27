package fr.uge.chatnoir.server;

import fr.uge.chatnoir.readers.*;
import fr.uge.chatnoir.protocol.ChatMessageProtocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public class ClientSession {
    private static final int BUFFER_SIZE = 1024;
    private final Server server;
    private final SelectionKey key;
    private final SocketChannel sc;
    private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
    private final Queue<Message> messages = new ArrayDeque<>();
    private String nickname;
    private boolean registered = false;
    private final Reader<String> authRequestReader = new AuthRequestReader();
    private final Reader<String> publicMessageReader = new PublicMessageReader();
    private final Reader<PrivateMessage> privateMessageReader = new PrivateMessageReader();
    private final Reader<List<FileShare>> fileShareReader = new FileShareReader();
    private final Reader<List<String>> fileUnshareReader = new FileUnshareReader();
    private final Reader<Void> fileListRequestReader = new FileListRequestReader();
    private final Reader<String> fileDownloadRequestReader = new FileDownloadRequestReader();

    public ClientSession(Server server, SelectionKey key, SocketChannel sc) {
        this.server = server;
        this.key = key;
        this.sc = sc;
    }

    public void doRead() throws IOException {
        bufferIn.clear();
        int read = sc.read(bufferIn);
        if (read == -1) {
            server.unregisterClient(nickname);
            sc.close();
            return;
        }
        processIn();
    }

    public void doWrite() throws IOException {
        bufferOut.flip();
        sc.write(bufferOut);
        bufferOut.compact();
        updateInterestOps();
    }

    private void processIn() {
        bufferIn.flip();
        while (bufferIn.remaining() >= 4) {
            bufferIn.mark();
            int messageType = bufferIn.getInt();
            Reader.ProcessStatus status;
            switch (messageType) {
                case ChatMessageProtocol.AUTH_REQUEST:
                    status = authRequestReader.process(bufferIn);
                    if (status == Reader.ProcessStatus.DONE) {
                        handleAuthRequest(authRequestReader.get());
                        authRequestReader.reset();
                    } else if (status == Reader.ProcessStatus.ERROR) {
                        bufferIn.reset();
                        return;
                    }
                    break;
                case ChatMessageProtocol.PUBLIC_MESSAGE:
                    status = publicMessageReader.process(bufferIn);
                    if (status == Reader.ProcessStatus.DONE) {
                        handleBroadcastMessage(publicMessageReader.get());
                        publicMessageReader.reset();
                    } else if (status == Reader.ProcessStatus.ERROR) {
                        bufferIn.reset();
                        return;
                    }
                    break;
                case ChatMessageProtocol.PRIVATE_MESSAGE:
                    status = privateMessageReader.process(bufferIn);
                    if (status == Reader.ProcessStatus.DONE) {
                        PrivateMessage privateMessage = privateMessageReader.get();
                        handlePrivateMessage(privateMessage.getRecipient(), privateMessage.getMessage());
                        privateMessageReader.reset();
                    } else if (status == Reader.ProcessStatus.ERROR) {
                        bufferIn.reset();
                        return;
                    }
                    break;
                case ChatMessageProtocol.FILE_SHARE:
                    status = fileShareReader.process(bufferIn);
                    if (status == Reader.ProcessStatus.DONE) {
                        handleFileShare(fileShareReader.get());
                        fileShareReader.reset();
                    } else if (status == Reader.ProcessStatus.ERROR) {
                        bufferIn.reset();
                        return;
                    }
                    break;
                case ChatMessageProtocol.FILE_UNSHARE:
                    status = fileUnshareReader.process(bufferIn);
                    if (status == Reader.ProcessStatus.DONE) {
                        handleFileUnshared(fileUnshareReader.get());
                        fileUnshareReader.reset();
                    } else if (status == Reader.ProcessStatus.ERROR) {
                        bufferIn.reset();
                        return;
                    }
                    break;
                case ChatMessageProtocol.FILE_LIST_REQUEST:
                    status = fileListRequestReader.process(bufferIn);
                    if (status == Reader.ProcessStatus.DONE) {
                        handleFileListRequest();
                        fileListRequestReader.reset();
                    } else if (status == Reader.ProcessStatus.ERROR) {
                        bufferIn.reset();
                        return;
                    }
                    break;
                case ChatMessageProtocol.FILE_DOWNLOAD_REQUEST:
                    status = fileDownloadRequestReader.process(bufferIn);
                    if (status == Reader.ProcessStatus.DONE) {
                        handleFileDownloadRequest(fileDownloadRequestReader.get());
                        fileDownloadRequestReader.reset();
                    } else if (status == Reader.ProcessStatus.ERROR) {
                        bufferIn.reset();
                        return;
                    }
                    break;
                default:
                    bufferIn.reset();
                    return;
            }
        }
        bufferIn.compact();
    }

    private void handleAuthRequest(String nickname) {
        this.nickname = nickname;
        registered = server.registerClient(nickname, this);
        if (!registered) {
            queueMessage(new Message("Server", "ERROR: Pseudonyme already in use."));
        } else {
            queueMessage(new Message("Server", "Welcome " + nickname + "!"));
        }
    }

    private void handleBroadcastMessage(String message) {
        server.broadcast(message, nickname);
    }

    private void handlePrivateMessage(String recipient, String message) {
        server.sendPrivateMessage(message, nickname, recipient);
    }

    private void handleFileShare(String fileName) {
        // handle file announcement
    }

    private void handleFileUnshared(String fileName) {
    }

    private void handleFileListRequest() {
        // handle file list request
    }

    private void handleFileDownloadRequest(String response) {
        // handle file download request
    }

    public void queueMessage(Message message) {
        messages.add(message);
        processOut();
        updateInterestOps();
    }

    private void processOut() {
        while (!messages.isEmpty() && bufferOut.remaining() > 0) {
            bufferOut.put(messages.poll().toByteBuffer());
        }
    }

    private void updateInterestOps() {
        int ops = 0;
        if (bufferIn.hasRemaining()) {
            ops |= SelectionKey.OP_READ;
        }
        if (bufferOut.position() > 0) {
            ops |= SelectionKey.OP_WRITE;
        }
        key.interestOps(ops);
    }
}
