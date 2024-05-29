package fr.uge.chatnoir.server;

import fr.uge.chatnoir.protocol.message.PublicMessage;
import org.junit.jupiter.api.Test;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientSessionTest {

    @Test
    void testHandleAuthRequest() throws Exception {
        Server server = mock(Server.class);
        SelectionKey key = mock(SelectionKey.class);
        SocketChannel sc = mock(SocketChannel.class);

        ClientSession session = new ClientSession(server, key, sc);
        session.handleAuthRequest("testUser");

        verify(server).registerClient(eq("testUser"), eq(session));
        assertEquals("testUser", session.getNickname());
    }

    @Test
    void testHandlePrivateMessage() throws Exception {
        Server server = mock(Server.class);
        SelectionKey key = mock(SelectionKey.class);
        SocketChannel sc = mock(SocketChannel.class);

        ClientSession session = new ClientSession(server, key, sc);
        session.handlePrivateMessage("recipient", "message");

        verify(server).sendPrivateMessage(eq("message"), eq("testUser"), eq("recipient"));
    }

    @Test
    void testQueueMessage() throws Exception {
        Server server = mock(Server.class);
        SelectionKey key = mock(SelectionKey.class);
        SocketChannel sc = mock(SocketChannel.class);

        ClientSession session = new ClientSession(server, key, sc);
        PublicMessage publicMessage = new PublicMessage("sender", "test message");

        session.queueMessage(publicMessage);
        Queue<PublicMessage> publicMessages = session.getMessages();
        assertEquals(1, publicMessages.size());
    }
}
