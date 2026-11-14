--liquibase formatted sql

-- Loaded only by the local changelog, so the endpoints can be tried from Swagger (D24).
-- Every plan is on both sides of its day limits; balances already reflect the withdrawals (A7).
--changeset time-deposit:sample-deposits
INSERT INTO "timeDeposits" (id, "planType", days, balance) VALUES
    (1, 'basic', 20, 1000.00),
    (2, 'basic', 45, 5000.00),
    (3, 'student', 100, 2000.00),
    (4, 'student', 400, 3000.00),
    (5, 'premium', 40, 10000.00),
    (6, 'premium', 90, 20000.00),
    (7, 'basic', 31, 1234.56),
    (8, 'basic', 730, 99999.99),
    (9, 'student', 365, 987.65),
    (10, 'student', 366, 4321.09),
    (11, 'premium', 45, 2500.50),
    (12, 'premium', 46, 15750.75),
    (13, 'gold', 200, 750.25);

INSERT INTO withdrawals (id, "timeDepositId", amount, date) VALUES
    (1, 2, 200.00, '2026-10-20'),
    (2, 3, 50.00, '2026-09-01'),
    (3, 3, 75.50, '2026-10-10'),
    (4, 6, 1000.00, '2026-10-01'),
    (5, 7, 15.44, '2026-10-28'),
    (6, 8, 0.01, '2025-12-31'),
    (7, 9, 12.35, '2026-03-17'),
    (8, 9, 499.99, '2026-08-05'),
    (9, 12, 249.25, '2026-11-02'),
    (10, 13, 49.75, '2026-06-30');
--rollback DELETE FROM withdrawals;
--rollback DELETE FROM "timeDeposits";
