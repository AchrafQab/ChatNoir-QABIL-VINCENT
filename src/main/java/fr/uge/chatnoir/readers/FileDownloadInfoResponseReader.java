package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.file.FileDownloadInfoRes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FileDownloadInfoResponseReader implements Reader<FileDownloadInfoRes> {
    private enum State { DONE, WAITING_IP_NUMBER, WAITING_IPS, WAITING_ID,  ERROR }
    private State state = State.WAITING_IP_NUMBER;
    private final IntReader intReader = new IntReader();
    private final StringReader stringReader = new StringReader();
    private int ipsNumber;
    private List<String> ips = new ArrayList<String>();

    private int id;

    private FileDownloadInfoRes value;


    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if (state == State.WAITING_IP_NUMBER) {
            var read = intReader.process(buffer);
            if (read == ProcessStatus.ERROR) {

                state = State.ERROR;
                return ProcessStatus.ERROR;
            }
            if (read == ProcessStatus.REFILL) {
                return ProcessStatus.REFILL;
            }

            ipsNumber = intReader.get();
            intReader.reset();
            state = State.WAITING_IPS;
        }

        if (state == State.WAITING_IPS) {

            for (int i = 0; i < ipsNumber; i++) {
                var read = stringReader.process(buffer);
                if (read == ProcessStatus.ERROR) {
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
                }
                if (read == ProcessStatus.REFILL) {
                    return ProcessStatus.REFILL;
                }

                ips.add(stringReader.get());
                stringReader.reset();
                state = State.WAITING_ID;
            }
        }

        if (state == State.WAITING_ID) {
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
            state = State.DONE;
        }

        value = new FileDownloadInfoRes(ips, id);
        return ProcessStatus.DONE;
    }

    @Override
    public FileDownloadInfoRes get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }

        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_IP_NUMBER;
        intReader.reset();
        stringReader.reset();
        ipsNumber = 0;
        ips.clear();
    }
}
