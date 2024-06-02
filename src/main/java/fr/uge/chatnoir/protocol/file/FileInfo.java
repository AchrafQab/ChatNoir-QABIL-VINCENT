package fr.uge.chatnoir.protocol.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

public class FileInfo {

    public final String title;
    public final String hash;
    public final int size;

    public FileInfo(Path path) throws NoSuchAlgorithmException, IOException {
        Objects.requireNonNull(path);
        this.title = path.getFileName().toString();
        this.hash = calculateSHA256(path);
        this.size = (int) path.toFile().length();
    }

    public FileInfo(String title, String hash, int size) {
        Objects.requireNonNull(title);
        Objects.requireNonNull(hash);
        this.title = title;
        this.hash = hash;
        this.size = size;
    }


    @Override
    public int hashCode() {
        return Objects.hash(title, hash, size);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo = (FileInfo) o;
        return size == fileInfo.size && title.equals(fileInfo.title) && hash.equals(fileInfo.hash);
    }

    private String calculateSHA256(Path path) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileContentBytes = Files.readAllBytes(path);
        byte[] hash = digest.digest(fileContentBytes);
        return Base64.getEncoder().encodeToString(hash);
    }

    @Override
    public String toString() {
        return "File{" +
                "title='" + title + '\'' +
                ", hash='" + hash + '\'' +
                ", size=" + size +
                '}';
    }

    public ByteBuffer toByteBuffer(Charset charset) {
        var bufferTitle = charset.encode(title);
        var bufferHash = charset.encode(hash);
        var buffer = ByteBuffer.allocate(3 * Integer.BYTES + bufferTitle.remaining() + bufferHash.remaining());
        buffer.putInt(bufferTitle.remaining());
        buffer.put(bufferTitle);
        buffer.putInt(bufferHash.remaining());
        buffer.put(bufferHash);
        buffer.putInt(size);
        buffer.flip();
        return buffer;
    }
}
