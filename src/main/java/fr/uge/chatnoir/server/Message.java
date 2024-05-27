package fr.uge.chatnoir.server;

import java.nio.ByteBuffer;
import java.util.Objects;

public class Message {
    private final String login;
    private final String text;

    public Message(String login, String text) {
        this.login = Objects.requireNonNull(login);
        this.text = Objects.requireNonNull(text);
    }

    public ByteBuffer toByteBuffer() {
        // Implémentez la méthode pour convertir le message en ByteBuffer
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + login.length() + Integer.BYTES + text.length());
        buffer.putInt(login.length()).put(login.getBytes()).putInt(text.length()).put(text.getBytes());
        buffer.flip();
        return buffer;
    }

    public String getLogin() {
        return login;
    }

    public String getText() {
        return text;
    }
}
