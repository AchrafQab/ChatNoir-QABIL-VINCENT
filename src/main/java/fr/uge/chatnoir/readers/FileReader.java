package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.file.FileInfo;

import java.nio.ByteBuffer;

public class FileReader implements Reader<FileInfo>{
    private enum State { DONE, WAITING_TITLE, WAITING_HASH, WAITING_SIZE, ERROR }

    private FileReader.State state = FileReader.State.WAITING_TITLE;
    private final StringReader stringReader = new StringReader();
    private final IntReader intReader = new IntReader();
    private String title;
    private String hash;
    private int size;
    private FileInfo value;

    @Override
    public Reader.ProcessStatus process(ByteBuffer buffer) {
        if (state == FileReader.State.DONE || state == FileReader.State.ERROR) {
            throw new IllegalStateException();
        }


        if(state == FileReader.State.WAITING_TITLE){
            var read = stringReader.process(buffer);
            if(read == Reader.ProcessStatus.ERROR){
                state= FileReader.State.ERROR;
                return Reader.ProcessStatus.ERROR;
            }
            if(read == Reader.ProcessStatus.REFILL){
                return Reader.ProcessStatus.REFILL;
            }

            title = stringReader.get();
            stringReader.reset();
            state = FileReader.State.WAITING_HASH;

        }

        if(state == FileReader.State.WAITING_HASH){
            var read = stringReader.process(buffer);
            if(read == Reader.ProcessStatus.ERROR){
                state= FileReader.State.ERROR;
                return Reader.ProcessStatus.ERROR;
            }
            if(read == Reader.ProcessStatus.REFILL){
                return Reader.ProcessStatus.REFILL;
            }

            hash = stringReader.get();
            stringReader.reset();
            state = FileReader.State.WAITING_SIZE;

        }

        if(state == FileReader.State.WAITING_SIZE){
            var read = intReader.process(buffer);
            if(read == Reader.ProcessStatus.ERROR){
                state= FileReader.State.ERROR;
                return Reader.ProcessStatus.ERROR;
            }
            if(read == Reader.ProcessStatus.REFILL){
                return Reader.ProcessStatus.REFILL;
            }

            size = intReader.get();
            intReader.reset();
            state = FileReader.State.DONE;

        }

        value = new FileInfo(title, hash, size);

        return Reader.ProcessStatus.DONE;
    }

    @Override
    public FileInfo get() {
        if (state != FileReader.State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = FileReader.State.WAITING_TITLE;
        intReader.reset();
        stringReader.reset();
    }

}
