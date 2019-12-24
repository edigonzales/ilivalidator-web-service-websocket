package ch.so.agi.ilivalidator;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.settings.Settings;
import ch.interlis.iom_j.itf.ItfReader;
import ch.interlis.iom_j.xtf.XtfReader;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox_j.EndTransferEvent;
import ch.interlis.iox_j.StartBasketEvent;
import org.interlis2.validator.Validator;

@Service
public class IlivalidatorService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public void foo() {
        System.out.println("**************");
    }

    public synchronized boolean validate(String allObjectsAccessible, String doConfigFile, String inputFileName, String logFileName)
            throws IoxException, IOException {
        
        System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
        System.setProperty("sun.net.client.defaultReadTimeout", "10000");

        Settings settings = new Settings();
        settings.setValue(Validator.SETTING_ILIDIRS, Validator.SETTING_DEFAULT_ILIDIRS);

//        settings.setValue(Validator.SETTING_LOGFILE, logFileName);
        
        if (allObjectsAccessible != null) {
            settings.setValue(Validator.SETTING_ALL_OBJECTS_ACCESSIBLE, Validator.TRUE);
        }

        boolean valid = Validator.runValidation(inputFileName, settings);

        
        return valid;
    }
}
