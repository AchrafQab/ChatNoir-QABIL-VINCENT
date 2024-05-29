package fr.uge.chatnoir.protocol.auth;


import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record AuthResTrame(Integer code, Integer protocol) implements Trame {

    public AuthResTrame(Integer code) {
        this(code, ChatMessageProtocol.AUTH_RESPONSE);
    }

    public ByteBuffer toByteBuffer(Charset charset) {
        var buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(code);
        buffer.flip();

        return buffer;
    }

}

