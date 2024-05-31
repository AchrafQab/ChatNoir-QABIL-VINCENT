package fr.uge.chatnoir.protocol.file;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

public record FileShare(List<FileInfo> fileInfos,Integer port, Integer protocol) implements Trame {


    public FileShare(List<FileInfo> fileInfos, Integer port){
      this(fileInfos, port, ChatMessageProtocol.FILE_SHARE);
    }



    @Override
    public ByteBuffer toByteBuffer(Charset charset) {
        int totalSize = Integer.BYTES * 2;

        for (FileInfo fileInfo : fileInfos) {
            ByteBuffer fileBuffer = fileInfo.toByteBuffer(charset);
            totalSize += fileBuffer.remaining();
        }
        var buffer = ByteBuffer.allocate(totalSize);

        buffer.putInt(fileInfos.size());

        for (FileInfo fileInfo : fileInfos) {
            ByteBuffer fileBuffer = fileInfo.toByteBuffer(charset);
            buffer.put(fileBuffer);
        }
        buffer.putInt(port);

        buffer.flip();
        return buffer;
    }
}
