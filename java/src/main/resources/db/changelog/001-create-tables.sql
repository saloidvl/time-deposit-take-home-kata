--liquibase formatted sql

-- Table and column names follow the README literally, so the camelCase ones are quoted identifiers (D11).
-- balance is unconstrained numeric so that a double balance is stored without rounding (D9).
--changeset time-deposit:001-create-tables
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
