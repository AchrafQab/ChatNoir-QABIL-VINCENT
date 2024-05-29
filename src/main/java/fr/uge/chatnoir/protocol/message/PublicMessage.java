package fr.uge.chatnoir.protocol.message;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record PublicMessage(String message, Integer protocol) implements Trame {

    public PublicMessage(String message) {
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
