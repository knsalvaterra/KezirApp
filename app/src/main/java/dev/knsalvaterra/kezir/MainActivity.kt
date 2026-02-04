package dev.knsalvaterra.kezir

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRectF
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
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var scanArea: RectF
    private var lastInvalidCode: String? = null
    private var lastInvalidScanTime: Long = 0

    private var eventId: String? = null  //"664544741697781760",
    private var currentSessionCookie: String? = null

    private val cameraPermissionLauncher: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_LONG).show()
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

    private fun cameraScannerInit() {
        cameraExecutor = Executors.newSingleThreadExecutor()

        barcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getStringExtra("EVENT_ID")
        currentSessionCookie = intent.getStringExtra("SESSION_COOKIE")

        if (!isValidSession()) {
            Toast.makeText(this, "Invalid session. Please login again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        cameraScannerInit()

        binding.manualInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {


                updateVerifyButtonState()
            }
        })
        binding.manualInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.manualInput.text.toString().trim().isEmpty()) {
                binding.manualInput.hint = "Inserir código do Bilhete"
            } else {
                binding.manualInput.hint = ""
            }
        }

        binding.scanButton.setOnClickListener {
            val manualCode = binding.manualInput.text.toString().trim()
            if (manualCode.isNotEmpty() && validCodeFormat(manualCode)) {
                verifyCode(manualCode)
                runOnUiThread {
                    vibratePhone()
                }
                binding.manualInput.text?.clear()
            }
        }
        updateVerifyButtonState() // initial state

        binding.viewFinder.post {
            updateScannerOverlay(binding.scannerOverlay.sizePercentage())
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun updateScannerOverlay(sizePercentage: Float) {
        val width = binding.viewFinder.width.toFloat()
        val height = binding.viewFinder.height.toFloat()

        val rectSize = min(width, height) * sizePercentage
        val left = (width - rectSize) / 2
        val top = (height - rectSize) / 2
        val right = left + rectSize
        val bottom = top + rectSize

        scanArea = RectF(left, top, right, bottom)
        binding.scannerOverlay.setTransparentRectangle(scanArea)
    }

    private fun updateVerifyButtonState() {
        val isTextEntered = binding.manualInput.text.toString().trim().isNotEmpty()
        val isSizeCorrect = binding.manualInput.text.toString().trim().length == 6
        binding.scanButton.isEnabled = isTextEntered && binding.manualInput.text.toString().trim().all { it.isDigit() } && isSizeCorrect
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(binding.viewFinder.width, binding.viewFinder.height))
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
        if (!::scanArea.isInitialized || imageProxy.image == null) {
            imageProxy.close()
            return
        }

        val imageScanArea = getTransformedScanArea(imageProxy)
        if (imageScanArea.isEmpty) {
            imageProxy.close()
            return
        }

        try {
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return
            }

            val rotation = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotation)

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    var scannedBarcode: Barcode? = null
                    for (barcode in barcodes) {
                        val boundingBox = barcode.boundingBox
                        if (boundingBox != null) {
                            val barcodeRect = boundingBox.toRectF()
                            if (imageScanArea.intersect(barcodeRect)) {
                                scannedBarcode = barcode
                                break
                            }
                        }
                    }
                    if (scannedBarcode != null) {
                        handleScannedBarcode(scannedBarcode)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Scanner", "Barcode scanning failed", exception)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (e: Exception) {
            Log.e("Scanner", "Error in processImageProxy: ${e.message}", e)
            imageProxy.close()
        }
    }


    //androidstudiox library
    @OptIn(ExperimentalGetImage::class)
    private fun getTransformedScanArea(imageProxy: ImageProxy): RectF {
        val mediaImage = imageProxy.image ?: return RectF()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val imageWidth = if (rotationDegrees == 90 || rotationDegrees == 270) mediaImage.height else mediaImage.width
        val imageHeight = if (rotationDegrees == 90 || rotationDegrees == 270) mediaImage.width else mediaImage.height

        val viewFinder = binding.viewFinder
        val viewWidth = viewFinder.width.toFloat()
        val viewHeight = viewFinder.height.toFloat()
        
        val scaleFactor = min(viewWidth / imageWidth, viewHeight / imageHeight)
        
        val postScaleWidth = imageWidth * scaleFactor
        val postScaleHeight = imageHeight * scaleFactor
        val xOffset = (viewWidth - postScaleWidth) / 2f
        val yOffset = (viewHeight - postScaleHeight) / 2f
        
        return RectF(
            (scanArea.left - xOffset) / scaleFactor,
            (scanArea.top - yOffset) / scaleFactor,
            (scanArea.right - xOffset) / scaleFactor,
            (scanArea.bottom - yOffset) / scaleFactor
        )
    }

    private fun handleScannedBarcode(barcode: Barcode) {
        val code = barcode.rawValue ?: return

        if (binding.manualInput.text.toString() == code) {
            return
        }

        if (validCodeFormat(code)) {
            lastInvalidCode = null
            runOnUiThread {
                binding.manualInput.setText(code)
                vibratePhone()
            }
        } else {
            val now = System.currentTimeMillis()
            if (lastInvalidCode != code || now - lastInvalidScanTime > 3000) { // 3-second cooldown
                lastInvalidCode = code
                lastInvalidScanTime = now
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.invalid_qr_code), Toast.LENGTH_SHORT).show()
                    vibratePhone(75)
                }
            }
        }
    }

    private fun validCodeFormat(code: String): Boolean {
        return code.length == 6 && code.all { it.isDigit() }
    }

    private fun verifyCode(code: String) {
        binding.manualInput.text?.clear()
        val cookie = currentSessionCookie ?: run {
            Toast.makeText(this, getString(R.string.unauthenticated), Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            try {
                // val response = ApiClient.api.verifyCode(cookie, VerifyRequest(code, eventId))
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

                val sheet = TicketViewBottomSheet(
                    success = response.success,
                    message = response.message ?: "",
                    order = response.order,
                    onDismissed = {},
                )
                sheet.show(supportFragmentManager, "result")
                binding.manualInput.text?.clear()
            } catch (e: Exception) {
                val sheet = TicketViewBottomSheet(
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

    private fun isValidSession(): Boolean {
        return currentSessionCookie != null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }
}
