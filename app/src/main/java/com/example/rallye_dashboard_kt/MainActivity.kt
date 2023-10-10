package com.example.rallye_dashboard_kt

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import com.example.rallye_dashboard_kt.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity(), LocationListener {

    companion object {
        private val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var locationManager: LocationManager
    private lateinit var mSpeedMeasures: SpeedMeasures
    private var mConfiguration: Configuration = Configuration()

    private lateinit var mRbLoader: RoadbookLoader
    private var mImgCaseA: ImageView? = null
    private var mImgCaseB: ImageView? = null
    private var mImgCaseC: ImageView? = null

    private var mTxtClock: TextView? = null
    private var mTxtTimer: TextView? = null
    private var mTxtOdometer: TextView? = null
    private var mTxtSpeed: TextView? = null
    private var mTxtMaxSpeed: TextView? = null
    private var mTxtAvgSpeed: TextView? = null

    private var startChronometer: Long = 0
    private var isChronometerRunning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mRbLoader = ViewModelProvider(this)[RoadbookLoader::class.java]
        mSpeedMeasures = ViewModelProvider(this)[SpeedMeasures::class.java]

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        initializeComponents()

        checkPermissions()
    }

    override fun onStart() {
        super.onStart()


    }

    private fun initializeComponents() {
        binding.btIncreaseDist.text =
            String.format("+ %d M", mConfiguration.DistanceIncrementation.toInt())
        binding.btDecreaseDist.text =
            String.format("- %d M", mConfiguration.DistanceIncrementation.toInt())

        mImgCaseA = binding.imageCaseA
        mImgCaseB = binding.imageCaseB
        mImgCaseC = binding.imageCaseC

        binding.btSelectRoadbook.setOnLongClickListener {
            openFolderPrompt.launch(null)
            true
        }
        binding.btSelectRoadbook.setOnClickListener {
            Toast.makeText(this, getString(R.string.click_rb), Toast.LENGTH_SHORT).show()
        }


        binding.btPageDown.setOnClickListener {
            mRbLoader.goNextCase()
            refreshRoadbookCases()
        }

        binding.btPageUp.setOnClickListener {
            mRbLoader.goPrevCase()
            refreshRoadbookCases()
        }

        binding.btPageUp.setOnLongClickListener {
            mRbLoader.goCase(0)
            refreshRoadbookCases()
            true
        }

        binding.btRAZ.setOnLongClickListener {
            raz()
            true
        }
        binding.btRAZ.setOnClickListener {
            Toast.makeText(this, getString(R.string.click_raz), Toast.LENGTH_SHORT).show()
        }

        binding.btIncreaseDist.setOnClickListener {
            mSpeedMeasures.increaseTotalDistance(mConfiguration.DistanceIncrementation)
            updateMeter()
        }
        binding.btDecreaseDist.setOnClickListener {
            mSpeedMeasures.decreaseTotalDistance(mConfiguration.DistanceIncrementation)
            updateMeter()
        }

        mTxtClock = binding.txtCurrentTime
        mTxtTimer = binding.txtTimer
        mTxtSpeed = binding.txtSpeedOMeter
        mTxtAvgSpeed = binding.txtAVGSpeed
        mTxtMaxSpeed = binding.txtTOPSpeed
        mTxtOdometer = binding.txtOdometer

        mHandlerClock.post(mRunnableClock)

        refreshRoadbookCases()
    }


    private fun checkPermissions() {
        val anyDenied = permissions.any {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_DENIED
        }
        if (anyDenied) {
            requestPermissions(permissions, 1)
        } else {
            checkGPS()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                checkGPS()
            } else {
                gpsNotEnabled()
            }
        }
    }

    private fun checkGPS() {
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!gpsEnabled) {

            val builder = AlertDialog.Builder(this)
            builder.setPositiveButton("Yes") { _: DialogInterface, _: Int -> enableLocation() }
                .setNegativeButton("No") { _: DialogInterface, _: Int -> gpsNotEnabled() }
                .setMessage("Please enable Location in system settings")
                .setTitle("Location Required")
            val alertDialog = builder.create()
            alertDialog.show()
        }

        Log.d("Main", "Check permission")

        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("Main", "Start update location")

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500,
                10.0f,
                this@MainActivity
            )
        }
    }

    private fun enableLocation() {
        val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(settingsIntent)
    }

    private fun gpsNotEnabled() {
        Toast.makeText(this, "App cannot function without GPS", Toast.LENGTH_LONG).show()
    }

    override fun onLocationChanged(p0: Location) {
        mSpeedMeasures.updateLocation(p0)

        // Update odometer and speed
        updateMeter()
    }

    private fun updateMeter() {
        mTxtOdometer?.text = String.format("%.2f km", mSpeedMeasures.getDistanceTotalM() / 1000)
        mTxtSpeed?.text = String.format("%03d km/h", (mSpeedMeasures.getSpeedMS() * 3.6).toInt())
        mTxtMaxSpeed?.text =
            String.format("%03d km/h", (mSpeedMeasures.getMaxSpeedMS() * 3.6).toInt())
    }

    /**
     * Display real time clock and update timer
     */
    private val mHandlerClock = Handler()
    private val mRunnableClock: Runnable = object : Runnable {
        override fun run() {
            val currentDate = Calendar.getInstance().time

            // Update current time
            val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            mTxtClock?.text = simpleDateFormat.format(currentDate)
            updateTimer(currentDate.time)

            // Update chronometer
            if (isChronometerRunning) {
                val currentDurationMS = currentDate.time - startChronometer
                // Update AVG Speed
                mTxtAvgSpeed?.text = String.format(
                    "%03d km/h",
                    (mSpeedMeasures.getAverageSpeed(currentDurationMS) * 3.6).toInt()
                )

                // Update chrono
                mTxtTimer?.text = String.format(
                    "%02dm%02ds",
                    (currentDurationMS / 60000).toInt(),
                    ((currentDurationMS / 1000) % 60).toInt()
                )

            } else if (mSpeedMeasures.getDistanceTotalM() > mConfiguration.MinimumDistanceToStartChrono) {
                isChronometerRunning = true
                startChronometer = currentDate.time
            }
            mHandlerClock.postDelayed(this, 1000)
        }
    }

    private fun updateTimer(currentTime: Long) {
        if (isChronometerRunning) {
            val simpleDateFormat = SimpleDateFormat("mm'm'ss's'", Locale.getDefault())
            mTxtTimer?.text = simpleDateFormat.format(Date(currentTime - startChronometer))
        }
    }

    private fun raz() {
        mSpeedMeasures.raz()
        isChronometerRunning = false
        startChronometer = 0
        mTxtTimer?.text = "00m00s"
        mTxtOdometer?.text = "0,00 km"
        mTxtSpeed?.text = "000 km/h"
        mTxtMaxSpeed?.text = "000 km/h"
        mTxtAvgSpeed?.text = "000 km/h"
    }

    private fun refreshRoadbookCases() {
        val index = mRbLoader.currentCase
        Log.d("Main", "Current case is " + index + " over " + mRbLoader.casesSize)
        val currentIndex = mRbLoader.currentCase
        //The case C is the lower and should draw the current case, then caseB then caseA (display order from bottom to top)
        val caseCindex = currentIndex
        val caseBindex = currentIndex + 1
        val caseAindex = currentIndex + 2
        this.loadCase(mImgCaseA, caseAindex)
        this.loadCase(mImgCaseB, caseBindex)
        this.loadCase(mImgCaseC, caseCindex)
    }

    private fun loadCase(theCase: ImageView?, index: Int) {
        if (theCase != null) {
            if (mRbLoader.isRoadbookLoaded) {
                val caseFile = mRbLoader.getCase(index)
                if (caseFile != null) theCase.setImageURI(caseFile.uri) else theCase.setImageResource(
                    R.drawable.case_empty
                )
            }
        }
    }

    private val openFolderPrompt =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }
            Log.d("Main", "Open document tree " + uri.toString())
            DocumentFile.fromTreeUri(this, uri)?.let { mRbLoader.setRoadbookDir(it) }
            refreshRoadbookCases()
        }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                mRbLoader.goNextCase()
                refreshRoadbookCases()
                true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                mRbLoader.goPrevCase()
                refreshRoadbookCases()
                true
            }

            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                mSpeedMeasures.increaseTotalDistance(mConfiguration.DistanceIncrementation)
                updateMeter()
                true
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                mSpeedMeasures.decreaseTotalDistance(mConfiguration.DistanceIncrementation)
                updateMeter()
                true
            }

            else -> super.onKeyUp(keyCode, event)
        }
    }

    @Override
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                raz()
                true
            }

            else -> super.onKeyLongPress(keyCode, event)
        }
    }

    // https://stuff.mit.edu/afs/sipb/project/android/docs/training/managing-audio/volume-playback.html
    // tester pour éviter d'appeler les fonctoins media du systeme
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return super.dispatchKeyEvent(event)
    }
}