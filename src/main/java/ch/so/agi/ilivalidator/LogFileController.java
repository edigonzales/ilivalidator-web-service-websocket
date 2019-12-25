package ch.so.agi.ilivalidator;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class LogFileController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = "/log/{id}/{file}", method = RequestMethod.GET)
    public ResponseEntity<?> log(@PathVariable String id, @PathVariable String file) {
        try {
            String logFileName = Paths.get(System.getProperty("java.io.tmpdir"), id, file).toFile().getAbsolutePath();
            File logFile = new File(logFileName);
            InputStream is = new FileInputStream(logFile);

            return ResponseEntity.ok().header("Content-Type", "text/plain; charset=utf-8")
                    .contentLength(logFile.length())
                    .body(new InputStreamResource(is));
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return ResponseEntity.badRequest().contentType(MediaType.parseMediaType("text/plain")).body(e.getMessage());
        }
    }
}
