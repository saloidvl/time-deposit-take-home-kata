# Architecture

Architecture of the solution. Fact and assumption IDs (F, A) refer to [01-assumptions.md](01-assumptions.md); decision IDs (D) refer to section 11 of this document.

## 1. Package layout and dependency rule

Single Maven module in `java/`, Spring Boot 3.5.x, Java 21 (D1).

```
org.ikigaidigital
├── TimeDeposit                  legacy shared class, unchanged, location frozen
├── TimeDepositCalculator        legacy API, signature frozen; facade over domain policies
├── TimeDepositApplication       Spring Boot main + the calculator bean
├── domain
│   ├── model                    PlanType, Withdrawal, TimeDepositDetails
│   └── policy                   InterestPolicy, Basic/Student/PremiumInterestPolicy
├── application                  TimeDepositService (inbound port)
│   └── port                     TimeDepositRepository (outbound port)
└── adapter
    ├── controller               TimeDepositController, TimeDepositApiMapper
    │   ├── config               OpenApiConfig, RequestLoggingFilter
    │   └── dto                  TimeDepositResponse, WithdrawalResponse
    └── persistence              JdbcTimeDepositRepository
```

- `TimeDeposit` and `TimeDepositCalculator` stay in `org.ikigaidigital`: moving them would change their fully qualified names and break existing callers. They count as domain. This is the one deliberate exception to the package layout, made for backward compatibility (D5).
- **Dependencies point inward:** `adapter → application → domain`. `controller` and `persistence` do not depend on each other.
- **Domain** has no Spring types and knows nothing about the application layer or the adapters. The ArchUnit rule covers the `domain..` packages; the two legacy classes in the root package are not covered by it.
- **Application** may use Spring's `@Service` and `@Transactional`, but no web, JDBC or adapter types (D13).
- **DTOs exist only in `adapter.controller.dto`;** the controller takes and returns DTOs, and everything behind it works with domain models (D17, D18).
- **Interfaces only where they earn their place:** `InterestPolicy` (three implementations) and the outbound port `TimeDepositRepository`, which keeps the dependency rule real. No interface has a single method and a single implementation (D13).
- The main class sits in the root package, as Spring Boot expects, and is the only place where everything is wired together.
- **Comment convention:** the code explains itself through names; a comment appears only where it says why the logic is the way it is, never what the code does. Where the reason is an assumption or a decision, the comment states it in one sentence and gives the ID, e.g. `// An unknown plan code has no policy and earns nothing, as in the legacy code (A2).` The README asks for the justification in the code, so an ID alone is not enough.

## 2. Interest calculation (strategy per plan)

```java
public enum PlanType {
    BASIC("basic"), STUDENT("student"), PREMIUM("premium"),
    UNKNOWN(null);                               // any other text

    public static PlanType fromCode(String code) // exact, case-sensitive; never throws (A2)
}

public interface InterestPolicy {
    PlanType planType();                         // never UNKNOWN
    double monthlyInterest(TimeDeposit deposit); // raw, unrounded; 0 when not eligible
}
```

- **`PlanType` holds codes only** (D16). `fromCode` compares the text with `equals` against the lower-case code of each known constant, never with `name()` or `valueOf`. The match is exact and case-sensitive, as in the legacy code: `basic` gives `BASIC`; `Basic` or any other text gives `UNKNOWN` (A2). The matching lives in this one method, so relaxing it later is a one-line change ([02-known-issues.md](02-known-issues.md)).
- **The enum selects the policy; it is not stored.** `TimeDeposit` keeps `planType` as a `String`, so the original text survives loading, updating and GET unchanged. There is deliberately no enum-to-text conversion: the text is never rebuilt from the enum, so nothing can be lost.
- **Each policy owns its rate and its whole eligibility window**, never the enum: basic `days > 30`, student `30 < days < 366`, premium `days > 45` (A8). Each evaluates exactly the legacy expression, e.g. `balance * 0.05 / 12`; precomputing `0.05 / 12` would change the floating-point result (F3).
- **Building the calculator.** The no-arg constructor, the only one, creates the three policies and collects them once into a `Map<PlanType, InterestPolicy>` with `Collectors.toMap(InterestPolicy::planType, identity())`; a duplicate plan type is rejected without guard code of our own. The map key is the policy's plan, so a policy needs no `isApplicable`-style method.
- **Why the policies are not injected (D19).** `new TimeDepositCalculator()` is the legacy way to obtain the calculator and must keep working. Without that contract the policies would be Spring beans collected by dependency injection, and a new plan would not touch the calculator at all. A no-arg constructor next to injection would keep the list in two places, so the list stays in one place and an ArchUnit rule fails when an `InterestPolicy` implementation is not created in the calculator.
- **`updateBalance` is short, with no `if` chains or plan logic.** For each deposit it looks up `policies.get(PlanType.fromCode(...))`; a missing entry, as for `UNKNOWN`, means interest 0 and a warning in the log. It then applies the legacy rounding once, centrally, `balance + new BigDecimal(interest).setScale(2, HALF_UP).doubleValue()`, and calls `setBalance` on every deposit, like the legacy code.
- **Adding a plan:** an enum constant, one `InterestPolicy` class, one entry in the calculator's constructor and its boundary tests. Forgetting the entry fails `ArchitectureTest`.
- A null plan type or balance is invalid input. The legacy code failed on it with an implicit `NullPointerException`; the calculator now checks both explicitly at the start of each deposit (A10).

## 3. Ports

- **Inbound:** `TimeDepositService` with `getAll()` and `updateAllBalances()`, a class without an interface: a use-case interface would have one implementation and one caller (D13).
- **Outbound:** `application.port.TimeDepositRepository`. It takes and returns domain models only (D18):
  - `List<TimeDeposit> findAllDeposits()`
  - `List<TimeDepositDetails> findAllWithWithdrawals()`
  - `void updateBalances(List<TimeDeposit>)`

## 4. Models, mapping and number conversion

- **Domain models** (`domain.model`, plain records without logic):
  - `Withdrawal(int id, int timeDepositId, BigDecimal amount, LocalDate date)`, the columns of the `withdrawals` table; the API mapper leaves `timeDepositId` out of the response (A9)
  - `TimeDepositDetails(TimeDeposit deposit, List<Withdrawal> withdrawals)`, which reuses `TimeDeposit` instead of adding a field to it (D22)
- **DTOs** (`adapter.controller.dto`, plain records): `TimeDepositResponse(id, planType, balance, days, List<WithdrawalResponse> withdrawals)` and `WithdrawalResponse(id, amount, date)`, ordered as in A9. The endpoints take no body, so there are no request DTOs.
- **Mapping is hand-written, with no mapping framework** (D17), and lives at each boundary:
  - REST: `TimeDepositApiMapper`, a final class with a static `toResponse(TimeDepositDetails)`; it is stateless, so it is not a bean.
  - Persistence: rows are mapped straight to and from domain models inside `JdbcTimeDepositRepository`. There are no persistence model classes (D18).
- **Double ↔ BigDecimal conversion happens only in the adapters** (D7):
  - Persistence: `BigDecimal.valueOf(double)` on write, `doubleValue()` on read, in the adapter's SQL parameters and row mapping.
  - REST: `BigDecimal.valueOf(double)` for `balance`, in the mapper.
- **Storage never rounds the balance** (D9). `valueOf` gives the shortest decimal form, and unconstrained `numeric` stores it exactly, so a double survives the round trip unchanged. Values such as `1001.6600000000001` stay visible ([02-known-issues.md](02-known-issues.md)).
- **No scientific notation.** `spring.jackson.generator.write-bigdecimal-as-plain: true` keeps a large whole-number balance such as `10000000` in plain notation. Otherwise `valueOf` would produce `1.0E+7`.

## 5. Persistence

- **Spring JDBC, not JPA** (D10): one `JdbcTemplate` for the queries and the batched balance update. The SQL is three named constants, and rows are mapped in small named methods. There are two tables and three queries, and the legacy `TimeDeposit` is not an entity. JPA would add entity classes, mapping and naming-strategy configuration.
- **Reading** takes two queries: the deposits, then all withdrawals. The withdrawals are grouped by `timeDepositId` in Java, so there is no N+1 problem.
- **Identifiers** are the README names verbatim, as quoted PostgreSQL identifiers (D11). The master changelog (`db/changelog/db.changelog-master.yaml`) includes one formatted-SQL file with the schema. The sample data is a separate file, included only by `db.changelog-local.yaml`, which the `local` profile selects (D24).

  | Table | Column | Type |
  |---|---|---|
  | `"timeDeposits"` | `id` | `integer` primary key |
  | `"timeDeposits"` | `"planType"` | `varchar NOT NULL` |
  | `"timeDeposits"` | `days` | `integer NOT NULL` |
  | `"timeDeposits"` | `balance` | `numeric NOT NULL` |
  | `withdrawals` | `id` | `integer` primary key |
  | `withdrawals` | `"timeDepositId"` | `integer NOT NULL` references `"timeDeposits"` |
  | `withdrawals` | `amount` | `numeric(19,2) NOT NULL` |
  | `withdrawals` | `date` | `date NOT NULL` |

  `balance` is unconstrained because it is calculated (D9). `amount` is never calculated, so two decimals suffice. IDs are plain integers, because nothing is inserted through the API.
- **Transactions:** `updateAllBalances()` is `@Transactional`. It reads all deposits, calls `calculator.updateBalance(list)` and writes the balances back, so either all of them are updated or none are. `getAll()` is `@Transactional(readOnly = true)`.
- **Batches:** the balance update is sent in JDBC batches of 1000 rows, all inside that one transaction (D21).
- **Accepted gaps:** all deposits are loaded into memory and written in one transaction, and a repeated POST accrues another month (A3). There is no locking: the service is assumed to run as a single instance with no overlapping updates, because overlapping updates would silently lose one of them (A11).

## 6. API and OpenAPI

- **Code-first with springdoc-openapi** (D12): `/v3/api-docs`, Swagger UI at `/swagger-ui.html`.

  | Method | Path | Response | Behaviour |
  |---|---|---|---|
  | `GET` | `/time-deposits` | `200` | Returns the list of `TimeDepositResponse` described in section 4 |
  | `POST` | `/time-deposits/balance-updates` | `204` | Accrues one month of interest; not idempotent (A3, D23) |

- **OpenAPI metadata lives in one configuration class**, `adapter.controller.config.OpenApiConfig`: the title, version, description and relative server URL, and a customizer that marks every response field as required. The response records carry no OpenAPI annotations; each controller method has a one-line `@Operation` summary.
- **Committed contract.** The generated contract is compared in strict mode with a committed `api-docs.json`. That pins **exactly these two operations** (D6). The README links to the file.

## 7. Test strategy

The [test requirements](05-test-requirements.md) apply: test behaviour, not interactions; never mock our own classes; expected values are hand-written literals or files, never recomputed with the production formula.

- **Unit tests** (plain JUnit) cover branch-heavy logic.
- **API tests** use the real Spring context, MockMvc and PostgreSQL in Testcontainers, with one main positive scenario per endpoint.

| Test | Kind | Covers |
|---|---|---|
| `TimeDepositCalculatorLegacyBehaviourTest` | Unit, parameterized | The behaviour `updateBalance` had before the refactoring, organised by scenario: one table of deposits that earn nothing (boundaries 29, 30, 45, 366 and codes that do not match exactly: `Basic`, `Student`, `Premium`, `gold`), one table of deposits that earn interest (boundaries 31, 46, 365, 366), then the rounding cases from 02-known-issues and the list behaviour (an empty list, an unknown plan before a known one, a repeated call). `PlanType` and the policies get no separate tests; these cases exercise every code and every window. |
| `ArchitectureTest` | Unit (ArchUnit) | The rules listed below: the dependency rule from section 1 and the policy registration rule (D19) |
| `GetTimeDepositsTest` | API | `GET` body compared with `response/success.json` (strict, order matters). The data includes a deposit without withdrawals and one with a whole-number balance of 10,000,000; a supplementary check on the raw body pins its plain notation, because JSON comparison is numeric. `@ExpectedDataSet` uses the same file as `@DataSet`, so the read provably changes nothing. |
| `UpdateBalancesTest` | API | `POST` returns `204`. `@ExpectedDataSet` holds hand-calculated balances for: one deposit per plan, an ineligible deposit, an unknown plan, and a balance with more than 2 decimals (D9). The scenario needs no withdrawals, so the table is listed empty in both datasets, which also shows that the update creates none (A5). |
| `OpenApiContractTest` | API | The full `/v3/api-docs` compared with `openapi/response/api-docs.json` in strict mode. The file is reviewed by hand once. |

- **`ArchitectureTest`** imports `org.ikigaidigital` main classes only (`ImportOption.DoNotIncludeTests`), because test classes also sit in the root package. Each rule is added in the commit where its packages first exist, so `allowEmptyShould` is left off and an empty match after a later refactoring fails loudly:
  - `..domain..` does not depend on `..application..`, `..adapter..` or `org.springframework..`.
  - Every class that implements `InterestPolicy` is created in `TimeDepositCalculator` (D19).
  - `..application..` does not depend on `..adapter..`, `org.springframework.web..` or `org.springframework.jdbc..`.
  - `..adapter.controller..` and `..adapter.persistence..` do not depend on each other.
- **Not tested separately:** the persistence adapter, the mapper and the service; the API tests exercise every query, mapped field and the orchestration. There are no negative API scenarios: neither endpoint takes input, and the README excludes invalid-input handling.

**Infrastructure** (D15):
- **`org.ikigaidigital.e2e.BaseApiTest`** (abstract) carries `@SpringBootTest`, `@AutoConfigureMockMvc`, `@Import(TestcontainersConfiguration.class)`, `@DBRider` and `@DBUnit(caseSensitiveTableNames = true, escapePattern = "\"?\"")` for the quoted identifiers (D11). It provides `mockMvc` and `getContentFromFile(path)`; its package lies under the main class's, so `@SpringBootTest` finds the configuration.
- **Datasets** use `CLEAN_INSERT` and list both tables, parent first, so rows left by another test are removed; a table the scenario does not need is listed empty. Decimal values are written as strings, so they never pass through `double` and compare exactly.
- **Resources:** one folder per endpoint, files named by scenario and referenced by full path:

  ```
  src/test/resources/e2e/
    get-time-deposits/   data-set/success.json   response/success.json
    update-balances/     data-set/success.json   expected-data-set/success.json
    openapi/             response/api-docs.json
  ```

## 8. Local run

- `cd java && ./mvnw spring-boot:run` starts PostgreSQL from `compose.yaml` through Spring Boot's Docker Compose support and runs the app with the `local` profile, which the Maven plugin activates. The profile loads the sample data and removes the container on shutdown, so the data is reset on every start. Prerequisites: JDK 21 and a running Docker (D14).
- The tests do not use Compose: `BaseApiTest` starts PostgreSQL with Testcontainers and loads its own datasets.
- `application.yml` keeps standard datasource properties and the schema-only changelog, so the app can also run against a real database without receiving sample rows.

## 9. Logging

- **API calls:** `RequestLoggingFilter` in `adapter.controller.config` writes one `INFO` line per call to the two endpoints: method, path, status and duration. Swagger UI and the contract are not logged (D20).
- **Use cases:** `TimeDepositService` logs how many deposits it returned, and how many balances it read and saved.
- **Unusual data:** `TimeDepositCalculator` writes a `WARN` line for a deposit with an unknown plan code, with the code as received, and for a negative balance.
- **SQL:** `JdbcTemplate` logs every statement at `DEBUG`; the `local` profile switches that level on, so the statements are visible on a local run and silent otherwise.

## 10. Refactoring sequence

The work goes in this order, and every step leaves the build and all tests green:

1. **Pin the existing behaviour.** Tests for `updateBalance` are written against the untouched legacy code: the plan rules, both sides of every day boundary, unknown plan codes and the rounding (F1–F3). They are the safety net for everything that follows.
2. **Set up the infrastructure:** Spring Boot, PostgreSQL in Testcontainers, the Liquibase schema.
3. **Refactor the domain under those tests:** the `if` chain becomes one `InterestPolicy` per plan behind the unchanged `updateBalance`. The tests are not edited to make this step pass.
4. **Add the read endpoint** as one slice through all layers, with its API test.
5. **Add the update endpoint** the same way.
6. **Pin the OpenAPI contract** once both operations exist.
7. **Review the whole solution:** architecture and dependency rules, dead code, comments, documents, then the full test run.

## 11. Technical decisions

| ID | Decision | Rationale |
|----|----------|-----------|
| D1 | Java 21, Spring Boot 3.5.x, Maven (with the Maven wrapper). | Java is the language of the solution. 21 is an LTS release available in the environment, and Boot 3.5 is the newest 3.x line, with stable springdoc 2.x, Testcontainers and Database Rider support. The wrapper removes the need for a local Maven installation. |
| D2 | PostgreSQL; Testcontainers for integration tests. | Preferred stack in README. |
| D3 | Liquibase for schema migrations and the local sample data. | Versioned, reproducible schema in plain SQL. |
| D4 | OpenAPI / Swagger UI for the API contract. | Preferred stack in README. |
| D5 | Hexagonal architecture: domain (deposits, interest policies) has no framework dependencies; REST and persistence are adapters. Domain models live in `domain.model`, interest policies in `domain.policy`; the legacy `TimeDeposit` and `TimeDepositCalculator` stay in the root package, outside the package layout. | Preferred stack in README. Separate `model` and `policy` packages keep data and calculation apart. Moving the two legacy classes would change their fully qualified names and break existing callers of the shared classes, so backward compatibility wins over a uniform layout. |
| D6 | Exactly two endpoints. | Hard requirement in README. |
| D7 | `BigDecimal` / `numeric` in DB and API, converted at the adapter boundary (the JDBC adapter's row mapping and the controller's DTO mapper); the domain keeps `TimeDeposit` with `Double`. | DB schema requires Decimal; `TimeDeposit` must not change. |
| D8 | Work is pushed to the fork `saloidvl/time-deposit-take-home-kata` with atomic commits. | Submission instructions. |
| D9 | `balance` is stored as unconstrained `numeric` and converted with `BigDecimal.valueOf(double)` / `doubleValue()`, so a double balance survives the round trip unchanged. Storage never rounds. | Rounding on save would change the results of later updates (F3). The visible extra decimals are listed in `02-known-issues.md`; rounding on save is a future improvement there. |
| D10 | Persistence uses Spring JDBC, not JPA: one `JdbcTemplate` for the two queries and the batched balance update. | Two tables and three queries; JPA would add entities, naming-strategy configuration and lazy-loading/N+1 concerns with no benefit. |
| D11 | Tables and columns use the README names exactly, as quoted PostgreSQL identifiers (`"timeDeposits"`, `"planType"`, `"timeDepositId"`). The schema is a Liquibase formatted-SQL file, included from a master changelog. | Matches the required schema literally; all SQL is in one adapter class and the changelog files, so the quoting cost stays small. |
| D12 | OpenAPI is code-first with springdoc-openapi (`/v3/api-docs`, Swagger UI at `/swagger-ui.html`). A test compares the full generated `/v3/api-docs` with a committed contract file (`src/test/resources/e2e/openapi/response/api-docs.json`) in **strict** mode. The README links to that file. The title, version and a relative server URL are declared in `adapter.controller.config.OpenApiConfig`, which also marks every response field as required; the response records carry no OpenAPI annotations, and the controller has only a one-line `@Operation` summary per endpoint. | Simplest way to provide a Swagger contract for two endpoints; no generator plugin or generated code. The file is reviewed by hand once. Strict mode makes an extra operation fail the test, which also enforces D6. |
| D13 | No interface with a single method and a single implementation. The inbound port is the public API of the class `TimeDepositService` (`getAll`, `updateAllBalances`), with no use-case interfaces. The only interfaces are `InterestPolicy` (three implementations) and the outbound port `application.port.TimeDepositRepository` (three methods). The domain (`domain..` plus the legacy `TimeDeposit` and `TimeDepositCalculator`) uses no Spring types; the application layer may use Spring `@Service`/`@Transactional`. An ArchUnit test enforces the rules: `domain..` must not depend on the application layer, the adapters or Spring; the application layer must not depend on adapters, web or JDBC; the two adapters must not depend on each other. | Interfaces with a single implementation and a single caller add no value. The outbound port is what keeps the application independent of JDBC. The `port` package makes the hexagon visible without new types. Transaction demarcation belongs to the use case. |
| D14 | The app runs locally with `./mvnw spring-boot:run`: Spring Boot's Docker Compose support starts PostgreSQL from `java/compose.yaml`, and the Maven plugin activates the `local` profile, which adds the sample data and removes the container on shutdown. The tests do not use Compose; they start PostgreSQL with Testcontainers. | JDK 21 and Docker are the only prerequisites. Removing the container on shutdown resets the sample data on every start. |
| D15 | API tests live in `org.ikigaidigital.e2e` and extend one abstract `BaseApiTest` (real Spring context, MockMvc, Testcontainers PostgreSQL). They compare the full response with a `response/*.json` file. Database state is set up and checked with Database Rider `@DataSet` / `@ExpectedDataSet` (`rider-junit5`, case-sensitive quoted names), not with JDBC assertions in the test body. Decimal values in datasets are written as strings. | Follows the [test requirements](05-test-requirements.md). Database Rider is a single test dependency; Spring's `@Sql` can only set up data and cannot check the expected state. The package lies under the main class's package, so `@SpringBootTest` finds it. JSON comparison uses Spring's built-in `content().json(...)`. |
| D16 | Plan codes are the enum `domain.model.PlanType` (`BASIC`, `STUDENT`, `PREMIUM`), which holds the codes only, plus `UNKNOWN` for any other text. `PlanType.fromCode(String)` never throws: it compares the text with `equals` against the lower-case code field (never `name()` or `valueOf`), so the match is exact and case-sensitive, and no match gives `UNKNOWN` (A2). `InterestPolicy.planType()` returns a `PlanType` and is never `UNKNOWN`, so `UNKNOWN` has no policy and earns 0. Rates and day windows live in the policy classes, never in the enum. The enum only selects the policy: `TimeDeposit.planType` stays a `String`, and there is deliberately no enum-to-text conversion, because the original text is never rebuilt from the enum and so nothing can be lost. | Explicit values instead of string constants, with a lookup that cannot fail on an unknown code. The matching lives in this one method, so the rule can be changed or reverted in a single line. Codes and rules change separately. `TimeDeposit` must not change. |
| D17 | The REST adapter is `adapter.controller`; every request/response type lives in `adapter.controller.dto` (only response records, since the endpoints take no body). The controller takes and returns DTOs only; the code behind it works with domain models. Mapping is hand-written in `TimeDepositApiMapper` (no MapStruct); DTOs and domain models are plain records without logic. | One clear boundary for the API contract. Mapping two small records by hand is shorter and easier to explain than a mapping framework. |
| D18 | There are no persistence model classes. The port `TimeDepositRepository` takes and returns domain models, and `JdbcTimeDepositRepository` maps rows to and from them directly. | With Spring JDBC there are no entities to keep apart from the domain; separate row classes would only be copied field by field into the domain models. The SQL and the row mapping stay in one adapter class. |
| D19 | The interest policies are listed in the no-arg constructor of `TimeDepositCalculator`, its only constructor, not injected by Spring. An ArchUnit rule checks that every `InterestPolicy` implementation is created in the calculator; two policies for the same plan fail at construction, because `Collectors.toMap` rejects a duplicate key. | `new TimeDepositCalculator()` is the legacy way to obtain the calculator and must keep working for existing callers. Without that contract the policies would be Spring beans collected by dependency injection, so that a new plan would need no change in the calculator. Keeping a no-arg constructor next to injection would put the list in two places that can drift apart, so backward compatibility is paid for with one extra line per new plan, and the test catches a policy that was written but not listed. |
| D20 | Logging uses SLF4J, which comes with Spring Boot. A servlet filter in `adapter.controller.config` writes one line per API call (method, path, status, duration). The service logs how many deposits it read and saved. The calculator warns about a deposit with an unknown plan code, printing the code as received, and about a negative balance. SQL statements are logged by `JdbcTemplate` itself at `DEBUG`, switched on only in the `local` profile. | A reader of the log can see what was called, how much was processed and why a deposit earned nothing, without a debugger. The standard `JdbcTemplate` logging needs no code; a small filter is used for API calls because Spring's `CommonsRequestLoggingFilter` reports neither the status nor the duration. SQL logging stays off by default because it prints every statement. |
| D21 | The balance update is sent to the database in JDBC batches of 1000 rows (`BATCH_SIZE` in the JDBC adapter), inside the one transaction of the use case. | Bounds the size of a single batch when there are many deposits, at the cost of one constant. All deposits are still loaded into memory and written in one transaction (see the accepted gaps in `03-architecture.md`). |
| D22 | The GET response uses a separate read model, `TimeDepositDetails`, that combines a deposit with its withdrawals. | `TimeDeposit` has no withdrawals field and must not change incompatibly. |
| D23 | The balance update is exposed as `POST`, not `PUT`. | The update is not idempotent (A3), and `PUT` implies idempotency. |
| D24 | Sample deposits and withdrawals are loaded only on the local run, so the endpoints can be exercised via Swagger right away. The default changelog creates the schema only, so a real database never receives sample rows. | No data source is specified. |
