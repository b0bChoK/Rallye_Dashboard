package com.example.rallye_dashboard_kt

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile

class RoadbookLoader(private val context: Context, val uri: Uri) {
    companion object {
        const val PREF_KEY = "ROAD_BOOK_LOADED"

        fun isDefined(context: Context): Boolean {
            val pref = Prefs.with(context).getString(PREF_KEY, "")
            if (!pref.isNullOrBlank()) {
                val documentFile = DocumentFile.fromTreeUri(context, pref.toUri())
                if (documentFile != null) {
                    return documentFile.exists() && documentFile.isDirectory
                }
            }
            return false
        }

        fun load(context: Context): RoadbookLoader? {
            val uri = Prefs.with(context).getString(PREF_KEY, "")
            if (uri.isNullOrEmpty()) {
                return null
            }
            return RoadbookLoader(context, uri.toUri())
        }

        fun forget(context: Context) {
            Prefs.with(context).edit().putString(PREF_KEY, "").apply()
        }
    }

    private var ordererFiles: Array<DocumentFile>? = null
    var currentCase = 0



    fun goNextCase() {
        if (isRoadbookLoaded) {
            if (currentCase < ordererFiles!!.size - 1) {
                currentCase += 1
            }
        }
    }

    fun goPrevCase() {
        if (isRoadbookLoaded) {
            if (currentCase > 0) {
                currentCase -= 1
            }
        }
    }

    fun goCase(n: Int) {
        if (n >= 0 && n < ordererFiles!!.size) {
            currentCase = n
        }
    }

    val casesSize: Int
        get() = if (isRoadbookLoaded) {
            ordererFiles!!.size
        } else 0

    fun getCase(nb: Int): DocumentFile? {
        return if (nb >= 0 && nb < ordererFiles!!.size) {
            ordererFiles!![nb]
        } else null
    }

    val isRoadbookLoaded: Boolean
        get() = ordererFiles != null

    fun loadCases() {
        var roadbookDir = DocumentFile.fromTreeUri(context, uri)

        if (roadbookDir!!.exists() && roadbookDir!!.isDirectory) {
            ordererFiles = roadbookDir!!.listFiles()
        }
        if (ordererFiles != null) {
            ordererFiles!!.sortWith(Comparator { o1: DocumentFile, o2: DocumentFile ->
                val o1splits = o1.name!!
                    .split("_|\\.").toTypedArray()
                val o2splits =
                    o2.name!!.split("_|\\.").toTypedArray()
                var os1weight = Int.MAX_VALUE
                var os2weight = Int.MAX_VALUE
                if (o1splits[0].equals("case", ignoreCase = true)) {
                    os1weight = Integer.valueOf(o1splits[1])
                }
                if (o2splits[0].equals("case", ignoreCase = true)) {
                    os2weight = Integer.valueOf(o2splits[1])
                }
                os1weight - os2weight
            })
            for (i in ordererFiles!!) {
                Log.d("RoadbookLoader", "FileName:" + i.name)
            }
            Log.i("RoadbookLoader", ordererFiles!!.size.toString() + " cases in roadbook")
        }
    }
}