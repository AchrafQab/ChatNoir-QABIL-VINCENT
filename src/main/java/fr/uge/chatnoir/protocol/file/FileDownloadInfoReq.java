package fr.uge.chatnoir.protocol.file;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record FileDownloadInfoReq(FileInfo fileInfo, Integer downloadMode, Integer protocol) implements Trame {


    public FileDownloadInfoReq(FileInfo fileInfo, Integer downloadMode){
      this(fileInfo, downloadMode, ChatMessageProtocol.FILE_DOWNLOAD_INFO_REQUEST);
    }



    @Override
    public ByteBuffer toByteBuffer(Charset charset) {


        ByteBuffer fileBuffer = fileInfo.toByteBuffer(charset);

        var buffer = ByteBuffer.allocate(fileBuffer.remaining() + Integer.BYTES);

        buffer.putInt(downloadMode);

        buffer.put(fileBuffer);

        buffer.flip();
        return buffer;
    }
}
