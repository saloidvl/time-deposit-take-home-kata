# Test Requirements

Rules for the tests in this repository. The test strategy and the test infrastructure are described in [03-architecture.md](03-architecture.md) (section 7 and decision D15); this document says what a good test looks like.

## 1. What a test must do

- A test verifies observable behaviour, not that code merely ran.
- A test earns its place only if it fails when business behaviour breaks. Line and branch coverage are not goals.
- One test covers one scenario with one expected outcome, and its intent is clear from the name, the setup and the assertions.
- Tests do not depend on execution order or on side effects of other tests.
- When an endpoint returns a body or changes stored state, a status code alone is not an assertion.

## 2. What is not tested

- Guard clauses, null checks and defensive branches that express no business rule. The scenarios that matter already pass through them.
- Values the test itself stubbed.
- Implementation details: call order, call count or `verify(...)` on collaborators, unless the call is the observable contract.
- "No exception was thrown", `isNotNull()` or `isNotEmpty()` as the only assertion of a scenario.

## 3. Expected values

- An expected value is a hand-written literal or a file. It is never recomputed with the production algorithm; interest is calculated by hand from the plan rules.
- A failing test is a bug or an intended behaviour change. An assertion is not weakened or deleted, and an expected file is not edited, to make it pass.
- The tests that pin the legacy behaviour of `updateBalance` are not edited to make a refactoring pass: a red one means the refactoring changed the behaviour.

```java
// Bad: the expected value repeats the production formula
assertThat(deposit.getBalance()).isEqualTo(1000.0 + 1000.0 * 0.01 / 12);

// Good: hand-calculated literal (1000 * 1% / 12 = 0.8333, rounded to 0.83)
assertThat(deposit.getBalance()).isEqualTo(1000.83);
```

## 4. Unit test or API test

- **Unit test** (plain JUnit, no Spring): branching logic and edge cases, such as the plan rules, their day boundaries and the rounding.
- **API test** (real Spring context, MockMvc, PostgreSQL in Testcontainers): the main flow of each endpoint, with one main positive scenario per endpoint.
- Combinatorial coverage stays in unit tests, and an endpoint's main flow is never covered by unit tests only.
- Neither endpoint takes input and the assignment excludes invalid-input handling, so there are no negative API scenarios.
- The domain (`TimeDepositCalculator` and the interest policies) gets detailed boundary tests; the REST and persistence adapters are covered through the API tests.

## 5. Mocks

- Our own classes (domain, service, adapters, DTOs) are never mocked; tests build real instances.
- API tests use the real Spring context, without `@MockBean` or `@SpyBean`.

## 6. API tests

API tests live in `org.ikigaidigital.e2e` and extend `BaseApiTest`, which provides `mockMvc` and `getContentFromFile(path)`.

- The full response is compared with a `response/<scenario>.json` file. Only truly dynamic fields may be left out; a few `jsonPath(...)` checks do not replace the comparison.
- Stored state is set up with `@DataSet` and verified with `@ExpectedDataSet` (Database Rider), never through JDBC or a repository in the test body.
- When a request must not change anything, the same file is used for both.
- Each scenario has its own dataset.
- Every dataset lists both tables, `"timeDeposits"` first and then `"withdrawals"`, so that extra or changed rows are revealed. A table the scenario does not need is listed empty (`"withdrawals": []`).
- Decimal values in datasets are written as strings.

```
src/test/resources/e2e/<endpoint>/
  data-set/<scenario>.json
  expected-data-set/<scenario>.json
  response/<scenario>.json
```

```java
// Bad: fragments of the response, no stored state
mockMvc.perform(get("/time-deposits"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$[0].planType").value("basic"));

// Good: full body from a file, and a read that provably changes nothing
@Test
@DataSet("e2e/get-time-deposits/data-set/success.json")
@ExpectedDataSet(value = "e2e/get-time-deposits/data-set/success.json", orderBy = "id")
void getTimeDeposits_existingDeposits_returnsAllWithWithdrawals() throws Exception {
    mockMvc.perform(get("/time-deposits"))
        .andExpect(status().isOk())
        .andExpect(content().json(
            getContentFromFile("e2e/get-time-deposits/response/success.json"),
            JsonCompareMode.STRICT));
}
```

## 7. Readability

- A test body is arrange, act, assert. Low-level setup sits behind clearly named helpers, but the important assertions stay in the test.
- Setup that repeats is extracted; one `@ParameterizedTest` replaces several near-identical tests.
- A resource path is one complete string literal, so it can be searched and clicked. It is not built from a constant and a suffix.
- A test with several assertions uses `AutoCloseableSoftAssertions`.

## 8. Naming

- Methods: `[methodOrScenario]_[condition]_[expectedOutcome]` in camelCase, for example `updateBalance_basicPlanOnDay30_balanceUnchanged`.
- Unit test classes: `[ClassName]Test`, in the package that mirrors the production class. The tests that pin the pre-refactoring behaviour of `updateBalance` say so in the name: `TimeDepositCalculatorLegacyBehaviourTest`.
- API test classes are named after the scenario, for example `UpdateBalancesTest`.
- Resource files: lowercase with hyphens, named by scenario (`success.json`).
