package com.tassiolima.regavaranda.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    private const val MAX_DIMENSION = 1280
    private const val JPEG_QUALITY = 82

    fun photosDir(context: Context, plantId: Long): File =
        File(context.filesDir, "plant_photos/plant_$plantId").apply { mkdirs() }

    fun newPhotoFile(context: Context, plantId: Long): File {
        val dir = photosDir(context, plantId)
        return File(dir, "photo_${System.currentTimeMillis()}.jpg")
    }

    fun uriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Copia o conteúdo de uma imagem escolhida na galeria (content:// Uri) para [destFile]. */
    fun copyUriToFile(context: Context, sourceUri: Uri, destFile: File): Boolean =
        runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            } != null
        }.getOrDefault(false)

    /** Corrige a orientação (EXIF) e reduz o tamanho da imagem, sobrescrevendo o arquivo original. */
    fun normalizeInPlace(file: File) {
        val orientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val rotated = rotateIfNeeded(original, orientation)
        val scaled = scaleDown(rotated)

        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }

        if (scaled !== original) original.recycle()
        if (rotated !== original && rotated !== scaled) rotated.recycle()
        scaled.recycle()
    }

    fun toBase64Jpeg(file: File): String {
        val bytes = file.readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun rotateIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide <= MAX_DIMENSION) return bitmap
        val scale = MAX_DIMENSION.toFloat() / largestSide
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
