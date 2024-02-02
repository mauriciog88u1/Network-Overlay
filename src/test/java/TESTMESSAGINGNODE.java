import csx55.overlay.node.MessagingNode;
import csx55.overlay.transport.TCPSender;
import csx55.overlay.wireformats.Register;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.Socket;

import static javax.management.Query.times;
import static jdk.internal.org.objectweb.asm.util.CheckClassAdapter.verify;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MessagingNodeTest {
    private MessagingNode messagingNode;
    private TCPSender sender;
    private Socket socket;

    @BeforeEach
    public void setup() {
        messagingNode = new MessagingNode("localhost", 1025);
        sender = mock(TCPSender.class);
        socket = mock(Socket.class);
    }

    @Test
    public void connectToRegistrySuccessfully() {
        try {
            when(socket.getInetAddress()).thenReturn(InetAddress.getLocalHost());
            when(socket.getPort()).thenReturn(1025);
            messagingNode.connectToRegistry();
            verify(socket, times(1)).getInetAddress();
            verify(socket, times(1)).getPort();
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
    }

    @Test
    public void registerWithRegistrySuccessfully() {
        try {
            when(socket.getInetAddress()).thenReturn(InetAddress.getLocalHost());
            when(socket.getPort()).thenReturn(1025);
            messagingNode.registerWithRegistry();
            verify(sender, times(1)).sendMessage(any());
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
    }

    @Test
    public void connectToRegistryFailsAndRetries() {
        try {
            when(socket.getInetAddress()).thenThrow(new RuntimeException());
            messagingNode.connectToRegistry();
            verify(socket, times(2)).getInetAddress();
        } catch (Exception e) {
            fail("Exception should not be thrown");
        }
    }
}

