/*
Copyright (c) 2024 DEWAS Albert

This license replace the general license of the software
and apply to all file in this directory "rb_loader".

Permission is hereby granted, free of charge, to any person obtaining
a copy of this package and associated documentation files (the "package"),
to deal in the package without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom
the package is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall
be included in all copies or substantial portions of the package.

THE package IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE package.
 */

package com.b0bchok.rallye_dashboard_kt.rd_loader

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class RoadbookLoader : ViewModel() {

    companion object {
        private const val TAG = "RoadbookLoader"
    }

    private lateinit var mRoadbookDir: DocumentFile
    private var ordererFiles: Array<DocumentFile>? = null
    var currentCase = 0
    val roadbookLoaded: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }

    fun setRoadbookDir(dir: DocumentFile) {
        mRoadbookDir = dir
        ordererFiles = null
        currentCase = 0
        roadbookLoaded.value = false
        GlobalScope.launch(Dispatchers.IO) {
            loadCases()
        }
    }

    fun getRoadbookName(): String? {
        return mRoadbookDir.name
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
        if (isRoadbookLoaded) {
            if (n >= 0 && n < ordererFiles!!.size) {
                currentCase = n
            }
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
        Log.d(TAG, "Load case for new RB")
        var result = true
        try {
            if (mRoadbookDir!!.exists() && mRoadbookDir!!.isDirectory) {
                ordererFiles = mRoadbookDir!!.listFiles()
            } else
                Log.w(TAG, "Uri not valid ! %s".format(mRoadbookDir))

            if (ordererFiles != null) {
                // Keep only image file
                ordererFiles = ordererFiles!!.filter {
                    it.isFile && it.name?.split(".")?.get(1)
                        .equals("jpg", true) || it.name?.split(".")
                        ?.get(1).equals("png", true)
                }.toTypedArray()

                // Sort by name
                ordererFiles!!.sortWith(Comparator { o1: DocumentFile, o2: DocumentFile ->
                    extractWeight(o1.name!!) - extractWeight(o2.name!!)
                })
                for (i in ordererFiles!!) {
                    Log.d(TAG, "FileName:" + i.name)
                }
                Log.i(TAG, ordererFiles!!.size.toString() + " cases in roadbook")
            }
        }catch (e: Exception) {
            Log.e(TAG, "Cannot load and sort the roadbook", e)
            result = false
        }
        roadbookLoaded.postValue(result)
    }

    private fun extractWeight(s: String): Int {
        val oSplits = s.split("_", "-", ".").toTypedArray()
        var oSweight = Int.MAX_VALUE
        if (oSplits[0].equals("case", ignoreCase = true)) {
            if (isNumeric(oSplits[1]))
                oSweight = Integer.valueOf(oSplits[1])
        } else {
            if (isNumeric(oSplits[0]))
                oSweight = Integer.valueOf(oSplits[0])
        }
        return oSweight
    }

    private fun isNumeric(toCheck: String): Boolean {
        return toCheck.toDoubleOrNull() != null
    }
}