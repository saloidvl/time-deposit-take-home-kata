package org.ikigaidigital.e2e;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.junit5.api.DBRider;
import org.ikigaidigital.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Base for API tests: real Spring context, MockMvc and PostgreSQL in Testcontainers (D15).
 * All API tests must share this one context: Database Rider keeps the first connection it gets,
 * so a test with its own context configuration would load its dataset into another database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DBRider
// The camelCase table and column names are quoted identifiers in PostgreSQL (D11).
@DBUnit(caseSensitiveTableNames = true, escapePattern = "\"?\"")
public abstract class BaseApiTest {

    @Autowired
    protected MockMvc mockMvc;

    protected String getContentFromFile(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
