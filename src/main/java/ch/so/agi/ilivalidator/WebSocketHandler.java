package ch.so.agi.ilivalidator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import ch.interlis.iox.IoxException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class WebSocketHandler extends AbstractWebSocketHandler {    
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static String FOLDER_PREFIX = "ilivalidator_";

    private static String LOG_ENDPOINT = "log";

    @Value("#{servletContext.contextPath}")
    protected String servletContextPath;
    
    @Value("${server.port}")
    protected String serverPort;

    @Autowired
    IlivalidatorService ilivalidator;

    String filename;
    File file;
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {        
        filename = message.getPayload();
        
        // ilivalidator must know if it is a ili1 or ili2 transfer file.
        Path copiedFile = Paths.get(file.getParent(), filename);
        Files.copy(file.toPath(), copiedFile, StandardCopyOption.REPLACE_EXISTING);
        
        session.sendMessage(new TextMessage("Received: " + filename));
        
        String logFilename = copiedFile.toFile().getAbsolutePath() + ".log";
        log.info(logFilename);
        
        // There is no option for config file support in the GUI at the moment.
        String configFile = "on";

        // Run the validation.
        String allObjectsAccessible = "true";
        boolean valid;
        try {
            session.sendMessage(new TextMessage("Validating..."));
            valid = ilivalidator.validate(allObjectsAccessible, configFile, copiedFile.toFile().getAbsolutePath(), logFilename);
        } catch (IoxException | IOException e) {
            e.printStackTrace();            
            log.error(e.getMessage());
            
            TextMessage errorMessage = new TextMessage("An error occured while validating the data:" + e.getMessage());
            session.sendMessage(errorMessage);
            
            return;
        }

        String resultText = "<span style='background-color:#58D68D;'>...validation done:</span>";
        if (!valid) {
            resultText = "<span style='background-color:#EC7063'>...validation failed:</span>";
        }
        
        
        log.info(servletContextPath);
        log.info(session.getUri().getScheme());
        log.info(session.getUri().getHost());
        
        String schema = session.getUri().getScheme().equalsIgnoreCase("wss") ? "https" : "http";
        String host = session.getUri().getHost();
        
        String port;
        if (serverPort.equalsIgnoreCase("80") || serverPort.equalsIgnoreCase("443") || serverPort.equalsIgnoreCase("") || serverPort == null) {
            port = "";
        } else if (host.contains("so.ch")) { 
            // FIXME: Am liebsten wäre es mir, wenn es mit relativen URL gehen würde. Da hatte ich aber Probleme im Browser/Client. Die haben nicht funktioniert in 
            // der GDI-Umgebung.
            // Variante: Absolute URL im Client zusammenstöpseln. Ob das aber für die Tests funktioniert, muss man schauen...
            port = "";
        } else {
            port = ":"+serverPort;
        }
        log.info(port);
        
        String logFileId = copiedFile.getParent().getFileName().toString();
        TextMessage resultMessage = new TextMessage(resultText + " <a href='"+schema+"://"+host+port+"/"+servletContextPath+"/"+LOG_ENDPOINT+"/"+logFileId+"/"+filename+".log' target='_blank'>Download log file.</a><br/><br/>   ");
        session.sendMessage(resultMessage);
    }
    
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        Path tmpDirectory = Files.createTempDirectory(FOLDER_PREFIX);
        
        // ilivalidator muss wissen, ob es sich um eine ili1- oder ili2-Datei handelt.
        // Der Namen muss jedoch separat mitgeschickt werden. Gespeichert wird die Datei mit einem
        // generischen Namen und anschliessend umbenannt.
        Path uploadFilePath = Paths.get(tmpDirectory.toString(), "data.file"); 
                
        FileChannel fc = new FileOutputStream(uploadFilePath.toFile().getAbsoluteFile(), false).getChannel();
        fc.write(message.getPayload());
        fc.close();

        file = uploadFilePath.toFile();
    }
}