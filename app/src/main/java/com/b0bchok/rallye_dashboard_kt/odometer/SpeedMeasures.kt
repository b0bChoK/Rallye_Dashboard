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

package com.b0bchok.rallye_dashboard_kt.odometer

import android.location.Location
import androidx.lifecycle.ViewModel

class SpeedMeasures : ViewModel() {
    private var endLongitude: String? = null
    private var endLatitude: String? = null

    private var speedMS = 0f
    private var maxSpeedMS = 0f
    private var distanceTotalM = 0f

    fun updateLocation(location: Location) {
        val locationResults = floatArrayOf(0f, 0f, 0f, 0f)
        val startLatitude = endLatitude
        val startLongitude = endLongitude

        // Get GNSS position
        endLatitude = location.latitude.toString()
        endLongitude = location.longitude.toString()

        // Get GNSS speed in m/s
        if(location.hasSpeed())
            speedMS = location.speed

        // Update max speed
        if (speedMS > maxSpeedMS) {
            maxSpeedMS = speedMS
        }
        if (endLatitude == null || startLatitude == null) {
        } else {
            Location.distanceBetween(
                startLatitude.toFloat().toDouble(),
                startLongitude!!.toFloat().toDouble(),
                endLatitude!!.toFloat().toDouble(),
                endLongitude!!.toFloat().toDouble(), locationResults
            )
            if (locationResults[0] > 0 && speedMS > 0) {
                distanceTotalM += locationResults[0]
            }
        }
    }

    fun getDistanceTotalM(): Float {
        return distanceTotalM
    }

    fun getSpeedMS(): Float {
        return speedMS
    }

    fun getMaxSpeedMS(): Float {
        return maxSpeedMS
    }

    fun increaseTotalDistance(value: Float) {
        distanceTotalM += value
    }

    fun decreaseTotalDistance(value: Float) {
        distanceTotalM -= value
        if (distanceTotalM < 0) distanceTotalM = 0f
    }

    fun raz() {
        distanceTotalM = 0f
        maxSpeedMS = 0f
        speedMS = 0f
    }

    fun getAverageSpeed(duration: Long): Float {
        return if(distanceTotalM > 0f && duration > 0f)
            distanceTotalM / (duration / 1000)
        else
            0f
    }
}