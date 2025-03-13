package com.asdevs.kinematix.postureCorrection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.pose.PoseLandmark

class GraphicOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val lock = Any()
    private val graphics = mutableListOf<Graphic>()

    private var scaleFactor = 1.0f
    private var postScaleWidthOffset = 0f
    private var postScaleHeightOffset = 0f
    private var isImageFlipped = false
    private var needUpdateTransformation = true
    private var previewWidth = 0
    private var previewHeight = 0

    abstract class Graphic(private val overlay: GraphicOverlay) {
        abstract fun draw(canvas: Canvas)

        protected fun translateX(x: Float): Float {
            return if (overlay.isImageFlipped) {
                overlay.width - (scale(x) * overlay.width + overlay.postScaleWidthOffset)
            } else {
                scale(x) * overlay.width + overlay.postScaleWidthOffset
            }
        }

        protected fun translateY(y: Float): Float {
            return scale(y) * overlay.height + overlay.postScaleHeightOffset
        }

        protected fun scale(imagePixel: Float): Float {
            return imagePixel * overlay.scaleFactor
        }

        protected fun getPaint(landmark: PoseLandmark, color: Int): android.graphics.Paint {
            val paint = android.graphics.Paint()
            paint.color = color
            paint.strokeWidth = if (landmark.inFrameLikelihood > 0.5f) 4.0f else 2.0f
            paint.style = android.graphics.Paint.Style.STROKE
            return paint
        }


    }

    fun clear() {
        synchronized(lock) {
            graphics.clear()
        }
        postInvalidate()
    }

    fun add(graphic: Graphic) {
        synchronized(lock) {
            graphics.add(graphic)
        }
    }

    fun remove(graphic: Graphic) {
        synchronized(lock) {
            graphics.remove(graphic)
        }
        postInvalidate()
    }

    fun setImageSourceInfo(imageWidth: Int, imageHeight: Int, isFlipped: Boolean) {
        synchronized(lock) {
            this.isImageFlipped = isFlipped
            needUpdateTransformation = true
        }
        postInvalidate()
    }

    fun setCameraInfo(previewWidth: Int, previewHeight: Int, isFrontFacing: Boolean) {
        synchronized(lock) {
            this.previewWidth = previewWidth
            this.previewHeight = previewHeight
            this.isImageFlipped = isFrontFacing
            needUpdateTransformation = true
        }
        postInvalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        needUpdateTransformation = true
    }


    private fun updateTransformationIfNeeded() {
        if (!needUpdateTransformation || width == 0 || height == 0 || previewWidth == 0 || previewHeight == 0) {
            return
        }
        val viewAspectRatio = width.toFloat() / height
        val imageAspectRatio = previewWidth.toFloat() / previewHeight

        postScaleWidthOffset = 0f
        postScaleHeightOffset = 0f
        if (viewAspectRatio > imageAspectRatio) {
            scaleFactor = width.toFloat() / previewWidth
            postScaleHeightOffset = (width / imageAspectRatio - height) / 2
        } else {
            scaleFactor = height.toFloat() / previewHeight
            postScaleWidthOffset = (height * imageAspectRatio - width) / 2
        }

        needUpdateTransformation = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized(lock) {
            updateTransformationIfNeeded()

            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }
}