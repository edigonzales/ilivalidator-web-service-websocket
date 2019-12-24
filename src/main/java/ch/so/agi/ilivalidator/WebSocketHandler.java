package ch.so.agi.ilivalidator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import ch.interlis.iox.IoxException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import javax.websocket.OnError;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class WebSocketHandler extends AbstractWebSocketHandler {    
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static String FOLDER_PREFIX = "ilivalidator_";

    @Autowired
    IlivalidatorService ilivalidator;

    String filename;
    File file;
    
//    @Override
//    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
//        System.out.println("********** TRANSPORT ERROR ********");
//    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        System.out.println("New Text Message Received @" + new Date().toString());
        System.out.println(session.getId());     
        
        System.out.println(message.toString());
        
        filename = message.getPayload();
        
        ilivalidator.foo(); 

        
//        boolean valid = ilivalidator.validate(allObjectsAccessible, configFile, inputFileName, logFileName);
//        try {
//            log.info(file.getAbsolutePath());
////            ilivalidator = new IlivalidatorService();
////            boolean valid = ilivalidator.validate("false", "false", file.getAbsolutePath(), "asdf");
//        } catch (IoxException e) {
//            e.printStackTrace();
//        }

        
        session.sendMessage(message);
    }

    
    // FIXME: Zuerst in eine data.tmp-Datei kopieren und anschliessend richtig umbenennen.
    
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        System.out.println("New Binary Message Received @" +  new Date().toString());
        System.out.println(session.getId());
        
        System.out.println(message.toString());

        ByteBuffer payload = message.getPayload();

        Path tmpDirectory = Files.createTempDirectory(FOLDER_PREFIX);
        
        // Ili1 muss itf als Endung haben, sonst wird falsch geprüft.
        Path uploadFilePath = Paths.get(tmpDirectory.toString(), "data.itf"); 
        
        
        System.out.println(uploadFilePath.toFile().getAbsolutePath());
        
        
        FileChannel fc = new FileOutputStream(uploadFilePath.toFile().getAbsoluteFile(), false).getChannel();
        fc.write(payload);
        fc.close();

        file = uploadFilePath.toFile();
        
//        session.sendMessage(message);
    }
}