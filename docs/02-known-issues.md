# Known Issues and Future Improvements

Issues found in the existing interest calculation and plan matching. They are **not fixed** in this solution, because `updateBalance` behaviour must stay unchanged (see A1, F3 in `01-assumptions.md`).

## Money calculation issues

- **Balance is a `double`.** Binary floating point cannot represent most decimal amounts exactly, so money values are approximate.
- **Error accumulates between updates.** Two consecutive updates of `1000.00` (basic, 31 days) give `1001.6600000000001` instead of `1001.66`.
- **Rounding depends on the binary value, not the decimal one.** `new BigDecimal(double)` uses the exact binary value, so a decimal half can round down: `18.00` (basic) → interest `0.015` is stored as `0.01499…` → rounded to `0.01`, not `0.02`.
- **Only the interest is rounded, not the balance.** A balance with more than 2 decimals keeps them: `100.123` → `100.203`.
- **These values reach the API unchanged.** Balances are stored and returned exactly as calculated (D9), for example `1001.6600000000001`.
- **`HALF_UP` rounding is biased.** Across many deposits, rounding errors drift upwards instead of cancelling out.

## Negative balance issue

- **A negative balance earns negative interest.** The calculation does not check the sign, so an eligible deposit with a balance of `-1000.00` (basic) becomes `-1000.83`. Whether a time deposit can be negative at all is a business question; the behaviour is kept because `updateBalance` must not change (A1), and invalid input is out of scope (README).

## Plan matching issue

- **Plan codes are case-sensitive.** `Basic` or `BASIC` is not recognised as the `basic` plan, so such a deposit silently earns no interest. From a business point of view these are the same plan. The behaviour is kept because `updateBalance` must not change (A2).

## Future improvements

- Use `BigDecimal` for balances and interest throughout the calculation.
- Round the resulting balance (not only the interest) to 2 decimals before saving. This is a product or accounting decision: it changes every later accrual, and balances once rounded cannot be restored (D9).
- Guard against duplicate balance updates, for example with a last-accrual date column. This goes beyond the required schema (A3).
- Lock the deposits during the balance update (`SELECT ... FOR UPDATE` or an advisory lock), so that overlapping updates or several service instances cannot lose an update (A11).
- Match plan codes regardless of letter case, or reject unknown codes when a deposit is created. The matching lives in one method, `PlanType.fromCode`, so this is a one-line change (D16).
- Use banker's rounding (`RoundingMode.HALF_EVEN`) so rounding errors are spread evenly and totals reconcile better with accounting.
