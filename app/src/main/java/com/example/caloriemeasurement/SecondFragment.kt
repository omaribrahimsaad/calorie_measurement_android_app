package com.example.caloriemeasurement

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.caloriemeasurement.databinding.FragmentSecondBinding
import androidx.camera.core.Camera
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.provider.MediaStore
import java.nio.ByteBuffer
import android.content.ContentResolver
import android.view.GestureDetector
import android.view.MotionEvent
import java.io.OutputStream

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var outputDirectory: File


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Request camera permissions if needed
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the capture button listener
        binding.cameraCaptureButton.setOnClickListener {
            takePhoto()
        }

        setupSwipeGesture(view)

    }

    private fun setupSwipeGesture(view: View) {
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // Check if swipe was horizontal enough
                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (velocityX > 0) { // Right swipe
                        // Handle page switch action here
                        findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
                        return true
                    }
                }
                return false
            }
        })

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // Get screen metrics used to set up camera preview
        val metrics = DisplayMetrics().also { binding.viewFinder.display.getRealMetrics(it) }

        // Preview
        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        // Select back camera as a default
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture)

        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireContext().externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else requireContext().filesDir
    }
    private fun takePhoto() {
        // Get a reference to the ImageCapture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped name and MediaStore entry.
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg") // Saving as JPEG
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-RedOnly")
            }
        }

        // Get resolver for MediaStore
        val resolver: ContentResolver = requireContext().contentResolver

        imageCapture.takePicture(ContextCompat.getMainExecutor(requireContext()),
            object:ImageCapture.OnImageCapturedCallback()
            {
                @OptIn(ExperimentalGetImage::class)
                override fun onCaptureSuccess(image: ImageProxy)
                {
                    processAndSaveImage(image,contentValues, resolver)
                }

                override fun onError(exception: ImageCaptureException )
                {

                }
            })
    }

    // --- Image Processing and Saving ---
    @OptIn(ExperimentalGetImage::class)
    private fun processAndSaveImage(
        imageProxy: ImageProxy,
        contentValues: ContentValues,
        contentResolver: android.content.ContentResolver
    ) {
        var bitmap: Bitmap? = null
        var rotatedBitmap: Bitmap? = null

        try {
            // 1. Convert ImageProxy to Bitmap
            bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap == null) {
                Log.e(TAG, "Failed to convert ImageProxy to Bitmap")
                imageProxy.close() // Close proxy even on failure
                return
            }

            // 2. Handle Rotation
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            // --- Close ImageProxy ASAP after converting ---
            imageProxy.close()
            Log.d(TAG, "ImageProxy closed.")

            // 3. Modify the Bitmap (Keep only Red and Alpha channels)
            val pixels = IntArray(rotatedBitmap.width * rotatedBitmap.height)
            rotatedBitmap.getPixels(pixels, 0, rotatedBitmap.width, 0, 0, rotatedBitmap.width, rotatedBitmap.height)

            for (i in pixels.indices) {
                // ARGB color format (int)
                // Alpha: 0xFF000000
                // Red:   0x00FF0000
                // Green: 0x0000FF00
                // Blue:  0x000000FF
                // Mask to keep only Alpha and Red: 0xFFFF0000
                pixels[i] = pixels[i] and 0xFFFF0000.toInt() // Keep A and R, zero out G and B
            }

            // Create a mutable bitmap and set the modified pixels
            // Ensure ARGB_8888 for saving transparency if needed, though JPEG won't keep it
            val modifiedBitmap = Bitmap.createBitmap(
                rotatedBitmap.width, rotatedBitmap.height, Bitmap.Config.ARGB_8888
            )
            modifiedBitmap.setPixels(pixels, 0, rotatedBitmap.width, 0, 0, rotatedBitmap.width, rotatedBitmap.height)

            // 4. Save the Modified Bitmap using MediaStore
            var outputStream: OutputStream? = null
            var imageUri: android.net.Uri? = null
            try {
                imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri == null) {
                    throw Exception("MediaStore returned null URI")
                }
                outputStream = contentResolver.openOutputStream(imageUri)
                if (outputStream == null) {
                    throw Exception("Failed to get output stream for URI: $imageUri")
                }

                if (!modifiedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                    throw Exception("Failed to save bitmap")
                }
                Log.d(TAG, "Modified image saved successfully: $imageUri")
                Toast.makeText(requireContext(), "Red image saved: $imageUri", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to save modified image", e)
                Toast.makeText(requireContext(), "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
                // Clean up entry if saving failed
                imageUri?.let { contentResolver.delete(it, null, null) }
            } finally {
                outputStream?.close()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during image processing/saving", e)
            Toast.makeText(requireContext(), "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
            // Ensure proxy is closed if an exception occurred before it was closed
            if (imageProxy.image != null) { // Check if proxy might still be open
                imageProxy.close()
            }
        } finally {
            // Recycle intermediate bitmaps if they are different from the final one
            bitmap?.recycle()
            if (rotatedBitmap != bitmap) rotatedBitmap?.recycle()
            // Don't recycle modifiedBitmap here as it might still be needed briefly
        }
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    // Helper function to convert ImageProxy (assuming JPEG format) to Bitmap
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val planeProxy = image.planes[0]
            val buffer: ByteBuffer = planeProxy.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e)
            null
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "SecondFragment"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

}