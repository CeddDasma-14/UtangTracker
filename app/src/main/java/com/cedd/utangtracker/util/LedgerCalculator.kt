package com.cedd.utangtracker.util

import com.cedd.utangtracker.data.local.entity.LedgerEntryEntity
import java.util.Calendar

object LedgerCalculator {

    /**
     * Starting balance = principal + (principal × rate × cycleMonths)
     * e.g. 17,700.16 + (17,700.16 × 0.10 × 3) = 23,010.21
     */
    fun startingBalance(principal: Double, interestRate: Double, cycleMonths: Int): Double =
        principal + (principal * (interestRate / 100.0) * cycleMonths)

    /**
     * Monthly bill = startingBalance / cycleMonths
     * e.g. 23,010.21 / 3 = 7,670.07
     */
    fun monthlyBill(principal: Double, interestRate: Double, cycleMonths: Int): Double =
        startingBalance(principal, interestRate, cycleMonths) / cycleMonths

    /**
     * Fixed interest per missed month = principal × rate
     * e.g. 17,700.16 × 0.10 = 1,770.016
     */
    fun fixedInterest(principal: Double, interestRate: Double): Double =
        principal * (interestRate / 100.0)

    /**
     * Compute a single month's ledger result.
     *
     * Rules (from Excel):
     * - If payment > 0 → no interest added, balance = opening + carryOver - payment
     * - If payment = 0 (missed) → interest added, balance = opening + fixedInterest + carryOver
     * - Shortfall = max(0, monthlyBill - payment) when payment > 0 but < bill
     */
    fun compute(
        openingBalance: Double,
        principal: Double,
        interestRate: Double,    // e.g. 10.0 (percent)
        carryOver: Double,       // carry-over to add this month (0 = none)
        monthlyBill: Double,
        paymentAmount: Double
    ): LedgerResult {
        val interest = fixedInterest(principal, interestRate)
        val isMissed = paymentAmount == 0.0
        val interestAdded = if (isMissed) interest else 0.0
        val shortfall = if (!isMissed && paymentAmount < monthlyBill)
            monthlyBill - paymentAmount else 0.0
        val closing = openingBalance + interestAdded + carryOver - paymentAmount
        return LedgerResult(
            interestAdded = interestAdded,
            carryOverAdded = carryOver,
            closingBalance = closing,
            isMissedPayment = isMissed,
            shortfall = shortfall
        )
    }

    /**
     * Returns the (year, month) of the next month after existing entries.
     * Defaults to debt creation month if no entries.
     */
    fun nextMonth(entries: List<LedgerEntryEntity>, startMillis: Long): Pair<Int, Int> {
        return if (entries.isEmpty()) {
            val cal = Calendar.getInstance().apply { timeInMillis = startMillis }
            cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
        } else {
            val last = entries.last()
            if (last.month == 12) (last.year + 1) to 1
            else last.year to (last.month + 1)
        }
    }

    data class LedgerResult(
        val interestAdded: Double,
        val carryOverAdded: Double,
        val closingBalance: Double,
        val isMissedPayment: Boolean,
        val shortfall: Double = 0.0
    )
}
