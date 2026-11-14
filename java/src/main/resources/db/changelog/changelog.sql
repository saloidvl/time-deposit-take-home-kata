--liquibase formatted sql

-- Table and column names follow the README literally, so the camelCase ones are quoted identifiers (D11).
-- balance is unconstrained numeric so a double balance is stored without rounding (D9).
--changeset time-deposit:001-create-schema
CREATE TABLE "timeDeposits" (
    id integer PRIMARY KEY,
    "planType" varchar NOT NULL,
    days integer NOT NULL,
    balance numeric NOT NULL
);

CREATE TABLE withdrawals (
    id integer PRIMARY KEY,
    "timeDepositId" integer NOT NULL REFERENCES "timeDeposits"(id),
    amount numeric(19,2) NOT NULL,
    date date NOT NULL
);
--rollback DROP TABLE withdrawals;
--rollback DROP TABLE "timeDeposits";

-- Sample data so the endpoints can be tried from Swagger right away; it covers every plan on both sides of its day thresholds (D24). Balances already reflect the withdrawals (A7).
--changeset time-deposit:002-seed-sample-data
INSERT INTO "timeDeposits" (id, "planType", days, balance) VALUES
    (1, 'basic', 20, 1000.00),
    (2, 'basic', 45, 5000.00),
    (3, 'student', 100, 2000.00),
    (4, 'student', 400, 3000.00),
    (5, 'premium', 40, 10000.00),
    (6, 'premium', 90, 20000.00);

INSERT INTO withdrawals (id, "timeDepositId", amount, date) VALUES
    (1, 2, 200.00, '2026-10-20'),
    (2, 3, 50.00, '2026-09-01'),
    (3, 3, 75.50, '2026-10-10'),
    (4, 6, 1000.00, '2026-10-01');
--rollback DELETE FROM withdrawals;
--rollback DELETE FROM "timeDeposits";
