package dev.knsalvaterra.kezir

import PinRequest
import VerifyRequest
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var manualInput: EditText
    private lateinit var scanButton: MaterialButton
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner

    private var isScanning = true
    private val EVENT_ID = "664544741697781760"
    private var currentSessionCookie: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components from your XML
        viewFinder = findViewById(R.id.viewFinder)
        manualInput = findViewById(R.id.manualInput)
        scanButton = findViewById(R.id.scanButton)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Configure barcode scanner for QR codes only
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        // 1. Initial Admin Login
        adminLogin("4728", EVENT_ID)

        // 2. Request Camera Permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }

        // 3. Manual Verification
        scanButton.setOnClickListener {
            val code = manualInput.text.toString()
            if (code.isNotEmpty()) {
                verifyCode(code)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("Scanner", "Binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && isScanning) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { code ->
                            // Only accept purely numeric QR codes
                            if (code.all { it.isDigit() }) {
                                isScanning = false
                                runOnUiThread { verifyCode(code) }
                            }
                        }
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    private fun verifyCode(code: String) {
        val cookie = currentSessionCookie ?: run {
            Toast.makeText(this, "Não autenticado", Toast.LENGTH_SHORT).show()
            isScanning = true // Allow scanning again
            return
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.api.verifyCode(cookie, VerifyRequest(code, EVENT_ID))

                // Open Popout with success or failure from API
                val sheet = ResultBottomSheet(
                    name = response.order?.buyer_name ?: "Desconhecido",
                    success = response.success,
                    message = response.message ?: "",
                    onDismissed = { isScanning = true },
                )
                sheet.show(supportFragmentManager, "result")
                manualInput.text.clear()

            } catch (e: Exception) {
                // If the API call fails, show the failure popout
                val sheet = ResultBottomSheet(
                    name = "",
                    success = false,
                    message = "Bilhete inválido ou erro de conexão.",
                    onDismissed = { isScanning = true },
                )
                sheet.show(supportFragmentManager, "result")
                manualInput.text.clear()
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
                    Toast.makeText(this@MainActivity, "PIN inválido", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Auth", "Login failed", e)
                Toast.makeText(this@MainActivity, "Erro de login", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }
}
