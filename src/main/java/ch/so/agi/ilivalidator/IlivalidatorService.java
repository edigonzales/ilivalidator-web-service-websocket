package ch.so.agi.ilivalidator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.ehi.basics.settings.Settings;
import ch.interlis.iom_j.itf.ItfReader;
import ch.interlis.iom_j.xtf.XtfReader;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox_j.EndTransferEvent;
import ch.interlis.iox_j.StartBasketEvent;

import org.apache.commons.io.FilenameUtils;
import org.interlis2.validator.Validator;

@Service
public class IlivalidatorService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${app.docBase}")
    private String docBase;

    @Value("${app.configDirectoryName}")
    private String configDirectoryName;

    /**
     * This method validates an INTERLIS transfer file with
     * <a href="https://github.com/claeis/ilivalidator">ilivalidator library</a>.
     * 
     * @param doConfigFile  Use ilivalidator configuration file for tailoring the validation.
     * @param fileName      Name of INTERLIS transfer file.
     * @throws IoxException If an error occurred when trying to figure out model name.
     * @throws IOException  If config file cannot be read or copied to file system.
     * @return boolean      True, if transfer file is valid. False, if errors were found.
     */
    public synchronized boolean validate(String allObjectsAccessible, String doConfigFile, String inputFileName, String logFileName)
            throws IoxException, IOException {        
        Settings settings = new Settings();
        settings.setValue(Validator.SETTING_LOGFILE, logFileName);
        settings.setValue(Validator.SETTING_XTFLOG, logFileName + ".xtf");
        settings.setValue(Validator.SETTING_ILIDIRS, Validator.SETTING_DEFAULT_ILIDIRS);
        
        // Leider scheint es nicht steuerbar zu sein via toml.
        // https://github.com/claeis/ilivalidator/issues/350
        // https://github.com/claeis/ilivalidator/issues/83
        if (allObjectsAccessible.toLowerCase().equalsIgnoreCase("true")) {
            settings.setValue(Validator.SETTING_ALL_OBJECTS_ACCESSIBLE, Validator.TRUE);
        }
        
        String modelName = getModelNameFromTransferFile(inputFileName);

        if (modelName.equalsIgnoreCase("VSADSSMINI_2020_LV95")) {
            settings.setValue(Validator.SETTING_ALL_OBJECTS_ACCESSIBLE, Validator.FALSE);
        }
        
        // Die Verzeichnis der entpackten oder hinzugefügten INTERLIS-Modelldateien
        // ilivalidator mittels --modeldir bekannt machen.
        File iliFiles = Paths.get(docBase, configDirectoryName, "ili").toFile();
        settings.setValue(Validator.SETTING_ILIDIRS, "%ITF_DIR;http://models.interlis.ch/;%JAR_DIR/ilimodels;"+iliFiles.getAbsolutePath());

        // Es wird nach einer Toml-Datei gesucht, die in Kleinbuchstaben gleich heisst, wie das Modell gegen
        // das geprüft werden soll.
        // Ist die Datei vorhanden, wird sie ilivalidator als Config-Datei bekannt gemacht.
        if (doConfigFile != null) {
            File configFile = Paths.get(docBase, configDirectoryName, "toml", modelName.toLowerCase() + ".toml").toFile();
            if (configFile.exists()) {
                settings.setValue(Validator.SETTING_CONFIGFILE, configFile.getAbsolutePath());
            }
        }
        
        log.info("Validation start.");
        boolean valid = Validator.runValidation(inputFileName, settings);
        log.info("Validation end.");

        return valid;
    }
    
    /**
     * Figure out INTERLIS model name from INTERLIS transfer file. Works with ili1
     * and ili2.
     */
    private String getModelNameFromTransferFile(String transferFileName) throws IoxException {
        String model = null;
        String ext = FilenameUtils.getExtension(transferFileName);
        IoxReader ioxReader = null;

        try {
            File transferFile = new File(transferFileName);

            if (ext.equalsIgnoreCase("itf")) {
                ioxReader = new ItfReader(transferFile);
            } else {
                ioxReader = new XtfReader(transferFile);
            }

            IoxEvent event;
            StartBasketEvent be = null;
            do {
                event = ioxReader.read();
                if (event instanceof StartBasketEvent) {
                    be = (StartBasketEvent) event;
                    break;
                }
            } while (!(event instanceof EndTransferEvent));

            ioxReader.close();
            ioxReader = null;

            if (be == null) {
                throw new IllegalArgumentException("no baskets in transfer-file");
            }

            String namev[] = be.getType().split("\\.");
            model = namev[0];

        } catch (IoxException e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw new IoxException("could not parse file: " + new File(transferFileName).getName());
        } finally {
            if (ioxReader != null) {
                try {
                    ioxReader.close();
                } catch (IoxException e) {
                    log.error(e.getMessage());
                    e.printStackTrace();
                    throw new IoxException(
                            "could not close interlise transfer file: " + new File(transferFileName).getName());
                }
                ioxReader = null;
            }
        }
        return model;
    } 
}
