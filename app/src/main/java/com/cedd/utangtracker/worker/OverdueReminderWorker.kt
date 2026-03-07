package com.cedd.utangtracker.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cedd.utangtracker.data.repository.UtangRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class OverdueReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repo: UtangRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        repo.markOverdueDebts()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ── Overdue notification ──────────────────────────────────────────
        val overdueDebts = repo.getOverdueDebtsSnapshot()
        if (overdueDebts.isNotEmpty()) {
            val (title, body) = if (overdueDebts.size == 1) {
                val debt   = overdueDebts.first()
                val name   = repo.getPersonById(debt.personId)?.name ?: "Unknown"
                "Overdue utang!" to "$name's debt of ₱${"%.0f".format(debt.amount)} is overdue."
            } else {
                "${overdueDebts.size} overdue utang!" to
                    "You have ${overdueDebts.size} debts past their due date."
            }
            nm.notify(
                NOTIF_ID_OVERDUE,
                NotificationCompat.Builder(context, CHANNEL_OVERDUE)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()
            )
        }

        // ── Upcoming due-date notification (3-day warning) ────────────────
        val upcomingDebts = repo.getUpcomingDueDebts(daysAhead = 3)
        if (upcomingDebts.isNotEmpty()) {
            val dateFmt = SimpleDateFormat("MMM d", Locale.getDefault())
            val (title, body) = if (upcomingDebts.size == 1) {
                val debt = upcomingDebts.first()
                val name = repo.getPersonById(debt.personId)?.name ?: "Unknown"
                val due  = debt.dateDue?.let { dateFmt.format(Date(it)) } ?: ""
                "Malapit nang mag-due!" to "$name's debt of ₱${"%.0f".format(debt.amount)} is due on $due."
            } else {
                "${upcomingDebts.size} debts due soon!" to
                    "${upcomingDebts.size} debts are due within 3 days."
            }
            nm.notify(
                NOTIF_ID_UPCOMING,
                NotificationCompat.Builder(context, CHANNEL_UPCOMING)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
            )
        }

        return Result.success()
    }

    companion object {
        const val CHANNEL_OVERDUE   = "overdue_reminders"
        const val CHANNEL_UPCOMING  = "upcoming_due"
        const val NOTIF_ID_OVERDUE  = 1001
        const val NOTIF_ID_UPCOMING = 1002
        const val WORK_NAME         = "overdue_daily_check"
    }
}
