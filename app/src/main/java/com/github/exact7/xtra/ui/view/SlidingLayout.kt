package com.github.exact7.xtra.ui.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.customview.widget.ViewDragHelper
import com.google.android.exoplayer2.ui.PlayerView

class SlidingLayout : LinearLayout {

    private val viewDragHelper = ViewDragHelper.create(this, 1f, SlidingCallback())

    private lateinit var playerView: PlayerView
    private lateinit var secondView: View
    private var playerViewTop = 0

    private var topBound = 0
    private var bottomBound = 0
    private var minimizeThreshold = 0

    private var minimizedLeft = 0
    private var minimizedRight = 0
    private var minimizedTop = 0
    private var minimizedBottom = 0

    private var clickStartX = 0f
    private var clickStartY = 0f
    private var clickEndX = 0f
    private var clickEndY = 0f
    private var clickDuration = 0L

    private var isMaximized = true
    private var isAnimating = false

    private var initialPlayerViewBottom = 0
    private var initialSecondViewLeft = 0
    private var minimizedSecondViewTranslationY = 0f

    private var callback: Callback? = null
    private val animationListener = object : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {}
        override fun onAnimationCancel(animation: Animator?) {}
        override fun onAnimationStart(animation: Animator?) { isAnimating = true }
        override fun onAnimationEnd(animation: Animator?) { isAnimating = false }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount < 2) {
            throw IllegalStateException()
        }
        playerView = getChildAt(0) as PlayerView
        secondView = getChildAt(1)
        playerView.post {
            topBound = paddingTop
            bottomBound = (secondView.height / 1.25f).toInt()
            minimizeThreshold = height / 4
            initialPlayerViewBottom = playerView.bottom
            when (orientation) {
                1 -> { //portrait
                    minimizedLeft = (width * 0.4f).toInt()
                    minimizedRight = (width * 0.9f).toInt()
                    minimizedTop = (height * 0.7f).toInt()
                    minimizedBottom = (height * 0.9f).toInt()
                    minimizedSecondViewTranslationY = minimizedBottom.toFloat()
                }
                else -> { //landscape
                    minimizedLeft = (width * 0.6f).toInt()
                    minimizedRight = (width * 0.95f).toInt()
                    minimizedTop = (height * 0.4f).toInt()
                    minimizedBottom = (height * 0.8f).toInt()
                    initialSecondViewLeft = secondView.left
                    minimizedSecondViewTranslationY = minimizedTop.toFloat()
                }
            }
            println("MAXIMIZED $isMaximized")
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (isMaximized) {
            playerView.layout(l, playerViewTop, r, playerViewTop + playerView.measuredHeight)
            secondView.layout(l, playerViewTop + playerView.measuredHeight, r, b)
        } else {

        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                viewDragHelper.cancel()
                return false
            }
            MotionEvent.ACTION_DOWN -> {
                if (ev.getPointerId(ev.actionIndex) == ViewDragHelper.INVALID_POINTER) {
                    return false
                }
            }
        }
        val interceptTap = viewDragHelper.isViewUnder(playerView, ev.x.toInt(), ev.y.toInt())
        return viewDragHelper.shouldInterceptTouchEvent(ev) || interceptTap
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isAnimating) {
            return true
        }
        if (isViewHit(playerView, event.x.toInt(), event.y.toInt()) && isClick(event)) {
            performClick()
            return true
        }
        viewDragHelper.processTouchEvent(event)
        return true
    }

    override fun performClick(): Boolean {
        return if (isMaximized) {
            println("CLICK")
            return super.performClick()
        } else {
            println("$top ${playerView.top}")
//            maximize()
            false
        }
    }

    private fun isViewHit(view: View, x: Int, y: Int): Boolean {
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val parentLocation = IntArray(2)
        getLocationOnScreen(parentLocation)
        val screenX = parentLocation[0] + x
        val screenY = parentLocation[1] + y
        return (screenX >= viewLocation[0]
                && screenX < viewLocation[0] + view.width
                && screenY >= viewLocation[1]
                && screenY < viewLocation[1] + view.height)
    }

    private fun isClick(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                clickStartX = event.x
                clickStartY = event.y
            }
            MotionEvent.ACTION_UP -> {
                clickEndX = event.x
                clickEndY = event.y
                clickDuration = event.eventTime - event.downTime
            }
        }
        return clickStartX == clickEndX && clickStartY == clickEndY && clickDuration >= 0 && clickDuration < 300
    }

    fun maximize() {
        isMaximized = true
        animate(0, 0, width, initialPlayerViewBottom)
        val l = PropertyValuesHolder.ofInt("left", initialSecondViewLeft)
        val r = PropertyValuesHolder.ofFloat("translationY", 0f)
        ObjectAnimator.ofPropertyValuesHolder(secondView, l, r).apply {
            duration = 270L
            addListener(animationListener)
            start()
        }
        callback?.onMaximize()
    }

    fun minimize() {
        isMaximized = false
        secondView.left = width
        secondView.translationY = minimizedSecondViewTranslationY
        animate(minimizedLeft, minimizedTop, minimizedRight, minimizedBottom)
        callback?.onMinimize()
    }

    private fun animate(left: Int, top: Int, right: Int, bottom: Int) {
        val l = PropertyValuesHolder.ofInt("left", left)
        val r = PropertyValuesHolder.ofInt("right", right)
        val t = PropertyValuesHolder.ofInt("top", top)
        val b = PropertyValuesHolder.ofInt("bottom", bottom)
        ObjectAnimator.ofPropertyValuesHolder(playerView, l, r, t, b).apply {
            duration = 300L
            addListener(animationListener)
            start()
        }
    }

    private fun smoothSlideTo(slideOffset: Float): Boolean {
        val left = (slideOffset * minimizedLeft).toInt()
        val top = (slideOffset * minimizedTop).toInt()
        if (viewDragHelper.smoothSlideViewTo(playerView, left, top)) {
            postInvalidateOnAnimation()
            return true
        }
        return false
    }

    private fun closeTo(left: Int) {
        if (viewDragHelper.smoothSlideViewTo(playerView, left, playerView.top)) {
            postInvalidateOnAnimation()
        }
        callback?.onClose()
    }

    override fun computeScroll() {
        if (viewDragHelper.continueSettling(true)) {
            postInvalidateOnAnimation()
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        return bundleOf("superState" to super.onSaveInstanceState(), "isMaximized" to isMaximized)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state.let {
            if (it is Bundle) {
                isMaximized = it.getBoolean("isMaximized")
                println("RESTORE $isMaximized")
                it.getParcelable("superState")
            } else {
                it
            }
        })
    }

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    private inner class SlidingCallback : ViewDragHelper.Callback() {

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return child == playerView
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return if (isMaximized) Math.min(Math.max(top, topBound), bottomBound) else child.top
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return if (!isMaximized) left else child.left
        }

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            playerViewTop = top
            secondView.requestLayout()
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            if (isMaximized) {
                when {
                    releasedChild.top >= minimizeThreshold -> minimize()
                    else -> smoothSlideTo(0f)
                }
            } else {
                when {
                    xvel > 1500 -> closeTo(width)
                    xvel < -1500 -> closeTo(-playerView.width)
                    else -> {
                        smoothSlideTo(1f)
                    }
                }
            }
        }
    }

    interface Callback {
        fun onMinimize()
        fun onMaximize()
        fun onClose()
    }
}