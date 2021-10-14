package com.elfefe.controller.manager.numberpicker

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.deleo.contour.R
import com.deleo.contour.controller.adapter.YearPickerAdapter
import android.util.DisplayMetrics
import android.widget.TextView
import androidx.core.math.MathUtils.clamp
import androidx.core.view.children
import com.deleo.contour.controller.manager.utils.extension.onMain
import com.deleo.contour.controller.manager.utils.log
import kotlinx.android.synthetic.main.item_year_picker.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*


open class YearPicker : RecyclerView {
    lateinit var linearLayoutManager: LinearLayoutManager

    var minValue: Int = 18
        set(value) {
            field = value
            swapAdapter(YearPickerAdapter(), true)
        }

    var maxValue: Int = 70
        set(value) {
            field = value
            swapAdapter(YearPickerAdapter(), true)
        }

    var defaultValue: Int = 30
        set(value) {
            field = value
            scrollToPosition(field - minValue)
        }

    var currentValue: Int = defaultValue
    var onValueChange: (Int) -> Unit = {}

    var sizeOfText: Float = 50f

    private var horizontalVelocity = 0
    private var isTouched = false
    private var isScrolling = AtomicBoolean(false)
    private var isSmoothScrolling = false
    private var currentPosition = 0

    private val isSmoothItemEffectRunning = AtomicBoolean(false)

    constructor(context: Context) : super(context) {
        initialise()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        processXmlAttributes(attrs)
        initialise()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        processXmlAttributes(attrs, defStyleAttr)
        initialise()
    }

    override fun onScrolled(dx: Int, dy: Int) {
        super.onScrolled(dx, dy)
        isScrolling.set(true)
        horizontalVelocity = dx

        smoothItemsEffects()

        if (!isTouched && !isSmoothScrolling) {
            if ((1..SNAP_VELOCITY).contains(dx))
                smoothSnapToPosition(
                    linearLayoutManager.findLastVisibleItemPosition(),
                    LinearSmoothScroller.SNAP_TO_END
                )
            else if ((-SNAP_VELOCITY..-1).contains(dx))
                smoothSnapToPosition(
                    linearLayoutManager.findFirstVisibleItemPosition(),
                    LinearSmoothScroller.SNAP_TO_START
                )
        }
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (state == 0) {
            smoothScrollToClosest()
            isScrolling.set(false)
            isSmoothScrolling = false
        }
    }

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        e?.let {
            isTouched = it.action != 1
        }
        return super.onTouchEvent(e)
    }

    private fun RecyclerView.smoothSnapToPosition(
        position: Int,
        snapMode: Int = LinearSmoothScroller.SNAP_TO_ANY
    ) {
        currentPosition = position
        isSmoothScrolling = true
        val smoothScroller = object : LinearSmoothScroller(this.context) {
            override fun getHorizontalSnapPreference(): Int = snapMode
            override fun calculateTimeForDeceleration(dx: Int): Int {
                val displayMetrics: DisplayMetrics = context.resources.displayMetrics
                val millisecPerPixel =
                    MILLISECONDS_PER_INCH_FOR_DECELERATION / displayMetrics.densityDpi
                val time = ceil((abs(dx) * millisecPerPixel).toDouble()).toInt()
                return ceil(time / .3356).toInt()
            }

            override fun onStop() {
                super.onStop()
                currentValue = (checkClosest().first as TextView).text.toString().toInt()
                onValueChange(currentValue)
            }
        }
        smoothScroller.targetPosition = position
        linearLayoutManager.startSmoothScroll(smoothScroller)
    }

    private fun processXmlAttributes(
        attrs: AttributeSet,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
    ) {
        val attributes = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.YearPicker,
            defStyleAttr,
            defStyleRes
        )

        try {
            minValue = attributes.getInt(R.styleable.YearPicker_minValue, minValue)
            maxValue = attributes.getInt(R.styleable.YearPicker_maxValue, maxValue)
            defaultValue = attributes.getInt(R.styleable.YearPicker_defaultValue, defaultValue)
            sizeOfText = attributes.getDimension(R.styleable.YearPicker_textSize, 20f)
        } finally {
            attributes.recycle()
        }
    }

    private fun initialise() {
        layoutManager = LinearLayoutManager(context)
        linearLayoutManager = layoutManager as LinearLayoutManager
        linearLayoutManager.orientation = HORIZONTAL

        adapter = YearPickerAdapter()
        itemEffects()
        scrollToPosition(defaultValue - minValue)
    }

    private fun smoothScrollToClosest() {
        if (!isSmoothScrolling) {
            checkClosest().run {
                first?.let {
                    linearLayoutManager.run {
                        val index = getPosition(it.parent as View)
                        smoothSnapToPosition(index, second)
                    }
                }
            }
        }
    }

    private fun checkClosest(): Pair<View?, Int> {
        var lastClosest = Pair<Float, Pair<View?, Int>>(width.toFloat(), Pair(null, LinearSmoothScroller.SNAP_TO_ANY))
        val center = width / 2f
        for (i in 0 until childCount) {
            val ageText = children.toList()[i].textview_age
            val offsetBounds = Rect()
            ageText.getDrawingRect(offsetBounds)
            offsetDescendantRectToMyCoords(ageText, offsetBounds)
            val closestLeft = center - offsetBounds.left
            val closestRight = center - offsetBounds.right
            val closest = min(abs(closestLeft) + center, abs(closestRight) + center) - center
//            log(this, "${ageText.text} -> Closest: $closest, " +
//                    "left: ${offsetBounds.left}($closestLeft), " +
//                    "right: ${offsetBounds.right}($closestRight), " +
//                    "center: $center/$width")
            if (abs(closest) <= lastClosest.first) {
                val currentClosest = min(closest, lastClosest.first)
                var snap = lastClosest.second.second
                lastClosest = Pair(
                    currentClosest,
                    Pair(
                        if (currentClosest == closest && currentClosest != lastClosest.first) {
                            snap = if (closest == abs(closestLeft)) LinearSmoothScroller.SNAP_TO_START else LinearSmoothScroller.SNAP_TO_END
                            ageText
                        } else lastClosest.second,
                        snap
                    )
                ) as Pair<Float, Pair<View?, Int>>
            }
        }
        return lastClosest.second
    }

    private fun smoothItemsEffects() {
        if (!isSmoothItemEffectRunning.getAndSet(true)) {
            GlobalScope.launch(Dispatchers.Default) {
                var time = 0
                while (isScrolling.get() && time < MILLISEC_END_EFFECT) {
                    delay(10)
                    onMain {
                        itemEffects()
                    }
                    time += 10
                }
                isSmoothItemEffectRunning.set(false)
            }
        }
    }

    private fun itemEffects() {
        adapter?.run {
            for (i in 0 until childCount) {
                val ageText = children.toList()[i].textview_age
                val offsetBounds = Rect()
                ageText.getDrawingRect(offsetBounds)
                offsetDescendantRectToMyCoords(ageText, offsetBounds)
                val max: Float = (width / 2).toFloat()
                val value: Float =
                    if (offsetBounds.right > max) (width - (offsetBounds.right - (offsetBounds.width() / 2))).toFloat() else offsetBounds.right.toFloat() - (offsetBounds.width() / 2)
                val percent: Float = if (value > 0) value / max else 0f
                ageText.textSize = clamp(percent * 130f, 50f, 102f)
            }
        }
    }

    companion object {
        const val SNAP_VELOCITY = 8
        const val MILLISECONDS_PER_INCH_FOR_DECELERATION = 200f
        const val MILLISEC_END_EFFECT = 10000
    }
}
