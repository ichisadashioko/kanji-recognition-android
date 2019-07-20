package com.example.kanji_recognition_tflite

import android.content.Context
import android.graphics.*
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.TextView


class Canvas(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var mPaint = Paint()
    private var mPath = Path()

    private var mCurX = 0f
    private var mCurY = 0f
    private var mStartX = 0f
    private var mStartY = 0f

    val bmWidth = 64
    val bmHeight = 64
    val characterBitmap = Bitmap.createBitmap(bmWidth, bmHeight, Bitmap.Config.ARGB_8888)
    val bmCanvas = Canvas(characterBitmap)
    var canvasScale = 1f
    var canvasOffset = Point()

    val pen = DrawingPen()
    val bmPath = Path()
    val bmStrokeWidth = 2f

    val srcRect = Rect(0, 0, bmWidth, bmHeight)
    val dstRect = Rect()

    /**
     * TextView for showing touch position
     */
    var tvLog: TextView? = null

    init {
        clearCanvas()
    }

    fun clearCanvas() {
        mPath.reset()
        bmPath.reset()
        invalidate()
    }

    fun scaleBitmapX(x: Float): Float {
        val bmX = (x - canvasOffset.x) / canvasScale
        return bmX
    }

    fun scaleBitmapY(y: Float): Float {
        val bmY = (y - canvasOffset.y) / canvasScale
        return bmY
    }

    fun scaleBitmapPosition(x: Float, y: Float): PointF {
        val bmX = (x - canvasOffset.x) / canvasScale
        val bmY = (y - canvasOffset.y) / canvasScale
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
        mPath.moveTo(x, y)
        mCurX = x
        mCurY = y
        if (!pen.isDown()) {
            pen.moveTo(x, y)
            bmPath.moveTo(scaleBitmapX(x), scaleBitmapY(y))
        }
    }

    private fun actionMove(x: Float, y: Float) {
        mPath.quadTo(mCurX, mCurY, (x + mCurX) / 2, (y + mCurY) / 2)
        if (pen.isDown()) {
//            bmCanvas.drawPath()
            /**
             * Path.quadTo()
             */
//            bmPath.quadTo(
//                scaleBitmapX(mCurX),
//                scaleBitmapY(mCurY),
//                scaleBitmapX(x + mCurX),
//                scaleBitmapY(y + mCurY)
//            )
            /**
             * Path.lineTo()
             */
            bmPath.lineTo(
                scaleBitmapX(x),
                scaleBitmapY(y)
            )
            /**
             * Path.addCircle()
             */
//            bmPath.moveTo(
//                scaleBitmapX(x),
//                scaleBitmapY(y)
//            )
//            bmPath.addCircle(
//                scaleBitmapX(x),
//                scaleBitmapY(y),
//                2f,
//                Path.Direction.CW
//            )
//            bmPath.reset()
//            bmPath.quadTo(, y)
        }
        mCurX = x
        mCurY = y

        /**
         * Logging stuff
         */
//        val logStr = "x: ${scaleBitmapX(mCurX)}\n" +
//                "y: ${scaleBitmapY(mCurY)}\n" +
//                "OffsetX: ${canvasOffset.x}\n" +
//                "OffsetY: ${canvasOffset.y}"
//        tvLog!!.text = logStr
    }

    private fun actionUp() {
        mPath.lineTo(mCurX, mCurY)

        // draw a dot on click
        if (mStartX == mCurX && mStartY == mCurY) {
            mPath.lineTo(mCurX, mCurY + 2)
            mPath.lineTo(mCurX + 1, mCurY + 2)
            mPath.lineTo(mCurX + 1, mCurY)
        }
        /**
         * Reset pen location
         */
        pen.up()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        /**
         * draw background
         */
        mPaint.apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(
            Rect(0, 0, this.width, this.height),
            mPaint
        )
        /**
         * draw strokes
         */
        mPaint.apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 16f
            isAntiAlias = true
        }
        canvas.drawPath(mPath, mPaint)
        /**
         * draw background on the character characterBitmap
         */
        mPaint.apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        bmCanvas.drawRect(srcRect, mPaint)
        /**
         * draw strokes on the character characterBitmap multiple times
         */
        mPaint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = bmStrokeWidth
            isAntiAlias = true
        }
        for (i in 1..16)
            bmCanvas.drawPath(bmPath, mPaint)
        /**
         * the the character characterBitmap on the View canvas
         */
        canvas.drawBitmap(characterBitmap, srcRect, dstRect, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        /**
         * Calculate the scale ratio to scale the 64x64 characterBitmap on the screen
         */
        canvasScale = Math.min((w / bmWidth).toFloat(), (h / bmHeight).toFloat())
        val offsetX = ((w - (bmWidth * canvasScale)) / 2).toInt()
        val offsetY = ((h - (bmHeight * canvasScale)) / 2).toInt()
        canvasOffset = Point(offsetX, offsetY)

        dstRect.left = canvasOffset.x
        dstRect.top = canvasOffset.y
        dstRect.right = canvasOffset.x + (bmWidth * canvasScale).toInt()
        dstRect.bottom = canvasOffset.y + (bmHeight * canvasScale).toInt()

        /**
         * update View contents
         */
//        clearCanvas()
        invalidate()
    }
}

class DrawingPen {
    /**
     * `touchId` to deal with multi-touch gestures
     * not implemented
     */
    var touchId = 0
    private var x = Float.NaN
    private var y = Float.NaN
    private var down = false

    fun moveTo(x: Float, y: Float) {
        this.x = x
        this.y = y
        down = true
    }

    fun up() {
        down = false
        x = Float.NaN
        y = Float.NaN
    }

    fun isDown(): Boolean {
        return down and (x != Float.NaN) and (y != Float.NaN)
    }
}