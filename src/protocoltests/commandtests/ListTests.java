package protocoltests.commandtests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocoltests.protocol.messages.Login;
import protocoltests.protocol.messages.Response;
import protocoltests.protocol.utils.Utils;

import java.io.*;
import java.util.*;
import java.net.Socket;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class ListTests {

    private static Properties props = new Properties();
    private static int ping_time_ms;
    private static int ping_time_ms_delta_allowed;
    private final static int max_delta_allowed_ms = 300;

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
    void TC6_1_correctListOfUsersIsReceived() throws JsonProcessingException {
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

        out1.println("LIST");
        out1.flush();
        Response<List<String>> res = Utils.messageToObject(receiveLineWithTimeout(in1));
        assertTrue(res.content().contains("user3"));
        assertTrue(res.content().contains("user2"));
    }

    @Test
    void TC6_2_notLoggedInUserCanSeeActiveUsersList() throws JsonProcessingException {
        receiveLineWithTimeout(in1); // WELCOME
        receiveLineWithTimeout(in2); // WELCOME
        receiveLineWithTimeout(in3); // WELCOME

        out2.println(Utils.objectToMessage(new Login("user2")));
        out2.flush();
        receiveLineWithTimeout(in2); // OK

        out3.println(Utils.objectToMessage(new Login("user3")));
        out3.flush();
        receiveLineWithTimeout(in3); // OK
        receiveLineWithTimeout(in2); // U3 IS HERE

        out1.println("LIST");
        out1.flush();
        Response<List<String>> res = Utils.messageToObject(receiveLineWithTimeout(in1));
        assertEquals("LIST", res.to());
        assertEquals(800, res.status());
        assertTrue(res.content().contains("user3"));
        assertTrue(res.content().contains("user2"));
    }

    @Test
    void TC6_3_ifUserAloneReturnEmptyList() throws JsonProcessingException {
        receiveLineWithTimeout(in1); // WELCOME

        out1.println("LIST");
        out1.flush();
        Response<List<String>> res = Utils.messageToObject(receiveLineWithTimeout(in1));
        assertEquals("LIST", res.to());
        assertEquals(800, res.status());
        assertEquals(List.of(), res.content());
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
    }
}