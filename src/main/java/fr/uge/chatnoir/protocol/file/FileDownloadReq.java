package fr.uge.chatnoir.protocol.file;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record FileDownloadReq(FileInfo fileInfo,Integer sectionStart, Integer sectionEnd, Integer id, Integer protocol) implements Trame {


    public FileDownloadReq(FileInfo fileInfo, Integer sectionStart, Integer sectionEnd, Integer id){
      this(fileInfo, sectionStart, sectionEnd, id, ChatMessageProtocol.FILE_DOWNLOAD_REQUEST);
    }



    @Override
    public ByteBuffer toByteBuffer(Charset charset) {


        ByteBuffer fileBuffer = fileInfo.toByteBuffer(charset);

        var buffer = ByteBuffer.allocate(fileBuffer.remaining() + Integer.BYTES * 4);



        buffer.put(fileBuffer);
        buffer.putInt(sectionStart);
        buffer.putInt(sectionEnd);
        buffer.putInt(id);

        buffer.flip();
        return buffer;
    }
}
