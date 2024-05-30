package fr.uge.chatnoir.protocol.file;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;
import fr.uge.chatnoir.server.ClientSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

public record FileDownloadRes(List<String> ips, Integer protocol) implements Trame {


    public FileDownloadRes(List<String> ips){
      this(ips, ChatMessageProtocol.FILE_DOWNLOAD_RESPONSE);
    }

    @Override
    public ByteBuffer toByteBuffer(Charset charset) {
        int totalSize = Integer.BYTES;

        for (String ip : ips) {
            totalSize += Integer.BYTES + charset.encode(ip).remaining();
        }

        var buffer = ByteBuffer.allocate(totalSize);

        buffer.putInt(ips.size());

        for (String ip : ips) {

            var bufferIp = charset.encode(ip);
            buffer.putInt(bufferIp.remaining());
            buffer.put(bufferIp);
        }
        //read buffer
        buffer.flip();


        return buffer;
    }
}
