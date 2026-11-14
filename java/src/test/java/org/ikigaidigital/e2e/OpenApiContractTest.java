package org.ikigaidigital.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.test.json.JsonCompareMode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiContractTest extends BaseApiTest {

    @Test
    void getApiDocs_generatedContract_matchesCommittedContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getContentFromFile("e2e/openapi/response/api-docs.json"),
                        JsonCompareMode.STRICT)); // strict: an extra operation or field fails the test (D6, D12)
    }
}
