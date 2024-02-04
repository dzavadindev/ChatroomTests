//package protocoltests;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import org.junit.jupiter.api.*;
//import protocoltests.protocol.messages.*;
//import protocoltests.protocol.utils.Utils;
//
//import java.io.*;
//import java.net.Socket;
//import java.time.Duration;
//import java.time.Instant;
//import java.util.Properties;
//
//import static java.time.Duration.ofMillis;
//import static org.junit.jupiter.api.Assertions.*;
//
//class GeneralTests {
//
//    private static Properties props = new Properties();
//    private static int ping_time_ms;
//    private static int ping_time_ms_delta_allowed;
//    private final static int max_delta_allowed_ms = 300;
//
//    private Socket s;
//    private BufferedReader in;
//    private PrintWriter out;
//
//    @BeforeAll
//    static void setupAll() throws IOException {
//        InputStream in = GeneralTests.class.getResourceAsStream("testconfig.properties");
//        props.load(in);
//        assert in != null;
//        in.close();
//
//        ping_time_ms = Integer.parseInt(props.getProperty("ping_time_ms", "10000"));
//        ping_time_ms_delta_allowed = Integer.parseInt(props.getProperty("ping_time_ms_delta_allowed", "100"));
//    }
//
//    @BeforeEach
//    void setup() throws IOException {
//        s = new Socket(props.getProperty("host"), Integer.parseInt(props.getProperty("port")));
//        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
//        out = new PrintWriter(s.getOutputStream(), true);
//    }
//
//    @AfterEach
//    void cleanup() throws IOException {
//        s.close();
//    }
//    private String receiveLineWithTimeout(BufferedReader reader) {
//        return assertTimeoutPreemptively(ofMillis(max_delta_allowed_ms), reader::readLine);
//    }
//
//    private void flushBufferedReader(BufferedReader reader) {
//        try {
//            while (reader.ready()) {
//                reader.readLine();
//            }
//        } catch (IOException e) {
//            // Log or handle the exception as needed
//            System.err.println("Error flushing BufferedReader: " + e.getMessage());
//        }
//    }
//
//}