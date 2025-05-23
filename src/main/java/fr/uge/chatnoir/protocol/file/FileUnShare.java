package fr.uge.chatnoir.protocol.file;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;


public record FileUnShare(List<FileInfo> fileInfos, Integer protocol) implements Trame {

    public FileUnShare(List<FileInfo> fileInfos){
        this(fileInfos, ChatMessageProtocol.FILE_UNSHARE);
    }

    @Override
    public ByteBuffer toByteBuffer(Charset charset) {
        int totalSize = Integer.BYTES;

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

        buffer.flip();
        return buffer;
    }
}
