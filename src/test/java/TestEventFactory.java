
import csx55.overlay.wireformats.Event;
import csx55.overlay.wireformats.EventFactory;
import csx55.overlay.wireformats.Protocol;
import csx55.overlay.wireformats.Register;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TestEventFactory {

    @Test
    public void testCreateRegisterEvent() {
        // Prepare a byte array representing a Register event
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            dos.writeInt(Protocol.REGISTER_REQUEST); // Event type for REGISTER_REQUEST
            String testIp = "127.0.0.1";
            byte[] ipBytes = testIp.getBytes();
            dos.writeInt(ipBytes.length); // Length of IP address
            dos.write(ipBytes); // IP address bytes
            int testPort = 8000;
            dos.writeInt(testPort); // Port
            dos.flush();

            byte[] testData = baos.toByteArray();

            // Use the EventFactory to create an event from the byte array
            Event event = EventFactory.createEvent(testData);

            // Assert the event is of the correct type and contains the correct data
            assertTrue(event instanceof Register, "Event should be an instance of Register");
            Register registerEvent = (Register) event;
            assertTrue(registerEvent.getIpAddress().equals(testIp), "IP address should match");
            assertTrue(registerEvent.getPort() == testPort, "Port should match");

        } catch (IOException e) {
            fail("IOException should not occur");
        }
    }
}
