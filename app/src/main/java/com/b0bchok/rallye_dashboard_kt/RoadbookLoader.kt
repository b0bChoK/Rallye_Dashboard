package com.b0bchok.rallye_dashboard_kt

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
    val roadbookLoaded : MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }

    fun setRoadbookDir(dir: DocumentFile) {
        mRoadbookDir = dir
        ordererFiles = null
        roadbookLoaded.value = false
        GlobalScope.launch(Dispatchers.IO){
            loadCases()
        }
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
        if (mRoadbookDir!!.exists() && mRoadbookDir!!.isDirectory) {
            ordererFiles = mRoadbookDir!!.listFiles()
        }

        if (ordererFiles != null) {
            // Keep only image file
            ordererFiles = ordererFiles!!.filter { it.isFile && it.name?.split(".")?.get(1).equals("jpg", true) || it.name?.split(".")?.get(1).equals("png", true) }.toTypedArray()

            // Sort by name
            ordererFiles!!.sortWith(Comparator { o1: DocumentFile, o2: DocumentFile ->
                extractWeight(o1.name!!) - extractWeight(o2.name!!)
            })
            for (i in ordererFiles!!) {
                Log.d(TAG, "FileName:" + i.name)
            }
            Log.i(TAG, ordererFiles!!.size.toString() + " cases in roadbook")
        }

        roadbookLoaded.postValue(true)
    }

    private fun extractWeight(s : String): Int {
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