package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.*;
import protocoltests.protocol.messages.*;
import protocoltests.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

public class LineEndings {

    private static Properties props = new Properties();
    private Socket s;
    private BufferedReader in;
    private PrintWriter out;
    private final static int max_delta_allowed_ms = 200;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = LineEndings.class.getResourceAsStream("testconfig.properties");
        props.load(in);
        in.close();
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
    void TC2_1_loginFollowedByBROADCASTWithWindowsLineEndingsReturnsOk() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        String message =
                Utils.objectToMessage(new Login("myname")) +
                        "\r\n" +
                        Utils.objectToMessage(new Broadcast("", "a")) +
                        "\r\n";
        out.print(message);
        out.flush();

        String serverResponse = receiveLineWithTimeout(in);
        Response<String> loginResp = Utils.messageToObject(serverResponse);
        assertEquals(800, loginResp.status());
        assertEquals("OK", loginResp.content());
        assertEquals("LOGIN", loginResp.to());

        serverResponse = receiveLineWithTimeout(in);
        Response<String> broadcastResp = Utils.messageToObject(serverResponse);
        assertEquals(800, broadcastResp.status());
        assertEquals("OK", broadcastResp.content());
        assertEquals("BROADCAST", broadcastResp.to());
    }

    @Test
    void TC2_2_loginFollowedByBROADCASTWithLinuxLineEndingsReturnsOk() throws JsonProcessingException {
        receiveLineWithTimeout(in); //welcome message
        String message = Utils.objectToMessage(new Login("myname")) +
                "\n" +
                Utils.objectToMessage(new Broadcast("", "a")) + "\n";

        System.out.println(message);

        out.print(message);
        out.flush();

        String loginResponse = receiveLineWithTimeout(in);
        System.out.println("Login Response: " + loginResponse);
        Response<String> loginResp = Utils.messageToObject(loginResponse);
        assertEquals(800, loginResp.status());
        assertEquals("OK", loginResp.content());
        assertEquals("LOGIN", loginResp.to());

        String broadcastResponse = receiveLineWithTimeout(in);
        System.out.println("Broadcast Response: " + broadcastResponse);
        Response<String> broadcastResp = Utils.messageToObject(broadcastResponse);
        assertEquals(800, broadcastResp.status());
        assertEquals("OK", broadcastResp.content());
        assertEquals("BROADCAST", broadcastResp.to());
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
    }

}