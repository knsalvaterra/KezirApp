package dev.knsalvaterra.kezir.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import dev.knsalvaterra.kezir.R
import kotlin.math.min

class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint().apply {
        color = Color.argb(160, 0, 0, 0) // Slightly darker for better focus
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val borderPaint: Paint
    private val transparentRect = RectF()
    private val cornerRadius: Float
    private val cornerLengthRatio: Float
    private val sizePercentage: Float
    private val verticalBias: Float
    private val path = Path()
    private var isRectManuallySet = false

    init {
        val typedArray = context.obtainStyledAttributes(
            attrs, R.styleable.ScannerOverlayView, defStyleAttr, 0
        )

        val borderColor = typedArray.getColor(R.styleable.ScannerOverlayView_overlay_borderColor, ContextCompat.getColor(context, R.color.chill_green))
        val borderWidth = typedArray.getDimension(R.styleable.ScannerOverlayView_overlay_borderWidth, 8f)
        cornerRadius = typedArray.getDimension(R.styleable.ScannerOverlayView_overlay_cornerRadius, 60f)
        cornerLengthRatio = typedArray.getFloat(R.styleable.ScannerOverlayView_overlay_cornerLengthRatio, 0.15f)
        sizePercentage = typedArray.getFloat(R.styleable.ScannerOverlayView_overlay_sizePercentage, 0.65f)
        verticalBias = typedArray.getFloat(R.styleable.ScannerOverlayView_overlay_verticalBias, 0.45f)

        typedArray.recycle()

        borderPaint = Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!isRectManuallySet) {
            updateTransparentRectangle(w, h)
        }
        updatePath()
    }

    fun sizePercentage(): Float = sizePercentage
    fun verticalBias(): Float = verticalBias

    fun setTransparentRectangle(rect: RectF) {
        isRectManuallySet = true
        transparentRect.set(rect)
        updatePath()
        invalidate()
    }

    private fun updateTransparentRectangle(viewWidth: Int, viewHeight: Int) {
        val width = viewWidth.toFloat()
        val height = viewHeight.toFloat()
        val rectSize = min(width, height) * sizePercentage
        val left = (width - rectSize) / 2
        val top = (height - rectSize) * verticalBias
        val right = left + rectSize
        val bottom = top + rectSize
        transparentRect.set(left, top, right, bottom)
    }

    private fun updatePath() {
        path.reset()
        if (transparentRect.isEmpty) return

        val cornerLength = transparentRect.width() * cornerLengthRatio

        // Top-left
        path.moveTo(transparentRect.left, transparentRect.top + cornerLength)
        path.lineTo(transparentRect.left, transparentRect.top + cornerRadius)
        path.quadTo(transparentRect.left, transparentRect.top, transparentRect.left + cornerRadius, transparentRect.top)
        path.lineTo(transparentRect.left + cornerLength, transparentRect.top)

        // Top-right
        path.moveTo(transparentRect.right - cornerLength, transparentRect.top)
        path.lineTo(transparentRect.right - cornerRadius, transparentRect.top)
        path.quadTo(transparentRect.right, transparentRect.top, transparentRect.right, transparentRect.top + cornerRadius)
        path.lineTo(transparentRect.right, transparentRect.top + cornerLength)

        // Bottom-left
        path.moveTo(transparentRect.left, transparentRect.bottom - cornerLength)
        path.lineTo(transparentRect.left, transparentRect.bottom - cornerRadius)
        path.quadTo(transparentRect.left, transparentRect.bottom, transparentRect.left + cornerRadius, transparentRect.bottom)
        path.lineTo(transparentRect.left + cornerLength, transparentRect.bottom)

        // Bottom-right
        path.moveTo(transparentRect.right - cornerLength, transparentRect.bottom)
        path.lineTo(transparentRect.right - cornerRadius, transparentRect.bottom)
        path.quadTo(transparentRect.right, transparentRect.bottom, transparentRect.right, transparentRect.bottom - cornerRadius)
        path.lineTo(transparentRect.right, transparentRect.bottom - cornerLength)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        if (!transparentRect.isEmpty) {
            canvas.drawRoundRect(transparentRect, cornerRadius, cornerRadius, clearPaint)
            canvas.drawPath(path, borderPaint)
        }
    }
}