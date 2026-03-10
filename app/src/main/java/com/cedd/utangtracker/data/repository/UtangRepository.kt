package com.cedd.utangtracker.data.repository

import com.cedd.utangtracker.data.local.dao.ComakerDao
import com.cedd.utangtracker.data.local.dao.ContractDao
import com.cedd.utangtracker.data.local.dao.DebtDao
import com.cedd.utangtracker.data.local.dao.LedgerDao
import com.cedd.utangtracker.data.local.dao.PaymentDao
import com.cedd.utangtracker.data.local.dao.PersonDao
import com.cedd.utangtracker.data.local.dao.ReservationDao
import com.cedd.utangtracker.data.local.entity.ComakerEntity
import com.cedd.utangtracker.data.local.entity.ContractEntity
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.local.entity.LedgerEntryEntity
import com.cedd.utangtracker.data.local.entity.PaymentEntity
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.local.entity.ReservationEntity
import com.cedd.utangtracker.data.local.relation.DebtWithPayments
import com.cedd.utangtracker.data.remote.ContractLinkRepository
import com.cedd.utangtracker.data.remote.RemoteBorrowerData
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UtangRepository @Inject constructor(
    private val personDao: PersonDao,
    private val debtDao: DebtDao,
    private val paymentDao: PaymentDao,
    private val contractDao: ContractDao,
    private val comakerDao: ComakerDao,
    private val reservationDao: ReservationDao,
    private val linkRepo: ContractLinkRepository,
    private val ledgerDao: LedgerDao
) {
    // Persons
    fun getAllPersons(): Flow<List<PersonEntity>> = personDao.getAllPersons()
    suspend fun getPersonById(id: Long): PersonEntity? = personDao.getPersonById(id)
    suspend fun savePerson(person: PersonEntity): Long = personDao.insert(person)
    suspend fun updatePerson(person: PersonEntity) = personDao.update(person)
    suspend fun deletePerson(person: PersonEntity) = personDao.delete(person)

    // Debts
    fun getAllDebts(): Flow<List<DebtEntity>> = debtDao.getAllDebts()
    fun getDebtsByType(type: String): Flow<List<DebtEntity>> = debtDao.getDebtsByType(type)
    fun getDebtWithPayments(id: Long): Flow<DebtWithPayments?> = debtDao.getDebtWithPayments(id)
    fun getTotalOwedToMe(): Flow<Double?> = debtDao.getTotalOwedToMe()
    fun getTotalIOwe(): Flow<Double?> = debtDao.getTotalIOwe()
    suspend fun saveDebt(debt: DebtEntity): Long = debtDao.insert(debt)
    suspend fun updateDebt(debt: DebtEntity) = debtDao.update(debt)
    suspend fun deleteDebt(debt: DebtEntity) = debtDao.delete(debt)
    suspend fun toggleDebtLock(debt: DebtEntity) = debtDao.update(debt.copy(isLocked = !debt.isLocked))

    // Payments
    fun getAllPayments(): Flow<List<PaymentEntity>> = paymentDao.getAllPayments()
    suspend fun insertPaymentDirect(payment: PaymentEntity) = paymentDao.insert(payment)
    suspend fun addPayment(payment: PaymentEntity, debt: DebtEntity) {
        paymentDao.insert(payment)
        val newPaid = debt.paidAmount + payment.amount
        val settled = newPaid >= debt.amount
        debtDao.update(
            debt.copy(
                paidAmount = newPaid,
                status = if (settled) "SETTLED" else debt.status
            )
        )
    }
    suspend fun deletePayment(payment: PaymentEntity, debt: DebtEntity) {
        paymentDao.delete(payment)
        val newPaid = (debt.paidAmount - payment.amount).coerceAtLeast(0.0)
        debtDao.update(
            debt.copy(
                paidAmount = newPaid,
                status = if (newPaid < debt.amount && debt.status == "SETTLED") "ACTIVE" else debt.status
            )
        )
    }

    /** Marks any ACTIVE debts past their due date as OVERDUE, then applies auto-interest. */
    suspend fun markOverdueDebts() {
        val now = System.currentTimeMillis()
        debtDao.getActiveOverdueDebts(now).forEach { debt ->
            debtDao.update(debt.copy(status = "OVERDUE"))
        }
        applyAutoInterest()
    }

    /**
     * Compounds monthly interest on OVERDUE debts that have autoApplyInterest=true.
     * interestRate is a monthly percentage (e.g., 2.0 = 2%/month).
     * Uses the last application timestamp, falling back to dateDue if never applied.
     */
    suspend fun applyAutoInterest() {
        val now = System.currentTimeMillis()
        val msPerMonth = 30L * 24 * 60 * 60 * 1000
        debtDao.getAutoInterestOverdueDebts().forEach { debt ->
            val reference = debt.lastInterestAppliedAt ?: debt.dateDue ?: return@forEach
            val monthsElapsed = ((now - reference) / msPerMonth).toInt()
            if (monthsElapsed < 1) return@forEach
            val newAmount = debt.amount * Math.pow(1.0 + debt.interestRate / 100.0, monthsElapsed.toDouble())
            debtDao.update(debt.copy(amount = newAmount, lastInterestAppliedAt = now))
        }
    }

    /** Returns all debts currently in OVERDUE status (one-shot, not a Flow). */
    suspend fun getOverdueDebtsSnapshot(): List<DebtEntity> = debtDao.getOverdueDebts()

    /** Returns ACTIVE debts whose due date falls within the next [daysAhead] days. */
    suspend fun getUpcomingDueDebts(daysAhead: Int = 3): List<DebtEntity> {
        val now = System.currentTimeMillis()
        val future = now + daysAhead * 24L * 60 * 60 * 1000
        return debtDao.getDebtsWithUpcomingDue(now, future)
    }

    fun getDebtsForPerson(personId: Long): Flow<List<DebtEntity>> = debtDao.getDebtsForPerson(personId)


    // Reservations
    fun getAllReservations(): Flow<List<ReservationEntity>> = reservationDao.getAll()
    fun getReservationsForPerson(personId: Long): Flow<List<ReservationEntity>> = reservationDao.getForPerson(personId)
    suspend fun getReservationById(id: Long): ReservationEntity? = reservationDao.getById(id)
    suspend fun saveReservation(r: ReservationEntity): Long = reservationDao.insert(r)
    suspend fun updateReservation(r: ReservationEntity) = reservationDao.update(r)
    suspend fun deleteReservation(r: ReservationEntity) = reservationDao.delete(r)

    // Contracts
    fun getContractForDebt(debtId: Long): Flow<ContractEntity?> = contractDao.getContractForDebt(debtId)
    suspend fun getNextContractNumber(): String {
        val count = contractDao.getCount() + 1
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return "UTC-$year-${count.toString().padStart(5, '0')}"
    }
    suspend fun saveContract(contract: ContractEntity): Long = contractDao.insert(contract)
    suspend fun updateContract(contract: ContractEntity) = contractDao.update(contract)
    suspend fun deleteContract(contract: ContractEntity) = contractDao.delete(contract)

    // Comakers
    fun getComakersForContract(contractId: Long): Flow<List<ComakerEntity>> =
        comakerDao.getByContractId(contractId)
    suspend fun addComaker(comaker: ComakerEntity): Long = comakerDao.insert(comaker)
    suspend fun deleteComaker(comaker: ComakerEntity) = comakerDao.delete(comaker)

    // Remote signing link
    suspend fun generateContractLink(contract: ContractEntity, debt: DebtEntity): Pair<String, String> =
        linkRepo.generateLink(contract, debt)

    fun listenForRemoteSigning(
        token: String,
        onCompleted: (RemoteBorrowerData) -> Unit
    ): ListenerRegistration = linkRepo.listenForCompletion(token, onCompleted)

    suspend fun saveBorrowerSignature(contractNumber: String, base64DataUri: String): String =
        linkRepo.saveBase64Signature(base64DataUri, linkRepo.localBorrowerSigFile(contractNumber))

    suspend fun saveBorrowerIdPhoto(contractNumber: String, base64DataUri: String): String =
        linkRepo.saveIdPhoto(base64DataUri, linkRepo.localIdPhotoFile(contractNumber))

    // ── Loan Ledger ────────────────────────────────────────────────────────────
    fun getLedgerEntries(debtId: Long) = ledgerDao.getEntriesForDebt(debtId)
    suspend fun getLatestLedgerEntry(debtId: Long) = ledgerDao.getLatestEntry(debtId)
    suspend fun insertLedgerEntry(entry: LedgerEntryEntity) = ledgerDao.insert(entry)
    suspend fun updateLedgerEntry(entry: LedgerEntryEntity) = ledgerDao.update(entry)
    suspend fun deleteLedgerEntry(entry: LedgerEntryEntity) = ledgerDao.delete(entry)
    suspend fun clearLedger(debtId: Long) = ledgerDao.deleteAllForDebt(debtId)
}
