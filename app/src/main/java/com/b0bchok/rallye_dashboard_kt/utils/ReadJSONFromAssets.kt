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