package ch.so.agi.ilivalidator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectAclRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class WebSocketHandler extends AbstractWebSocketHandler {    
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static String FOLDER_PREFIX = "ilivalidator_";
    private static String LOG_ENDPOINT = "log";
    private static String HEX_COLOR_SUCCESS = "#58D68D";
    private static String HEX_COLOR_FAIL = "#EC7063";
    
    @Value("${server.port}")
    protected String serverPort;
    
    @Value("${app.s3Bucket}")
    private String s3Bucket;

    @Value("${app.workDirectory}")
    private String workDirectory;

    @Autowired
    IlivalidatorService ilivalidator;

    // Dient dem Verwalten der Websocket-Session und
    // dem Zugriff der zu prüfenden Datein über verschiedene Methoden
    // hinweg. Die Kombination Session-Id und Datei ist eindeutig,
    // während der Prüfung keine weitere Datei hochgeladen werden.
    HashMap<String, File> sessionFileMap = new HashMap<String, File>();
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {        
        File file = sessionFileMap.get(session.getId());
        String filename = message.getPayload();
        
        // "file" hat nicht mehr der Original-Dateinamen, weil der Payload der
        // Binary-Message, diesen nicht mitschickt.
        Path namedFile = Paths.get(file.getParent(), filename);
        Files.copy(file.toPath(), namedFile, StandardCopyOption.REPLACE_EXISTING);
        
        session.sendMessage(new TextMessage("Received: " + filename));
        
        String logFilename = namedFile.toFile().getAbsolutePath() + ".log";
        log.info(logFilename);
        
        // Zurzeit wird diese Option im GUI nicht exponiert.
        String configFile = "on";

        // Zurzeit wird diese Option im GUI nicht exponiert.
        String allObjectsAccessible = "true";

        boolean valid;
        String logKey;
        String xtfLogKey;
        try {
            // Run the validation.
            session.sendMessage(new TextMessage("Validating..."));
            valid = ilivalidator.validate(allObjectsAccessible, configFile, namedFile.toFile().getAbsolutePath(), logFilename);
            
            // Upload log file to S3.
            log.info("log file: " + logFilename);
            Region region = Region.EU_CENTRAL_1;
            S3Client s3 = S3Client.builder().region(region).build();
                    
            String subfolder = new File(new File(logFilename).getParent()).getName();
            String s3Logfilename = new File(logFilename).getName();
            logKey = subfolder + "/" + s3Logfilename;
            String s3XtfLogfilename = new File(logFilename + ".xtf").getName();
            xtfLogKey = subfolder + "/" + s3XtfLogfilename;
  
            log.info("Uploading objects... " + logKey + ", " + xtfLogKey);
            s3.putObject(PutObjectRequest.builder().bucket(s3Bucket).key(logKey).contentType("plain/text; charset=utf-8").build(), new File(logFilename).toPath());
            s3.putObjectAcl(PutObjectAclRequest.builder().bucket(s3Bucket).key(logKey).acl(ObjectCannedACL.PUBLIC_READ).build());
            s3.putObject(PutObjectRequest.builder().bucket(s3Bucket).key(xtfLogKey).contentType("text/xml").build(), new File(logFilename + ".xtf").toPath());
            s3.putObjectAcl(PutObjectAclRequest.builder().bucket(s3Bucket).key(xtfLogKey).acl(ObjectCannedACL.PUBLIC_READ).build());
            log.info("Upload complete");
            
            s3.close();            
                        
            FileUtils.deleteDirectory(new File(file.getParent()));
        } catch (Exception e) {
            e.printStackTrace();            
            log.error(e.getMessage());
            
            TextMessage errorMessage = new TextMessage("An error occured while validating the data:<br>" + e.getMessage());
            session.sendMessage(errorMessage);
            
            // Temp-Verzeichnis mit der Transferdatei löschen.
            // TODO: Warum nicht aus der Session löschen???
            // finally und sonst der Rest hier?
            FileUtils.deleteDirectory(namedFile.toFile().getParentFile());
            return;
        } 
                
        // Browser response.
        // TODO: "getHost()" oder ähnlich (siehe unten).
        String resultText = "<span style='background-color:"+HEX_COLOR_SUCCESS+";'>...validation done:</span>";
        if (!valid) {
            resultText = "<span style='background-color:"+HEX_COLOR_FAIL+"'>...validation failed:</span>";
        }
        
        TextMessage resultMessage = new TextMessage(resultText + " <a href='https://s3."+Region.EU_CENTRAL_1.id()+".amazonaws.com/"+s3Bucket+"/"+logKey+"' target='_blank'>Download log file</a> / "
                + " <a href='https://s3."+Region.EU_CENTRAL_1.id()+".amazonaws.com/"+s3Bucket+"/"+xtfLogKey+"' target='_blank'>Download XTF log file.</a><br/><br/>   ");
        session.sendMessage(resultMessage);
        
        // Die Websocket-Session und damit das dazugehörige Transferfile aus
        // der Websocket-Map löschen. Das Temp-Verzeichnis in dem die Transferdatei
        // gespeichert wurde, wird ebenfalls gelöscht.
        sessionFileMap.remove(session.getId());
        FileUtils.deleteDirectory(namedFile.toFile().getParentFile());
    }
    
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        Path tmpDirectory = Files.createTempDirectory(Paths.get(workDirectory), FOLDER_PREFIX);
        
        // ilivalidator muss wissen, ob es sich um eine ili1- oder ili2-Datei handelt.
        // Der Namen muss jedoch separat mitgeschickt werden. Gespeichert wird die Datei mit einem
        // generischen Namen und anschliessend umbenannt.
        Path uploadFilePath = Paths.get(tmpDirectory.toString(), "data.file"); 
        log.info(uploadFilePath.toAbsolutePath().toString());
                
        FileChannel fc = new FileOutputStream(uploadFilePath.toFile().getAbsoluteFile(), false).getChannel();
        fc.write(message.getPayload());
        fc.close();

        // Die Datei wird in eine Map kopiert, damit man via
        // Websocket-Session-Id Zugriff hat und in einer anderen Methode (wenn man alle 
        // benötigten Infos hat) die Prüfung durchführen kann.
        sessionFileMap.put(session.getId(), uploadFilePath.toFile());
    }
    
    private String getHost() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}