# Assumptions

The assignment and the existing code leave some points open, and there is nobody to ask. Each open point is resolved here by choosing one interpretation, and the choice is justified in a code comment that names its ID, as the assignment requires.

Technical decisions are not assumptions; they are listed in [03-architecture.md](03-architecture.md) (section 11). Problems found in the existing code are listed in [02-known-issues.md](02-known-issues.md).

## 1. Facts taken from the existing code

These are not assumptions. The code states them, and the assignment requires the behaviour of `updateBalance` to stay unchanged.

| ID | Fact | Source |
|----|------|--------|
| F1 | Interest is monthly: the annual rate divided by 12, applied once per `updateBalance` call. | The existing implementation. |
| F2 | Day boundaries: no interest while `days <= 30`; premium earns only when `days > 45`; student earns only while `days < 366` (day 365 earns, day 366 does not). | The exact conditions in the existing code. |
| F3 | Interest is rounded to 2 decimals with `new BigDecimal(double).setScale(2, HALF_UP)` and added to the `double` balance. This must be preserved bit for bit. | The existing code, and the requirement that the behaviour of `updateBalance` stays unchanged: any other rounding would alter balances. |

## 2. Assumptions

| ID | Assumption | Rationale |
|----|------------|-----------|
| A1 | The existing `TimeDepositCalculator.updateBalance` is the source of truth for interest rules; where README wording differs, code wins. | README: "The existing `updateBalance` method is functioning correctly. Ensure its behavior remains unchanged." |
| A2 | An unknown `planType` is not an error: the balance stays unchanged. Plan codes are matched exactly and case-sensitively, so `Basic` is not the `basic` plan and earns nothing. The stored text is kept as it is and returned unchanged by GET. | Existing behaviour of `updateBalance`, which must not change; README says invalid input handling is not required. Treating `Basic` and `basic` as different plans is questionable from a business point of view and is listed in `02-known-issues.md`. |
| A3 | The service does **not** control who calls the balance update or how many times. The update is **not idempotent**: each call accrues one more month of interest (compounded on the new balance), and `days` is not incremented. Calling it exactly once per month is the caller's responsibility (e.g. an external scheduler). | The required `timeDeposits` schema has no "last accrual date" column, and `updateBalance` behaviour must not change. **Known risk:** production would need a guard against duplicate runs. |
| A4 | `days` is maintained outside this service (it is input data, not derived from a date). | No opening date exists in the required schema. |
| A5 | Withdrawals do not affect interest calculation; they are stored and returned by the GET endpoint only. | README does not link withdrawals to interest. |
| A6 | Only Java is delivered. The other starter ports (Kotlin, C#, TypeScript, Python) are removed from the fork to keep the repository focused on the solution. | The solution is delivered in one language. |
| A7 | The stored `balance` is the current balance and **already reflects all withdrawals**. The balance update does not subtract or otherwise take withdrawals into account; interest is calculated on `balance` as stored. | README does not define how withdrawals affect the balance; recalculating from history would change `updateBalance` behaviour. |
| A8 | The 30-day grace period belongs to each existing plan, not to all plans globally: every interest policy defines its own full eligibility window (basic `> 30`, student `> 30` and `< 366`, premium `> 45`). A future plan may define a different grace period. | README: "No interest is applied for the first 30 days for any **existing** plans." Result is identical to the current code. |
| A9 | The GET response has the README fields, and each withdrawal appears as `{id, amount, date}` (the table columns without the FK). Deposits are ordered by `id`, withdrawals by `date` and then `id`. The balance update `POST` returns `204 No Content`; the result is visible through GET. | README does not define the shape of `withdrawals`; stable ordering makes the output predictable and testable. |
| A10 | A null plan type or a null balance is invalid input: `updateBalance` fails on that deposit with a `NullPointerException`, and the check is explicit. One case differs from the legacy code: a deposit with a null plan type and `days <= 30` used to pass through unchanged and now fails as well. | The legacy code failed on these nulls implicitly, when it dereferenced the plan type of a deposit older than 30 days or unboxed the balance. An explicit check gives the same failure with a clear cause. Invalid input is out of scope (README), and neither value can come from the database, where both columns are `NOT NULL`. |
| A11 | The service runs as a single instance, and the balance update is never called concurrently with itself: a second call starts only after the first has finished. | No locking is implemented. With several instances, or overlapping calls on one instance, both calls would read the same balances and the later write would overwrite the earlier one: a silent lost update, with one month of interest applied instead of two. Preventing that needs row or advisory locks, which is listed in `02-known-issues.md`. |
