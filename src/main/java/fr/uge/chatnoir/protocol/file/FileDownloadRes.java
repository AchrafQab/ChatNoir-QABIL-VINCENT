package fr.uge.chatnoir.protocol.file;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

public record FileDownloadRes(String title,String content,Integer protocol) implements Trame {


    public FileDownloadRes( String title, String content){
      this(title, content, ChatMessageProtocol.FILE_DOWNLOAD_RESPONSE);
    }

    @Override
    public ByteBuffer toByteBuffer(Charset charset) {

        ByteBuffer titleBuffer = charset.encode(title);
        ByteBuffer fileBuffer = charset.encode(content);

        ByteBuffer buffer = ByteBuffer.allocate(2 * Integer.BYTES + titleBuffer.remaining() + fileBuffer.remaining());
        buffer.putInt(titleBuffer.remaining());
        buffer.put(titleBuffer);
        buffer.putInt(fileBuffer.remaining());
        buffer.put(fileBuffer);

        buffer.flip();

        return buffer;
    }
}
