package fr.uge.chatnoir.server;

import fr.uge.chatnoir.readers.*;
import fr.uge.chatnoir.protocol.ChatMessageProtocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    private final Reader<String> authRequestReader = new AuthRequestReader();
    private final Reader<Message> publicMessageReader = new PublicMessageReader();
    private final Reader<PrivateMessage> privateMessageReader = new PrivateMessageReader();
    private final Reader<List<FileShare>> fileShareReader = new FileShareReader();
    private final Reader<List<String>> fileUnshareReader = new FileUnshareReader();
    private final Reader<Void> fileListRequestReader = new FileListRequestReader();
    private final Reader<String> fileDownloadRequestReader = new FileDownloadRequestReader();

    private  final Reader<Integer> opReader = new opReader();
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
        for (;;) {
            Reader.ProcessStatus status = opReader.process(bufferIn);
            switch (status) {
                case DONE:
                    //var value = opReader.get();
                    // server.broadcast(value);
                    //ToDo use frame reader
                    // create class Frame
                    System.out.println("==> "+value);
                   opReader.reset();

                    break;
                case REFILL:
                    return;
                case ERROR:
                    silentlyClose(key);
                    return;
            }
        }
    }
    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
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

    private void handleBroadcastMessage(Message message) {
        server.broadcast(message.text(), nickname);
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
            bufferOut.put(messages.poll().toByteBuffer(UTF_8));
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
