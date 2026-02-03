package dev.knsalvaterra.kezir

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
    private var lastDetectedQrCode: String? = null

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    private fun vibratePhone(timeMillis: Long = 150) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(timeMillis, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timeMillis)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                .build()
        )

        login("4728", EVENT_ID)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.manualInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.scanButton.isEnabled = !s.isNullOrBlank() || lastDetectedQrCode != null
            }
        })

        binding.scanButton.setOnClickListener {
            vibratePhone()
            val manualCode = binding.manualInput.text.toString().trim()
            if (manualCode.isNotEmpty()) {
                verifyCode(manualCode)
            } else if (lastDetectedQrCode != null) {
                verifyCode(lastDetectedQrCode!!)
                lastDetectedQrCode = null // Consume the code
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

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor, ::processImageProxy)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            } catch (exc: Exception) {
                Log.e("Scanner", "Camera binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val isTextEntered = binding.manualInput.text.toString().trim().isNotEmpty()
                    if (barcodes.isNotEmpty()) {
                        val code = barcodes.firstOrNull()?.rawValue
                        if (code != null && code.all { it.isDigit() }) {
                            lastDetectedQrCode = code
                            binding.scanButton.isEnabled = true
                        } else {
                            lastDetectedQrCode = null
                            binding.scanButton.isEnabled = isTextEntered
                        }
                    } else {
                        lastDetectedQrCode = null
                        binding.scanButton.isEnabled = isTextEntered
                    }
                }
                .addOnFailureListener {
                    lastDetectedQrCode = null
                    binding.scanButton.isEnabled = binding.manualInput.text.toString().trim().isNotEmpty()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun verifyCode(code: String) {
        val cookie = currentSessionCookie ?: run {
            Toast.makeText(this, getString(R.string.unauthenticated), Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = VerifyResponse(
                    success = true,
                    message = "Código verificado e marcado como resgatado!",
                    order = Order(
                        buyer_name = "Kenedy Salvaterra",
                        tickets = listOf(
                            Ticket(
                                ticket_name = "Normal",
                                quantity = "4"
                            ),
                            Ticket(
                                ticket_name = "VIP",
                                quantity = "2"
                            )
                        )
                    )
                )

                val sheet = ResultBottomSheet(
                    success = response.success,
                    message = response.message ?: "",
                    order = response.order,
                    onDismissed = {},
                )
                sheet.show(supportFragmentManager, "result")
                binding.manualInput.text?.clear()

            } catch (e: Exception) {
                val sheet = ResultBottomSheet(
                    success = false,
                    message = getString(R.string.invalid_ticket_or_connection_error),
                    order = null,
                    onDismissed = {},
                )
                sheet.show(supportFragmentManager, "result")
                binding.manualInput.text?.clear()
            }
        }
    }

    private fun login(pin: String, eventId: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.api.verifyPin(PinRequest(pin, eventId))
                if (response.isSuccessful && response.body()?.success == true) {
                    currentSessionCookie = response.headers()["Set-Cookie"]?.split(";")?.get(0)
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