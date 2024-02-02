import csx55.overlay.node.MessagingNode;
import csx55.overlay.transport.TCPSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.InetAddress;
import java.net.Socket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MessagingNodeTest {
    private MessagingNode messagingNode;
    private TCPSender mockSender;
    private Socket mockSocket;

    @BeforeEach
    void setUp() throws Exception {
        // Mock dependencies
        mockSender = mock(TCPSender.class);
        mockSocket = mock(Socket.class);

        // Assuming MessagingNode can accept mocked objects or using Reflection to inject them
        messagingNode = new MessagingNode("0.0.0.0", 6969);

        // Inject mocks
        setInternalState(messagingNode, "sender", mockSender);
        setInternalState(messagingNode, "socket", mockSocket);
    }

    @Test
    void testConnectToRegistry_Success() throws Exception {
        // Configure the socket to simulate a successful connection
        when(mockSocket.getInetAddress()).thenReturn(InetAddress.getLocalHost());
        when(mockSocket.getPort()).thenReturn(1025);

        messagingNode.connectToRegistry();

        verify(mockSocket, times(1)).connect(any(), eq(1025));
          verify(mockSender, times(1)).sendMessage(any(byte[].class));

    }

    @Test
    void testConnectToRegistry_RetrySuccess() throws Exception {


    }

    @Test
    void testRegisterWithRegistry_Success() throws Exception {
        when(mockSocket.getInetAddress()).thenReturn(InetAddress.getLocalHost());
        when(mockSocket.getPort()).thenReturn(1025);

        messagingNode.registerWithRegistry();

        verify(mockSender, times(1)).sendMessage(any(byte[].class));
    }

    // Utility method to set private fields, assuming such utility is available or using Reflection
    private void setInternalState(Object target, String fieldName, Object value) {
        // Implementation using Reflection to set the private field
    }
}
