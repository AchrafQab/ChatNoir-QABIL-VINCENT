package fr.uge.chatnoir.client;

import fr.uge.chatnoir.protocol.Trame;
import fr.uge.chatnoir.protocol.file.FileDownloadReq;
import fr.uge.chatnoir.protocol.file.FileDownloadRes;
import fr.uge.chatnoir.protocol.proxy.ProxyReq;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientServer {
    public int PORT;
    private static final Logger logger = Logger.getLogger(ClientServer.class.getName());
    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final Object lock = new Object();

    private final List<ProxyReq> proxyList = new ArrayList<>();

    private final HashMap<ClientSession, Integer> proxyMap = new HashMap<>();


    public ClientServer() throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.selector = Selector.open();
        this.serverSocketChannel.bind(new InetSocketAddress(0));
        this.serverSocketChannel.configureBlocking(false);
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.PORT = serverSocketChannel.socket().getLocalPort();
    }

    public void launch() throws IOException {
        logger.info("ClientServer started... on port " + PORT);
        while (!Thread.interrupted()) {
            selector.select(this::treatKey);
        }
    }

    private void treatKey(SelectionKey key) {

        try {
            if (key.isValid() && key.isAcceptable()) {
                doAccept(key);
            }
            if (key.isValid() && key.isReadable()) {

                ((ClientSession) key.attachment()).doRead();
            }
            if (key.isValid() && key.isWritable()) {
                System.out.println("write");
                ((ClientSession) key.attachment()).doWrite();
            }
        } catch (IOException ioe) {
            logger.log(Level.INFO, "Connection closed with client due to IOException", ioe);
            silentlyClose(key);
        }
    }

    private void doAccept(SelectionKey key) throws IOException {
        SocketChannel sc = serverSocketChannel.accept();
        if (sc == null) {
            return;
        }
        sc.configureBlocking(false);
        SelectionKey clientKey = sc.register(selector, SelectionKey.OP_READ);

        clientKey.attach(new ClientSession(this, clientKey, sc));

        logger.info("Accepted connection from client.");
    }

    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();

        try {
            sc.close();
        } catch (IOException e) {
            // Ignore exception
        }
    }

    public void sendFile(FileDownloadReq trame, ClientSession clientSession) {

        System.out.println("send file => "+ trame);
        System.out.println("proxy list => "+proxyList);
        for (ProxyReq proxyReq : proxyList) {
            try {
                String clientIp = ((InetSocketAddress) clientSession.sc.getRemoteAddress()).getAddress().getHostAddress();
                if (proxyReq.id() == trame.id()) {
                    System.out.println("Proxy found for client " + clientIp + " with ID " + proxyReq.id());
                    proxyMap.put(clientSession, proxyReq.id());
                    connectToClientServer(proxyReq.ipDest(), proxyReq.portDest(), trame);
                    return;
                }
            } catch (IOException e) {
              System.err.println("Erreur lors de la récupération de l'adresse IP du client : " + e.getMessage());
            }
        }

        var file = new File("src/main/resources/upload/"+trame.fileInfo().title);

        if(!file.exists()){
            System.out.println("file not found");
            return;
        }
        try {

            System.out.println("reading file ...");
            byte[] fileContent = Files.readAllBytes(file.toPath());
            System.out.println("end read ...");
            //System.out.println("content => "+content);
            FileDownloadRes fileDownloadRes = new FileDownloadRes(trame.fileInfo().title, fileContent, trame.id());

            clientSession.queueTrame(fileDownloadRes);

        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
        }
    }

    public void applyProxy(ProxyReq trame) {
        System.out.println("apply proxy => "+trame);
        proxyList.add(trame);
    }


    private void connectToClientServer(String ipDest, int portDest, FileDownloadReq trame) {
        try {
            System.out.println("Connecting to client server ...");
            SocketChannel clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            Selector selector = Selector.open();
            SelectionKey key = clientChannel.register(selector, SelectionKey.OP_CONNECT);
            ClientSession session = new ClientSession(this, key, clientChannel);
            key.attach(session);
            clientChannel.connect(new InetSocketAddress(ipDest, portDest));
            while (!clientChannel.finishConnect()) {
            }
            session.queueTrame(trame);
            while (selector.select() > 0) {
                for (SelectionKey selKey : selector.selectedKeys()) {
                    treatKey(selKey);
                }
            }
            System.out.println("Connected to client server");
        } catch (IOException e) {
            System.err.println("Erreur lors de la connexion au serveur client : " + e.getMessage());
        }
    }

    public void retransmit(FileDownloadRes trame) {
        System.out.println("retransmit => "+trame);
        for (ClientSession clientSession : proxyMap.keySet()) {
            if (proxyMap.get(clientSession) == trame.id()) {
                System.out.println("retransmit to client ...");
                clientSession.queueTrame(trame);
                try {
                    //ToDo : Je n'ai pas trouvé d'autre moyen pour envoyer la trame au client
                    clientSession.doWrite();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }
        System.out.println("No corresponding session found for proxy ID: " + trame.id());
    }
}
