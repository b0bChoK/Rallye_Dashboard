package com.b0bchok.rallye_dashboard_kt.rd_loader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import com.b0bchok.rallye_dashboard_kt.utils.ReadJSONFromAssets
import com.google.gson.Gson

enum class LineSelection {
    LEFT, RIGHT, TOP, BOTTOM, COLUMN_A, COLUMN_B, NONE
}

class RoadbookPreView(context: Context, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatImageView(context, attrs) {

    private val linePaint: Paint = Paint()
    private val selectedLinePaint: Paint = Paint()

    companion object {
        private const val TAG = "RoadbookPreView"
    }

    // In percent of the size of the page
    private var jsonString = ReadJSONFromAssets(context, "CFRR_config.json")
    var pageConfig : ConverterConfigData = Gson().fromJson(jsonString, ConverterConfigData::class.java)

    var selectedLine : LineSelection = LineSelection.NONE

    init {
        linePaint.color = Color.CYAN
        linePaint.strokeWidth = 3f

        selectedLinePaint.color = Color.RED
        selectedLinePaint.strokeWidth = 3f
    }

    // https://stackoverflow.com/questions/6178896/how-to-draw-a-line-in-imageview-on-android
    // https://stackoverflow.com/questions/3616676/how-to-draw-a-line-in-android

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        Log.d(TAG, "View size %d %d".format(width, height))
        val leftMarginPX = pageConfig.leftMargin * width
        val rightMarginPX = pageConfig.rightMargin * width
        val topMarginPX = pageConfig.topMargin * height
        val bottomMarginPX = pageConfig.bottomMargin * height
        val columnAWidthPX = pageConfig.columnAWidth * width
        val columnBWidthPX = pageConfig.columnBWidth * width

        // right line
        canvas.drawLine(
            leftMarginPX,
            topMarginPX,
            leftMarginPX,
            height - bottomMarginPX,
            if (selectedLine == LineSelection.RIGHT) selectedLinePaint else linePaint
        )
        // left line
        canvas.drawLine(
            width - rightMarginPX,
            topMarginPX,
            width - rightMarginPX,
            height - bottomMarginPX,
            if (selectedLine == LineSelection.LEFT) selectedLinePaint else linePaint
        )
        // top line
        canvas.drawLine(
            leftMarginPX,
            topMarginPX,
            width - rightMarginPX,
            topMarginPX,
            if (selectedLine == LineSelection.TOP) selectedLinePaint else linePaint
        )
        // bottom line
        canvas.drawLine(
            leftMarginPX,
            height - bottomMarginPX,
            width - rightMarginPX,
            height - bottomMarginPX,
            if (selectedLine == LineSelection.BOTTOM) selectedLinePaint else linePaint
        )
        // end column A line
        canvas.drawLine(
            leftMarginPX + columnAWidthPX,
            topMarginPX,
            leftMarginPX + columnAWidthPX,
            height - bottomMarginPX,
            if (selectedLine == LineSelection.COLUMN_A) selectedLinePaint else linePaint
        )
        // start column B line
        canvas.drawLine(
            width - (rightMarginPX + columnBWidthPX),
            topMarginPX,
            width - (rightMarginPX + columnBWidthPX),
            height - bottomMarginPX,
            if (selectedLine == LineSelection.COLUMN_B) selectedLinePaint else linePaint
        )

        // inner case lines
        val caseHeight = (height - (topMarginPX + bottomMarginPX)) / pageConfig.lineNumber
        for (i in 1..< pageConfig.lineNumber) {
            canvas.drawLine(
                leftMarginPX,
                topMarginPX + (i * caseHeight),
                width - rightMarginPX,
                topMarginPX + (i * caseHeight),
                linePaint
            )
        }
    }

    fun loadCFRRConfig(){
        jsonString = ReadJSONFromAssets(context, "CFRR_config.json")
        pageConfig = Gson().fromJson(jsonString, ConverterConfigData::class.java)

        this.invalidate()
    }

    fun loadTrippyConfig(){
        jsonString = ReadJSONFromAssets(context, "trippy_config.json")
        pageConfig = Gson().fromJson(jsonString, ConverterConfigData::class.java)

        this.invalidate()
    }
}