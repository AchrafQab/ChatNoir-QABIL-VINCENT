package fr.uge.chatnoir.server;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public interface Frame {
    ByteBuffer toByteBuffer(Charset charset);
}
