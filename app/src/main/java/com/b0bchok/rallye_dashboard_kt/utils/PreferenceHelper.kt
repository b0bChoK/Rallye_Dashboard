/*
The program Rallye Dashboard is a combination of a roadbook reader
and a odometer destined to french road racer.

Copyright (C) 2024 DEWAS Albert

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
    val CUSTOM_PAGE_CONFIG = "CUSTOM_PAGE_CONFIG"

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

    var SharedPreferences.customPageConfig
        get() = getString(CUSTOM_PAGE_CONFIG, "")
        set(value) {
            editMe {
                it.putString(CUSTOM_PAGE_CONFIG, value)
            }
        }
}