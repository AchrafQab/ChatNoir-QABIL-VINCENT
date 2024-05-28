package fr.uge.chatnoir.server;

import fr.uge.chatnoir.protocol.*;
import fr.uge.chatnoir.readers.*;

import java.io.IOException;
import java.net.InetSocketAddress;
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
    private final Queue<Trame> queue = new ArrayDeque<>();
    public String nickname;
    private boolean registered = false;
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    private final Reader<Message> publicMessageReader = new PublicMessageReader();
    private final Reader<PrivateMessage> privateMessageReader = new PrivateMessageReader();
    private final Reader<List<FileShare>> fileShareReader = new FileShareReader();
    private final Reader<List<String>> fileUnshareReader = new FileUnshareReader();
    private final Reader<Void> fileListRequestReader = new FileListRequestReader();
    private final Reader<String> fileDownloadRequestReader = new FileDownloadRequestReader();



    private final Reader<Trame> trameReader = new TrameReader();

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

        System.out.println("send ");
        bufferOut.flip();
        sc.write(bufferOut);
        bufferOut.compact();
        updateInterestOps();
    }

    private void processIn() {
        for (;;) {
            Reader.ProcessStatus status = trameReader.process(bufferIn);
            switch (status) {
                case DONE:

                    var trame = trameReader.get();

                    System.out.println("Frame ==> "+trame);
                    System.out.println("protocol ==> "+trame.protocol());
                    trameReader.reset();

                    switch(trame.protocol()){
                        case ChatMessageProtocol.AUTH_REQUEST -> {
                            nickname = ((AuthTrame) trame).login();
                            if(!server.registerClient(nickname, this)){
                                //server.sendAuthRes(200, this);
                                queueMessage(new Message("ERROR: Pseudonyme already in use."));

                            }else{
                               // server.sendAuthRes(403, this);
                                queueMessage(new Message("Welcome " + nickname + "!"));
                            }
                            System.out.println("clients ==> "+ server.clients);
                            /*try {
                                var sa = server.clients.get("Alex").sc.getRemoteAddress();
                                System.out.println("==> "+sa);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }*/


                        }
                    }
                    return;
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
        /*if (!registered) {
            queueMessage(new Message("Server", "ERROR: Pseudonyme already in use."));
        } else {
            queueMessage(new Message("Server", "Welcome " + nickname + "!"));
        }*/
    }

    private void handleBroadcastMessage(Message message) {
        server.broadcast(message.message(), nickname);
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
        queue.add(message);
        processOut();
        updateInterestOps();
    }

    private void processOut() {
        while (!queue.isEmpty()) {
            var trame = queue.peek();
            var bbMsg = trame.toByteBuffer(UTF_8);
            if(bufferOut.remaining() >= bbMsg.remaining()) {
                queue.poll();
                System.out.println("==> "+trame +" -- "+trame.protocol());
                bufferOut.putInt(trame.protocol());
                bufferOut.put(bbMsg);

            }else {
                break;
            }
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
