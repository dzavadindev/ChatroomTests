package protocoltests.commandtests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.*;
import org.opentest4j.AssertionFailedError;
import protocoltests.protocol.messages.Login;
import protocoltests.protocol.messages.NotFound;
import protocoltests.protocol.messages.Private;
import protocoltests.protocol.messages.Response;
import protocoltests.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class PrivateTests {

    private static Properties props = new Properties();
    private static int ping_time_ms;
    private static int ping_time_ms_delta_allowed;
    private final static int max_delta_allowed_ms = 100;

    private Socket s1, s2, s3;
    private BufferedReader in1, in2, in3;
    private PrintWriter out1, out2, out3;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = ListTests.class.getResourceAsStream("../testconfig.properties");
        props.load(in);
        assert in != null;
        in.close();

        ping_time_ms = Integer.parseInt(props.getProperty("ping_time_ms", "10000"));
        ping_time_ms_delta_allowed = Integer.parseInt(props.getProperty("ping_time_ms_delta_allowed", "100"));
    }

    @BeforeEach
    void setup() throws IOException {
        s1 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        in1 = new BufferedReader(new InputStreamReader(s1.getInputStream()));
        out1 = new PrintWriter(s1.getOutputStream(), true);

        s2 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        in2 = new BufferedReader(new InputStreamReader(s2.getInputStream()));
        out2 = new PrintWriter(s2.getOutputStream(), true);

        s3 = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        in3 = new BufferedReader(new InputStreamReader(s3.getInputStream()));
        out3 = new PrintWriter(s3.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        s1.close();
        s2.close();
        s3.close();
    }


    @Test
    void TC7_1_canSendAPrivateMessageOtherUsersCantRead() throws IOException {
        receiveLineWithTimeout(in1); // WELCOME
        receiveLineWithTimeout(in2); // WELCOME
        receiveLineWithTimeout(in3); // WELCOME

        out1.println(Utils.objectToMessage(new Login("user1")));
        out1.flush();
        receiveLineWithTimeout(in1); // OK

        out2.println(Utils.objectToMessage(new Login("user2")));
        out2.flush();
        receiveLineWithTimeout(in2); // OK
        receiveLineWithTimeout(in1); // U2 IS HERE

        out3.println(Utils.objectToMessage(new Login("user3")));
        out3.flush();
        receiveLineWithTimeout(in3); // OK
        receiveLineWithTimeout(in1); // U3 IS HERE
        receiveLineWithTimeout(in2); // U3 IS HERE

        out1.println(Utils.objectToMessage(new Private("user2", "howdy")));
        out1.flush();
        Response<String> res = Utils.messageToObject(receiveLineWithTimeout(in1));
        assertEquals("OK", res.content());
        assertEquals("PRIVATE", res.to());
        assertEquals(800, res.status());

        Private privateMessage = Utils.messageToObject(receiveLineWithTimeout(in2));
        assertEquals("howdy", privateMessage.message());
        assertEquals("user1", privateMessage.username());
        assertThrows(AssertionFailedError.class, () -> receiveLineWithTimeout(in1));
    }

    @Test
    void TC7_2_cantSendAPrivateWhenNotLoggedIn() throws JsonProcessingException {
        receiveLineWithTimeout(in1); // WELCOME
        receiveLineWithTimeout(in2); // WELCOME

        out2.println(Utils.objectToMessage(new Login("user2")));
        out2.flush();
        receiveLineWithTimeout(in2); // OK

        out1.println(Utils.objectToMessage(new Private("user2", "howdy")));
        out1.flush();
        Response<String> res = Utils.messageToObject(receiveLineWithTimeout(in1));
        assertEquals("ERROR", res.content());
        assertEquals("LOGIN", res.to());
        assertEquals(710, res.status());
    }

    @Test
    void TC7_3_cantSendPrivateMessageToYourself() throws JsonProcessingException {
        receiveLineWithTimeout(in1); // WELCOME

        out1.println(Utils.objectToMessage(new Login("user1")));
        out1.flush();
        receiveLineWithTimeout(in1); // OK

        out1.println(Utils.objectToMessage(new Private("user1", "howdy")));
        out1.flush();
        Response<String> res = Utils.messageToObject(receiveLineWithTimeout(in1));
        assertEquals("ERROR", res.content());
        assertEquals("PRIVATE", res.to());
        assertEquals(822, res.status());
    }

    @Test
    void TC7_4_sendingAPrivateMessageToNonExistentUser() throws JsonProcessingException {
        receiveLineWithTimeout(in1); // WELCOME
        receiveLineWithTimeout(in2); // WELCOME

        out1.println(Utils.objectToMessage(new Login("user1")));
        out1.flush();
        receiveLineWithTimeout(in1); // OK

        out1.println(Utils.objectToMessage(new Private("peter", "howdy")));
        out1.flush();
        Response<String> res = Utils.messageToObject(receiveLineWithTimeout(in1));
        assertEquals(new NotFound("receiver", "peter"), Utils.jsonToObject(res.content(), NotFound.class));
        assertEquals("PRIVATE", res.to());
        assertEquals(711, res.status());
    }


    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
    }


}