package fr.uge.chatnoir.client;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.Trame;
import fr.uge.chatnoir.protocol.file.FileDownloadReq;
import fr.uge.chatnoir.protocol.file.FileDownloadRes;
import fr.uge.chatnoir.readers.TrameReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

public class ClientSession {
    private static final int BUFFER_SIZE = 1024;
    private final ClientServer clientServer;
    private final SelectionKey key;
    public final SocketChannel sc;
    private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
    private final Queue<Trame> queue = new ArrayDeque<>();
    public String nickname;
    private boolean registered = false;
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    private final Reader<Trame> trameReader = new TrameReader();

    public ClientSession(ClientServer clientServer, SelectionKey key, SocketChannel sc) {
        this.clientServer = clientServer;
        this.key = key;
        this.sc = sc;
    }

    public void doRead() throws IOException {
        bufferIn.clear();
        int read = sc.read(bufferIn);
        if (read == -1) {
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
                        case ChatMessageProtocol.FILE_DOWNLOAD_REQUEST -> {
                            clientServer.sendFile(((FileDownloadReq) trame), this);
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



    public void queueTrame(Trame trame) {
        queue.add(trame);
        processOut();
        updateInterestOps();
    }

    private void processOut() {
        while (!queue.isEmpty()) {
            var trame = queue.peek();
            var bbMsg = trame.toByteBuffer(UTF_8);
            if(bufferOut.remaining() >= bbMsg.remaining()) {
                queue.poll();
                System.out.println("==> send "+trame +" -- "+trame.protocol());
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
