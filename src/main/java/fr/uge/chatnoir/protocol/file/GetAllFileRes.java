package fr.uge.chatnoir.protocol.file;

import fr.uge.chatnoir.protocol.ChatMessageProtocol;
import fr.uge.chatnoir.protocol.Trame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

public record GetAllFileRes(List<FileInfo> files, Integer protocol) implements Trame {

    public GetAllFileRes(List<FileInfo> files) {
        this(files, ChatMessageProtocol.FILE_LIST_RESPONSE);
    }

    @Override
    public String toString() {
        //pretty print the list of files in array format with index
        // file 1 : title | size bytes

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < files.size(); i++) {
            FileInfo fileInfo = files.get(i);
            sb.append("file ").append(i + 1).append(" : ").append(fileInfo.title).append(" | ").append(fileInfo.size).append(" bytes\n");
        }
        return sb.toString();

    }



    @Override
    public ByteBuffer toByteBuffer(Charset charset) {
        int totalSize = Integer.BYTES;

        for (FileInfo fileInfo : files) {
            ByteBuffer fileBuffer = fileInfo.toByteBuffer(charset);
            totalSize += fileBuffer.remaining();
        }
        var buffer = ByteBuffer.allocate(totalSize);

        buffer.putInt(files.size());

        for (FileInfo fileInfo : files) {
            ByteBuffer fileBuffer = fileInfo.toByteBuffer(charset);
            buffer.put(fileBuffer);
        }
        buffer.flip();
        return buffer;
    }
}
