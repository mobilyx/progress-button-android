package com.invozone.progress_button

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import java.security.InvalidParameterException

class CustomProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Default max sweep angle
    private val DEFAULT_MAX_SWEEP_ANGLE = 360

    // Default View top origin
    private val DEFAULT_ORIGIN_TOP = 40

    private val MINIMUM_MARGIN_ADJUSTMENT = 3
    private val PROGRESS_STARTING_ANGLE = 270 // By default start angle is 270 degree

    // Bundle state flags
    private val KEY_INSTANCE_STATE = "KEY_INSTANCE_STATE"
    private val KEY_CURRENT_PROGRESS = "KEY_CURRENT_PROGRESS"
    private val KEY_MAX_PROGRESS = "KEY_MAX_PROGRESS"

    // Circle stroke width from dimen
    private val circleStrokeWidth: Int
    // Sets boundaries to progress circle
    private val progressiveCircleRect = RectF()
    private val innerCircleRect = RectF()
    private lateinit var progressiveArcPaint: Paint
    private lateinit var nonProgressiveArcPaint: Paint
    private lateinit var percentageTextPaint: Paint
    private lateinit var symbolPaint: Paint
    private lateinit var innerStrokedCirclePaint: Paint

    @IntRange(from = 0, to = DEFAULT_MAX_SWEEP_ANGLE)
    private var mAnimatingSweepAngle: Int = 0

    // Load attributes
    @ColorInt
    private var primaryProgressBarColor: Int = 0

    @ColorInt
    private var secondaryProgressBarColor: Int = 0

    @ColorInt
    private var progressPercentageTextColor: Int = 0

    @ColorInt
    private var progressBackgroundColor: Int = 0

    private var downloadDrawable: Drawable? = null
    private var showDownloadPercentage: Boolean = false
    private var showDownloadIndicator: Boolean = false
    private var progress: Int = 0
    private var maximumProgress: Int = 100 // By default 100

    private var randomAlpha: Int = 0

    init {
        circleStrokeWidth = context.resources.getDimensionPixelSize(R.dimen.progress_circle_radius)
        // loading attributes
        loadAttributes(context, attrs, defStyleAttr)
        reset() // Reset all values
    }

    fun getDownloadDrawable(): Drawable? {
        return downloadDrawable
    }

    fun setDownloadDrawable(downloadDrawable: Drawable?) {
        this.downloadDrawable = downloadDrawable
    }

    fun isShowDownloadPercentage(): Boolean {
        return showDownloadPercentage
    }

    fun setShowDownloadPercentage(showDownloadPercentage: Boolean) {
        this.showDownloadPercentage = showDownloadPercentage
    }

    fun isShowDownloadIndicator(): Boolean {
        return showDownloadIndicator
    }

    fun setShowDownloadIndicator(showDownloadIndicator: Boolean) {
        this.showDownloadIndicator = showDownloadIndicator
    }

    private fun loadAttributes(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val attributes = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.BubbledProgressBar,
            defStyleAttr,
            0
        )

        // Progressbar color
        primaryProgressBarColor = attributes.getColor(
            R.styleable.BubbledProgressBar_primary_progress_color,
            ContextCompat.getColor(context, android.R.color.white)
        )
        secondaryProgressBarColor = attributes.getColor(
            R.styleable.BubbledProgressBar_secondary_progress_color,
            ContextCompat.getColor(context, R.color.cloud_progress_get_started_bg_color)
        )
        progressBackgroundColor = attributes.getColor(
            R.styleable.BubbledProgressBar_progress_background_color,
            ContextCompat.getColor(context, android.R.color.black)
        )
        progress = attributes.getInt(R.styleable.BubbledProgressBar_progress, 0)
        maximumProgress = attributes.getInt(R.styleable.BubbledProgressBar_max, 100)

        downloadDrawable = attributes.getDrawable(R.styleable.BubbledProgressBar_download_icon)
        showDownloadIndicator = attributes.getBoolean(R.styleable.BubbledProgressBar_show_indicator, true)
        showDownloadPercentage = attributes.getBoolean(R.styleable.BubbledProgressBar_show_percentage, true)

        // Text Color
        progressPercentageTextColor = attributes.getColor(
            R.styleable.BubbledProgressBar_progress_text_color,
            ContextCompat.getColor(context, android.R.color.white)
        )
    }

    /**
     * It sets primary progress color
     *
     * @param primaryProgressBarColor - progressing color
     */
    fun setPrimaryProgressBarColor(primaryProgressBarColor: Int) {
        this.primaryProgressBarColor = primaryProgressBarColor
    }

    fun getSecondaryProgressBarColor(): Int {
        return secondaryProgressBarColor
    }

    /**
     * It sets unanimated progress color
     *
     * @param secondaryProgressBarColor - sets secondary color.
     */
    fun setSecondaryProgressBarColor(secondaryProgressBarColor: Int) {
        this.secondaryProgressBarColor = secondaryProgressBarColor
    }

    /**
     * It used set maximum progress value.
     *
     * @param maximumProgress - maximum progress value, by default 100.
     */
    fun setMaxProgress(maximumProgress: Int) {
        if (maximumProgress > 0) {
            this.maximumProgress = maximumProgress
        }
    }

    /**
     * It returns maximum progress value
     *
     * @return - maximum progress value
     */
    fun getMaximumProgress(): Int {
        return maximumProgress
    }

    private fun initiatePainters() {
        val res = context.resources
        val staticScoreTextSize = res.getDimensionPixelSize(R.dimen.text_size_progress_circle).toFloat()
        percentageTextPaint = percentageTextPaint(staticScoreTextSize)
        symbolPaint = percentageTextPaint(staticScoreTextSize / 1.25f)
        progressiveArcPaint = getProgressivePaint(primaryProgressBarColor, false)
        nonProgressiveArcPaint = getProgressivePaint(secondaryProgressBarColor, false)
        innerStrokedCirclePaint = innerStrokedCirclePaint()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minimumSize = Math.min(widthMeasureSpec, heightMeasureSpec)
        if (minimumSize <= 0) {
            val res = resources
            minimumSize = res.getDimensionPixelSize(R.dimen.progress_circle_h_w)
        }
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        var width: Int
        var height: Int
        // Measure Width
        width = when {
            widthMode == MeasureSpec.EXACTLY -> widthSize
            widthMode == MeasureSpec.AT_MOST -> Math.min(minimumSize, widthSize)
            else -> minimumSize
        }
        // Measure Height
        height =

            when {
                heightMode == MeasureSpec.EXACTLY -> heightSize
                heightMode == MeasureSpec.AT_MOST -> Math.min(minimumSize, heightSize)
                else -> minimumSize
            }
        // MUST CALL THIS
        setMeasuredDimension(width, height)
    }

    private fun drawCrossOnView(canvas: Canvas) {
        val paint = getDefaultPaint(Paint.Style.STROKE)
        paint.color = Color.RED
        paint.strokeWidth = 2f
        canvas.drawLine(
            width * 0.25f,
            0f,
            width * 0.25f,
            height.toFloat(),
            paint
        )
        canvas.drawLine(
            0f,
            width * 0.25f,
            height.toFloat(),
            width * 0.25f,
            paint
        )
        canvas.drawLine(
            width * 0.5f,
            0f,
            width * 0.5f,
            height.toFloat(),
            paint
        )
        canvas.drawLine(
            0f,
            width * 0.5f,
            height.toFloat(),
            width * 0.5f,
            paint
        )
        canvas.drawLine(
            width * 0.75f,
            0f,
            width * 0.75f,
            height.toFloat(),
            paint
        )
        canvas.drawLine(
            0f,
            width * 0.75f,
            height.toFloat(),
            width * 0.75f,
            paint
        )
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT) // Background color as TRANSPARENT
        drawProgressiveArc(canvas)
        drawFilledStrokedCircle(canvas)
        if (progress >= 0) {
            if (showDownloadPercentage) {
                drawScoreAndPercentageCharacter(canvas)
            }
        } else {
            drawImage(canvas)
        }
        // drawCrossOnView(canvas) // For development purpose only enable it
        super.onDraw(canvas)
    }

    fun startBlink() {
        val animBlink = AnimationUtils.loadAnimation(
            context,
            R.anim.blinkable
        )
        startAnimation(animBlink)
    }

    private fun drawImage(canvas: Canvas) {
        val res = resources
        if (downloadDrawable == null) {
            downloadDrawable =
                VectorDrawableCompat.create(res, R.drawable.ic_cloud_download, context.theme)
        }
        val bitmap = getBitmapFromDrawable(downloadDrawable)
        if (bitmap != null) {
            val optimizedCenterValue = getArcCenterValue() // Find center of the circle
            val positionX = optimizedCenterValue - (bitmap.width * 0.5f)
            val positionY = optimizedCenterValue - (bitmap.height * 0.5f)
            canvas.drawBitmap(bitmap, positionX, positionY, getDefaultPaint(Paint.Style.FILL_AND_STROKE))
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    /**
     * It draws the Assessment score along with "%" character with different text sizes
     *
     * @param canvas
     */
    private fun drawScoreAndPercentageCharacter(canvas: Canvas) {
        val scoreData = progress.toString()
        val textHeight = percentageTextPaint.descent() + percentageTextPaint.ascent()
        val symbolTextWidth = symbolPaint.measureText("%")
        val percentageTextWidth = ((width - percentageTextPaint.measureText(scoreData) - symbolTextWidth) / 2.0f)
        val optimizedCenterValue = getArcCenterValue() // Find center of the circle
        val yAxis = (optimizedCenterValue - (textHeight * 0.5)).toFloat()
        canvas.drawText(
            scoreData,
            percentageTextWidth - MINIMUM_MARGIN_ADJUSTMENT,
            yAxis,
            percentageTextPaint
        )
        canvas.drawText(
            "%",
            percentageTextWidth + percentageTextPaint.measureText(scoreData) + MINIMUM_MARGIN_ADJUSTMENT,
            yAxis,
            symbolPaint
        )
    }

    /**
     * Find center of the circle
     *
     * @return returns the arc center point, which is the view center even view size is MIN or MAX. It will align center of the view.
     */
    private fun getArcCenterValue(): Float {
        val arcHeight = (progressiveCircleRect.top + progressiveCircleRect.bottom)
        val arcWidth = (progressiveCircleRect.left + progressiveCircleRect.right)
        val optimizeValue = (arcHeight + arcWidth) / 2 // Find center of the circle
        return optimizeValue / 2
    }

    /**
     * It draws the progressive arc
     *
     * @param canvas
     */
    private fun drawProgressiveArc(canvas: Canvas) {
        if (progressiveCircleRect.isEmpty) {
            progressiveCircleRect.set(
                circleStrokeWidth.toFloat(), // left
                (DEFAULT_ORIGIN_TOP - circleStrokeWidth).toFloat(), // top
                (width - circleStrokeWidth).toFloat(), // right
                (height - circleStrokeWidth).toFloat() // bottom
            )
        }
        if (showDownloadIndicator) {
            canvas.drawArc(
                progressiveCircleRect,
                0f,
                DEFAULT_MAX_SWEEP_ANGLE.toFloat(),
                true,
                nonProgressiveArcPaint
            )
            canvas.drawArc(
                progressiveCircleRect,
                PROGRESS_STARTING_ANGLE.toFloat(),
                mAnimatingSweepAngle.toFloat(),
                false,
                progressiveArcPaint
            )
        }
    }

    /**
     * It draws inner filled stroked semi-transparent circle.
     *
     * @param canvas
     */
    private fun drawFilledStrokedCircle(canvas: Canvas) {
        if (innerCircleRect.isEmpty) {
            innerCircleRect.set(progressiveCircleRect)
            val padding = (circleStrokeWidth * 0.5).toFloat()
            innerCircleRect.inset(padding, padding)
        }
        canvas.drawOval(innerCircleRect, innerStrokedCirclePaint) // It draws the inner circle with FILL background for star symbol
    }

    /**
     * It provides Default [Paint] object.
     *
     * @param style the style to be set as STROKE or FILL_AND_STROKE
     * @return loaded [Paint] instance.
     */
    @NonNull
    private fun getDefaultPaint(style: Paint.Style): Paint {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = style
        return paint
    }

    /**
     * It gives [Paint] object for percentage text rendering.
     *
     * @param percentageTextSize - progressed percentage
     * @return - returns paint instance.
     */
    @NonNull
    private fun percentageTextPaint(percentageTextSize: Float): Paint {
        val mTextPaint = getDefaultPaint(Paint.Style.FILL_AND_STROKE)
        mTextPaint.color = progressPercentageTextColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTextPaint.letterSpacing = -0.07f
        }
        mTextPaint.textSize = (percentageTextSize / 1.25).toFloat()
        return mTextPaint
    }

    /**
     * It gives [Paint] object for inner stroked and fill circle rendering.
     *
     * @return - returns paint instance.
     */
    @NonNull
    private fun innerStrokedCirclePaint(): Paint {
        val

                paint = getDefaultPaint(Paint.Style.FILL_AND_STROKE)
        paint.color = progressBackgroundColor
        paint.alpha = 140 // sets transparent level
        return paint
    }

    /**
     * It provides [Paint] instance for draw an arc with progressive feature.
     *
     * @param color                - optional to set according to progressing level.
     * @param isTransparentEnabled
     * @return - It returns the [Paint] instance to indicating progressing level to user by given value.
     */
    private fun getProgressivePaint(color: Int, isTransparentEnabled: Boolean): Paint {
        val paint = getDefaultPaint(Paint.Style.STROKE)
        paint.color = color
        paint.strokeCap = Paint.Cap.ROUND // gives curve shape to paint
        if (isTransparentEnabled) {
            paint.alpha = 70
            paint.strokeWidth = (circleStrokeWidth * 1.5).toFloat()
        } else {
            paint.strokeWidth = circleStrokeWidth.toFloat()
        }
        return paint
    }

    @IntRange(from = 0, to = DEFAULT_MAX_SWEEP_ANGLE)
    fun getSweepAngle(): Int {
        return mAnimatingSweepAngle
    }

    /**
     * It sets sweep animation value
     *
     * @param mRedSweepAngle - returns animating value
     */
    fun setSweepAngle(mRedSweepAngle: Int) {
        this.mAnimatingSweepAngle = mRedSweepAngle
    }

    /**
     * It returns maximum sweeping animation angle.
     *
     * @return returns max sweep angle
     */
    fun getMaxAngle(): Int {
        return DEFAULT_MAX_SWEEP_ANGLE
    }

    /**
     * It will reset the progress bar state as IN_ACTIVE
     */
    fun reset() {
        // Reset or Assigning the Paints
        initiatePainters()

        // Reset the progress
        setProgress(-1) // add extra work here while user reset progress.

        setSweepAngle(0) // reset the sweep angle.
    }

    @Suppress("unused")
    private fun reInitiateSecondaryProgressColor() {
        secondaryProgressBarColor = ContextCompat.getColor(context, android.R.color.black)
        if (nonProgressiveArcPaint.color == secondaryProgressBarColor) return
        nonProgressiveArcPaint = getProgressivePaint(secondaryProgressBarColor, true)
    }

    /**
     * It gives current progress value.
     *
     * @return progress - current progress score value
     */
    fun getProgress(): Int {
        return progress
    }

    /**
     * This method will update the every progress value in [Canvas] rendering.
     *
     * @param progress - current progress value
     */
    fun setProgress(progress: Int) {
        if (progress == -1) {
            this.progress = progress
        } else if (progress >= 0 && progress <= maximumProgress) {
            this.progress = progress
            // reInitiateSecondaryProgressColor()
            // this.progress %= maximumProgress // If values come more than Maximum value
            setSweepAngle((progress * DEFAULT_MAX_SWEEP_ANGLE) / maximumProgress)
        } else {
            throw InvalidParameterException(String.format("Invalid progress(%d) value has been passed!", progress))
        }
        invalidate()
    }

    @Nullable
    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable(KEY_INSTANCE_STATE, super.onSaveInstanceState())
        bundle.putInt(KEY_CURRENT_PROGRESS, progress)
        bundle.putInt(KEY_MAX_PROGRESS, maximumProgress)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            setProgress(state.getInt(KEY_CURRENT_PROGRESS))
            setMaxProgress(state.getInt(KEY_MAX_PROGRESS))
            initiatePainters()
            super.onRestoreInstanceState(state.getParcelable(KEY_INSTANCE_STATE))
            return
        }
        super.onRestoreInstanceState(state)
    }

    /**
     * This methods used to create [Bitmap] instance from given [Drawable] with expected image size.
     *
     * @param drawable - given image drawable.
     * @return - bitmap of given drawable
     */
    private fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
        var bitmap: Bitmap? = null
        if (drawable == null) {
            return null
        }
        if (drawable is BitmapDrawable) {
            val bitmapDrawable = drawable as BitmapDrawable
            if (bitmapDrawable.bitmap != null) {
                return bitmapDrawable.bitmap
            }
        }
        if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
        } else {
            val ratio = 1f // By default 1f
            val width = (drawable.intrinsicWidth * ratio).toInt()
            val height = (drawable.intrinsicHeight * ratio).toInt()
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap!!)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}