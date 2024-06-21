package com.b0bchok.rallye_dashboard_kt.rd_loader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import com.b0bchok.rallye_dashboard_kt.utils.ReadJSONFromAssets
import com.google.gson.Gson
import kotlin.math.abs

enum class LineSelection {
    LEFT, RIGHT, TOP, BOTTOM, COLUMN_A, COLUMN_B, NONE
}

class RoadbookPreView(context: Context, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatImageView(context, attrs) {

    private val linePaint: Paint = Paint()
    private val selectedLinePaint: Paint = Paint()

    private var touchDownX: Float = .0F
    private var touchDownY: Float = .0F
    private var touchX: Float = .0F
    private var touchY: Float = .0F

    private var initialized = false
    private var leftMarginPX: Float = .0F
    private var rightMarginPX: Float = .0F
    private var topMarginPX: Float = .0F
    private var bottomMarginPX: Float = .0F
    private var columnAWidthPX: Float = .0F
    private var columnBWidthPX: Float = .0F
    private var numberLine: Int = 0


    companion object {
        private const val TAG = "RoadbookPreView"
        private const val EPSILON_SELECTION: Float = 30.0F
        private const val MINIMUM_MOTION: Float = 10.0F
        private const val MINIMUM_HORIZONTAL_SEPARATION: Float = 20.0F
        private const val MINIMUM_VERTICAL_SEPARATION: Float = 50.0F
    }

    // In percent of the size of the page
    private var jsonString = ReadJSONFromAssets(context, "CFRR_config.json")
    private var pageConfig: ConverterConfigData =
        Gson().fromJson(jsonString, ConverterConfigData::class.java)

    private var selectedLine: LineSelection = LineSelection.NONE

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
        if (!initialized) {
            loadConfig()
            initialized = true
        }

        // right line
        canvas.drawLine(
            leftMarginPX,
            topMarginPX,
            leftMarginPX,
            height - bottomMarginPX,
            if (selectedLine == LineSelection.LEFT) selectedLinePaint else linePaint
        )
        // left line
        canvas.drawLine(
            width - rightMarginPX,
            topMarginPX,
            width - rightMarginPX,
            height - bottomMarginPX,
            if (selectedLine == LineSelection.RIGHT) selectedLinePaint else linePaint
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
            columnAWidthPX,
            topMarginPX,
            columnAWidthPX,
            height - bottomMarginPX,
            if (selectedLine == LineSelection.COLUMN_A) selectedLinePaint else linePaint
        )
        // start column B line
        canvas.drawLine(
            width - columnBWidthPX,
            topMarginPX,
            width - columnBWidthPX,
            height - bottomMarginPX,
            if (selectedLine == LineSelection.COLUMN_B) selectedLinePaint else linePaint
        )

        // inner case lines
        val caseHeight = (height - (topMarginPX + bottomMarginPX)) / numberLine
        for (i in 1..<numberLine) {
            canvas.drawLine(
                leftMarginPX,
                topMarginPX + (i * caseHeight),
                width - rightMarginPX,
                topMarginPX + (i * caseHeight),
                linePaint
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y

                //Select a line
                selectLine(event.x, event.y)
            }

            MotionEvent.ACTION_MOVE -> {
                //Mouve selected line
                touchX = event.x
                touchY = event.y

                if (selectedLine != LineSelection.NONE) {
                    moveLine(event.x, event.y)
                }
            }
        }

        Log.d(TAG, "onTouchEvent %s".format(event.toString()))

        this.invalidate()

        return true
    }

    private fun selectLine(x: Float, y: Float) {
        // First look horizontal line
        // Top margin
        if ((topMarginPX <= y + EPSILON_SELECTION) and (topMarginPX >= y - EPSILON_SELECTION)) {
            selectedLine = LineSelection.TOP
            return
        }
        // Bottom margin
        if ((height - bottomMarginPX <= y + EPSILON_SELECTION) and (height - bottomMarginPX >= y - EPSILON_SELECTION)) {
            selectedLine = LineSelection.BOTTOM
            return
        }
        // Left margin
        if ((leftMarginPX <= x + EPSILON_SELECTION) and (leftMarginPX >= x - EPSILON_SELECTION)) {
            selectedLine = LineSelection.LEFT
            return
        }
        // Right margin
        if ((width - rightMarginPX <= x + EPSILON_SELECTION) and (width - rightMarginPX >= x - EPSILON_SELECTION)) {
            selectedLine = LineSelection.RIGHT
            return
        }
        // Column A
        if ((columnAWidthPX <= x + EPSILON_SELECTION) and (columnAWidthPX >= x - EPSILON_SELECTION)) {
            selectedLine = LineSelection.COLUMN_A
            return
        }
        // Column B
        if ((width - columnBWidthPX <= x + EPSILON_SELECTION) and (width - columnBWidthPX >= x - EPSILON_SELECTION)) {
            selectedLine = LineSelection.COLUMN_B
            return
        }

        selectedLine = LineSelection.NONE
    }

    private fun moveLine(x: Float, y: Float) {
        Log.d(TAG, "Selected line %s".format(selectedLine.toString()))

        // Check minimum motion
        if ((abs(touchDownY - y) > MINIMUM_MOTION) or (abs(touchDownX - x) > MINIMUM_MOTION)) {

            touchDownX = -MINIMUM_MOTION
            touchDownY = -MINIMUM_MOTION

            when (selectedLine) {
                LineSelection.TOP -> {
                    if(y < height - bottomMarginPX - MINIMUM_VERTICAL_SEPARATION)
                        topMarginPX = y
                }

                LineSelection.BOTTOM -> {
                    if(y > topMarginPX + MINIMUM_VERTICAL_SEPARATION)
                        bottomMarginPX = height - y
                }

                LineSelection.LEFT -> {
                    if(x < width - columnBWidthPX - MINIMUM_HORIZONTAL_SEPARATION)
                        leftMarginPX = x
                }

                LineSelection.RIGHT -> {
                    if(x > columnAWidthPX + MINIMUM_HORIZONTAL_SEPARATION )
                        rightMarginPX = width - x
                }

                LineSelection.COLUMN_A -> {
                    if ((x > leftMarginPX + MINIMUM_HORIZONTAL_SEPARATION) and (x < width - columnBWidthPX - MINIMUM_HORIZONTAL_SEPARATION))
                        columnAWidthPX = x
                }

                LineSelection.COLUMN_B -> {
                    if ((x > columnAWidthPX + MINIMUM_HORIZONTAL_SEPARATION) and (x < width - rightMarginPX - MINIMUM_HORIZONTAL_SEPARATION))
                        columnBWidthPX = width - x
                }

                else -> return
            }
        }
    }

    private fun loadConfig() {
        leftMarginPX = pageConfig.leftMargin * width
        rightMarginPX = pageConfig.rightMargin * width
        topMarginPX = pageConfig.topMargin * height
        bottomMarginPX = pageConfig.bottomMargin * height
        columnAWidthPX = pageConfig.columnAWidth * width
        columnBWidthPX = pageConfig.columnBWidth * width
        numberLine = pageConfig.lineNumber
    }

    fun updateCurrentPageConfig() : ConverterConfigData {
        pageConfig.lineNumber = numberLine
        pageConfig.leftMargin = leftMarginPX / width
        pageConfig.rightMargin = rightMarginPX / width
        pageConfig.topMargin = topMarginPX / height
        pageConfig.bottomMargin = bottomMarginPX / height
        pageConfig.columnAWidth = columnAWidthPX / width
        pageConfig.columnBWidth = columnBWidthPX / width

        return pageConfig
    }

    fun loadCFRRConfig() {
        jsonString = ReadJSONFromAssets(context, "CFRR_config.json")
        pageConfig = Gson().fromJson(jsonString, ConverterConfigData::class.java)

        loadConfig()

        this.invalidate()
    }

    fun loadTrippyConfig() {
        jsonString = ReadJSONFromAssets(context, "trippy_config.json")
        pageConfig = Gson().fromJson(jsonString, ConverterConfigData::class.java)

        loadConfig()

        this.invalidate()
    }

    fun changeNumberLine(n : Int) {
        numberLine = n

        this.invalidate()
    }
}