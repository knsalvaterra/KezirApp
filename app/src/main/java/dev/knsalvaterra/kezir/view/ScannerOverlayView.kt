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
import dev.knsalvaterra.kezir.R
import kotlin.math.min

class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint().apply {
        color = Color.argb(128, 0, 0, 0) // Semi-transparent black
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val borderPaint: Paint
    private val transparentRect = RectF()
    private val cornerRadius: Float
    private val cornerLengthRatio: Float
    private val sizePercentage: Float
    private val path = Path()
    private var isRectManuallySet = false

    init {
        val typedArray = context.obtainStyledAttributes(
            attrs, R.styleable.ScannerOverlayView, defStyleAttr, 0
        )

        val borderColor = typedArray.getColor(R.styleable.ScannerOverlayView_overlay_borderColor, Color.WHITE)
        val borderWidth = typedArray.getDimension(R.styleable.ScannerOverlayView_overlay_borderWidth, 10f)
        cornerRadius = typedArray.getDimension(R.styleable.ScannerOverlayView_overlay_cornerRadius, 40f)
        cornerLengthRatio = typedArray.getFloat(R.styleable.ScannerOverlayView_overlay_cornerLengthRatio, 0.1f)
        sizePercentage = typedArray.getFloat(R.styleable.ScannerOverlayView_overlay_sizePercentage, 0.8f)

        typedArray.recycle()

        borderPaint = Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
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
    fun getRectangle(): RectF {
        return transparentRect
    }

    fun sizePercentage() : Float {
        return sizePercentage
    }

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
        val top = (height - rectSize) / 2
        val right = left + rectSize
        val bottom = top + rectSize
        transparentRect.set(left, top, right, bottom)
    }

    private fun updatePath() {
        path.reset()
        if (transparentRect.isEmpty) {
            return
        }

        val cornerLength = transparentRect.width() * cornerLengthRatio

        // Top-left corner
        path.moveTo(transparentRect.left, transparentRect.top + cornerLength)
        path.lineTo(transparentRect.left, transparentRect.top + cornerRadius)
        path.arcTo(
            RectF(
                transparentRect.left,
                transparentRect.top,
                transparentRect.left + 2 * cornerRadius,
                transparentRect.top + 2 * cornerRadius
            ), 180f, 90f, false)
        path.lineTo(transparentRect.left + cornerLength, transparentRect.top)

        // Top-right corner
        path.moveTo(transparentRect.right - cornerLength, transparentRect.top)
        path.lineTo(transparentRect.right - cornerRadius, transparentRect.top)
        path.arcTo(
            RectF(
                transparentRect.right - 2 * cornerRadius,
                transparentRect.top,
                transparentRect.right,
                transparentRect.top + 2 * cornerRadius
            ), 270f, 90f, false)
        path.lineTo(transparentRect.right, transparentRect.top + cornerLength)

        // Bottom-left corner
        path.moveTo(transparentRect.left + cornerLength, transparentRect.bottom)
        path.lineTo(transparentRect.left + cornerRadius, transparentRect.bottom)
        path.arcTo(
            RectF(
                transparentRect.left,
                transparentRect.bottom - 2 * cornerRadius,
                transparentRect.left + 2 * cornerRadius,
                transparentRect.bottom
            ), 90f, 90f, false)
        path.lineTo(transparentRect.left, transparentRect.bottom - cornerLength)

        // Bottom-right corner
        path.moveTo(transparentRect.right, transparentRect.bottom - cornerLength)
        path.lineTo(transparentRect.right, transparentRect.bottom - cornerRadius)
        path.arcTo(
            RectF(
                transparentRect.right - 2 * cornerRadius,
                transparentRect.bottom - 2 * cornerRadius,
                transparentRect.right,
                transparentRect.bottom
            ), 0f, 90f, false)
        path.lineTo(transparentRect.right - cornerLength, transparentRect.bottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // semi-transparent background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // transparent rectangle in the center and the border
        if (!transparentRect.isEmpty) {
            canvas.drawRoundRect(transparentRect, cornerRadius, cornerRadius ,clearPaint)
            canvas.drawPath(path, borderPaint)
        }
    }
}
