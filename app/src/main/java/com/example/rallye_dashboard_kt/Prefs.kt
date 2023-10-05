package com.example.rallye_dashboard_kt

import android.content.Context
import android.content.SharedPreferences

object Prefs {

    private var mPrefs: SharedPreferences? = null

    private fun getFileName(context: Context) = "${context.packageName}.prefs"

    fun with(context: Context): SharedPreferences {
        if (mPrefs == null) {
            val appContext = context.applicationContext
            if (appContext != null) {
                mPrefs = appContext.getSharedPreferences(
                    getFileName(appContext), Context.MODE_PRIVATE
                )
            } else {
                throw IllegalArgumentException("context.getApplicationContext() returned null")
            }
        }
        return mPrefs!!
    }
}