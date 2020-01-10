package ch.so.agi.ilivalidator;

import java.net.URI;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTests {
    Logger logger = LoggerFactory.getLogger(IntegrationTests.class);

    @LocalServerPort
    private String port;
    
    @BeforeAll
    public static void setup() {
        //SpringApplication.run(IlivalidatorWebServiceApplication.class, new String[0]);
    }

    @Test
    public void testGetLog() throws Exception {
        String defaultEndpoint =  "ws://localhost:"+port+"/ilivalidator";

        logger.info(defaultEndpoint);
        logger.info("port: " + port);
        System.out.println(defaultEndpoint);
        Thread.sleep(100000);
    }


}
