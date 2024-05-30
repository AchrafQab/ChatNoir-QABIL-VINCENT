package fr.uge.chatnoir.protocol.file;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record GetAllFileReq(Integer protocol) implements Trame {

    public GetAllFileReq() {
        this(ChatMessageProtocol.FILE_LIST_REQUEST);
    }

    @Override
    public ByteBuffer toByteBuffer(Charset charset) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(protocol).flip();
    }
}
