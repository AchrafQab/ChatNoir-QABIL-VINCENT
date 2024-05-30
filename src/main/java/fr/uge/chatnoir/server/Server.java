package fr.uge.chatnoir.server;

import fr.uge.chatnoir.protocol.file.FileInfo;
import fr.uge.chatnoir.protocol.message.PrivateMessage;
import fr.uge.chatnoir.protocol.message.PublicMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final int PORT = 2002;
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    public final Map<String, ClientSession> clients = new HashMap<>();
    private final Map<FileInfo, List<ClientSession>> fileRegistry = new HashMap<FileInfo, List<ClientSession>>();
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
        } catch (IOException ioe) {
            logger.log(Level.INFO, "Connection closed with client due to IOException", ioe);
            silentlyClose(key);
        }
    }

    private void doAccept(SelectionKey key) throws IOException {
        SocketChannel sc = serverSocketChannel.accept();
        System.out.println("new client");
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
        var cs = (ClientSession) key.attachment();
        unregisterClient(cs.nickname);
        System.out.println("clients ==> "+clients);
        try {
            sc.close();
        } catch (IOException e) {
            // Ignore exception

        }
    }

    public boolean registerClient(String nickname, ClientSession session) {

        synchronized (lock) {
            System.out.println("register ==> "+nickname);
            if (clients.containsKey(nickname)) {
                return false;
            }
            clients.put(nickname, session);
            return true;
        }
    }

    public void unregisterClient(String nickname) {
        synchronized (lock) {
            System.out.println("remove ==> "+nickname);
            clients.remove(nickname);
            fileRegistry.remove(nickname);
        }
    }

    public void broadcast(PublicMessage message) {
        synchronized (lock) {
            for (ClientSession client : clients.values()) {
                client.queueTrame(message);
            }
        }
    }

    public void sendPrivateMessage(PrivateMessage message) {
        synchronized (lock) {
            ClientSession client = clients.get(message.nickname());
            if (client != null) {
                client.queueTrame(message);
            }else{
                System.out.println("Client "+message.nickname()+" not found");
            }
        }
    }

    public void registerFiles(List<FileInfo> files, ClientSession client) {
        synchronized (lock) {
            for (FileInfo file : files) {
                if (!fileRegistry.containsKey(file)) {
                    fileRegistry.put(file, new ArrayList<>());
                }
                //check if client is already in the list
                if (!fileRegistry.get(file).contains(client)) {
                    fileRegistry.get(file).add(client);
                }
            }
            System.out.println(fileRegistry);
        }
    }

    /*
    public void unregisterFiles(String nickname) {
        synchronized (lock) {
            fileRegistry.remove(nickname);
        }
    }
    */

/*
    public Map<String, SharedFileRegistry> getFileRegistry() {
        synchronized (lock) {
            return new HashMap<>(fileRegistry);
        }
    }


*/

/*
    {
        "test.txt"=["127.0.0.1", "128.0.0.1"]
    }
*/

    public static void main(String[] args) throws IOException {
        new Server().launch();
    }
}
