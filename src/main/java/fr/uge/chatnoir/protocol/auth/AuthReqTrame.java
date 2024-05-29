package fr.uge.chatnoir.protocol.auth;


import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record AuthReqTrame(String login, Integer protocol) implements Trame {

    public AuthReqTrame(String login) {
        this(login, ChatMessageProtocol.AUTH_REQUEST);
    }

    public ByteBuffer toByteBuffer(Charset charset) {
        var bufferLogin = charset.encode(login);
        var buffer = ByteBuffer.allocate(Integer.BYTES + bufferLogin.remaining());
        buffer.putInt(bufferLogin.remaining());
        buffer.put(bufferLogin);
        buffer.flip();

        return buffer;
    }

}

