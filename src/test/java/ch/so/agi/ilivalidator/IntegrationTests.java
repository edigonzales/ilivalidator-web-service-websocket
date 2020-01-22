package ch.so.agi.ilivalidator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTests {
    Logger logger = LoggerFactory.getLogger(IntegrationTests.class);

    @LocalServerPort
    private String port;
    
    @Value("#{servletContext.contextPath}")
    private String servletContextPath;

    @BeforeAll
    public static void setup() {}

    public class ClientSocketHandler implements WebSocketHandler {
        Logger logger = LoggerFactory.getLogger(ClientSocketHandler.class);

        private WebSocketSession webSocketSession;
        private String returnedMessage;

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            webSocketSession = session;
        }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
            String result = message.getPayload().toString();
            this.returnedMessage = result;
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            logger.error("Got a handleTransportError: " + exception.getMessage());
            exception.printStackTrace();
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }

        public boolean isConnected() {
            return webSocketSession != null;
        }

        public void sendMessage(Object msg) throws Exception {
            if (msg instanceof File) {
                byte[] fileContent = Files.readAllBytes(((File) msg).toPath());
                webSocketSession.sendMessage(new BinaryMessage(fileContent));
            } else {
                webSocketSession.sendMessage(new TextMessage(msg.toString()));
            }
        }

        public void closeConnection() throws IOException {
            if (isConnected()) {
                webSocketSession.close();
            }
        }
        
        public String getMessage() {
            return this.returnedMessage;
        }
    }

    @Test
    public void validation_Ok_ili1() throws Exception {
        String endpoint = "ws://localhost:" + port + "/ilivalidator/socket";

        StandardWebSocketClient client = new StandardWebSocketClient();
        ClientSocketHandler clientHandler = new ClientSocketHandler();
        client.doHandshake(clientHandler, endpoint);

        Thread.sleep(2000);
        assertTrue(clientHandler.isConnected());
        
        File file = new File("src/test/data/ch_254900.itf");
        clientHandler.sendMessage(file);
        clientHandler.sendMessage(file.getName());

        Thread.sleep(10000);
        
        String returnedMessage = clientHandler.getMessage();
        assertTrue(returnedMessage.contains("...validation done:"));
        
        Document document = Jsoup.parse(returnedMessage);
        Elements links = document.select("a[href]");
        
        String link = links.get(0).attr("href");
        URL logfileUrl = new URL("http://localhost:"+port+servletContextPath+"/"+link);
                
        String logfileContents = null;
        try (InputStream in = logfileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            logfileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        assertTrue(logfileContents.contains("Info: ...validation done")); 
    }
    
    @Test
    public void validation_Fail_ili2() throws Exception {
        String endpoint = "ws://localhost:" + port + "/ilivalidator/socket";

        StandardWebSocketClient client = new StandardWebSocketClient();
        ClientSocketHandler clientHandler = new ClientSocketHandler();
        client.doHandshake(clientHandler, endpoint);

        Thread.sleep(2000);
        assertTrue(clientHandler.isConnected());

        File file = new File("src/test/data/2457_Messen_vorher.xtf");
        clientHandler.sendMessage(file);
        clientHandler.sendMessage(file.getName());

        Thread.sleep(60000);
        
        String returnedMessage = clientHandler.getMessage();
        assertTrue(returnedMessage.contains("...validation failed:"));


    }
    

}
