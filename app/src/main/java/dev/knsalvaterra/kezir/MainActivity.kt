package dev.knsalvaterra.kezir

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dev.knsalvaterra.kezir.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var cameraProvider: ProcessCameraProvider

    private val EVENT_ID = "664544741697781760"
    private var currentSessionCookie: String? = null

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        adminLogin("4728", EVENT_ID)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.scanButton.setOnClickListener {
            val code = binding.manualInput.text.toString()
            if (code.isNotEmpty()) {
                verifyCode(code)
            } else {
                scanBarcode()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            val viewPort = binding.viewFinder.viewPort
            if (viewPort != null) {
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val useCaseGroup = UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(preview)
                    .addUseCase(imageAnalysis)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup)
                } catch (exc: Exception) {
                    Log.e("Scanner", "Binding failed", exc)
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun scanBarcode() {
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(imageProxy, imageAnalysis)
        }

        try {
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, imageAnalysis)
        } catch (exc: Exception) {
            Log.e("Scanner", "Binding failed", exc)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy, imageAnalysis: ImageAnalysis) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { code ->
                                if (code.all { it.isDigit() }) {
                                    runOnUiThread { verifyCode(code) }
                                }
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    cameraProvider.unbind(imageAnalysis)
                }
        } else {
            imageProxy.close()
            cameraProvider.unbind(imageAnalysis)
        }
    }

    private fun verifyCode(code: String) {
        val cookie = currentSessionCookie ?: run {
            Toast.makeText(this, getString(R.string.unauthenticated), Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.api.verifyCode(cookie, VerifyRequest(code, EVENT_ID))

                val sheet = ResultBottomSheet(
                    success = response.success,
                    message = response.message ?: "",
                    order = response.order,
                    onDismissed = { },
                )
                sheet.show(supportFragmentManager, "result")
                binding.manualInput.text.clear()

            } catch (e: Exception) {
                val sheet = ResultBottomSheet(
                    success = false,
                    message = getString(R.string.invalid_ticket_or_connection_error),
                    order = null,
                    onDismissed = { },
                )
                sheet.show(supportFragmentManager, "result")
                binding.manualInput.text.clear()
            }
        }
    }

    private fun adminLogin(pin: String, eventId: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.api.verifyPin(PinRequest(pin, eventId))
                if (response.isSuccessful && response.body()?.success == true) {
                    val cookieHeader = response.headers()["Set-Cookie"]
                    currentSessionCookie = cookieHeader?.split(";")?.get(0)
                } else {
                    Toast.makeText(this@MainActivity, getString(R.string.invalid_pin), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Auth", "Login failed", e)
                Toast.makeText(this@MainActivity, getString(R.string.login_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }
}
