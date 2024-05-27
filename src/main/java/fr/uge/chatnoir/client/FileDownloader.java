package fr.uge.chatnoir.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileDownloader {

    public void downloadFile(String fileName, String sourceAddress, int sourcePort) throws IOException {
        try (SocketChannel sc = SocketChannel.open(new InetSocketAddress(sourceAddress, sourcePort))) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.putInt(fileName.length());
            buffer.put(fileName.getBytes());
            buffer.flip();
            sc.write(buffer);

            buffer.clear();
            sc.read(buffer);
            buffer.flip();

            byte[] fileContent = new byte[buffer.remaining()];
            buffer.get(fileContent);
            saveFile(fileName, fileContent);
        }
    }

    /*
     *
     */
    public void downloadFileHidden(String fileName, String sourceAddress, int sourcePort, String proxyAddress, int proxyPort) throws IOException {
    }

    private void saveFile(String fileName, byte[] data) throws IOException {
        Files.write(Path.of(fileName), data);
    }
}
