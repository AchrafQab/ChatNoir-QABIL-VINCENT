package fr.uge.chatnoir.server;

import fr.uge.chatnoir.protocol.file.FileDownloadInfoReq;
import fr.uge.chatnoir.protocol.file.FileDownloadInfoRes;
import fr.uge.chatnoir.protocol.file.FileInfo;
import fr.uge.chatnoir.protocol.file.GetAllFileRes;
import fr.uge.chatnoir.protocol.message.PrivateMessage;
import fr.uge.chatnoir.protocol.message.PublicMessage;
import fr.uge.chatnoir.protocol.proxy.ProxyReq;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final int PORT = 2002;
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    public final Map<String, ClientSession> clients = new HashMap<>();
    private final Map<FileInfo, HashMap<ClientSession, Integer>> fileRegistry = new HashMap<FileInfo, HashMap<ClientSession, Integer>>();
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
        unregisterClient(cs);
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

    public void unregisterClient(ClientSession client) {
        synchronized (lock) {
            System.out.println("remove ==> "+client.nickname);
            clients.remove(client.nickname);
            // remove client from all array list in fileRegistry
            for (Map.Entry<FileInfo, HashMap<ClientSession, Integer>> entry : fileRegistry.entrySet()) {
                entry.getValue().remove(client);
                if (entry.getValue().isEmpty()) {
                    fileRegistry.remove(entry.getKey());
                }
            }

            System.out.println(fileRegistry);
        }
    }

    public void broadcast(PublicMessage message) {
        synchronized (lock) {
            for (ClientSession client : clients.values()) {
                client.queueTrame(message);
            }
        }
    }
    public void sendFilesList(ClientSession clientSession) {
        synchronized (lock) {
            List<FileInfo> files = new ArrayList<>(fileRegistry.keySet());
            clientSession.queueTrame(new GetAllFileRes(files));
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

    public void registerFiles(List<FileInfo> files, ClientSession client, Integer port) {
        synchronized (lock) {
            for (FileInfo file : files) {
                if (!fileRegistry.containsKey(file)) {
                    fileRegistry.put(file, new HashMap<>());
                }
                //check if client is already in the hashmap
                if (fileRegistry.get(file).containsKey(client)) {
                    fileRegistry.get(file).replace(client, port);
                } else {
                    fileRegistry.get(file).put(client, port);
                }
            }
            System.out.println(fileRegistry);
        }
    }
    public void sendFileInfo(FileDownloadInfoReq trame, ClientSession clientSession) {
        synchronized (lock) {
            FileInfo fileInfo = trame.fileInfo();
            var downloadMode = trame.downloadMode();
            int id = new Random().nextInt(1000);
                HashMap<ClientSession, Integer> clientsOfFile = fileRegistry.get(fileInfo);
                if (clientsOfFile == null) {
                    clientSession.queueTrame(new FileDownloadInfoRes(new ArrayList<>(), 0));
                } else {

                    List<String> ips = new ArrayList<>();
                    for (Map.Entry<ClientSession, Integer> entry : clientsOfFile.entrySet()) {
                        System.out.println("==> " + entry);
                        try {
                            if(downloadMode == 1){
                                ClientSession selectedClient = selectProxy(clientSession, clients, clientsOfFile);
                                if(selectedClient != null){

                                    selectedClient.queueTrame(new ProxyReq(((InetSocketAddress) selectedClient.sc.getRemoteAddress()).getAddress().getHostAddress(), ((InetSocketAddress) entry.getKey().sc.getRemoteAddress()).getAddress().getHostAddress(), entry.getValue(), id));
                                    ips.add(((InetSocketAddress) selectedClient.sc.getRemoteAddress()).getAddress().getHostAddress() + ":" + selectedClient.port);
                                }
                            }else{
                                ips.add(((InetSocketAddress) entry.getKey().sc.getRemoteAddress()).getAddress().getHostAddress() + ":" + entry.getValue());
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    clientSession.queueTrame(new FileDownloadInfoRes(ips, id));
                }
            }
        }

    private ClientSession selectProxy(ClientSession clientRequesting, Map<String, ClientSession> allClients, Map<ClientSession, Integer> fileHosts) {
        for (ClientSession client : allClients.values()) {
            if (!client.equals(clientRequesting) && !fileHosts.containsKey(client)) {
                return client;
            }
        }
        return null;
    }
    public void unregisterFiles(List<FileInfo> files, ClientSession client) {
        synchronized (lock) {
            for (FileInfo file : files) {
                if (fileRegistry.containsKey(file)) {
                    fileRegistry.get(file).remove(client);
                    if (fileRegistry.get(file).isEmpty()) {
                        fileRegistry.remove(file);
                    }
                }
            }
            System.out.println(fileRegistry);
        }
    }

    public static void main(String[] args) throws IOException {
        new Server().launch();
    }



}
