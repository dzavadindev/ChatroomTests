package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.*;
import protocoltests.protocol.messages.*;
import protocoltests.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class GeneralTests {

    private static Properties props = new Properties();
    private static int ping_time_ms;
    private static int ping_time_ms_delta_allowed;
    private final static int max_delta_allowed_ms = 300;

    private Socket s;
    private BufferedReader in;
    private PrintWriter out;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = GeneralTests.class.getResourceAsStream("testconfig.properties");
        props.load(in);
        assert in != null;
        in.close();

        ping_time_ms = Integer.parseInt(props.getProperty("ping_time_ms", "10000"));
        ping_time_ms_delta_allowed = Integer.parseInt(props.getProperty("ping_time_ms_delta_allowed", "100"));
    }

    @BeforeEach
    void setup() throws IOException {
        s = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        out = new PrintWriter(s.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        s.close();
    }

    @Test
    void TC5_1_initialConnectionToServerReturnsWelcomeMessage() throws JsonProcessingException {
        String firstLine = receiveLineWithTimeout(in);
        Welcome welcome = Utils.messageToObject(firstLine);
        assertEquals(new Welcome("Welcome to the chatroom! Please login to start chatting!"), welcome);
    }

    @Test
    void TC5_2_invalidJsonMessageReturnsParseError() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println("LOGIN {\"}");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        ParseError parseError = Utils.messageToObject(serverResponse);
        assertNotNull(parseError);
    }

    @Test
    void TC5_3_emptyJsonMessageReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println("LOGIN ");
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        Response<String> loginResp = Utils.messageToObject(serverResponse);
        assertEquals(811, loginResp.status());
        assertEquals("ERROR", loginResp.content());
        assertEquals("LOGIN", loginResp.to());
    }

    @Test
    void TC5_4_pongWithoutPingReturnsErrorMessage() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        out.println(Utils.objectToMessage(new Pong()));
        out.flush();
        String serverResponse = receiveLineWithTimeout(in);
        Response<String> pongError = Utils.messageToObject(serverResponse);
        assertEquals(830, pongError.status());
        assertEquals("ERROR", pongError.content());
        assertEquals("PONG", pongError.to());
    }

    @Test
    void AsteriskTC5_5_pingIsReceivedAtExpectedTime(TestReporter testReporter) throws JsonProcessingException {
        // this test is a bit weird, when ran isolated it passes
        // otherwise it's all the LEFT messages that the client receives
        // for some reason, when being SUPPOSEDLY the only one running
        // during the testcase. As a solution I tried emptying the buffer
        // before the expected 10 seconds pass,so that when the PING is
        // received it will be the last line in the reader. I don't know
        // why test client of this test receives the LEFT messages of
        // all others. This might just be the servers state that needs
        // resetting after each testcase.
        receiveLineWithTimeout(in); //welcome message
        out.println(Utils.objectToMessage(new Login("myname")));
        out.flush();

        System.err.println(ping_time_ms);
        System.err.println(ping_time_ms_delta_allowed);

        receiveLineWithTimeout(in); //server response

        //Make sure the test does not hang when no response is received by using assertTimeoutPreemptively
        assertTimeoutPreemptively(ofMillis(ping_time_ms + ping_time_ms_delta_allowed), () -> {
            String pingString;
            Instant start = Instant.now();
            // This is a rather messy solution, but so the test can
            // be run together with all the others, I think its fine
            //
            // On the other hand, this might even be better, because
            // now if the server was to do some additional sending
            // in the meantime that WAS intended, what would also
            // be ignored until ping is received. Like a blocking call
            while ((pingString = in.readLine()) != null && !pingString.equals("PING")) {
            }
            Instant finish = Instant.now();

            // Make sure the correct response is received
            System.out.println(pingString);
            Ping ping = Utils.messageToObject(pingString);

            assertNotNull(ping);

            // Also make sure the response is not received too early
            long timeElapsed = Duration.between(start, finish).toMillis();
            testReporter.publishEntry("timeElapsed", String.valueOf(timeElapsed));
            assertTrue(timeElapsed > ping_time_ms - ping_time_ms_delta_allowed);
        });
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
    }

}