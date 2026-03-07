package com.cedd.utangtracker.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.cedd.utangtracker.data.local.entity.ComakerEntity
import com.cedd.utangtracker.data.local.entity.ContractEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContractPdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val phpFmt = NumberFormat.getNumberInstance(Locale("fil", "PH")).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }

    fun generate(contract: ContractEntity, isPaid: Boolean, comakers: List<ComakerEntity> = emptyList()): Uri? {
        return try {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = doc.startPage(pageInfo)
            draw(page.canvas, contract, isPaid, comakers)
            doc.finishPage(page)

            val dir = File(context.filesDir, "contracts").also { it.mkdirs() }
            val file = File(dir, "contract_${contract.contractNumber}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }

    fun generateAcknowledgment(contract: ContractEntity): Uri? {
        return try {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            drawAcknowledgment(page.canvas, contract)
            doc.finishPage(page)

            val dir = File(context.filesDir, "contracts").also { it.mkdirs() }
            val file = File(dir, "acknowledgment_${contract.contractNumber}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }

    private fun drawAcknowledgment(canvas: Canvas, c: ContractEntity) {
        val ML  = 50f
        val MR  = 545f
        val TW  = MR - ML
        var y   = 50f

        fun mkPaint(size: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface  = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                textSize  = size
                color     = Color.BLACK
                textAlign = align
            }

        val pTitle  = mkPaint(13f, bold = true, align = Paint.Align.CENTER)
        val pHead   = mkPaint(10f, bold = true)
        val pBody   = mkPaint(9.5f)
        val pBold   = mkPaint(9.5f, bold = true)
        val pSmall  = mkPaint(8.5f)
        val pSmallB = mkPaint(8.5f, bold = true)
        val pGray   = mkPaint(8f)
        val pLine   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 0.8f }

        fun wrapText(text: String, x: Float, startY: Float, paint: Paint, maxW: Float, lineH: Float): Float {
            var cy = startY
            val words = text.split(" ")
            var line  = ""
            for (word in words) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(test) <= maxW) line = test
                else {
                    if (line.isNotEmpty()) { canvas.drawText(line, x, cy, paint); cy += lineH }
                    line = word
                }
            }
            if (line.isNotEmpty()) { canvas.drawText(line, x, cy, paint); cy += lineH }
            return cy
        }

        // Date parts from borrowerSignedAt or now
        val signTs = c.borrowerSignedAt ?: System.currentTimeMillis()
        val signDate = Date(signTs)
        val cal = Calendar.getInstance().apply { time = signDate }
        val day   = cal.get(Calendar.DAY_OF_MONTH)
        val month = SimpleDateFormat("MMMM", Locale.ENGLISH).format(signDate)
        val year  = cal.get(Calendar.YEAR)

        val borrowerFullName = c.remoteBorrowerFullName ?: c.borrowerName
        val borrowerAddress  = c.remoteBorrowerAddress ?: ""
        val idType           = c.remoteBorrowerIdType ?: ""
        val idNumber         = c.remoteBorrowerIdNumber ?: ""

        // ── Title ─────────────────────────────────────────────────────────────
        canvas.drawText("BORROWER'S ACKNOWLEDGMENT", (ML + MR) / 2, y, pTitle); y += 20f
        canvas.drawLine(ML, y, MR, y, pLine); y += 16f

        // ── Republic / City header ─────────────────────────────────────────────
        canvas.drawText("REPUBLIC OF THE PHILIPPINES)", ML, y, pHead); y += 14f
        val cityLabel = "CITY OF "
        val cityValue = if (borrowerAddress.isNotBlank()) borrowerAddress.lines().first().uppercase() else "_____________________________"
        canvas.drawText("$cityLabel$cityValue ) S.S.", ML, y, pHead); y += 20f

        // ── Intro paragraph ────────────────────────────────────────────────────
        val intro = "BEFORE ME, this $day day of $month, $year, personally appeared the following " +
            "parties with their respective valid government-issued identification documents, known " +
            "to me and to each other as the same persons who executed the foregoing Loan Agreement " +
            "(Contract No. ${c.contractNumber}) and acknowledged before me that the same is their " +
            "free and voluntary act and deed."
        y = wrapText(intro, ML, y, pBody, TW, 14f); y += 16f

        // ── Parties table ──────────────────────────────────────────────────────
        val col1W = 160f; val col2W = 145f; val col3W = TW - col1W - col2W
        val tableTop = y

        // Header row
        canvas.drawText("Name", ML + 4f, y + 12f, pSmallB)
        canvas.drawText("ID Type", ML + col1W + 4f, y + 12f, pSmallB)
        canvas.drawText("ID Number", ML + col1W + col2W + 4f, y + 12f, pSmallB)
        y += 18f

        // Lender row
        canvas.drawText(c.lenderName, ML + 4f, y + 12f, pSmall)
        canvas.drawText("(Lender)", ML + 4f, y + 22f, pGray)
        y += 30f

        // Borrower row
        canvas.drawText(borrowerFullName, ML + col1W + 4f - col1W, y + 12f, pSmall)
        canvas.drawText("(Borrower)", ML + 4f, y + 22f, pGray)
        canvas.drawText(idType, ML + col1W + 4f, y + 12f, pSmall)
        canvas.drawText(idNumber, ML + col1W + col2W + 4f, y + 12f, pSmall)
        y += 30f

        // Table borders
        val tableBot = y
        canvas.drawRect(ML, tableTop, MR, tableBot, pLine.apply { style = Paint.Style.STROKE })
        canvas.drawLine(ML, tableTop + 18f, MR, tableTop + 18f, pLine)            // header separator
        canvas.drawLine(ML, tableTop + 18f + 30f, MR, tableTop + 18f + 30f, pLine) // row separator
        canvas.drawLine(ML + col1W, tableTop, ML + col1W, tableBot, pLine)         // col 1|2
        canvas.drawLine(ML + col1W + col2W, tableTop, ML + col1W + col2W, tableBot, pLine) // col 2|3
        y += 20f

        // ── Witness clause ─────────────────────────────────────────────────────
        y = wrapText(
            "IN WITNESS WHEREOF, the parties have affixed their signatures on the date and place first above written.",
            ML, y, pBody, TW, 14f
        ); y += 20f

        // ── Signature block ────────────────────────────────────────────────────
        val sigW = (TW / 2f) - 10f
        val s1X  = ML
        val s2X  = ML + sigW + 20f
        val sigH = 70f

        c.lenderSignaturePath?.let { path ->
            BitmapFactory.decodeFile(path)?.let { bmp ->
                canvas.drawBitmap(Bitmap.createScaledBitmap(bmp, sigW.toInt(), sigH.toInt(), true), s1X, y, null)
            }
        }
        c.borrowerSignaturePath?.let { path ->
            BitmapFactory.decodeFile(path)?.let { bmp ->
                canvas.drawBitmap(Bitmap.createScaledBitmap(bmp, sigW.toInt(), sigH.toInt(), true), s2X, y, null)
            }
        }
        y += sigH + 4f

        canvas.drawLine(s1X, y, s1X + sigW, y, pLine)
        canvas.drawLine(s2X, y, s2X + sigW, y, pLine)
        y += 12f
        canvas.drawText(c.lenderName, s1X, y, pSmallB)
        canvas.drawText(borrowerFullName, s2X, y, pSmallB)
        y += 12f
        canvas.drawText("Lender", s1X, y, pGray)
        canvas.drawText("Borrower", s2X, y, pGray)
        y += 12f
        c.borrowerSignedAt?.let {
            val tsFmt = SimpleDateFormat("MMMM d, yyyy  h:mm a", Locale.ENGLISH)
            canvas.drawText("Signed: ${tsFmt.format(Date(it))}", s2X, y, pGray)
        }
        y += 28f

        // ── Notary block ───────────────────────────────────────────────────────
        canvas.drawLine(ML, y, MR, y, pLine.apply { strokeWidth = 0.4f }); y += 14f
        val swornText = "SUBSCRIBED AND SWORN to before me this _______ day of ________________, 20____ " +
            "at ______________________________, Philippines."
        y = wrapText(swornText, ML, y, pBody, TW, 14f); y += 28f

        canvas.drawLine(ML, y, ML + 180f, y, pLine); y += 12f
        canvas.drawText("NOTARY PUBLIC", ML, y, pSmallB); y += 12f
        canvas.drawText("Until ____________________________", ML, y, pSmall); y += 20f

        canvas.drawText("PTR No. _________________________", ML, y, pSmall)
        canvas.drawText("Commission No. ___________________", ML + 200f, y, pSmall); y += 14f
        canvas.drawText("Roll No. ________________________", ML, y, pSmall)
        canvas.drawText("IBP No. _________________________", ML + 200f, y, pSmall); y += 20f

        // ── Doc numbers ───────────────────────────────────────────────────────
        canvas.drawLine(ML, y, MR, y, pLine.apply { strokeWidth = 0.8f }); y += 12f
        canvas.drawText(
            "Doc. No. ______;   Page No. ______;   Book No. ______;   Series of $year.",
            ML, y, pSmall
        )
    }

    private fun draw(canvas: Canvas, c: ContractEntity, isPaid: Boolean, comakers: List<ComakerEntity> = emptyList()) {
        val s  = ContractStrings.forLang(c.language)
        val W  = 595f
        val ML = 44f          // left margin
        val MR = 551f         // right margin
        val TW = MR - ML      // usable text width
        var y  = 38f

        // ── Paints ────────────────────────────────────────────────────────────
        fun mkPaint(
            size: Float,
            bold: Boolean = false,
            color: Int = Color.BLACK,
            align: Paint.Align = Paint.Align.LEFT
        ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textSize  = size
            this.color = color
            textAlign = align
        }

        val pTitle   = mkPaint(13f, bold = true)
        val pSection = mkPaint(10.5f, bold = true)
        val pBody    = mkPaint(9f)
        val pBold    = mkPaint(9f, bold = true)
        val pSmall   = mkPaint(8f)
        val pSmallB  = mkPaint(8f, bold = true)
        val pGray    = mkPaint(7.5f, color = Color.DKGRAY)
        val pLine    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 0.8f }
        val pThin    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; strokeWidth = 0.5f }

        // ── Helpers ───────────────────────────────────────────────────────────
        fun wrapText(
            text: String, x: Float, startY: Float,
            paint: Paint, maxW: Float, lineH: Float
        ): Float {
            var cy = startY
            val words = text.split(" ")
            var line  = ""
            for (word in words) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(test) <= maxW) {
                    line = test
                } else {
                    if (line.isNotEmpty()) { canvas.drawText(line, x, cy, paint); cy += lineH }
                    line = word
                }
            }
            if (line.isNotEmpty()) { canvas.drawText(line, x, cy, paint); cy += lineH }
            return cy
        }

        fun bullet(
            text: String, x: Float, startY: Float,
            paint: Paint, maxW: Float, lineH: Float
        ): Float {
            canvas.drawText("•", x, startY, paint)
            return wrapText(text, x + 12f, startY, paint, maxW - 12f, lineH)
        }

        // ── Date parts ────────────────────────────────────────────────────────
        val cal   = Calendar.getInstance().apply { timeInMillis = c.dateCreated }
        val day   = cal.get(Calendar.DAY_OF_MONTH)
        val month = SimpleDateFormat("MMMM", Locale.ENGLISH).format(cal.time)
        val year  = cal.get(Calendar.YEAR)
        val dueStr = c.dateDue?.let {
            SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).format(Date(it))
        } ?: "N/A"

        // ══════════════════════════════════════════════════════════════════════
        // TITLE
        // ══════════════════════════════════════════════════════════════════════
        canvas.drawText(s.title, ML, y, pTitle); y += 17f
        canvas.drawText(s.subtitle, ML, y, pTitle); y += 13f
        canvas.drawLine(ML, y, MR, y, pLine); y += 12f

        // ── Opening paragraph ─────────────────────────────────────────────────
        val opening = s.openingFn(day, month, year, c.lenderName, c.borrowerName)
        y = wrapText(opening, ML, y, pBody, TW, 13f); y += 9f

        // ══════════════════════════════════════════════════════════════════════
        // 1. LOAN DETAILS
        // ══════════════════════════════════════════════════════════════════════
        canvas.drawText("1. ${s.sec1}", ML, y, pSection); y += 13f

        val amtWords = amountToWords(c.amount)
        y = bullet(s.amountBullet("₱${phpFmt.format(c.amount)}", amtWords), ML, y, pBody, TW, 12.5f)
        y = bullet(s.purposeBullet(c.purpose), ML, y, pBody, TW, 12.5f)
        y = bullet(if (c.interestRate > 0) s.interestBullet(c.interestRate) else s.interestNone, ML, y, pBody, TW, 12.5f)
        y = bullet(s.dueBullet(dueStr), ML, y, pBody, TW, 12.5f)
        y += 9f

        // ══════════════════════════════════════════════════════════════════════
        // 2. REPAYMENT TERMS
        // ══════════════════════════════════════════════════════════════════════
        canvas.drawText("2. ${s.sec2}", ML, y, pSection); y += 13f
        y = bullet(s.repay1, ML, y, pBody, TW, 12.5f)
        y = bullet(s.repay2, ML, y, pBody, TW, 12.5f)
        y = bullet(s.repay3, ML, y, pBody, TW, 12.5f)
        y = bullet(s.repay4, ML, y, pBody, TW, 12.5f)
        y += 9f

        // ══════════════════════════════════════════════════════════════════════
        // 3. COLLATERAL (only rendered if provided)
        // ══════════════════════════════════════════════════════════════════════
        if (!c.collateral.isNullOrBlank()) {
            canvas.drawText("3. ${s.sec3Collateral}", ML, y, pSection); y += 13f
            y = bullet(s.col1(c.collateral), ML, y, pBody, TW, 12.5f)
            y = bullet(s.col2, ML, y, pBody, TW, 12.5f)
            y = bullet(s.col3, ML, y, pBody, TW, 12.5f)
            y = bullet(s.col4, ML, y, pBody, TW, 12.5f)
            y = bullet(s.col5, ML, y, pBody, TW, 12.5f)
            y += 9f
        }

        // ══════════════════════════════════════════════════════════════════════
        // 4. DIGITAL SIGNATURES
        // ══════════════════════════════════════════════════════════════════════
        val sigSectionNum = if (!c.collateral.isNullOrBlank()) "4" else "3"
        canvas.drawText("$sigSectionNum. ${s.sec4Signatures}", ML, y, pSection); y += 13f
        y = wrapText(s.sigIntro, ML, y, pBody, TW, 12.5f)
        y += 10f

        // ── Two-column signature block ────────────────────────────────────────
        val colW  = TW / 2f - 8f
        val c1X   = ML
        val c2X   = ML + colW + 16f

        canvas.drawText(s.sigLender, c1X, y, pBold)
        canvas.drawText(s.sigBorrower, c2X, y, pBold)
        y += 8f

        val sigH = 80f

        // draw sig images (or leave blank)
        c.lenderSignaturePath?.let { path ->
            BitmapFactory.decodeFile(path)?.let { bmp ->
                canvas.drawBitmap(
                    Bitmap.createScaledBitmap(bmp, colW.toInt(), sigH.toInt(), true),
                    c1X, y, null
                )
            }
        }
        c.borrowerSignaturePath?.let { path ->
            BitmapFactory.decodeFile(path)?.let { bmp ->
                canvas.drawBitmap(
                    Bitmap.createScaledBitmap(bmp, colW.toInt(), sigH.toInt(), true),
                    c2X, y, null
                )
            }
        }
        y += sigH + 4f

        // signature lines
        canvas.drawLine(c1X, y, c1X + colW, y, pLine)
        canvas.drawLine(c2X, y, c2X + colW, y, pLine)
        y += 11f

        canvas.drawText("Name: ${c.lenderName}", c1X, y, pSmall)
        canvas.drawText("Name: ${c.borrowerName}", c2X, y, pSmall)
        y += 13f

        // Borrower government ID (type + last 5 digits) and optional photo
        val idType   = c.remoteBorrowerIdType
        val idNumber = c.remoteBorrowerIdNumber
        if (idType != null && idNumber != null) {
            val masked = if (idNumber.length > 5) "*".repeat(idNumber.length - 5) + idNumber.takeLast(5) else idNumber
            canvas.drawText("$idType: $masked", c2X, y, pSmall); y += 11f
        }
        c.remoteBorrowerIdImagePath?.let { imgPath ->
            BitmapFactory.decodeFile(imgPath)?.let { bmp ->
                val idPhotoW = (colW * 0.85f).toInt()
                val idPhotoH = (idPhotoW * 0.63f).toInt() // standard ID aspect ratio ~1.585
                canvas.drawBitmap(Bitmap.createScaledBitmap(bmp, idPhotoW, idPhotoH, true), c2X, y, null)
                y += idPhotoH + 6f
            }
        }

        // borrower signed timestamp
        c.borrowerSignedAt?.let { ts ->
            val tsFmt = SimpleDateFormat("MMMM d, yyyy  h:mm a", Locale.ENGLISH)
            canvas.drawText("${s.borrowerSignedLabel} ${tsFmt.format(Date(ts))}", c2X, y, pSmall)
            y += 11f
        }

        // witness row
        c.witnessName?.let { wName ->
            canvas.drawText(s.sigWitness, c1X, y, pSmall)
            // witness sig
            c.witnessSignaturePath?.let { path ->
                BitmapFactory.decodeFile(path)?.let { bmp ->
                    val wsigW = (colW * 0.55f).toInt()
                    canvas.drawBitmap(
                        Bitmap.createScaledBitmap(bmp, wsigW, (sigH * 0.8f).toInt(), true),
                        c1X + 80f, y - (sigH * 0.6f), null
                    )
                }
            }
            val lineEnd = c1X + colW * 0.7f
            canvas.drawLine(c1X + 80f, y, lineEnd, y, pLine); y += 11f
            canvas.drawText("($wName)", c1X + 80f, y, pSmall); y += 16f
        } ?: run { y += 8f }

        // ── Alternative Contacts (Comakers) ──────────────────────────────────
        if (comakers.isNotEmpty()) {
            y += 6f
            canvas.drawLine(ML, y, MR, y, pThin); y += 10f
            canvas.drawText("ALTERNATIVE CONTACTS (COMAKERS)", ML, y, pSmallB); y += 12f
            val cCol1 = 160f; val cCol2 = 120f
            canvas.drawText("Name", ML + 2f, y, pGray)
            canvas.drawText("Mobile", ML + cCol1 + 2f, y, pGray)
            canvas.drawText("Address", ML + cCol1 + cCol2 + 2f, y, pGray)
            y += 3f
            canvas.drawLine(ML, y, MR, y, pThin); y += 10f
            comakers.forEach { cm ->
                canvas.drawText(cm.fullName, ML + 2f, y, pSmall)
                canvas.drawText(cm.mobileNumber, ML + cCol1 + 2f, y, pSmall)
                canvas.drawText(cm.address, ML + cCol1 + cCol2 + 2f, y, pSmall)
                y += 12f
            }
        }

        // ── Footer ─────────────────────────────────────────────────────────────
        canvas.drawLine(ML, y, MR, y, pThin); y += 10f
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(c.createdAt))
        canvas.drawText("Contract No: ${c.contractNumber}   |   Generated: $ts", ML, y, pGray); y += 11f
        canvas.drawText(s.footerDisclaimer, ML, y, pGray)

        // ── PAID watermark ────────────────────────────────────────────────────
        if (isPaid) {
            val wm = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color     = Color.argb(55, 46, 125, 50)
                textSize  = 90f
                typeface  = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.save()
            canvas.rotate(-35f, W / 2, 421f)
            canvas.drawText("PAID", W / 2, 421f, wm)
            canvas.restore()
        }
    }

    // ── Amount to words (PHP) ─────────────────────────────────────────────────
    private fun amountToWords(amount: Double): String {
        val whole    = amount.toLong()
        val centavos = ((amount - whole) * 100).toLong()
        return if (centavos > 0)
            "${numberToWords(whole)} Pesos and ${numberToWords(centavos)} Centavos"
        else
            "${numberToWords(whole)} Pesos"
    }

    private fun numberToWords(n: Long): String {
        if (n == 0L) return "Zero"
        val ones = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
        )
        val tens = arrayOf(
            "", "", "Twenty", "Thirty", "Forty", "Fifty",
            "Sixty", "Seventy", "Eighty", "Ninety"
        )
        fun below1000(x: Long): String = when {
            x < 20  -> ones[x.toInt()]
            x < 100 -> tens[(x / 10).toInt()] +
                       if (x % 10 > 0) " ${ones[(x % 10).toInt()]}" else ""
            else    -> "${ones[(x / 100).toInt()]} Hundred" +
                       if (x % 100 > 0) " ${below1000(x % 100)}" else ""
        }
        return when {
            n >= 1_000_000_000 -> "${below1000(n / 1_000_000_000)} Billion" +
                                  if (n % 1_000_000_000 > 0) " ${numberToWords(n % 1_000_000_000)}" else ""
            n >= 1_000_000     -> "${below1000(n / 1_000_000)} Million" +
                                  if (n % 1_000_000 > 0) " ${numberToWords(n % 1_000_000)}" else ""
            n >= 1_000         -> "${below1000(n / 1_000)} Thousand" +
                                  if (n % 1_000 > 0) " ${numberToWords(n % 1_000)}" else ""
            else               -> below1000(n)
        }
    }
}
