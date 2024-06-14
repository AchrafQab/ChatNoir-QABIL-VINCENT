package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;
import fr.uge.chatnoir.protocol.file.FileDownloadReq;
import fr.uge.chatnoir.protocol.file.FileInfo;
import fr.uge.chatnoir.protocol.proxy.ProxyReq;

import java.nio.ByteBuffer;

public class ProxyReqReader implements Reader<ProxyReq> {
    private enum State { DONE, WAITING_IP_SOURCE, WAITING_IP_DEST, WAITING_PORT_DEST, WAITING_ID, ERROR }

    private State state = State.WAITING_IP_SOURCE;
    private final IntReader intReader = new IntReader();
    private final StringReader stringReader = new StringReader();
    private String ipSource;
    private String ipDest;

    private int id;
    private int portDest;
    private ProxyReq value;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if (state == State.WAITING_IP_SOURCE) {
            var read = stringReader.process(buffer);
            if (read == ProcessStatus.ERROR) {
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }
            if (read == ProcessStatus.REFILL) {
                return ProcessStatus.REFILL;
            }

            ipSource = stringReader.get();
            stringReader.reset();
            state = State.WAITING_IP_DEST;
        }

        if (state == State.WAITING_IP_DEST) {
            var read = stringReader.process(buffer);
            if (read == ProcessStatus.ERROR) {
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }
            if (read == ProcessStatus.REFILL) {
                return ProcessStatus.REFILL;
            }

            ipDest = stringReader.get();
            stringReader.reset();
            state = State.WAITING_PORT_DEST;
        }

        if (state == State.WAITING_PORT_DEST) {
            var read = intReader.process(buffer);
            if (read == ProcessStatus.ERROR) {
                state = State.ERROR;
                return ProcessStatus.ERROR;
            }
            if (read == ProcessStatus.REFILL) {
                return ProcessStatus.REFILL;
            }

            portDest = intReader.get();
            intReader.reset();
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
            state = State.DONE;
        }

        value = new ProxyReq(ipSource, ipDest, portDest, id);
        return ProcessStatus.DONE;
    }

    @Override
    public ProxyReq get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }

        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING_IP_SOURCE;
        intReader.reset();
        stringReader.reset();

    }
}
