
import csx55.overlay.transport.TCPSender;
import csx55.overlay.wireformats.Register;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestRegister {

    @Test
    public void testSerializationAndDeserialization() throws IOException {
        // Original Register object
        String ipAddress = "127.0.0.1";
        int port = 8080;
        Register originalRegister = new Register("local",ipAddress, port);

        byte[] serializedData = originalRegister.getBytes();

        System.out.println("Serialized data: " + Arrays.toString(serializedData));

        Register deserializedRegister = new Register(serializedData);
        System.out.println("Deserialized Register: " + deserializedRegister.getType());

        assertEquals(originalRegister.getIpAddress(), deserializedRegister.getIpAddress(), "IP Addresses should match");
        assertEquals(originalRegister.getPort(), deserializedRegister.getPort(), "Ports should match");
    }

    @Test
    public void testIncorrectMessageTypeThrowsException() {
        byte[] incorrectData = new byte[] {0, 0, 0, 2};

        assertThrows(IllegalArgumentException.class, () -> new Register(incorrectData), "Should throw IllegalArgumentException for incorrect message type");
    }
}
