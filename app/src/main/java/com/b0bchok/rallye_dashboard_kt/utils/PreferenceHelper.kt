package com.b0bchok.rallye_dashboard_kt.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object PreferenceHelper {

    val CHRONOMETER_DISTANCE = "CHRONOMETER_DISTANCE"
    val ODOMETER_INCREMENT = "ODOMETER_INCREMENT"
    val ODOMETER_PRECISION = "ODOMETER_PRECISION"
    val ROADBOOK_URI = "ROADBOOK_URI"
    val AUTO_LOAD_ROADBOOK = "AUTO_LOAD_ROADBOOK"
    val CONTROLLER_CONFIG = "CONTROLLER_CONFIG"
    val HIGHLIGHT_AVG_SPEED = "HIGHLIGHT_AVG_SPEED"
    val AVG_SPEED_TARGET = "AVG_SPEED_TARGET"
    val AVG_SPEED_GREEN_RANGE = "AVG_SPEED_GREEN_RANGE"

    fun defaultPreference(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    fun customPreference(context: Context, name: String): SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)

    inline fun SharedPreferences.editMe(operation: (SharedPreferences.Editor) -> Unit) {
        val editMe = edit()
        operation(editMe)
        editMe.apply()
    }

    var SharedPreferences.chronometerDistance
        get() = getString(CHRONOMETER_DISTANCE, "40")
        set(value) {
            editMe {
                it.putString(CHRONOMETER_DISTANCE, value)
            }
        }

    var SharedPreferences.odometerIncrement
        get() = getString(ODOMETER_INCREMENT, "10")
        set(value) {
            editMe {
                it.putString(ODOMETER_INCREMENT, value)
            }
        }

    var SharedPreferences.odometerPrecision
        get() = getBoolean(ODOMETER_PRECISION, true)
        set(value) {
            editMe {
                it.putBoolean(ODOMETER_PRECISION, value)
            }
        }

    var SharedPreferences.roadbookUri
        get() = getString(ROADBOOK_URI, "")
        set(value) {
            editMe {
                it.putString(ROADBOOK_URI, value)
            }
        }

    var SharedPreferences.autoLoadRoadbook
        get() = getBoolean(AUTO_LOAD_ROADBOOK, false)
        set(value) {
            editMe {
                it.putBoolean(AUTO_LOAD_ROADBOOK, value)
            }
        }

    var SharedPreferences.highlightAvgSpeed
        get() = getBoolean(HIGHLIGHT_AVG_SPEED, false)
        set(value) {
            editMe {
                it.putBoolean(HIGHLIGHT_AVG_SPEED, value)
            }
        }
    var SharedPreferences.avgSpeedTarget
        get() = getInt(AVG_SPEED_TARGET, 55)
        set(value) {
            editMe {
                it.putInt(AVG_SPEED_TARGET, value)
            }
        }
    var SharedPreferences.avgSpeedGreenRange
        get() = getInt(AVG_SPEED_GREEN_RANGE, 3)
        set(value) {
            editMe {
                it.putInt(AVG_SPEED_GREEN_RANGE, value)
            }
        }

    var SharedPreferences.clearValues
        get() = { }
        set(value) {
            editMe {
                it.clear()
            }
        }

    var SharedPreferences.controllerConfig
        get() = getString(CONTROLLER_CONFIG, "")
        set(value) {
            editMe {
                it.putString(CONTROLLER_CONFIG, value)
            }
        }
}