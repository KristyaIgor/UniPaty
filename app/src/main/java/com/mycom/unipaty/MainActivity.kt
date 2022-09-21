package com.mycom.unipaty

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.mycom.unipaty.databinding.ActivityMainBinding
import com.mycom.unipaty.enums.RequestCodes
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    lateinit var currentPhotoPath: String

    private val binding by lazy {
        ActivityMainBinding.inflate(LayoutInflater.from(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            RequestCodes.start.value
        )

        binding.searchButton.setOnClickListener {
            if (binding.searchField.text.toString().isBlank()) {
                Toast.makeText(this, "Nu ati introdus parametri pentru cautare!", Toast.LENGTH_LONG)
                    .show()
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://www.google.com/search?q=${binding.searchField.text}")
                )
                binding.searchField.text.clear()
                startActivity(browserIntent)
            }
        }

        binding.selectedBackCamera.setOnCheckedChangeListener { _, b ->
            if (binding.checkOpenCameraAuto.isChecked && b)
                openCamera(true)
        }
        binding.selectedFrontCamera.setOnCheckedChangeListener { _, b ->
            if (binding.checkOpenCameraAuto.isChecked && b)
                openCamera(false)
        }

        binding.openCamera.setOnClickListener {
            openCamera(binding.selectedBackCamera.isChecked)
        }

        binding.checkOpenCameraAuto.setOnCheckedChangeListener { _, b ->
            if (b) {
                binding.radioGroup.clearCheck()
                binding.openCamera.isEnabled = false
            } else binding.openCamera.isEnabled = true
        }

        binding.showNotification.setOnClickListener {
            showNotification()
        }

    }

    private fun showNotification() {
        val mNotifyMgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    "unic",
                    "UniPaty",
                    NotificationManager.IMPORTANCE_HIGH
                )
            mNotifyMgr.createNotificationChannel(channel)
        }
        val notificationBuilder =
            NotificationCompat.Builder(this, "unic")
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("Push notificare")
                .setContentText("Notificarea va disparea peste 10 s.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)

        val resultIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            resultIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder.setContentIntent(pendingIntent)

        val incomingCallNotification = notificationBuilder.build()

        mNotifyMgr.notify(1, incomingCallNotification)

        val h = Handler()
        val delayInMilliseconds: Long = 10000
        h.postDelayed(
            { mNotifyMgr.cancel(1) },
            delayInMilliseconds
        )
    }

    private fun openCamera(backCamera: Boolean) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                // Ensure that there's a camera activity to handle the intent
                takePictureIntent.resolveActivity(packageManager)?.also {
                    // Create the File where the photo should go
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        null
                    }
                    // Continue only if the File was successfully created
                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                            "com.mycom.unipaty.fileprovider",
                            it
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

                        if (!backCamera) {
                            // Extras for displaying the front camera on most devices
                            takePictureIntent.putExtra(
                                "com.google.assistant.extra.USE_FRONT_CAMERA",
                                true
                            )
                            takePictureIntent.putExtra(
                                "android.intent.extra.USE_FRONT_CAMERA",
                                true
                            )
                            takePictureIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                            takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1)

                            // Extras for displaying the front camera on Samsung
                            takePictureIntent.putExtra("camerafacing", "front")
                            takePictureIntent.putExtra("previous_mode", "Selfie")

                            // Extras for displaying the front camera on Huawei
                            takePictureIntent.putExtra("default_camera", "1")
                            takePictureIntent.putExtra(
                                "default_mode",
                                "com.huawei.camera2.mode.photo.PhotoMode"
                            )
                        }
                        startActivityForResult(takePictureIntent, RequestCodes.camera.value)
                    }
                }
            }
        } else ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            RequestCodes.permission.value
        )
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RequestCodes.permission.value -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openCamera(binding.selectedBackCamera.isChecked)
                }
                return
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCodes.camera.value && resultCode == RESULT_OK) {
            val intent = Intent(this, PicturesActivity::class.java)
            intent.putExtra("uri", currentPhotoPath)
            startActivity(intent)
        }
    }


}