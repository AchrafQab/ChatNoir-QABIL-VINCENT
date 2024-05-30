package fr.uge.chatnoir.client;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;
import fr.uge.chatnoir.protocol.auth.AuthReqTrame;
import fr.uge.chatnoir.protocol.auth.AuthResTrame;
import fr.uge.chatnoir.protocol.file.FileInfo;
import fr.uge.chatnoir.protocol.file.FileShare;
import fr.uge.chatnoir.protocol.file.GetAllFileReq;
import fr.uge.chatnoir.protocol.file.GetAllFileRes;
import fr.uge.chatnoir.protocol.message.PrivateMessage;
import fr.uge.chatnoir.readers.PublicMessageReader;
import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.message.PublicMessage;
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
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

public class Client {

    static private class Context {
        private final SelectionKey key;
        private final SocketChannel sc;
        private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
        private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
        private final ArrayDeque<Trame> queue = new ArrayDeque<>();
        private Charset UTF8 = StandardCharsets.UTF_8;
        private final PublicMessageReader publicMessageReader = new PublicMessageReader();
        private final Reader<Trame> trameReader = new TrameReader();
        private boolean closed = false;


        private Context(SelectionKey key) {
            this.key = key;
            this.sc = (SocketChannel) key.channel();
        }

        /**
         * Process the content of bufferIn
         *
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

                            case ChatMessageProtocol.FILE_LIST_RESPONSE -> {
                                System.out.println("files ==> " + ((GetAllFileRes) value));
                            }
                        }
                        trameReader.reset();
                        return;
                    case REFILL:
                        return;
                    case ERROR:
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
                    // System.out.println("==> "+trame +" -- "+trame.protocol());
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
                silentlyClose();
            }


        }

        private void silentlyClose() {
            try {
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
            updateInterestOps();
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

    public Client(String login, InetSocketAddress serverAddress) throws IOException {
        this.serverAddress = serverAddress;
        this.login = login;
        this.sc = SocketChannel.open();
        this.selector = Selector.open();
        this.console = Thread.ofPlatform().unstarted(this::consoleRun);
        this.auth = Thread.ofPlatform().unstarted(() -> {
            try {
                sendCommand(new AuthReqTrame(login));
            } catch (InterruptedException e) {
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
                System.out.println("5. Quitter");
                System.out.print("Votre choix: ");

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
                            System.out.print("Entrez le path du fichier: ");
                            var files = new ArrayList<FileInfo>();
                            while (scanner.hasNextLine()) {
                                System.out.print("Entrez le path du fichier: ");
                                var path = scanner.nextLine();
                                if(path.isEmpty()){
                                    break;
                                }
                                var file = new FileInfo(Path.of(path));
                                files.add(file);
                                // sendCommand(new FileShare(file));
                            }
                            System.out.println("files ==> "+files);
                            sendCommand(new FileShare(files));
                            break;

                        case "4":
                            sendCommand(new GetAllFileReq());
                            break;

                        case "5":
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

    public void launch() throws IOException {
        sc.configureBlocking(false);
        var key = sc.register(selector, SelectionKey.OP_CONNECT);
        uniqueContext = new Context(key);
        key.attach(this);
        sc.connect(serverAddress);


        auth.start();
        //console.start();

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

    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
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
