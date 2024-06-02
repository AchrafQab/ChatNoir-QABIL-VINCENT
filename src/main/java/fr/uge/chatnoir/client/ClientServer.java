package fr.uge.chatnoir.client;

import fr.uge.chatnoir.protocol.Trame;
import fr.uge.chatnoir.protocol.file.FileDownloadReq;
import fr.uge.chatnoir.protocol.file.FileDownloadRes;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientServer {
    public int PORT;
    private static final Logger logger = Logger.getLogger(ClientServer.class.getName());
    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final Object lock = new Object();

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

        //get the real file

        var file = new File("src/main/resources/upload/"+trame.fileInfo().title);

        if(!file.exists()){
            System.out.println("file not found");
            return;
        }
        try {
            // Lisez le contenu du fichier sous forme de bytes
            byte[] fileContent = Files.readAllBytes(file.toPath());

            // Convertir le contenu du fichier en chaîne de caractères si nécessaire
            String content = new String(fileContent, StandardCharsets.UTF_8);

            // Créez un nouvel objet FileDownloadRes avec le contenu du fichier
            FileDownloadRes fileDownloadRes = new FileDownloadRes(trame.fileInfo().title, content);

            // Mettez l'objet FileDownloadRes dans la file d'attente
            clientSession.queueTrame(fileDownloadRes);
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
        }



        //clientSession.queueTrame(new FileDownloadRes());


    }


}
