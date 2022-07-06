package ch.so.agi.ilivalidator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class IlivalidatorWebServiceApplication {
    @Value("${app.connectTimeout}")
    private String connectTimeout;
    
    @Value("${app.readTimeout}")
    private String readTimeout;

	public static void main(String[] args) {
		SpringApplication.run(IlivalidatorWebServiceApplication.class, args);
	}
	
    @Bean
    CommandLineRunner init() {
        return args -> {
            System.setProperty("sun.net.client.defaultConnectTimeout", connectTimeout);
            System.setProperty("sun.net.client.defaultReadTimeout", readTimeout);
        };
    }
}
