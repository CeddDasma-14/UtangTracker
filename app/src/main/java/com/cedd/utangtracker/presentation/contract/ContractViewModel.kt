package com.cedd.utangtracker.presentation.contract

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.utangtracker.data.local.entity.ComakerEntity
import com.cedd.utangtracker.data.remote.ComakerInput
import com.cedd.utangtracker.data.local.entity.ContractEntity
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.repository.UtangRepository
import com.cedd.utangtracker.pdf.ContractPdfGenerator
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ContractUiState(
    val contract: ContractEntity? = null,
    val debt: DebtEntity? = null,
    val person: PersonEntity? = null,
    val isSaving: Boolean = false,
    // Remote signing link state
    val isGeneratingLink: Boolean = false,
    val shareableLink: String? = null,
    val linkError: String? = null
)

@HiltViewModel
class ContractViewModel @Inject constructor(
    private val repo: UtangRepository,
    private val pdfGen: ContractPdfGenerator,
    @ApplicationContext private val context: Context,
    savedState: SavedStateHandle
) : ViewModel() {

    private val debtId = savedState.get<Long>("debtId") ?: -1L

    private val _linkState = MutableStateFlow(
        Triple(false, null as String?, null as String?) // isGenerating, link, error
    )

    val uiState: StateFlow<ContractUiState> = combine(
        repo.getContractForDebt(debtId),
        repo.getDebtWithPayments(debtId),
        repo.getAllPersons(),
        _linkState
    ) { contract, dwp, persons, (isGenerating, link, error) ->
        ContractUiState(
            contract = contract,
            debt = dwp?.debt,
            person = persons.find { it.id == dwp?.debt?.personId },
            isGeneratingLink = isGenerating,
            shareableLink = link ?: contract?.secureLinkToken?.let {
                "${com.cedd.utangtracker.data.remote.ContractLinkRepository.WEB_BASE_URL}/sign/$it"
            },
            linkError = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContractUiState())

    // Comakers — reactive list tied to the current contract id
    val comakers: StateFlow<List<ComakerEntity>> = uiState
        .flatMapLatest { s ->
            val contractId = s.contract?.id ?: return@flatMapLatest kotlinx.coroutines.flow.flowOf(emptyList())
            repo.getComakersForContract(contractId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addComaker(fullName: String, mobileNumber: String, address: String) = viewModelScope.launch {
        val contractId = uiState.value.contract?.id ?: return@launch
        repo.addComaker(ComakerEntity(contractId = contractId, fullName = fullName, mobileNumber = mobileNumber, address = address))
    }

    fun deleteComaker(comaker: ComakerEntity) = viewModelScope.launch {
        repo.deleteComaker(comaker)
    }

    private var linkListener: ListenerRegistration? = null

    init {
        // If a link was previously generated, resume listening
        viewModelScope.launch {
            uiState.filter { it.contract?.secureLinkToken != null }.first().let { state ->
                val token = state.contract?.secureLinkToken ?: return@let
                if (state.contract.remoteBorrowerStatus != "completed") {
                    startLinkListener(token)
                }
            }
        }
    }

    fun createContract(
        lenderName: String,
        borrowerName: String,
        witnessName: String?,
        collateral: String? = null,
        language: String = "en"
    ) = viewModelScope.launch {
        val debt = uiState.value.debt ?: return@launch
        val number = repo.getNextContractNumber()
        repo.saveContract(
            ContractEntity(
                debtId = debtId,
                contractNumber = number,
                lenderName = lenderName,
                borrowerName = borrowerName,
                amount = debt.amount,
                purpose = debt.purpose,
                dateDue = debt.dateDue,
                interestRate = debt.interestRate,
                witnessName = witnessName?.ifBlank { null },
                collateral = collateral?.ifBlank { null },
                language = language
            )
        )
    }

    fun saveSignature(role: String, paths: List<DrawnPath>, canvasWidthPx: Int, canvasHeightPx: Int) = viewModelScope.launch {
        val contract = uiState.value.contract ?: return@launch
        val sigDir = File(context.filesDir, "signatures")
        val path = saveSignatureToPng(paths, sigDir, "${contract.contractNumber}_$role", canvasWidthPx, canvasHeightPx)

        val updated = when (role) {
            "lender"   -> contract.copy(lenderSignaturePath = path)
            "borrower" -> contract.copy(borrowerSignaturePath = path)
            "witness"  -> contract.copy(witnessSignaturePath = path)
            else       -> return@launch
        }

        // Lock if both lender and borrower signed
        val locked = updated.lenderSignaturePath != null && updated.borrowerSignaturePath != null
        repo.updateContract(updated.copy(isSigned = if (locked) 1 else 0))
    }

    fun exportAndShare() = viewModelScope.launch {
        val contract = uiState.value.contract ?: return@launch
        val isPaid = uiState.value.debt?.status == "SETTLED"
        val uri = pdfGen.generate(contract, isPaid, comakers.value) ?: return@launch

        repo.updateContract(contract.copy(pdfPath = uri.path))

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Debt Contract ${contract.contractNumber}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Contract PDF")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun exportAcknowledgment() = viewModelScope.launch {
        val contract = uiState.value.contract ?: return@launch
        val uri = pdfGen.generateAcknowledgment(contract) ?: return@launch

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Borrower's Acknowledgment ${contract.contractNumber}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Acknowledgment PDF")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun deleteContract() = viewModelScope.launch {
        linkListener?.remove()
        uiState.value.contract?.let { repo.deleteContract(it) }
    }

    // ── Remote signing ────────────────────────────────────────────────────────

    fun regenerateLink() = viewModelScope.launch {
        val contract = uiState.value.contract ?: return@launch
        linkListener?.remove()
        // Clear all remote borrower data so the contract is ready for a fresh signing
        repo.updateContract(
            contract.copy(
                remoteBorrowerStatus               = "none",
                remoteBorrowerFullName             = null,
                remoteBorrowerAddress              = null,
                remoteBorrowerIdType               = null,
                remoteBorrowerIdNumber             = null,
                remoteBorrowerIdImagePath          = null,
                remoteBorrowerSignaturePath        = null,
                borrowerSignaturePath              = null,
                borrowerSignedAt                   = null,
                isSigned                           = 0,
                secureLinkToken                    = null,
                secureLinkExpiresAt                = null,
                remoteBorrowerIdVerificationStatus = "none",
                remoteBorrowerIdVerificationNote   = null
            )
        )
        _linkState.value = Triple(false, null, null)
        generateLink()
    }

    fun generateLink() = viewModelScope.launch {
        val contract = uiState.value.contract ?: return@launch
        val debt = uiState.value.debt ?: return@launch
        _linkState.value = Triple(true, null, null)
        try {
            val (token, url) = repo.generateContractLink(contract, debt)
            repo.updateContract(
                contract.copy(
                    secureLinkToken = token,
                    secureLinkExpiresAt = System.currentTimeMillis() + 72L * 60 * 60 * 1000,
                    remoteBorrowerStatus = "pending"
                )
            )
            _linkState.value = Triple(false, url, null)
            startLinkListener(token)
        } catch (e: Exception) {
            _linkState.value = Triple(false, null, "Failed to generate link. Check your internet connection.")
        }
    }

    private fun startLinkListener(token: String) {
        linkListener?.remove()
        linkListener = repo.listenForRemoteSigning(token) { remoteData ->
            viewModelScope.launch {
                val contract = uiState.value.contract ?: return@launch

                val sigPath = repo.saveBorrowerSignature(
                    contract.contractNumber, remoteData.signatureBase64
                )

                // Use borrower-provided collateral if lender didn't set one
                val resolvedCollateral = if (contract.collateral.isNullOrBlank() &&
                    remoteData.borrowerCollateral.isNotBlank())
                    remoteData.borrowerCollateral else contract.collateral

                val updated = contract.copy(
                    remoteBorrowerStatus        = "completed",
                    remoteBorrowerFullName      = remoteData.fullName,
                    remoteBorrowerAddress       = remoteData.address,
                    remoteBorrowerIdType        = remoteData.idType,
                    remoteBorrowerIdNumber      = remoteData.idNumber,
                    remoteBorrowerSignaturePath = sigPath.ifBlank { null },
                    borrowerSignaturePath       = contract.borrowerSignaturePath
                        ?: sigPath.ifBlank { null },
                    borrowerSignedAt            = remoteData.completedAt,
                    collateral                  = resolvedCollateral
                ).let { u ->
                    val locked = u.lenderSignaturePath != null && u.borrowerSignaturePath != null
                    u.copy(isSigned = if (locked) 1 else 0)
                }
                repo.updateContract(updated)

                // Save comakers submitted by borrower via web form
                remoteData.comakers.forEach { cm ->
                    repo.addComaker(
                        ComakerEntity(
                            contractId   = updated.id,
                            fullName     = cm.fullName,
                            mobileNumber = cm.mobileNumber,
                            address      = cm.address
                        )
                    )
                }

                // System notification
                try {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val notif = NotificationCompat.Builder(context, "contract_signing")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Borrower has signed!")
                        .setContentText("${remoteData.fullName} signed contract ${contract.contractNumber}")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build()
                    nm.notify(contract.contractNumber.hashCode(), notif)
                } catch (_: Exception) {}

                linkListener?.remove()
            }
        }
    }

    /** Save a photo of the borrower's government ID for lender review (no AI verification). */
    fun saveIdPhoto(uri: Uri) = viewModelScope.launch {
        val contract = uiState.value.contract ?: return@launch
        val destFile = File(context.filesDir, "signatures/${contract.contractNumber}_id_photo.jpg")
        destFile.parentFile?.mkdirs()
        try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            } ?: return@launch
            destFile.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            bitmap.recycle()
        } catch (_: Exception) { return@launch }
        repo.updateContract(contract.copy(remoteBorrowerIdImagePath = destFile.absolutePath))
    }

    override fun onCleared() {
        super.onCleared()
        linkListener?.remove()
    }
}
