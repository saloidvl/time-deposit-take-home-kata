package org.ikigaidigital.e2e;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UpdateBalancesTest extends BaseApiTest {

    @Test
    @DataSet("e2e/update-balances/data-set/success.json")
    // Sorted by id because DBUnit compares rows by position, and the input dataset deliberately is not in id order.
    @ExpectedDataSet(value = "e2e/update-balances/expected-data-set/success.json", orderBy = "id")
    void updateBalances_depositsOfAllPlans_accruesInterestOnEligibleDepositsOnly() throws Exception {
        mockMvc.perform(post("/time-deposits/balance-updates"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }
}
