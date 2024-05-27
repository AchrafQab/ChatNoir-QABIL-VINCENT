package fr.uge.chatnoir.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record Message(String message, Integer protocol) implements Trame {

    public Message(String message) {
        this(message, ChatMessageProtocol.PUBLIC_MESSAGE);
    }

    public ByteBuffer toByteBuffer(Charset charset) {
        var bufferMessage = charset.encode(message);
        var buffer = ByteBuffer.allocate(Integer.BYTES + bufferMessage.remaining());

        buffer.putInt(bufferMessage.remaining());
        buffer.put(bufferMessage);
        buffer.flip();
        return buffer;
    }

}
