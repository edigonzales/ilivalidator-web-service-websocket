package ch.so.agi.ilivalidator;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

@SpringBootApplication
public class IlivalidatorWebServiceApplication {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${app.connectTimeout}")
    private String connectTimeout;
    
    @Value("${app.readTimeout}")
    private String readTimeout;
    
    @Value("${app.docBase}")
    private String docBase;

    @Value("${app.configDirectoryName}")
    private String configDirectoryName;

    @Value("${app.unpackConfigFiles}")
    private boolean unpackConfigFiles;

	public static void main(String[] args) {
		SpringApplication.run(IlivalidatorWebServiceApplication.class, args);
	}
	
	// CommandLineRunner: Anwendung live aber nicht ready.
    @Bean
    CommandLineRunner init() {
        return args -> {
            System.setProperty("sun.net.client.defaultConnectTimeout", connectTimeout);
            System.setProperty("sun.net.client.defaultReadTimeout", readTimeout);

            // Root-Verzeichnis und das sichtbare "config"-Verzeichnis und
            // Unterverzeichnisse für das Directory-Listing erstellen.
            // Die toml- und ili-Dateien werden in die entsprechenden
            // Verzeichnisse kopiert. 
            if (!new File(docBase).exists()) {
                new File(docBase).mkdir();
            }
            
            File configDirectory = Paths.get(docBase, configDirectoryName).toFile();
            if (!configDirectory.exists()) {
                configDirectory.mkdir();
            }

            File iliDirectory = Paths.get(docBase, configDirectoryName, "ili").toFile();
            if (!iliDirectory.exists()) {
                iliDirectory.mkdir();
            }

            if (unpackConfigFiles) {
                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource[] resources = resolver.getResources("classpath:ili/*.ili");
                for (Resource resource : resources) {
                    InputStream is = resource.getInputStream();
                    File tomlFile = Paths.get(iliDirectory.getAbsolutePath(), resource.getFilename()).toFile();
                    log.info("copying " + resource.getFilename() + " to " + iliDirectory.getAbsolutePath());
                    Files.copy(is, tomlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    IOUtils.closeQuietly(is);
                }
            }

            File tomlDirectory = Paths.get(docBase, configDirectoryName, "toml").toFile();
            if (!tomlDirectory.exists()) {
                tomlDirectory.mkdir();
            }
            
            if (unpackConfigFiles) {
                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource[] resources = resolver.getResources("classpath:toml/*.toml");
                for (Resource resource : resources) {
                    InputStream is = resource.getInputStream();
                    File tomlFile = Paths.get(tomlDirectory.getAbsolutePath(), resource.getFilename()).toFile();
                    log.info("copying " + resource.getFilename() + " to " + tomlDirectory.getAbsolutePath());
                    Files.copy(is, tomlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    IOUtils.closeQuietly(is);
                }
            }
            
            // Die XSL-Datei in das Directory-Listing-Root-Verzeichnis
            // kopieren.
            String LISTING_XSL = "listing.xsl";
            File listingXslFile = Paths.get(docBase, LISTING_XSL).toFile();
            InputStream listingXslResource = new ClassPathResource(LISTING_XSL).getInputStream();
            Files.copy(listingXslResource, listingXslFile.toPath(), StandardCopyOption.REPLACE_EXISTING);      
            
        };
    }
}
