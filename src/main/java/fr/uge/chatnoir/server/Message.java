package fr.uge.chatnoir.server;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

public record Message(String login, String text) {

    public Message(String login, String text) {
        this.login = Objects.requireNonNull(login);
        this.text = Objects.requireNonNull(text);
    }

    public ByteBuffer toByteBuffer(Charset charset) {
        var bufferLogin = charset.encode(login);
        var bufferMessage = charset.encode(text);
        var buffer = ByteBuffer.allocate(2*Integer.BYTES + bufferLogin.remaining() + bufferMessage.remaining());
        buffer.putInt(bufferLogin.remaining());
        buffer.put(bufferLogin);
        buffer.putInt(bufferMessage.remaining());
        buffer.put(bufferMessage);
        buffer.flip();
        return buffer;
    }

}
