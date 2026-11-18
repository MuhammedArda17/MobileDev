package com.example.mobiledev.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Foto's als Base64 (H4).
 * Een Firestore-document mag max 1 MiB zijn en Base64 maakt alles ~1/3 groter,
 * dus we verkleinen eerst naar max 800 px en comprimeren als JPEG (kwaliteit 70).
 *
 * LET OP: deze functies zijn "zwaar". Roep ze op vanuit een achtergrondthread
 * (dat doet de ViewModel met Dispatchers.IO / Dispatchers.Default).
 */
object ImageUtils {

    private const val MAX_SIDE = 800
    private const val JPEG_QUALITY = 70

    /** Leest de ruwe bytes van de gekozen foto. Geeft null terug als dat mislukt. */
    fun readBytes(resolver: ContentResolver, uri: Uri): ByteArray? = try {
        resolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null
    }

    /** Ruwe foto-bytes -> verkleinde JPEG -> Base64-tekst (voor het veld photoBase64). */
    fun compressToBase64(bytes: ByteArray): String? {
        // 1) Enkel de afmetingen lezen (nog geen foto in het geheugen)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // 2) Grof verkleinen tijdens het inlezen (spaart veel geheugen)
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_SIDE) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null

        // 3) Exact verkleinen tot max 800 px en juist draaien
        val scaled = scaleDown(decoded)
        val rotated = rotate(scaled, readRotation(bytes))

        // 4) Comprimeren als JPEG en omzetten naar tekst (NO_WRAP = geen enters in de tekst)
        val out = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /** Base64-tekst uit Firestore -> afbeelding die Compose kan tonen. */
    fun base64ToImageBitmap(base64: String): ImageBitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (e: IllegalArgumentException) {
            null // geen geldige Base64
        }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val scale = MAX_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)
        if (scale >= 1f) return bitmap // is al klein genoeg
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
        )
    }

    /** Camera-foto's staan soms "gedraaid" opgeslagen; dat staat in de EXIF-gegevens. */
    private fun readRotation(bytes: ByteArray): Float = try {
        when (ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } catch (e: IOException) {
        0f
    }

    private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
