package protocoltests.commandtests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocoltests.protocol.messages.Broadcast;
import protocoltests.protocol.messages.Login;
import protocoltests.protocol.messages.Response;
import protocoltests.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.from;
import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class BroadcastTests {
    private static Properties props = new Properties();

    private Socket socketUser1, socketUser2;
    private BufferedReader inUser1, inUser2;
    private PrintWriter outUser1, outUser2;

    private final static int max_delta_allowed_ms = 100;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = BroadcastTests.class.getResourceAsStream("../testconfig.properties");
        props.load(in);
        in.close();
    }

    @BeforeEach
    void setup() throws IOException {
        socketUser1 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        inUser1 = new BufferedReader(new InputStreamReader(socketUser1.getInputStream()));
        outUser1 = new PrintWriter(socketUser1.getOutputStream(), true);

        socketUser2 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        inUser2 = new BufferedReader(new InputStreamReader(socketUser2.getInputStream()));
        outUser2 = new PrintWriter(socketUser2.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        socketUser1.close();
        socketUser2.close();
    }

    @Test
    void TC3_2_broadcastMessageIsReceivedByOtherConnectedClients() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); // WELCOME
        receiveLineWithTimeout(inUser2); // WELCOME

        // Connect user1
        outUser1.println(Utils.objectToMessage(new Login("user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); // OK

        // Connect user2
        outUser2.println(Utils.objectToMessage(new Login("user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); // OK
        receiveLineWithTimeout(inUser1); // U2 JOINED

        //send BROADCAST from user 1
        outUser1.println(Utils.objectToMessage(new Broadcast("", "messagefromuser1")));
        outUser1.flush();
        String fromUser1 = receiveLineWithTimeout(inUser1);
        Response<String> broadcastResp1 = Utils.messageToObject(fromUser1);

        assertEquals(800, broadcastResp1.status());
        assertEquals("OK", broadcastResp1.content());
        assertEquals("BROADCAST", broadcastResp1.to());

        String fromUser2 = receiveLineWithTimeout(inUser2);
        System.out.println(fromUser2);
        Broadcast broadcast2 = Utils.messageToObject(fromUser2);
        assertEquals(new Broadcast("user1", "messagefromuser1"), broadcast2);

        //send BROADCAST from user 2
        outUser2.println(Utils.objectToMessage(new Broadcast("", "messagefromuser2")));
        outUser2.flush();
        fromUser2 = receiveLineWithTimeout(inUser2);
        Response<String> broadcastResp2 = Utils.messageToObject(fromUser2);
        assertEquals(800, broadcastResp2.status());
        assertEquals("OK", broadcastResp2.content());
        assertEquals("BROADCAST", broadcastResp2.to());

        fromUser1 = receiveLineWithTimeout(inUser1);
        System.out.println(fromUser1);
        Broadcast broadcast1 = Utils.messageToObject(fromUser1);
        assertEquals(new Broadcast("user2", "messagefromuser2"), broadcast1);
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
    }

}
