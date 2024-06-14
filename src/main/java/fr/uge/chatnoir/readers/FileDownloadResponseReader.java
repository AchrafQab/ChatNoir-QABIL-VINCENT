package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.file.FileDownloadReq;
import fr.uge.chatnoir.protocol.file.FileDownloadRes;
import fr.uge.chatnoir.protocol.file.FileInfo;

import java.io.File;
import java.nio.ByteBuffer;

public class FileDownloadResponseReader implements Reader<FileDownloadRes> {
    private enum State { DONE, WAITING_TITLE, WAITING_FILE, WAITING_ID, ERROR }

    private State state = State.WAITING_TITLE;
    private final StringReader stringReader = new StringReader();
    private final ByteTabReader byteTabReader = new ByteTabReader();

    private final IntReader intReader = new IntReader();
    private byte[] file;
    private String title;

    private int id;
    private FileDownloadRes value;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if(state == State.WAITING_TITLE){
            var read = stringReader.process(buffer);
            if (read == ProcessStatus.ERROR) {
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }
            if (read == ProcessStatus.REFILL) {
                return ProcessStatus.REFILL;
            }
            title = stringReader.get();
            System.out.println("title => "+title);
            stringReader.reset();
            state = State.WAITING_ID;
        }
        if(state == State.WAITING_ID){
            var read = intReader.process(buffer);
            if (read == ProcessStatus.ERROR) {
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }
            if (read == ProcessStatus.REFILL) {
                return ProcessStatus.REFILL;
            }
            id = intReader.get();
            intReader.reset();
            System.out.println("id => "+id);
            state = State.WAITING_FILE;
        }

        if(state == State.WAITING_FILE){
            System.out.println("waiting file ...");
            var read = byteTabReader.process(buffer);
            if (read == ProcessStatus.ERROR) {
                System.out.println("error in file download response reader");
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }
            if (read == ProcessStatus.REFILL) {
                return ProcessStatus.REFILL;
            }

            file = byteTabReader.get();

            byteTabReader.reset();
            state = State.DONE;
        }




        value = new FileDownloadRes(title, file, id);
        return ProcessStatus.DONE;
    }

    @Override
    public FileDownloadRes get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }

        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_TITLE;
        stringReader.reset();
        byteTabReader.reset();
        intReader.reset();

    }
}
