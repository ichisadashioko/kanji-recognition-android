package com.example.kanji_recognition_tflite

import android.content.Context
import android.graphics.*
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.TextView


class WritingCanvas(context: Context, attrs: AttributeSet) :
    View(context, attrs) {

    private var mPaint = Paint()

    private var mCurX = 0f
    private var mCurY = 0f
    private var mStartX = 0f
    private var mStartY = 0f

    val imageWidth = 64
    val imageHeight = 64
    /**
     * `canvasImage` is the an image that we will draw in.
     * When we need to get the drawing content to run inference,
     * we will refer to the content of `canvasImage`
     */
    val canvasImage = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
    // `imageCanvas` will help us draw stuff on the `canvasImage`.
    val imageCanvas = Canvas(canvasImage)
    // The size of `canvasImage` is never changed.
    // I only scale it up or down.
    var imageScale = 1f
    var imageOffset = Point()

    var penDown = false
    // `imagePath is used to store what to draw on the `canvasImage`.
    val imagePath = Path()
    // The size of writing stroke.
    val imageStrokeWidth = 2.5f

    /**
     * When we render the `canvasImage`, we will need to know
     * which part of the Bitmap we want to draw (`srcRect`)
     * and where to draw it on the View (`dstRect`).
     *
     * `dstRect` top, left, right, and bottom will be changed
     * based on the `imageWidth`, `imageHeight`, `imageScale`, and `imageOffset` before being passed to the `WritingCanvas.drawBitmap()` function.
     */
    val srcRect = Rect(0, 0, imageWidth, imageHeight)
    val dstRect = Rect()

    init {
        clearCanvas()
    }

    fun clearCanvas() {
        imagePath.reset()
        invalidate()
    }

    fun scaleBitmapX(x: Float): Float {
        val bmX = (x - imageOffset.x) / imageScale
        return bmX
    }

    fun scaleBitmapY(y: Float): Float {
        val bmY = (y - imageOffset.y) / imageScale
        return bmY
    }

    fun scaleBitmapPosition(x: Float, y: Float): PointF {
        val bmX = (x - imageOffset.x) / imageScale
        val bmY = (y - imageOffset.y) / imageScale
        return PointF(bmX, bmY)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mStartX = x
                mStartY = y
                actionDown(x, y)
            }
            MotionEvent.ACTION_MOVE -> actionMove(x, y)
            MotionEvent.ACTION_UP -> actionUp()
        }

        invalidate()
        return true
    }

    private fun actionDown(x: Float, y: Float) {
        mCurX = x
        mCurY = y
        if (!penDown) {
            penDown = true
            imagePath.moveTo(scaleBitmapX(x), scaleBitmapY(y))
        }
    }

    private fun actionMove(x: Float, y: Float) {
        if (penDown) {
            imagePath.lineTo(
                scaleBitmapX(x),
                scaleBitmapY(y)
            )
        }
        mCurX = x
        mCurY = y
    }

    private fun actionUp() {
        penDown = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // draw background
        mPaint.apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val viewRect = Rect(0, 0, this.width, this.height)
        canvas.drawRect(
            viewRect,
            mPaint
        )
        // TODO Draw frame around `imageCanvas`
        mPaint.apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val borderSize = 20f
        val borderRadius = 10f
        var imageBorder = RectF(
            dstRect.left - borderSize,
            dstRect.top - borderSize,
            dstRect.right + borderSize,
            dstRect.bottom + borderSize
        )
        canvas.drawRoundRect(imageBorder, borderRadius, borderRadius, mPaint)

        // draw background on the character canvasImage
        mPaint.apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        imageCanvas.drawRect(srcRect, mPaint)

        // draw strokes on the `canvasImage`
        mPaint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = imageStrokeWidth
            isAntiAlias = true
        }
        imageCanvas.drawPath(imagePath, mPaint)
        canvas.drawBitmap(canvasImage, srcRect, dstRect, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Calculate the scale ratio to scale the 64x64 canvasImage on the screen        
        imageScale = Math.min((w / imageWidth).toFloat(), (h / imageHeight).toFloat())
        val offsetX = ((w - (imageWidth * imageScale)) / 2).toInt()
        val offsetY = ((h - (imageHeight * imageScale)) / 2).toInt()
        imageOffset = Point(offsetX, offsetY)

        dstRect.left = imageOffset.x
        dstRect.top = imageOffset.y
        dstRect.right = imageOffset.x + (imageWidth * imageScale).toInt()
        dstRect.bottom = imageOffset.y + (imageHeight * imageScale).toInt()
        invalidate()
    }
}