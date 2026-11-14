package org.ikigaidigital.e2e;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import org.junit.jupiter.api.Test;
import org.springframework.test.json.JsonCompareMode;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GetTimeDepositsTest extends BaseApiTest {

    @Test
    @DataSet("e2e/get-time-deposits/data-set/success.json")
    // Both sides are sorted by id because DBUnit compares rows by position, and the dataset deliberately is not in id order.
    @ExpectedDataSet(value = "e2e/get-time-deposits/data-set/success.json", orderBy = "id")
    void getTimeDeposits_existingDeposits_returnsAllWithWithdrawals() throws Exception {
        mockMvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getContentFromFile("e2e/get-time-deposits/response/success.json"),
                        JsonCompareMode.STRICT)) // strict: the field set and the order of the lists are part of the contract
                // JSON comparison is numeric, so only the raw text shows that a large balance is not written as 1.0E+7.
                .andExpect(content().string(containsString("\"balance\":10000000,")));
    }
}
