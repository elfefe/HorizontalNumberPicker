package com.elfefe.controller.adapter

import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.deleo.contour.R
import com.deleo.contour.controller.BaseApplication
import com.deleo.contour.controller.manager.numberpicker.YearPicker
import com.deleo.contour.controller.manager.utils.log
import kotlinx.android.synthetic.main.item_year_picker.view.*

class YearPickerAdapter: RecyclerView.Adapter<YearPickerAdapter.ViewHolder>() {
    lateinit var ages: List<Int>
    var min: Int = 18
    var max: Int = 70

    lateinit var yearPicker: YearPicker

    private val inflater: LayoutInflater by lazy { LayoutInflater.from(BaseApplication.INSTANCE) }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        yearPicker = recyclerView as YearPicker
        ages = (yearPicker.minValue - 2..yearPicker.maxValue + 2).toList()
        min = yearPicker.minValue
        max = yearPicker.maxValue
        log(this, "$ages ${yearPicker.minValue} ${yearPicker.maxValue}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(inflater.inflate(R.layout.item_year_picker, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.update(ages[position])
    }

    override fun getItemCount(): Int = ages.size

    inner class ViewHolder(val item: View): RecyclerView.ViewHolder(item) {
        fun update(age: Int) {
            with(item) {
                if ((min..max).contains(age)) {
                    textview_age.text = age.toString()
                } else
                    textview_age.text = ""
            }
        }
    }
}
