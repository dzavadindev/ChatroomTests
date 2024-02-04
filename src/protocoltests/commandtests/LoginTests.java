package protocoltests.commandtests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.*;
import protocoltests.protocol.messages.Arrived;
import protocoltests.protocol.messages.Login;
import protocoltests.protocol.messages.Pong;
import protocoltests.protocol.messages.Response;
import protocoltests.protocol.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

class LoginTests {

    private static Properties props = new Properties();

    private Socket socketUser1, socketUser2;
    private BufferedReader inUser1, inUser2;
    private PrintWriter outUser1, outUser2;

    private final static int max_delta_allowed_ms = 1000;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream inUser1 = LoginTests.class.getResourceAsStream("../testconfig.properties");
        props.load(inUser1);
        inUser1.close();
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
    void TC1_1_userNameWithThreeCharactersIsAccepted() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //welcome message
        outUser1.println(Utils.objectToMessage(new Login("mym")));
        outUser1.flush();
        String serverResponse = receiveLineWithTimeout(inUser1);
        Response<String> loginResp = Utils.messageToObject(serverResponse);
        assertEquals(800, loginResp.status());
        assertEquals("OK", loginResp.content());
        assertEquals("LOGIN", loginResp.to());
    }

    @Test
    void TC1_2_userNameWithTwoCharactersReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //welcome message
        outUser1.println(Utils.objectToMessage(new Login("my")));
        outUser1.flush();
        String serverResponse = receiveLineWithTimeout(inUser1);
        Response<String> loginResp = Utils.messageToObject(serverResponse);
        assertEquals(811, loginResp.status());
        assertEquals("ERROR", loginResp.content());
        assertEquals("LOGIN", loginResp.to());
    }

    @Test
    void TC1_3_userNameWith14CharactersIsAccepted() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //welcome message
        outUser1.println(Utils.objectToMessage(new Login("abcdefghijklmn")));
        outUser1.flush();
        String serverResponse = receiveLineWithTimeout(inUser1);
        Response<String> loginResp = Utils.messageToObject(serverResponse);
        assertEquals(800, loginResp.status());
        assertEquals("OK", loginResp.content());
        assertEquals("LOGIN", loginResp.to());
    }

    @Test
    void TC1_4_userNameWith15CharactersReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //welcome message
        outUser1.println(Utils.objectToMessage(new Login("abcdefghijklmop")));
        outUser1.flush();
        String serverResponse = receiveLineWithTimeout(inUser1);
        Response<String> loginResp = Utils.messageToObject(serverResponse);
        assertEquals(811, loginResp.status());
        assertEquals("ERROR", loginResp.content());
        assertEquals("LOGIN", loginResp.to());
    }

    @Test
    void TC1_5_userNameWithStarReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //welcome message
        outUser1.println(Utils.objectToMessage(new Login("*a*")));
        outUser1.flush();
        String serverResponse = receiveLineWithTimeout(inUser1);
        Response<String> loginResp = Utils.messageToObject(serverResponse);
        assertEquals(811, loginResp.status());
        assertEquals("ERROR", loginResp.content());
        assertEquals("LOGIN", loginResp.to());
    }

    @Test
    void TC1_6_loggingInTwiceReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //welcome message
        outUser1.println(Utils.objectToMessage(new Login("abcd"))); // first log in
        outUser1.flush();
        String serverResponse = receiveLineWithTimeout(inUser1);
        Response<String> loginResp = Utils.messageToObject(serverResponse);
        assertEquals(800, loginResp.status());
        assertEquals("OK", loginResp.content());
        assertEquals("LOGIN", loginResp.to());

        outUser1.println(Utils.objectToMessage(new Login("abcd"))); // second login
        outUser1.flush();
        String secondServerResponse = receiveLineWithTimeout(inUser1);
        Response<String> secondLoginResp = Utils.messageToObject(secondServerResponse);
        assertEquals(810, secondLoginResp.status());
        assertEquals("ERROR", secondLoginResp.content());
        assertEquals("LOGIN", secondLoginResp.to());
    }

    @Test
    void TC1_7_loginMessageWithAlreadyConnectedUsernameReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //welcome message
        receiveLineWithTimeout(inUser2); //welcome message

        // Connect user 1
        outUser1.println(Utils.objectToMessage(new Login("user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK

        // Connect using same username
        outUser2.println(Utils.objectToMessage(new Login("user1")));
        outUser2.flush();
        String resUser2 = receiveLineWithTimeout(inUser2);
        Response<String> loginResp = Utils.messageToObject(resUser2);
        assertEquals("ERROR", loginResp.content());
        assertEquals(812, loginResp.status());
        assertEquals("LOGIN", loginResp.to());
    }

    @Test
    void TC1_8_joinedIsReceivedByOtherUserWhenUserConnects() throws JsonProcessingException {
        receiveLineWithTimeout(inUser1); //WELCOME
        receiveLineWithTimeout(inUser2); //WELCOME

        // Connect user1
        outUser1.println(Utils.objectToMessage(new Login("user1")));
        outUser1.flush();
        outUser1.println(Utils.objectToMessage(new Pong()));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); //OK
        receiveLineWithTimeout(inUser1); //PING

        // Connect user2
        outUser2.println(Utils.objectToMessage(new Login("user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); //OK

        //JOINED is received by user1 when user2 connects
        String resIdent = receiveLineWithTimeout(inUser1);
        Arrived joined = Utils.messageToObject(resIdent);

        assertEquals(new Arrived("user2"), joined);
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
    }

}