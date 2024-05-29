package fr.uge.chatnoir.protocol.message;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record PrivateMessage(String message, String nickname, Integer protocol) implements Trame {

    public PrivateMessage(String message, String nickname) {
        this(message, nickname, ChatMessageProtocol.PRIVATE_MESSAGE);
    }

    public ByteBuffer toByteBuffer(Charset charset) {
        var bufferLogin = charset.encode(nickname);
        var bufferMessage = charset.encode(message);
        var buffer = ByteBuffer.allocate(2*Integer.BYTES + bufferLogin.remaining() + bufferMessage.remaining());
        buffer.putInt(bufferLogin.remaining());
        buffer.put(bufferLogin);
        buffer.putInt(bufferMessage.remaining());
        buffer.put(bufferMessage);
        buffer.flip();
        return buffer;
    }

}
