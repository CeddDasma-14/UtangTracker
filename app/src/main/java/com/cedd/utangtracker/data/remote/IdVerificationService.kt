package com.cedd.utangtracker.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.cedd.utangtracker.data.preferences.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class IdVerificationResult {
    data class Verified(val extractedName: String) : IdVerificationResult()
    data class Mismatch(val extractedName: String) : IdVerificationResult()
    object Unreadable : IdVerificationResult()
    object NoApiKey : IdVerificationResult()
    data class Error(val message: String) : IdVerificationResult()
}

@Singleton
class IdVerificationService @Inject constructor(
    private val prefs: PreferencesRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL   = "claude-haiku-4-5-20251001"
        private const val VERSION = "2023-06-01"
        private const val PROMPT  =
            "This is a Philippine government-issued ID card (UMID, SSS, GSIS, PhilHealth, " +
            "Passport, Driver's License, PhilSys National ID, etc.). " +
            "Examine the card carefully and find the cardholder's full name. " +
            "On Philippine IDs the name is usually printed near the top or center of the card, " +
            "often labeled 'NAME', 'FULL NAME', or simply printed prominently. " +
            "The format is commonly 'SURNAME, FIRST NAME MIDDLE NAME' or 'FIRST NAME MIDDLE NAME SURNAME'. " +
            "Respond with ONLY the full name exactly as printed on the ID — no extra words, no punctuation changes. " +
            "If after careful examination you truly cannot read any name, respond with exactly: UNREADABLE"
    }

    suspend fun verify(idImageFile: File, submittedFullName: String): IdVerificationResult =
        withContext(Dispatchers.IO) {
            val apiKey = prefs.anthropicApiKey.first()
            if (apiKey.isBlank()) return@withContext IdVerificationResult.NoApiKey
            if (!idImageFile.exists()) return@withContext IdVerificationResult.Unreadable

            // Resize to ≤1500px and encode as JPEG to stay well under Anthropic's 5MB limit.
            val imageBase64 = resizeAndEncodeJpeg(idImageFile)
            val requestJson = buildRequestJson(imageBase64)
            val body = requestJson.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(API_URL)
                .header("x-api-key", apiKey)
                .header("anthropic-version", VERSION)
                .header("Content-Type", "application/json")
                .post(body)
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext IdVerificationResult.Error("API error ${response.code}: $responseBody")
                }
                val extractedName = parseExtractedName(responseBody)
                    ?: return@withContext IdVerificationResult.Unreadable

                if (namesMatch(extractedName, submittedFullName))
                    IdVerificationResult.Verified(extractedName)
                else
                    IdVerificationResult.Mismatch(extractedName)
            } catch (e: Exception) {
                IdVerificationResult.Error(e.message ?: "Network error")
            }
        }

    /**
     * Scales the image down so neither dimension exceeds [MAX_PX], then compresses to JPEG.
     * Anthropic's API rejects images over 5 MB; a 1500-px JPEG is ~200–400 KB — well within limits
     * while still being sharp enough for text OCR on an ID card.
     */
    private fun resizeAndEncodeJpeg(file: File): String {
        val original = BitmapFactory.decodeFile(file.absolutePath)
            ?: return Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        val maxPx = 1500
        val scale = minOf(1f, maxPx.toFloat() / maxOf(original.width, original.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            ).also { if (it !== original) original.recycle() }
        } else {
            original
        }
        val baos = java.io.ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 88, baos)
        scaled.recycle()
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    private fun buildRequestJson(imageBase64: String): String {
        val imageSource = JSONObject().apply {
            put("type", "base64")
            put("media_type", "image/jpeg")
            put("data", imageBase64)
        }
        val imageContent = JSONObject().apply {
            put("type", "image")
            put("source", imageSource)
        }
        val textContent = JSONObject().apply {
            put("type", "text")
            put("text", PROMPT)
        }
        val message = JSONObject().apply {
            put("role", "user")
            put("content", JSONArray().apply { put(imageContent); put(textContent) })
        }
        return JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", 200)
            put("messages", JSONArray().apply { put(message) })
        }.toString()
    }

    private fun parseExtractedName(responseBody: String): String? {
        return try {
            val text = JSONObject(responseBody)
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()
            if (text.equals("UNREADABLE", ignoreCase = true) || text.isBlank()) null else text
        } catch (_: Exception) { null }
    }

    /**
     * Fuzzy name matching for Filipino names:
     * 1. Normalize: uppercase, strip commas/periods, collapse whitespace
     * 2. Exact match → true
     * 3. Remove single-char tokens (middle initials), check bidirectional subset
     *    e.g. "DELA CRUZ, JUAN P." matches "Juan Dela Cruz"
     * 4. ≥70% token overlap for minor OCR noise tolerance
     */
    fun namesMatch(extracted: String, submitted: String): Boolean {
        val norm = { s: String ->
            s.uppercase().replace(Regex("[,.]"), " ").replace(Regex("\\s+"), " ").trim()
        }
        val e = norm(extracted)
        val s = norm(submitted)
        if (e == s) return true

        val eCore = e.split(" ").filter { it.length > 1 }
        val sCore = s.split(" ").filter { it.length > 1 }

        val sInE = sCore.isNotEmpty() && sCore.all { st -> eCore.any { et -> et == st } }
        val eInS = eCore.isNotEmpty() && eCore.all { et -> sCore.any { st -> st == et } }
        if (sInE || eInS) return true

        val matchCount = sCore.count { st -> eCore.any { et -> et == st } }
        return sCore.isNotEmpty() && matchCount.toDouble() / sCore.size >= 0.70
    }
}
