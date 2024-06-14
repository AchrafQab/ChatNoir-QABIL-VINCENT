package fr.uge.chatnoir.client;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;
import fr.uge.chatnoir.protocol.auth.AuthReqTrame;
import fr.uge.chatnoir.protocol.auth.AuthResTrame;
import fr.uge.chatnoir.protocol.file.*;
import fr.uge.chatnoir.protocol.message.PrivateMessage;
import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.message.PublicMessage;
import fr.uge.chatnoir.protocol.proxy.ProxyReq;
import fr.uge.chatnoir.readers.TrameReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

public class Client {

    static private class Context {
        private final SelectionKey key;
        private final SocketChannel sc;
        private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
        private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
        private final ArrayDeque<Trame> queue = new ArrayDeque<>();
        private final Charset UTF8 = StandardCharsets.UTF_8;
        private final Reader<Trame> trameReader = new TrameReader();
        public boolean closed = false;
        private List<FileInfo> files = new ArrayList<>();


        public Context(SelectionKey key) {
            this.key = key;
            this.sc = (SocketChannel) key.channel();
        }

        /**
         * Process the content of bufferIn
         * The convention is that bufferIn is in write-mode before the call to process
         * and after the call
         *
         */
        private void processIn() {
            // TODO
            for (;;) {

                Reader.ProcessStatus status = trameReader.process(bufferIn);
                switch (status) {
                    case DONE:
                        var value = trameReader.get();
                        switch (value.protocol()){
                            case ChatMessageProtocol.AUTH_RESPONSE -> {
                                if(((AuthResTrame) value).code() == 200){
                                    ((Client) key.attachment()).console.start();
                                }else{
                                    System.out.println("Echec : "+((AuthResTrame) value).code());
                                }

                            }
                            case ChatMessageProtocol.PUBLIC_MESSAGE -> {
                                System.out.println("new message => "+value);
                            }

                            case ChatMessageProtocol.PRIVATE_MESSAGE -> {
                                System.out.println("new message => "+value);
                            }

                            case ChatMessageProtocol.FILE_LIST_RESPONSE -> {
                                System.out.println(((GetAllFileRes) value));
                                files = ((GetAllFileRes) value).files();
                            }

                            case ChatMessageProtocol.FILE_DOWNLOAD_INFO_RESPONSE -> {
                                System.out.println("File download info response => "+((FileDownloadInfoRes) value)  );
                                  ((Client) key.attachment()).connectToClientServer((FileDownloadInfoRes) value);
                            }

                            case ChatMessageProtocol.FILE_DOWNLOAD_RESPONSE -> {


                                System.out.println("File download response => "+((FileDownloadRes) value));
                                ((Client) key.attachment()).createFile(((FileDownloadRes) value));
                                silentlyClose();
                            }

                            case ChatMessageProtocol.PROXY_REQUEST -> {
                                System.out.println("Proxy request => "+value);
                                ((Client) key.attachment()).clientServer.applyProxy((ProxyReq) value);
                            }
                        }
                        trameReader.reset();
                        return;
                    case REFILL:
                        System.out.println("refill");
                        return;
                    case ERROR:
                        System.out.println("close in processIn");
                        silentlyClose();
                        return;
                }
            }

        }

        /**
         * Add a message to the message queue, tries to fill bufferOut and updateInterestOps
         *
         * @param trame
         */
        private void queueTrame(Trame trame) {
            // TODO
            queue.add(trame);
            processOut();
            updateInterestOps();


        }

        /**
         * Try to fill bufferOut from the message queue
         *
         */
        private void processOut() {
            // TODO

            while (!queue.isEmpty()) {
                var trame = queue.peek();
                var bbMsg = trame.toByteBuffer(UTF8);
                if(bufferOut.remaining() >= bbMsg.remaining()) {
                    queue.poll();
                    bufferOut.putInt(trame.protocol());
                    bufferOut.put(bbMsg);

                }else {
                    break;
                }
            }
        }

        /**
         * Update the interestOps of the key looking only at values of the boolean
         * closed and of both ByteBuffers.
         *
         * The convention is that both buffers are in write-mode before the call to
         * updateInterestOps and after the call. Also it is assumed that process has
         * been be called just before updateInterestOps.
         */

        private void updateInterestOps() {
            // TODO
            int ops = 0;

            if(!closed && bufferIn.hasRemaining()){
                ops |= SelectionKey.OP_READ;
            }
            if(bufferOut.position() > 0){

                ops |= SelectionKey.OP_WRITE;
            }
            if(ops != 0){
                key.interestOps(ops);
            }
            else{

                System.out.println("close in updateInterestOps");

                silentlyClose();
            }


        }

        private void silentlyClose() {
            System.out.println("silently close");
            try {
                closed = true;
                key.cancel();
                sc.close();
            } catch (IOException e) {
                // ignore exception
            }
        }

        /**
         * Performs the read action on sc
         *
         * The convention is that both buffers are in write-mode before the call to
         * doRead and after the call
         *
         * @throws IOException
         */
        private void doRead() throws IOException {
            // TODO
            closed = (sc.read(bufferIn)) == -1;
            processIn();
            //updateInterestOps();
        }

        /**
         * Performs the write action on sc
         *
         * The convention is that both buffers are in write-mode before the call to
         * doWrite and after the call
         *
         * @throws IOException
         */

        private void doWrite() throws IOException {
            // TODO
            //System.out.println("doWrite");
            bufferOut.flip();
            sc.write(bufferOut);
            bufferOut.compact();
            processOut();
            updateInterestOps();

        }

        public void doConnect() throws IOException {
            // TODO
            if (!sc.finishConnect())
                return; // the selector gave a bad hint

            System.out.println("Connected to server");
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private static int BUFFER_SIZE = 10_000;
    private static Logger logger = Logger.getLogger(Client.class.getName());
    private final ArrayBlockingQueue<Trame> blockQueue = new ArrayBlockingQueue<Trame>(10);
    private final SocketChannel sc;
    private final Selector selector;
    private final InetSocketAddress serverAddress;
    private final String login;
    private Boolean running = true;
    private final Thread console;

    private final Thread auth;

    private Context uniqueContext;
    private final Object lock = new Object();
    private final Object lockPeer = new Object();
    private ClientServer clientServer;
    private FileInfo requestedFile;


    public Client(String login, InetSocketAddress serverAddress) throws IOException {
        this.serverAddress = serverAddress;
        this.login = login;

        this.sc = SocketChannel.open();
        this.selector = Selector.open();
        this.console = Thread.ofPlatform().unstarted(this::consoleRun);
        this.auth = Thread.ofPlatform().unstarted(() -> {
            try {
                this.clientServer = new ClientServer();

                sendCommand(new AuthReqTrame(login, clientServer.PORT));

                clientServer.launch();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    private void consoleRun() {
        System.out.println("start console ...");
        try (var scanner = new Scanner(System.in)) {
            while (running) {
                System.out.println("Veuillez choisir une option:");
                System.out.println("1. Envoyer un message public");
                System.out.println("2. Envoyer un message privé");
                System.out.println("3. Partager un fichier");
                System.out.println("4. Voir les fichiers partagés");
                System.out.println("5. Télécharger un fichier");
                System.out.println("6. Retirer un fichier partagé");
                System.out.println("7. Quitter");

                if (scanner.hasNextLine()) {
                    var input = scanner.nextLine();
                    switch (input) {
                        case "1":
                            System.out.print("Entrez votre message: ");
                            if (scanner.hasNextLine()) {
                                var command = scanner.nextLine();
                                sendCommand(new PublicMessage(command));
                            }
                            break;
                        case "2":
                            System.out.print("Entrez votre message: ");
                            if (scanner.hasNextLine()) {
                                var message = scanner.nextLine();
                                System.out.print("Entrez le pseudonyme: ");
                                if (scanner.hasNextLine()) {
                                    var nickname = scanner.nextLine();
                                    sendCommand(new PrivateMessage(message, nickname));
                                }
                            }
                            break;
                        case "3":
                        String basePath = "src/main/resources/upload/";
                        var files = new ArrayList<FileInfo>();
                        while (true) {
                            System.out.print("Entrez le nom du fichier (ou appuyez sur Entrée pour terminer): ");
                            var fileName = scanner.nextLine();
                            if (fileName.isEmpty()) {
                                break;
                            }
                            var fullPath = Path.of(basePath + fileName);
                            if (!Files.exists(fullPath)) {
                                System.out.println("Fichier non trouvé");
                                continue;
                            }
                            var file = new FileInfo(fullPath);
                            files.add(file);
                        }
                        if (!files.isEmpty()) {
                            System.out.println(files);
                            sendCommand(new FileShare(files, clientServer.PORT));
                        }
                        break;


                        case "4":
                            sendCommand(new GetAllFileReq());
                            break;

                        case "5":
                            // Select file by names in files list
                            System.out.print("Entrez le nom du fichier: ");
                            if (scanner.hasNextLine()) {
                                var fileName = scanner.nextLine();
                                var file = uniqueContext.files.stream().filter(f -> f.title.equals(fileName)).findFirst();

                                if (file.isPresent()) {
                                    requestedFile = file.get();
                                    System.out.println("Entrez votre mode de téléchargement: (1 caché, 2 visible)");
                                    if (scanner.hasNextLine()) {
                                        var mode = scanner.nextLine();
                                        sendCommand(new FileDownloadInfoReq(file.get(), Integer.parseInt(mode)));
                                    } else {
                                        System.out.println("Fichier non trouvé ");
                                    }
                                } else {
                                    System.out.println("Fichier non trouvé (essayez de voir les fichiers partagés puis réessayer)");
                                }
                            }
                            break;

                        case "6":
                            System.out.print("Entrez le nom du fichier à retirer: ");
                            if (scanner.hasNextLine()) {
                                var fileName = scanner.nextLine();
                                var file = uniqueContext.files.stream().filter(f -> f.title.equals(fileName)).findFirst();
                                if (file.isPresent()) {
                                    sendCommand(new FileUnShare(List.of(file.get())));
                                } else {
                                    System.out.println("Fichier non trouvé (essayez de voir les fichiers partagés puis réessayer)");
                                }
                            }
                            break;
                        case "7":
                            System.out.println("Arrêt de la console...");
                            running = false;
                            break;
                        default:
                            System.out.println("Choix invalide, veuillez réessayer.");
                            break;
                    }
                }
            }
            logger.info("Console thread stopping");
        } catch (Exception e) {
            logger.severe("Erreur dans le thread de la console: " + e.getMessage());
        }
    }

    /**
     * Send instructions to the selector via a BlockingQueue and wake it up
     *
     * @param trame
     * @throws InterruptedException
     */

    private void sendCommand(Trame trame) throws InterruptedException {
        // TODO
        Objects.requireNonNull(trame);
        synchronized (lock) {
            System.out.println("send command ==> "+trame);
            blockQueue.add(trame);
            selector.wakeup();
        }

    }

    /**
     * Processes the command from the BlockingQueue
     */

    private void processCommands() {
        // TODO

        synchronized(lock) {
            Trame trame = blockQueue.poll();
            if(trame != null) {
                // System.out.println("process ==> "+ trame);
                uniqueContext.queueTrame(trame);
            }

        }

    }

    private void processCommandsPeer(Context context) {
        // TODO

        synchronized(lockPeer) {
            Trame trame = blockQueue.poll();
            if(trame != null) {
                System.out.println("process ==> "+ trame);
                context.queueTrame(trame);
            }

        }

    }

    public void launch() throws IOException {
        sc.configureBlocking(false);
        var key = sc.register(selector, SelectionKey.OP_CONNECT);
        uniqueContext = new Context(key);
        key.attach(this);
        sc.connect(serverAddress);
        auth.start();

        while (!Thread.interrupted() && running) {
            try {
                selector.select(this::treatKey);
                processCommands();
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
        }
    }

    private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isConnectable()) {
                uniqueContext.doConnect();
            }
            if (key.isValid() && key.isWritable()) {
                uniqueContext.doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                uniqueContext.doRead();
            }
        } catch (IOException ioe) {
            // lambda call in select requires to tunnel IOException
            throw new UncheckedIOException(ioe);
        }
    }

    private void treatKeyPeer(SelectionKey key, Context context) {
        try {
            if (key.isValid() && key.isConnectable()) {
                //System.out.println("doConnect");
                context.doConnect();
            }
            if (key.isValid() && key.isWritable()) {
                //System.out.println("doWrite");
                context.doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                //System.out.println("doRead");
                context.doRead();
            }


        } catch (IOException ioe) {
            // lambda call in select requires to tunnel IOException
            throw new UncheckedIOException(ioe);
        }
    }

    public void createFile(FileDownloadRes trame) {
        Path filePath = Paths.get("C:/Users/jjlac/OneDrive/Bureau/projet/ChatNoir-QABIL-VINCENT/src/main/java/fr/uge/chatnoir/download/", trame.title());

        try {
            // Écrivez le contenu dans le nouveau fichier
            byte[] content = trame.content();

            // Vérification de la longueur du contenu
            System.out.println("Content length: " + content.length);

            // Écrivez le contenu dans le nouveau fichier
            Files.write(filePath, content, StandardOpenOption.CREATE_NEW);

            System.out.println("Fichier créé : " + filePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du fichier : " + e.getMessage());
        }


    }

    public void connectToClientServer(FileDownloadInfoRes trame) {
        System.out.println("Connecting to client server...");
        for (String ipPort : trame.ips()) {

                try {
                    System.out.println("Connecting to client server: " + ipPort);
                    String[] parts = ipPort.split(":");
                    SocketChannel clientChannel = SocketChannel.open();
                    Selector selector = Selector.open();
                    clientChannel.configureBlocking(false);
                    var key = clientChannel.register(selector, SelectionKey.OP_CONNECT);
                    var context = new Context(key);
                    key.attach(this);
                    clientChannel.connect(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
                    sendCommand(new FileDownloadReq(requestedFile, 0, requestedFile.size, trame.id()));
                    System.out.println("Waiting for client server response...");
                    while (!context.closed) {
                            selector.select(k -> treatKeyPeer(k, context));
                            processCommandsPeer(context);

                    }
                    System.out.println("Client server connection closed");
                } catch (IOException e) {
                    System.err.println("Failed to connect to client server: " + e.getMessage());
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + e.getMessage());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
        }
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length != 3) {
            usage();
            return;
        }
        new Client(args[0], new InetSocketAddress(args[1], Integer.parseInt(args[2]))).launch();
    }


    private static void usage() {
        System.out.println("Usage : ClientChat login hostname port");
    }
}
