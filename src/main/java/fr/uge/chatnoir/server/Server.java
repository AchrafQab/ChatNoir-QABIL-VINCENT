package fr.uge.chatnoir.server;

import fr.uge.chatnoir.client.SharedFileRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final int PORT = 2002;
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private final Map<String, ClientSession> clients = new HashMap<>();
    private final Map<String, SharedFileRegistry> fileRegistry = new HashMap<>();
    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final Object lock = new Object();

    public Server() throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.selector = Selector.open();
        this.serverSocketChannel.bind(new InetSocketAddress(PORT));
        this.serverSocketChannel.configureBlocking(false);
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void launch() throws IOException {
        logger.info("Server started...");
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
                ((ClientSession) key.attachment()).doWrite();
            }
        } catch (IOException e) {
            logger.log(Level.INFO, "Connection closed with client due to IOException", e);
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

    public boolean registerClient(String nickname, ClientSession session) {
        synchronized (lock) {
            if (clients.containsKey(nickname)) {
                return false;
            }
            clients.put(nickname, session);
            return true;
        }
    }

    public void unregisterClient(String nickname) {
        synchronized (lock) {
            clients.remove(nickname);
            fileRegistry.remove(nickname);
        }
    }

    public void broadcast(String message, String sender) {
        synchronized (lock) {
            for (ClientSession client : clients.values()) {
                client.queueMessage(new Message(sender, message));
            }
        }
    }

    public void sendPrivateMessage(String message, String sender, String recipient) {
        synchronized (lock) {
            ClientSession client = clients.get(recipient);
            if (client != null) {
                client.queueMessage(new Message(sender, "Private from " + sender + ": " + message));
            }
        }
    }

    public void registerFiles(String nickname, SharedFileRegistry registry) {
        synchronized (lock) {
            fileRegistry.put(nickname, registry);
        }
    }

    public void unregisterFiles(String nickname) {
        synchronized (lock) {
            fileRegistry.remove(nickname);
        }
    }

    public Map<String, SharedFileRegistry> getFileRegistry() {
        synchronized (lock) {
            return new HashMap<>(fileRegistry);
        }
    }

    public static void main(String[] args) throws IOException {
        new Server().launch();
    }
}
