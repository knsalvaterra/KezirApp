package dev.knsalvaterra.kezir

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRectF
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dev.knsalvaterra.kezir.api.TicketResult
import dev.knsalvaterra.kezir.databinding.ActivityMainBinding
import dev.knsalvaterra.kezir.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val barcodeScanner: BarcodeScanner by lazy {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    private lateinit var cameraProvider: ProcessCameraProvider
    private var camera: Camera? = null
    
    private lateinit var scanArea: RectF
    private var lastInvalidCode: String? = null
    private var lastInvalidScanTime: Long = 0
    
    private var isFlashOn = false

    private var eventId: String? = null
    private var userPin: String? = null

    private val scanDuration = 850L
    private var activeResultSheet: TicketViewBottomSheet? = null

    companion object {
        private val VIBRATION_SUCCESS_PATTERN = longArrayOf(0, 150)
        private val VIBRATION_FAILURE_PATTERN = longArrayOf(0, 75, 100, 75)
    }

    private val cameraPermissionLauncher: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            showPermissionDeniedDialog()
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.camera_permission_title))
            .setMessage(getString(R.string.camera_permission_message))
            .setPositiveButton(getString(R.string.camera_permission_settings)) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.camera_permission_cancel)) { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getStringExtra("EVENT_ID")
        userPin = intent.getStringExtra("USER_PIN")

        if (!isValidSession()) {
            Toast.makeText(this, getString(R.string.session_invalid), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initializeUI()
        observeViewModel()
    }

    private fun initializeUI() {
        binding.manualInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateButtonState()
            }
        })

        binding.scanButton.setOnClickListener {
            val code = binding.manualInput.text.toString().trim()
            if (code.isEmpty()) {
                if (viewModel.isScanning.value != true) {
                    viewModel.triggerScan(scanDuration)
                }
            } else if (validCodeFormat(code)) {
                verifyCode(code)
            }
        }

        binding.flashToggleButton.setOnClickListener {
            isFlashOn = !isFlashOn
            updateFlashState()
        }

        binding.logoutButton.setOnClickListener {
            LogoutBottomSheet().show(supportFragmentManager, "logout_sheet")
        }

        updateButtonState()

        binding.viewFinder.post {
            updateScannerOverlay(binding.scannerOverlay.sizePercentage(), binding.scannerOverlay.verticalBias())
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.ticketResult.observe(this) { result ->
            showTicketResult(result)
        }
        
        viewModel.isScanning.observe(this) { 
            updateButtonState()
        }

        viewModel.isVerifying.observe(this) { isVerifying ->
            binding.scanButton.isEnabled = !isVerifying
            if (isVerifying) {
                binding.scanButton.text = getString(R.string.label_button_verifying)
            } else {
                updateButtonState()
            }
        }
    }

    private fun updateScannerOverlay(sizePercentage: Float, verticalBias: Float = 0.5f) {
        val width = binding.viewFinder.width.toFloat()
        val height = binding.viewFinder.height.toFloat()

        val rectSize = min(width, height) * sizePercentage
        val left = (width - rectSize) / 2
        val top = (height - rectSize)  * verticalBias
        val right = left + rectSize
        val bottom = top + rectSize

        scanArea = RectF(left, top, right, bottom)
        binding.scannerOverlay.setTransparentRectangle(scanArea)
    }

    private fun updateButtonState() {
        if (viewModel.isVerifying.value == true) return

        if (viewModel.isScanning.value == true) {
            binding.scanButton.text = getString(R.string.label_button_scanning)
            binding.scanButton.isEnabled = false
            return
        }

        val code = binding.manualInput.text.toString().trim()
        if (code.isEmpty()) {
            binding.scanButton.text = getString(R.string.label_button_scan)
            binding.scanButton.isEnabled = true
        } else {
            binding.scanButton.text = getString(R.string.label_button_verify)
            binding.scanButton.isEnabled = validCodeFormat(code)
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
                .setResolutionSelector( ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(Size(binding.viewFinder.width, binding.viewFinder.height), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER))
                    .build())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor, ::processImageProxy)

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                updateFlashState()
            } catch (exc: Exception) {
                Log.e("Scanner", "Camera binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateFlashState() {
        val flashIcon = if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        binding.flashToggleButton.setImageResource(flashIcon)

        camera?.cameraControl?.enableTorch(isFlashOn)
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (viewModel.shouldScan.value != true || !::scanArea.isInitialized || imageProxy.image == null) {
            imageProxy.close()
            return
        }

        viewModel.setShouldScan(false)

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
                    } else {
                        runOnUiThread {
                            vibrate(VIBRATION_FAILURE_PATTERN)
                            Toast.makeText(this, getString(R.string.qr_code_not_found), Toast.LENGTH_SHORT).show()
                        }
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

    @OptIn(ExperimentalGetImage::class)
    private fun getTransformedScanArea(imageProxy: ImageProxy): RectF {
        val mediaImage = imageProxy.image ?: return RectF()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val imageWidth = if (rotationDegrees == 90 || rotationDegrees == 270) mediaImage.height else mediaImage.width
        val imageHeight = if (rotationDegrees == 90 || rotationDegrees == 270) mediaImage.width else mediaImage.height

        val viewFinder = binding.viewFinder
        val viewWidth = viewFinder.width.toFloat()
        val viewHeight = viewFinder.height.toFloat()
        
        val scaleFactor = max(viewWidth / imageWidth, viewHeight / imageHeight)
        
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

        if (binding.manualInput.text.toString().isNotEmpty() ) {
            return
        }

        if (validCodeFormat(code)) {
            lastInvalidCode = null
            runOnUiThread {
                binding.manualInput.setText(code)
            }
            verifyCode(code)
        } else {
            val now = System.currentTimeMillis()
            if (lastInvalidCode != code || now - lastInvalidScanTime > 3000) {
                lastInvalidCode = code
                lastInvalidScanTime = now
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.invalid_qr_code), Toast.LENGTH_SHORT).show()
                    vibrate(VIBRATION_FAILURE_PATTERN)
                }
            }
        }
    }

    private fun validCodeFormat(code: String): Boolean {

        return code.isNotBlank()
    }

    private fun verifyCode(code: String) {
        val pin = userPin ?: run {
            Toast.makeText(this, getString(R.string.unauthenticated), Toast.LENGTH_LONG).show()
            return
        }

        viewModel.verifyCode(this, pin, code, eventId)
    }

    private fun showTicketResult(result: TicketResult) {
        if (activeResultSheet?.isAdded == true) return

        val onDismissed = {
            binding.manualInput.text?.clear()
            activeResultSheet = null
            updateButtonState()
        }

        if (result is TicketResult.Success) {
            vibrate(VIBRATION_SUCCESS_PATTERN)
        } else {
            vibrate(VIBRATION_FAILURE_PATTERN)
        }

        activeResultSheet = when (result) {
            is TicketResult.Success -> TicketViewBottomSheet(
                success = true,
                message = result.message,
                order = result.order,
                onDismissed = onDismissed,
            )
            is TicketResult.Error -> TicketViewBottomSheet(
                success = false,
                message = result.message,
                order = result.order,
                onDismissed = onDismissed,
            )
        }
        activeResultSheet?.show(supportFragmentManager, "result")
    }

    private fun isValidSession(): Boolean {
        return userPin != null && eventId != null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }
}
