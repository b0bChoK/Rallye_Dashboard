package com.b0bchok.rallye_dashboard_kt

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel

class RoadbookLoader : ViewModel() {
    private lateinit var mRoadbookDir: DocumentFile
    private var ordererFiles: Array<DocumentFile>? = null
    var currentCase = 0

    fun setRoadbookDir(dir: DocumentFile) {
        mRoadbookDir = dir
        loadCases()
    }

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
        if (mRoadbookDir!!.exists() && mRoadbookDir!!.isDirectory) {
            ordererFiles = mRoadbookDir!!.listFiles()
        }

        if (ordererFiles != null) {
            // Keep only image file
            ordererFiles = ordererFiles!!.filter { it.isFile && it.name?.split(".")?.get(1).equals("jpg", true) || it.name?.split(".")?.get(1).equals("png", true) }.toTypedArray()

            // Sort by name
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