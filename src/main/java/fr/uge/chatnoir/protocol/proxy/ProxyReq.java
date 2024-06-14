package fr.uge.chatnoir.protocol.proxy;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record ProxyReq(String ipSource, String ipDest, Integer portDest, Integer id,Integer protocol) implements Trame {
    public ProxyReq(String ipSource, String ipDest, Integer portDest, Integer id) {
        this(ipSource, ipDest, portDest, id, ChatMessageProtocol.PROXY_REQUEST);
    }

    public ByteBuffer toByteBuffer(Charset charset) {
        var bufferIpSource = charset.encode(ipSource);
        var bufferIpDest = charset.encode(ipDest);
        var buffer = ByteBuffer.allocate(4*Integer.BYTES + bufferIpSource.remaining() + bufferIpDest.remaining());
        buffer.putInt(bufferIpSource.remaining());
        buffer.put(bufferIpSource);
        buffer.putInt(bufferIpDest.remaining());
        buffer.put(bufferIpDest);
        buffer.putInt(portDest);
        buffer.putInt(id);
        buffer.flip();

        return buffer;
    }
}