package ch.so.agi.ilivalidator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration
public class AppConfig {
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
    
    @Bean
    public void setPluginClasses() {
        System.setProperty("ch.ehi.iox-ili.pluginClasses", 
                "ch.so.agi.ilivalidator.ext.AreaIoxPlugin,"
                + "ch.so.agi.ilivalidator.ext.LengthIoxPlugin,"
                + "ch.so.agi.ilivalidator.ext.IsHttpResourceIoxPlugin,"
                + "ch.so.agi.ilivalidator.ext.IsValidDocumentsCycleIoxPlugin,"
                + "ch.so.agi.ilivalidator.ext.RingSelfIntersectionIoxPlugin,"
                + "ch.so.agi.ilivalidator.ext.TooFewPointsPolylineIoxPlugin");
    }
}
