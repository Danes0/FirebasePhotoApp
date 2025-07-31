package com.example.firebaseappproject2

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.net.Uri
import android.widget.Button
import android.widget.ImageView
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import java.io.File
import com.google.firebase.storage.FirebaseStorage
import android.graphics.BitmapFactory

class CameraActivity : AppCompatActivity() {

    // UI elements
    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var btnRetrieve: Button
    private lateinit var imageView: ImageView
    // Camera capture use case
    private lateinit var imageCapture: ImageCapture
    // Firebase Storage reference
    private val storage = FirebaseStorage.getInstance()

    // Required permission and request code
    private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    private val REQUEST_CODE_PERMISSIONS = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Bind UI components
        previewView = findViewById(R.id.previewView)
        btnCapture = findViewById(R.id.btnCapture)
        btnRetrieve = findViewById(R.id.btnRetrieve)
        imageView = findViewById(R.id.imageView)

        // Request camera permission and start camera if granted
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this,
                REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        // Handle "Capture" button click
        btnCapture.setOnClickListener {
            takePhoto()
        }

        // Handle "Retrieve" button click
        btnRetrieve.setOnClickListener {
            retrievePhotoFromFirebase()
        }
    }

    // Check if permission is granted
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // Handle the result of permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // Always call the superclass implementation
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if the result corresponds to our camera permission request
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // If the user granted the permission, start the camera
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // If permission was denied, show a message and close the activity
                Toast.makeText(this, "Camera permission is required",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // Initialize the camera and set up live preview
    private fun startCamera() {
        // Get the camera provider instance (responsible for managing camera lifecycle)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Wait for the camera provider to become available
        cameraProviderFuture.addListener({
            // Once available, retrieve the camera provider
            val cameraProvider = cameraProviderFuture.get()

            // Set up the Preview use case to show the camera feed on screen
            val preview = Preview.Builder().build().apply {
                // Set up the Preview use case to show the camera feed on screen
                surfaceProvider = previewView.surfaceProvider
            }
            // Initialize the image capture use case
            imageCapture = ImageCapture.Builder().build()
            // Choose the back camera as the default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind previous use cases and bind preview and capture to the activity
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                // Show error if camera fails to start
                Toast.makeText(this, "Error starting camera: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this)) // Run on the main thread
    }

    // Capture a photo and upload it to Firebase Storage
    private fun takePhoto() {
        // Create a file to save the image with a timestamp as the name
        val file = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
        // Set up output options using the file created
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        // Capture image using the imageCapture use case
        imageCapture.takePicture(
            outputOptions,
            // Run callbacks on the main thread
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                // Called when the image is successfully saved
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(file)
                    Toast.makeText(this@CameraActivity, "Photo saved: $savedUri",
                        Toast.LENGTH_SHORT).show()

                    // Upload photo to Firebase Storage
                    val photoRef  = storage.reference.child("photos/${file.name}")
                    photoRef.putFile(savedUri).addOnSuccessListener {
                        // Notify user on successful upload
                        Toast.makeText(this@CameraActivity,
                            "Photo uploaded to Firebase Storage", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        // Notify user if upload fails
                        Toast.makeText(this@CameraActivity,
                            "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }// Called when image capture fails
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity,
                        "Error taking photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    // Retrieve a photo from Firebase Storage and display it in the ImageView
    private fun retrievePhotoFromFirebase() {
        val fileName = "photo.jpg" // Name of the image to retrieve (must exist in Firebase)
        val photoRef = storage.reference.child("photos/$fileName")
        // Create a temporary local file to store the downloaded image
        val tempFile = File.createTempFile("temp", ".jpg")

        // Download the image from Firebase Storage
        photoRef.getFile(tempFile).addOnSuccessListener {
            // Convert the file into a Bitmap and display it
            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
            imageView.setImageBitmap(bitmap)
            Toast.makeText(this, "Photo loaded", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            // Show an error if the download fails
            Toast.makeText(this, "Failed to load photo: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}