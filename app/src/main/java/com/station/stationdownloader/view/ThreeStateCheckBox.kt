package com.station.stationdownloader.view

import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatCheckBox
import com.station.stationdownloader.R

class ThreeStateCheckbox : AppCompatCheckBox {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val middle_states = intArrayOf(R.attr.complex_state_part)
    private val focus_middle_states= intArrayOf(R.attr.complex_state_part,android.R.attr.state_middle)
    private var isMiddle: Boolean = false
    private var mBroadcasting: Boolean = false
    private var onStateChangeListener: OnStateChangeListener? = null

    private val checkDrawable = resources.getDrawable(R.drawable.ic_c)
    private val unCheckDrawable = ...
    private val middleDrawable = ...

    init {
        val stateListDrawable = StateListDrawable()
        stateListDrawable.addState(middle_states, middleDrawable)
        stateListDrawable.addState(intArrayOf(),)
        stateListDrawable.addState(intArrayOf(),)

        stateListDrawable.addState(intArrayOf(),)

        stateListDrawable.addState(intArrayOf(),)

        stateListDrawable.addState(intArrayOf(android.R.attr.state_checked), checkDrawable)
        stateListDrawable.addState(intArrayOf(), unCheckDrawable)
        stateListDrawable.setBounds(0, 0, stateListDrawable.minimumWidth, stateListDrawable.minimumHeight)
        setCompoundDrawables(stateListDrawable, null, null, null)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
//        buttonDrawable = null
        buttonDrawable = StateListDrawable()   //有些低版本手机还是会显示默认的框，用这个方式去掉
    }

    override fun toggle() {
        if (isMiddle) {
            isChecked = true
        } else {
            super.toggle()
        }
    }

    override fun setChecked(checked: Boolean) {
        val checkedChanged = isChecked != checked
        super.setChecked(checked)
        val wasMiddle = isMiddle
        setMiddleState(false, false)
        if (wasMiddle || checkedChanged) {
            notifyStateListener()
        }
    }

    /**
     * 设置中间状态
     */
    fun setMiddleState(indeterminate: Boolean) {
        setMiddleState(indeterminate, true)
    }

    private fun getState(): Boolean? {
        return if (isMiddle) null else isChecked
    }

    /**
     * 设置状态，null表示中间状态
     */
    fun setState(state: Boolean?) {
        if (state != null) {
            isChecked = state
        } else {
            setMiddleState(true)
        }
    }

    private fun setMiddleState(isMiddle: Boolean, notify: Boolean) {
        if (this.isMiddle != isMiddle) {
            this.isMiddle = isMiddle
            refreshDrawableState()
            if (notify) {
                notifyStateListener()
            }
        }
    }

    private fun notifyStateListener() {
        if (mBroadcasting) {
            return
        }

        mBroadcasting = true
        if (onStateChangeListener != null) {
            onStateChangeListener!!.onStateChanged(this, getState())
        }
        mBroadcasting = false
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (getState() == null) {
            View.mergeDrawableStates(drawableState, states)
        }
        return drawableState
    }

    fun setOnStateChangeListener(listener: OnStateChangeListener) {
        this.onStateChangeListener = listener
    }

    interface OnStateChangeListener {
        fun onStateChanged(checkbox: ThreeStateCheckbox, newState: Boolean?)
    }
}