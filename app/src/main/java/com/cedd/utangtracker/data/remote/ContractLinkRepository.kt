package com.cedd.utangtracker.data.remote

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import com.cedd.utangtracker.data.local.entity.ContractEntity
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.remote.model.ContractRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContractLinkRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val COLLECTION = "contract_requests"
        private const val TTL_MS = 72L * 60 * 60 * 1000   // 72 hours
        const val WEB_BASE_URL = "https://utangtracker-81cfa.web.app"
    }

    /**
     * Generates a UUID token, writes a [ContractRequest] doc to Firestore,
     * and returns Pair(token, shareableUrl).
     */
    suspend fun generateLink(contract: ContractEntity, debt: DebtEntity): Pair<String, String> {
        val token = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val request = ContractRequest(
            contractNumber = contract.contractNumber,
            lenderName     = contract.lenderName,
            borrowerName   = contract.borrowerName,
            amount         = contract.amount,
            purpose        = contract.purpose,
            dateDue        = contract.dateDue,
            interestRate   = contract.interestRate,
            collateral     = contract.collateral ?: "",
            status         = "pending",
            expiresAt      = now + TTL_MS,
            createdAt      = now
        )

        firestore.collection(COLLECTION).document(token).set(request).await()

        val url = "$WEB_BASE_URL/sign/$token"
        return Pair(token, url)
    }

    /**
     * Attaches a Firestore real-time listener on the given token's document.
     * When status == "completed", calls [onCompleted] with [RemoteBorrowerData]
     * carrying the borrower's signature as a base64 PNG data-URI.
     */
    fun listenForCompletion(
        token: String,
        onCompleted: (RemoteBorrowerData) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION).document(token)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val status = snapshot.getString("status") ?: return@addSnapshotListener
                if (status != "completed") return@addSnapshotListener

                val request = snapshot.toObject(ContractRequest::class.java)
                    ?: return@addSnapshotListener

                @Suppress("UNCHECKED_CAST")
                val rawComakers = (snapshot.get("comakers") as? List<*>)
                    ?.filterIsInstance<Map<String, Any>>() ?: emptyList()
                val parsedComakers = rawComakers.map { m ->
                    ComakerInput(
                        fullName     = m["fullName"]     as? String ?: "",
                        mobileNumber = m["mobileNumber"] as? String ?: "",
                        address      = m["address"]      as? String ?: ""
                    )
                }

                onCompleted(
                    RemoteBorrowerData(
                        fullName          = request.borrowerFullName,
                        address           = request.borrowerAddress,
                        phone             = request.borrowerPhone,
                        idType            = request.borrowerIdType,
                        idNumber          = request.borrowerIdNumber,
                        signatureBase64   = request.borrowerSignatureBase64,
                        idPhotoBase64     = request.borrowerIdPhotoBase64,
                        borrowerCollateral = request.borrowerCollateral,
                        completedAt       = request.completedAt?.time,
                        comakers          = parsedComakers
                    )
                )
            }
    }

    /**
     * Decodes a base64 PNG data-URI and saves it as a local PNG file.
     * Returns the absolute local path, or empty string if input is blank.
     */
    fun saveBase64Signature(base64DataUri: String, localFile: File): String {
        if (base64DataUri.isBlank()) return ""
        val base64 = base64DataUri.removePrefix("data:image/png;base64,")
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        localFile.parentFile?.mkdirs()
        localFile.writeBytes(bytes)
        return localFile.absolutePath
    }

    /** Local file path for the borrower's remote signature PNG. */
    fun localBorrowerSigFile(contractNumber: String): File =
        File(context.filesDir, "signatures/${contractNumber}_remote_borrower.png")

    /** Local file path for the borrower's ID photo JPEG. */
    fun localIdPhotoFile(contractNumber: String): File =
        File(context.filesDir, "signatures/${contractNumber}_id_photo.jpg")

    /**
     * Decodes a base64 image data-URI (JPEG/PNG/WEBP) and saves it as a local JPEG file.
     * Returns the absolute local path, or empty string if input is blank.
     */
    fun saveIdPhoto(base64DataUri: String, localFile: File): String {
        if (base64DataUri.isBlank()) return ""
        val base64 = base64DataUri
            .removePrefix("data:image/jpeg;base64,")
            .removePrefix("data:image/png;base64,")
            .removePrefix("data:image/webp;base64,")
            .trim()
        val rawBytes = Base64.decode(base64, Base64.DEFAULT)
        localFile.parentFile?.mkdirs()
        // Re-encode as JPEG via Bitmap to guarantee a valid JPEG file regardless
        // of whatever format the web form produced (prevents media_type mismatch with AI API).
        val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
        if (bitmap != null) {
            localFile.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            bitmap.recycle()
        } else {
            // Fallback: write raw bytes (shouldn't happen with valid image data)
            localFile.writeBytes(rawBytes)
        }
        return localFile.absolutePath
    }
}

/** A co-maker entry submitted by the borrower via the web form. */
data class ComakerInput(
    val fullName: String = "",
    val mobileNumber: String = "",
    val address: String = ""
)

/** Carrier for borrower data received from the Firestore listener. */
data class RemoteBorrowerData(
    val fullName: String,
    val address: String,
    val phone: String = "",
    val idType: String,
    val idNumber: String,
    val signatureBase64: String,
    val idPhotoBase64: String = "",            // JPEG data-URI of government ID photo
    val borrowerCollateral: String = "",       // Collateral items submitted by borrower
    val completedAt: Long? = null,             // epoch ms from serverTimestamp()
    val comakers: List<ComakerInput> = emptyList()
)
