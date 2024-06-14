package fr.uge.chatnoir.protocol.file;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

public record FileDownloadRes(String title, byte[] content, Integer id, Integer protocol) implements Trame {


    public FileDownloadRes(String title, byte[] content, Integer id){
      this(title, content, id, ChatMessageProtocol.FILE_DOWNLOAD_RESPONSE);
    }

    @Override
    public ByteBuffer toByteBuffer(Charset charset) {

        ByteBuffer titleBuffer = charset.encode(title);
        ByteBuffer fileBuffer = ByteBuffer.wrap(content);


        ByteBuffer buffer = ByteBuffer.allocate(3 * Integer.BYTES + titleBuffer.remaining() + fileBuffer.remaining());
        buffer.putInt(titleBuffer.remaining());
        buffer.put(titleBuffer);
        buffer.putInt(id);
        System.out.println("fileBuffer.remaining() => "+fileBuffer.remaining() );
        buffer.putInt(fileBuffer.remaining());
        buffer.put(fileBuffer);
        System.out.println("id => "+id);

        buffer.flip();

        return buffer;
    }
}
