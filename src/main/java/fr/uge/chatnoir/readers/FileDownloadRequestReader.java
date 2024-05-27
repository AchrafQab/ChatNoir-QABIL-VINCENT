package fr.uge.chatnoir.readers;

import fr.uge.chatnoir.protocol.Reader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FileDownloadRequestReader implements Reader<String> {
    private enum State { DONE, WAITING_MODE, WAITING_FILENAME, WAITING_RESPONSE, WAITING_LENGTH, WAITING_NUM_CLIENTS, WAITING_IP_PORT, ERROR }

    private State state = State.WAITING_MODE;
    private final IntReader intReader = new IntReader();
    private final StringReader stringReader = new StringReader();
    private int mode;
    private String fileName;
    private int responseCode;
    private int fileLength;
    private int numClients;
    private final List<ClientInfo> clients = new ArrayList<>();

    private static class ClientInfo {
        String ip;
        int port;

        ClientInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        switch (state) {
            case WAITING_MODE:
                if (intReader.process(buffer) == ProcessStatus.DONE) {
                    mode = intReader.get();
                    stringReader.reset();
                    state = State.WAITING_FILENAME;
                } else {
                    return ProcessStatus.REFILL;
                }
                break;

            case WAITING_FILENAME:
                if (stringReader.process(buffer) == ProcessStatus.DONE) {
                    fileName = stringReader.get();
                    state = State.WAITING_RESPONSE;
                    intReader.reset();
                } else {
                    return ProcessStatus.REFILL;
                }
                break;

            case WAITING_RESPONSE:
                if (intReader.process(buffer) == ProcessStatus.DONE) {
                    responseCode = intReader.get();
                    if (responseCode != 200) {
                        state = State.DONE;
                        return ProcessStatus.DONE;
                    }
                    state = State.WAITING_LENGTH;
                } else {
                    return ProcessStatus.REFILL;
                }
                break;

            case WAITING_LENGTH:
                if (intReader.process(buffer) == ProcessStatus.DONE) {
                    fileLength = intReader.get();
                    state = State.WAITING_NUM_CLIENTS;
                } else {
                    return ProcessStatus.REFILL;
                }
                break;

            case WAITING_NUM_CLIENTS:
                if (intReader.process(buffer) == ProcessStatus.DONE) {
                    numClients = intReader.get();
                    state = State.WAITING_IP_PORT;
                } else {
                    return ProcessStatus.REFILL;
                }
                break;

            case WAITING_IP_PORT:
                while (clients.size() < numClients) {
                    StringReader ipReader = new StringReader();
                    IntReader portReader = new IntReader();
                    if (ipReader.process(buffer) == ProcessStatus.DONE && portReader.process(buffer) == ProcessStatus.DONE) {
                        clients.add(new ClientInfo(ipReader.get(), portReader.get()));
                    } else {
                        return ProcessStatus.REFILL;
                    }
                }
                state = State.DONE;
                break;

            default:
                throw new IllegalStateException();
        }

        return ProcessStatus.DONE;
    }

    @Override
    public String get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Mode: ").append(mode).append(", Filename: ").append(fileName).append(", Response: ").append(responseCode)
                .append(", File Length: ").append(fileLength).append(", Clients: ");
        for (ClientInfo client : clients) {
            sb.append(client.ip).append(":").append(client.port).append(" ");
        }
        return sb.toString();
    }

    @Override
    public void reset() {
        state = State.WAITING_MODE;
        intReader.reset();
        stringReader.reset();
        mode = 0;
        fileName = null;
        responseCode = 0;
        fileLength = 0;
        numClients = 0;
        clients.clear();
    }
}
