package ch.so.agi.ilivalidator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class DockerIntegrationTests extends IntegrationTests {

    @BeforeAll
    public void setup() {
        port = "8888";
        servletContextPath = "/ilivalidator";
    }
}
