package ch.so.agi.ilivalidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

public abstract class IntegrationTests {
    Logger logger = LoggerFactory.getLogger(IntegrationTests.class);

    @LocalServerPort
    protected String port;
    
    @Value("#{servletContext.contextPath}")
    protected String servletContextPath;

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
        URL logfileUrl = new URL(link);
                        
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

        Thread.sleep(180000);
        //Thread.sleep(60000);
        
        String returnedMessage = clientHandler.getMessage();
        assertTrue(returnedMessage.contains("...validation failed:"));

        Document document = Jsoup.parse(returnedMessage);
        Elements links = document.select("a[href]");
        
        String link = links.get(0).attr("href");
        URL logfileUrl = new URL(link);
                
        String logfileContents = null;
        try (InputStream in = logfileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            logfileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        
        assertTrue(logfileContents.contains("so_nutzungsplanung_20171118.toml"));
        assertTrue(logfileContents.contains("Warning: line 5: SO_Nutzungsplanung_20171118.Rechtsvorschriften.Dokument: tid d3c20374-f6c5-48f9-8e1e-232b87a9d80a: invalid format of INTERLIS.URI value <34-Messen/Entscheide/34-36_45-E.pdf> in attribute TextImWeb"));
        assertTrue(logfileContents.contains("Error: line 61: SO_Nutzungsplanung_20171118.Rechtsvorschriften.HinweisWeitereDokumente: tid 6: Association SO_Nutzungsplanung_20171118.Rechtsvorschriften.HinweisWeitereDokumente must not have an OID (6)"));
        assertTrue(logfileContents.contains("Error: line 412: SO_Nutzungsplanung_20171118.Nutzungsplanung.Grundnutzung: tid 2d285daf-a5ab-4106-a453-58eef2e921ab: duplicate coord at (2599932.281, 1216063.38, NaN)"));
        assertTrue(logfileContents.contains("Error: line 140: SO_Nutzungsplanung_20171118.Nutzungsplanung.Typ_Ueberlagernd_Flaeche: tid 0723a0c8-46e4-4e4f-aba4-c75e90bece14: Attributwert Verbindlichkeit ist nicht identisch zum Objektkatalog: 'orientierend' - '6110'"));
        assertTrue(logfileContents.contains("Dokument 'https://geo.so.ch/docs/ch.so.arp.zonenplaene/Zonenplaene_pdf/24-Brunnenthal/Reglemente/024_BZR.pdf' wurde nicht gefunden"));
        // TODO fix when areArea logging is in production
        //assertTrue(logfileContents.contains("Error: line 1177: SO_Nutzungsplanung_20171118.Nutzungsplanung.Ueberlagernd_Flaeche: tid 0f55e390-9aba-4833-aaa5-c6b98fae7633: Set Constraint "));
        assertTrue(logfileContents.contains("Info: ...validation failed"));
        //assertFalse(logfileContents.contains("Info: assume"));
    }
    
    @Test
    public void validation_Ok_ili2() throws Exception {
        String endpoint = "ws://localhost:" + port + "/ilivalidator/socket";

        StandardWebSocketClient client = new StandardWebSocketClient();
        ClientSocketHandler clientHandler = new ClientSocketHandler();
        client.doHandshake(clientHandler, endpoint);

        Thread.sleep(2000);
        assertTrue(clientHandler.isConnected());

        File file = new File("src/test/data/2457_Messen_nachher.xtf");
        clientHandler.sendMessage(file);
        clientHandler.sendMessage(file.getName());

        Thread.sleep(180000);
        
        String returnedMessage = clientHandler.getMessage();
        assertTrue(returnedMessage.contains("...validation done:"));

        Document document = Jsoup.parse(returnedMessage);
        Elements links = document.select("a[href]");
        
        String link = links.get(0).attr("href");
        URL logfileUrl = new URL(link);
                
        String logfileContents = null;
        try (InputStream in = logfileUrl.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            logfileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        
        assertTrue(logfileContents.contains("so_nutzungsplanung_20171118.toml"));
    }
}
