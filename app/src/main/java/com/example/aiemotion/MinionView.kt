package com.example.aiemotion

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class MinionView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gogglePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val jeansPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var eyeRadius = 0f
    private var leftEyeX = 0f
    private var rightEyeX = 0f
    private var eyeY = 0f
    private var leftPupilX = 0f
    private var rightPupilX = 0f
    private var pupilY = 0f
    private var mouthY = 0f
    private var smileAmount = 0f

    init {
        backgroundPaint.color = Color.YELLOW
        gogglePaint.color = Color.LTGRAY
        jeansPaint.color = Color.YELLOW // Dark blue for jeans
        paint.color = Color.WHITE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        eyeRadius = w * 0.2f
        leftEyeX = w * 0.3f
        rightEyeX = w * 0.7f
        eyeY = h * 0.35f
        leftPupilX = leftEyeX
        rightPupilX = rightEyeX
        pupilY = eyeY
        mouthY = h * 0.65f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Jeans
        canvas.drawRect(0f, height * 0.7f, width.toFloat(), height.toFloat(), jeansPaint)

        // Goggles
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = eyeRadius * 0.1f
        canvas.drawCircle(leftEyeX, eyeY, eyeRadius, gogglePaint)
        canvas.drawCircle(rightEyeX, eyeY, eyeRadius, gogglePaint)

        // Eyes
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawCircle(leftEyeX, eyeY, eyeRadius * 0.9f, paint)
        canvas.drawCircle(rightEyeX, eyeY, eyeRadius * 0.9f, paint)

        // Pupils
        paint.color = Color.DKGRAY
        canvas.drawCircle(leftPupilX, pupilY, eyeRadius * 0.4f, paint)
        canvas.drawCircle(rightPupilX, pupilY, eyeRadius * 0.4f, paint)

        // Mouth
//        paint.color = Color.BLACK
//        paint.style = Paint.Style.STROKE
//        paint.strokeWidth = eyeRadius * 0.1f
//        val mouthPath = Path()
//        mouthPath.moveTo(width * 0.3f, mouthY)
//        mouthPath.quadTo(width * 0.5f, mouthY + smileAmount * eyeRadius, width * 0.7f, mouthY)
//        canvas.drawPath(mouthPath, paint)
        // Mouth
//        paint.color = Color.BLACK
//        paint.style = Paint.Style.STROKE
//        paint.strokeWidth = eyeRadius * 0.1f
//        val mouthPath = Path()
//        mouthPath.moveTo(width * 0.3f, mouthY)
//        if (isTalking) {
//            // Khi đang nói, miệng mở và đóng
//            mouthPath.quadTo(width * 0.5f, mouthY + mouthOpenAmount * eyeRadius * 0.5f, width * 0.7f, mouthY)
//        } else {
//            // Khi không nói, miệng cười hoặc cau có như trước
//            mouthPath.quadTo(width * 0.5f, mouthY + smileAmount * eyeRadius, width * 0.7f, mouthY)
//        }
//        canvas.drawPath(mouthPath, paint)

        // Eyes
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        val eyeYOffset = if (isListening) -eyeRadius * 0.1f * attentionAmount else 0f
        canvas.drawCircle(leftEyeX, eyeY + eyeYOffset, eyeRadius * 0.9f, paint)
        canvas.drawCircle(rightEyeX, eyeY + eyeYOffset, eyeRadius * 0.9f, paint)

        // Pupils
        paint.color = Color.DKGRAY
        val pupilSize = if (isListening) eyeRadius * (0.4f + 0.1f * attentionAmount) else eyeRadius * 0.4f
        canvas.drawCircle(leftPupilX, pupilY + eyeYOffset, pupilSize, paint)
        canvas.drawCircle(rightPupilX, pupilY + eyeYOffset, pupilSize, paint)

        // Mouth
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = eyeRadius * 0.1f
        val mouthPath = Path()
        mouthPath.moveTo(width * 0.3f, mouthY)
        if (isTalking) {
            // Khi đang nói, miệng mở và đóng
            mouthPath.quadTo(width * 0.5f, mouthY + mouthOpenAmount * eyeRadius * 0.5f, width * 0.7f, mouthY)
        } else if (isListening) {
            // Khi đang nghe, miệng hơi mở
            mouthPath.quadTo(width * 0.5f, mouthY + eyeRadius * 0.1f, width * 0.7f, mouthY)
        } else {
            // Trạng thái bình thường
            mouthPath.quadTo(width * 0.5f, mouthY + smileAmount * eyeRadius, width * 0.7f, mouthY)
        }
        canvas.drawPath(mouthPath, paint)
    }

    fun startListening() {
        isListening = true
        isTalking = false
        mouthAnimator.cancel()
        listeningAnimator.start()
    }

    fun stopListening() {
        isListening = false
        listeningAnimator.cancel()
        attentionAmount = 0f
        invalidate()
    }

    fun startTalking() {
        isTalking = true
        mouthAnimator.start()
    }

    fun stopTalking() {
        isTalking = false
        mouthAnimator.cancel()
        mouthOpenAmount = 0f
        invalidate()
    }

    fun lookLeft() {
        animatePupils(leftPupilX - eyeRadius * 0.3f, rightPupilX - eyeRadius * 0.3f, pupilY)
    }

    fun lookRight() {
        animatePupils(leftPupilX + eyeRadius * 0.3f, rightPupilX + eyeRadius * 0.3f, pupilY)
    }

    fun lookUp() {
        animatePupils(leftPupilX, rightPupilX, pupilY - eyeRadius * 0.3f)
    }

    fun lookDown() {
        animatePupils(leftPupilX, rightPupilX, pupilY + eyeRadius * 0.3f)
    }

    fun lookStraight() {
        animatePupils(leftEyeX, rightEyeX, eyeY)
    }

    fun smile() {
        animateSmile(1f)
    }

    fun frown() {
        animateSmile(-1f)
    }

    fun neutral() {
        animateSmile(0f)
    }

    private fun animatePupils(newLeftX: Float, newRightX: Float, newY: Float) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                leftPupilX = leftPupilX + (newLeftX - leftPupilX) * fraction
                rightPupilX = rightPupilX + (newRightX - rightPupilX) * fraction
                pupilY = pupilY + (newY - pupilY) * fraction
                invalidate()
            }
        }.start()
    }

    private fun animateSmile(newSmileAmount: Float) {
        ValueAnimator.ofFloat(smileAmount, newSmileAmount).apply {
            duration = 300
            addUpdateListener { animator ->
                smileAmount = animator.animatedValue as Float
                invalidate()
            }
        }.start()
    }

    private var isTalking = false
    private var mouthOpenAmount = 0f
    private val mouthAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 300
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener { animator ->
            mouthOpenAmount = animator.animatedValue as Float
            invalidate()
        }
    }

    private var isListening = false
    private var attentionAmount = 0f
    private val listeningAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000 // 2 giây cho một chu kỳ
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener { animator ->
            attentionAmount = animator.animatedValue as Float
            invalidate()
        }
    }
}