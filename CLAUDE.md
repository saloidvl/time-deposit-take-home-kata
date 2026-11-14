# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## time-deposit-take-home-kata

Take-home refactoring kata (XA Bank time deposits), solved in Java (`java/`); the user is a Java developer. `README.md` is the assignment and the source of truth for requirements — re-read it before planning. Facts (F*) and assumptions (A*) live in `docs/01-assumptions.md`, technical decisions (D*) in `docs/03-architecture.md`; money-calculation issues deliberately left unfixed live in `docs/02-known-issues.md`.

## Policy

- Never make breaking changes to the shared `TimeDeposit` class (`id`, `planType`, `balance` as `Double`, `days`).
- Never change the signature or behaviour of `TimeDepositCalculator.updateBalance(List<TimeDeposit>)` — the README states it works correctly. Keep it as a facade over the new domain logic.
- Exactly two REST endpoints: update all balances, and get all deposits (`id`, `planType`, `balance`, `days`, `withdrawals`). No others. The update is non-idempotent by design (A3).
- DB tables `timeDeposits` and `withdrawals` (FK `timeDepositId`, `amount`, `date`) exactly as the README specifies.
- Agreed stack: Java 21, Spring Boot 3, Maven, PostgreSQL + Testcontainers, Liquibase (not Flyway), OpenAPI/Swagger, hexagonal architecture.
- Resolve every ambiguity with an assumption: add it to `docs/01-assumptions.md` and justify it in a code comment referencing its ID.
- Comments say why, never what: no Javadoc or comment that restates the code. Keep a comment only where the logic follows from an assumption or decision, one sentence plus its ID.
- Write all files in English (code, comments, tests, docs, commit messages).
- Atomic commits: one logical change per commit; the project builds and tests pass at every commit. Message: short imperative English summary line (e.g. `Add characterization tests for updateBalance`), optional body explaining why.
- Commit only after the user approves; commit locally first. Push to `origin` (the user's public fork `saloidvl/time-deposit-take-home-kata`) only when the user explicitly says so. Never open a PR or branch against `ikigai-digital`.
- Never commit agent framework files or working drafts; they stay local and out of git.
- `docs/` holds only final, clean documents numbered in the order the stages produced them (`01-…`, `02-…`); drafts and discarded versions never go there.

## Where things are

- Legacy code: `java/src/main/java/org/ikigaidigital/` (`TimeDeposit`, `TimeDepositCalculator`); both stay in the root package for backward compatibility (D5). The legacy behaviour is pinned by `TimeDepositCalculatorLegacyBehaviourTest`.
- Easy-to-miss README deliverables: Swagger instructions for triggering the endpoints; documentation of the AI setup (tools, rules/prompts like this file, which parts were AI-assisted and why). The AI workflow is described in `docs/04-ai-setup.md`.

## Running and verifying

- Run Maven from `java/`: `./mvnw test`; single class / method: `./mvnw test -Dtest=TimeDepositCalculatorLegacyBehaviourTest#methodName`. Docker must be running for Testcontainers.
- Run the app locally with `./mvnw spring-boot:run` (Docker Compose, `local` profile with sample data, D14).

## Conventions that differ from defaults

- `updateBalance` behaviour to pin with characterization tests before refactoring: no interest unless `days > 30`; `basic` 1%/12; `student` 3%/12 only while `days < 366`; `premium` 5%/12 only when `days > 45`; unknown plan type → balance unchanged; plan type match is case-sensitive.
- Monthly interest is rounded with `new BigDecimal(double).setScale(2, HALF_UP)` and added to the balance as a `double`; tests must assert this exact rounding. Do not fix the issues in `docs/02-known-issues.md`.
- Characterization tests first, including boundaries (days 30/31, 45/46, 365/366, unknown plan, rounding). Test behaviour/outcomes, not interactions; avoid heavy mocking and redundant tests.
- Hexagonal layering: domain (deposits, interest policies) must not depend on Spring/JPA/web; REST and persistence adapters depend inward. Extensible per-plan interest strategy, SOLID, no over-engineering.
- Follow the user's sequence and say which step you are on and why: requirements → code analysis → identify issues → tests → coverage review → refactoring → architecture/dependency review → dead-code removal → final review → full test run.
- Small steps; run the relevant tests after each; every generated line must be explainable by the user.


## Test rules

- Tests must follow [docs/05-test-requirements.md](docs/05-test-requirements.md).
