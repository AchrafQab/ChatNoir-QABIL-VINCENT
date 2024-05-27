package fr.uge.chatnoir.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public interface Trame {

    Integer protocol();


    ByteBuffer toByteBuffer(Charset charset);
}
