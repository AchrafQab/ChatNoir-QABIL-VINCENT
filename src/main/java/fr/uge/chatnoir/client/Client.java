package fr.uge.chatnoir.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {

    private static final int PORT = 2002;
    private static final String HOST = "localhost";
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private final SocketChannel sc;
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final String nickname;

    public Client(String nickname) throws IOException {
        this.sc = SocketChannel.open(new InetSocketAddress(HOST, PORT));
        this.nickname = nickname;
    }

    public void start() throws IOException {
        authenticate();
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String message = scanner.nextLine();
                sendMessage(message);
                readResponse();
            }
        }
    }

    private void authenticate() throws IOException {
        buffer.clear();
        buffer.putInt(nickname.length());
        buffer.put(nickname.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        sc.write(buffer);
        logger.info("Authenticated with nickname: " + nickname);
    }

    public void sendMessage(String message) throws IOException {
        buffer.clear();
        buffer.putInt(message.length());
        buffer.put(message.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        sc.write(buffer);
        logger.info("Message sent: " + message);
    }

    public void readResponse() throws IOException {
        buffer.clear();
        int bytesRead = sc.read(buffer);
        if (bytesRead == -1) {
            logger.warning("Connection closed by server.");
            sc.close();
            return;
        }
        buffer.flip();
        String response = StandardCharsets.UTF_8.decode(buffer).toString();
        logger.info("Response received: " + response);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java Client <nickname>");
            return;
        }
        new Client(args[0]).start();
    }
}
