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
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

// https://www.delasign.com/blog/android-studio-kotlin-read-json/

fun ReadJSONFromAssets(context: Context, path: String): String {
    val identifier = "[ReadJSON]"
    try {
        val file = context.assets.open("$path")
        Log.d(
            identifier,
            "Found File: $file.",
        )
        val bufferedReader = BufferedReader(InputStreamReader(file))
        val stringBuilder = StringBuilder()
        bufferedReader.useLines { lines ->
            lines.forEach {
                stringBuilder.append(it)
            }
        }
        Log.d(
            identifier,
            "getJSON stringBuilder: $stringBuilder.",
        )
        val jsonString = stringBuilder.toString()
        Log.d(
            identifier,
            "JSON as String: $jsonString.",
        )
        return jsonString
    } catch (e: Exception) {
        Log.e(
            identifier,
            "Error reading JSON: $e.",
        )
        e.printStackTrace()
        return ""
    }
}