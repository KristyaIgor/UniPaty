package com.mycom.unipaty

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.mycom.unipaty.databinding.ActivityPcturesBinding
import java.io.File
import java.io.IOException
import java.io.InputStream

class PicturesActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityPcturesBinding.inflate(LayoutInflater.from(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val path = intent.getStringExtra("uri")
        val cameraImageFile = path?.let { File(it) }
        val uriFromCameraFile = Uri.fromFile(cameraImageFile)
        var bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uriFromCameraFile)
        bitmap = checkIfNeedRotateImage(bitmap, uriFromCameraFile)
        bitmap = getScaledDownBitmap(bitmap)
        binding.capturedImage.setImageBitmap(bitmap)

        binding.closeButton.setOnClickListener {
            finish()
        }
    }

    private fun checkIfNeedRotateImage(bm: Bitmap, uri: Uri): Bitmap {
        var bmp = bm
        var ei: ExifInterface? = null
        try {
            val inputStream: InputStream = contentResolver.openInputStream(uri)!!
            ei = ExifInterface(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val orientation =
            ei!!.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            val matrix = Matrix()
            matrix.postRotate(90f)
            bmp = Bitmap.createBitmap(
                bmp, 0, 0, bm.width, bm.height,
                matrix, true
            )
        }
        return bmp
    }

    private fun getScaledDownBitmap(
        bitmap: Bitmap
    ): Bitmap? {
        val width = bitmap.width
        val height = bitmap.height
        var newWidth = width
        var newHeight = height
        val threshold = 1024
        val isNecessaryToKeepOrig = true
        if (width > height && width > threshold) {
            newWidth = threshold
            newHeight = (height * newWidth.toFloat() / width).toInt()
        }
        if (width > height && width <= threshold) {
            //the bitmap is already smaller than our required dimension, no need to resize it
            return bitmap
        }
        if (width < height && height > threshold) {
            newHeight = threshold
            newWidth = (width * newHeight.toFloat() / height).toInt()
        }
        if (width < height && height <= threshold) {
            //the bitmap is already smaller than our required dimension, no need to resize it
            return bitmap
        }
        if (width == height && width > threshold) {
            newWidth = threshold
            newHeight = newWidth
        }
        return if (width == height && width <= threshold) {
            //the bitmap is already smaller than our required dimension, no need to resize it
            bitmap
        } else getResizedBitmap(bitmap, newWidth, newHeight, isNecessaryToKeepOrig)
    }

    private fun getResizedBitmap(
        bm: Bitmap,
        newWidth: Int,
        newHeight: Int,
        isNecessaryToKeepOrig: Boolean
    ): Bitmap? {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        // CREATE A MATRIX FOR THE MANIPULATION
        val matrix = Matrix()
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight)

        // "RECREATE" THE NEW BITMAP
        val resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false)
        if (!isNecessaryToKeepOrig) {
            bm.recycle()
        }
        return resizedBitmap
    }
}