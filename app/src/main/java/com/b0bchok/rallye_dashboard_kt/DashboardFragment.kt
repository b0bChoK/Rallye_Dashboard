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

package com.b0bchok.rallye_dashboard_kt

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.b0bchok.rallye_dashboard_kt.controller.ControllerConfigData
import com.b0bchok.rallye_dashboard_kt.databinding.DashboardFragmentBinding
import com.b0bchok.rallye_dashboard_kt.odometer.SpeedMeasures
import com.b0bchok.rallye_dashboard_kt.rd_loader.RoadbookLoader
import com.b0bchok.rallye_dashboard_kt.utils.PreferenceHelper
import com.b0bchok.rallye_dashboard_kt.utils.PreferenceHelper.autoLoadRoadbook
import com.b0bchok.rallye_dashboard_kt.utils.PreferenceHelper.avgSpeedGreenRange
import com.b0bchok.rallye_dashboard_kt.utils.PreferenceHelper.avgSpeedTarget
import com.b0bchok.rallye_dashboard_kt.utils.PreferenceHelper.chronometerDistance
import com.b0bchok.rallye_dashboard_kt.utils.PreferenceHelper.controllerConfig
import com.b0bchok.rallye_dashboard_kt.utils.PreferenceHelper.highlightAvgSpeed
import com.b0bchok.rallye_dashboard_kt.utils.PreferenceHelper.odometerIncrement
import com.b0bchok.rallye_dashboard_kt.utils.PreferenceHelper.odometerPrecision
import com.b0bchok.rallye_dashboard_kt.utils.PreferenceHelper.roadbookUri
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.Builder.IMPLICIT_MIN_UPDATE_INTERVAL
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

// This big fragment manage the roadbook interaction and the location
class DashboardFragment : Fragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        private const val TAG = "DashboardFragment"

        private val permissionsGps = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private val permissionsStorage = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private val TAG_IS_CHRONOMTER_RUNNING = "is-chronometer-running"
    private val TAG_CHRONOMETER_VALUE = "chronometer-value"

    private var _binding: DashboardFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var haveAccessToLocation: Boolean = false

    private lateinit var mSpeedMeasures: SpeedMeasures

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

    //Value get from shared preference
    private var minimumDistanceToStartChrono: Int = 40
    private var distanceIncrementation: Int = 10
    private lateinit var odometerFormat: String
    private var highlightAvgSpeed: Boolean = false
    private var avgSpeedTarget: Int = 55
    private var avgSpeedTargetRange: Int = 3

    //Controller configuration
    private var ActionCaseNext: Int = KeyEvent.KEYCODE_UNKNOWN
    private var ActionCasePrev: Int = KeyEvent.KEYCODE_UNKNOWN
    private var ActionIncreaseOdometer: Int = KeyEvent.KEYCODE_UNKNOWN
    private var ActionDecreaseOdometer: Int = KeyEvent.KEYCODE_UNKNOWN
    private var ActionRAZ: Int = KeyEvent.KEYCODE_UNKNOWN


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DashboardFragmentBinding.inflate(inflater, container, false)

        var theActivity = requireActivity()

        if (savedInstanceState != null) {
            isChronometerRunning = savedInstanceState.getBoolean(TAG_IS_CHRONOMTER_RUNNING)
            startChronometer = savedInstanceState.getLong(TAG_CHRONOMETER_VALUE)
        }

        mRbLoader = ViewModelProvider(theActivity)[RoadbookLoader::class.java]

        val roadbookLoadedObserver = Observer<Boolean> { status ->
            if (status && mRbLoader.isRoadbookLoaded) {
                Log.d(TAG, "End of loading")
                refreshRoadbookCases()
                binding.progressBar.visibility = View.GONE
            }
        }
        mRbLoader.roadbookLoaded.observe(viewLifecycleOwner, roadbookLoadedObserver)

        mSpeedMeasures = ViewModelProvider(this)[SpeedMeasures::class.java]

        theActivity.onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            callbackBackPressedCallback
        );

        updatePrefs()

        // Check access to location, if not, request it
        checkGNSSPermissions()

        // Initialize location callback, but not started yet
        // Callback is started if location access is granted and
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                locationResult.lastLocation?.let { updateLocation(it) }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeComponents()
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putLong(TAG_CHRONOMETER_VALUE, startChronometer)
        bundle.putBoolean(TAG_IS_CHRONOMTER_RUNNING, isChronometerRunning)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLocationUpdates()
        callbackBackPressedCallback.remove()
        razTimer.cancel()
        increaseTotalTimer.cancel()
        decreaseTotalTimer.cancel()
        _binding = null
    }

    private val callbackBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(getString(R.string.close_rallye_dashboard))
                .setMessage(getString(R.string.quit_the_app))
                .setCancelable(true)
                .setPositiveButton(
                    requireContext().getString(R.string.text_yes)
                ) { _, _ ->
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
                .setNegativeButton(requireContext().getString(R.string.text_no)) { _, _ ->
                    return@setNegativeButton
                }
            val dialog = builder.create()
            dialog.show()

            val dismissalDelayMillis = "5000" // 5 seconds
            val handler = Handler()
            handler.postDelayed({
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }, dismissalDelayMillis.toLong())
        }
    }

    private var isIncreaseTotalTimerRunning = false
    private val increaseTotalTimer: CountDownTimer = object : CountDownTimer(Long.MAX_VALUE, 500) {
        override fun onTick(l: Long) {
            isIncreaseTotalTimerRunning = true
            mSpeedMeasures.increaseTotalDistance(distanceIncrementation.toFloat())
            updateMeter()
        }

        override fun onFinish() {
            isIncreaseTotalTimerRunning = false
        }
    }

    private var isDecreaseTotalTimerRunning = false
    private val decreaseTotalTimer: CountDownTimer = object : CountDownTimer(Long.MAX_VALUE, 500) {
        override fun onTick(l: Long) {
            isDecreaseTotalTimerRunning = true
            mSpeedMeasures.decreaseTotalDistance(distanceIncrementation.toFloat())
            updateMeter()
        }

        override fun onFinish() {
            isDecreaseTotalTimerRunning = false
        }
    }

    private var isRazTimerRunning = false
    private val razTimer: CountDownTimer = object : CountDownTimer(2000, 500) {
        override fun onTick(l: Long) {
            isRazTimerRunning = true
        }

        override fun onFinish() {
            isRazTimerRunning = false
            raz()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeComponents() {
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val ratio = max(displayMetrics.heightPixels, displayMetrics.widthPixels).toFloat() / min(
            displayMetrics.heightPixels,
            displayMetrics.widthPixels
        ).toFloat()

        if (ratio < 1.9) {
            if (binding.caseLayout != null) {
                (binding.caseLayout?.layoutParams as ConstraintLayout.LayoutParams)
                    .matchConstraintPercentWidth = 0.6F
                binding.caseLayout!!.requestLayout()
            }
        }

        loadControllerConfiguration()

        updateButton()

        mImgCaseA = binding.imageCaseA
        mImgCaseB = binding.imageCaseB
        mImgCaseC = binding.imageCaseC

        binding.btSelectRoadbook.setOnLongClickListener {
            //Open menu
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdvancedMenuFragment(), "openRB")
                .addToBackStack(null)
                .commit()

            true
        }
        binding.btSelectRoadbook.setOnClickListener {
            Toast.makeText(
                context,
                requireContext().getString(R.string.click_rb),
                Toast.LENGTH_SHORT
            ).show()
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
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(requireContext().getString(R.string.rewind_roadbook))
                .setMessage(requireContext().getString(R.string.are_you_sure_you_want_to_return_to_first_case))
                .setCancelable(true)
                .setPositiveButton(
                    requireContext().getString(R.string.text_yes)
                ) { _, _ ->
                    Log.d(TAG, "Reset roadbook")
                    mRbLoader.goCase(0)
                    refreshRoadbookCases()
                }
                .setNegativeButton(requireContext().getString(R.string.text_no)) { _, _ ->
                    return@setNegativeButton
                }
            val dialog = builder.create()
            dialog.show()

            true
        }

        binding.btRAZ.setOnLongClickListener {
            raz()
            true
        }
        binding.btRAZ.setOnClickListener {
            Toast.makeText(
                requireContext(),
                requireContext().getString(R.string.click_raz),
                Toast.LENGTH_SHORT
            )
                .show()
        }

        binding.btIncreaseDist.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                isIncreaseTotalTimerRunning = true
                increaseTotalTimer.start();
            }
            if (event.action == MotionEvent.ACTION_UP) {
                isIncreaseTotalTimerRunning = false
                increaseTotalTimer.cancel();
            }
            true
        }
        binding.btDecreaseDist.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                isDecreaseTotalTimerRunning = true
                decreaseTotalTimer.start();
            }
            if (event.action == MotionEvent.ACTION_UP) {
                isDecreaseTotalTimerRunning = false
                decreaseTotalTimer.cancel();
            }
            true
        }

        mTxtClock = binding.txtCurrentTime
        mTxtTimer = binding.txtTimer
        mTxtSpeed = binding.txtSpeedOMeter
        mTxtAvgSpeed = binding.txtAVGSpeed
        mTxtMaxSpeed = binding.txtTOPSpeed
        mTxtOdometer = binding.txtOdometer

        mHandlerClock.post(mRunnableClock)

        refreshRoadbookCases()
        updateMeter()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, s: String?) {
        Log.d(TAG, "New preference !")
        updatePrefs()
        updateButton()
    }

    private fun updatePrefs() {
        val prefs = PreferenceHelper.defaultPreference(requireContext())

        minimumDistanceToStartChrono = prefs.chronometerDistance?.toInt() ?: 40
        distanceIncrementation = prefs.odometerIncrement?.toInt() ?: 10
        if (prefs.odometerPrecision)
            odometerFormat = requireContext().getString(R.string.odometer_format_10m)
        else
            odometerFormat = requireContext().getString(R.string.odometer_format_100m)

        if (!prefs.autoLoadRoadbook)
            prefs.roadbookUri = ""

        highlightAvgSpeed = prefs.highlightAvgSpeed
        if (highlightAvgSpeed) //HIGHLIGHT_AVG_SPEED
        {
            avgSpeedTarget = prefs.avgSpeedTarget
            avgSpeedTargetRange = prefs.avgSpeedGreenRange
        }

        val roadbookUri = prefs.roadbookUri
        if (roadbookUri != "" && prefs.autoLoadRoadbook && !mRbLoader.isRoadbookLoaded) {
            Log.d(TAG, "Auto load roadbook \"%s\" - %b".format(roadbookUri, prefs.autoLoadRoadbook))
            val uri = Uri.parse(roadbookUri)
            val dir = DocumentFile.fromTreeUri(requireActivity(), uri)

            if (dir != null) {
                binding.progressBar.visibility = View.VISIBLE
                mRbLoader.setRoadbookDir(dir)
            }
        }
    }

    private fun updateButton() {
        binding.btIncreaseDist.text =
            String.format(getString(R.string.increase_button_pattern), distanceIncrementation)
        binding.btDecreaseDist.text =
            String.format(getString(R.string.decrease_button_pattern), distanceIncrementation)
    }

    override fun onStart() {
        super.onStart()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    @SuppressLint("MissingPermission")
    // Check if location is available after getting authorization
    private fun requestLocation() {
        if(haveAccessToLocation) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

            locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
                .setWaitForAccurateLocation(false)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setMinUpdateIntervalMillis(IMPLICIT_MIN_UPDATE_INTERVAL)
                .setMinUpdateDistanceMeters(10F)
                .build()

            val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)

            val client: SettingsClient = LocationServices.getSettingsClient(requireActivity())
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

            task.addOnSuccessListener { locationSettingsResponse ->
                // All location settings are satisfied. The client can initialize
                // location requests here.
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        Log.d(TAG, "Location : %s".format(location))

                        // Got last known location. In some rare situations this can be null.
                        if (location != null)
                            updateLocation(location)

                        startLocationUpdates()
                    }
            }

            task.addOnFailureListener { exception ->
                if (exception is ResolvableApiException){
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    Toast.makeText(requireContext(),
                        getString(R.string.app_cannot_function_without_gps), Toast.LENGTH_LONG)
                        .show()
                    Log.w(TAG, "Location settings are not satisfied.")
                }
            }
        }
    }

    private fun updateLocation(location: Location) {
        // Got last known location. In some rare situations this can be null.
        mSpeedMeasures.updateLocation(location)

        // Update odometer and speed
        if (context != null)
            updateMeter()
        else
            Log.w(TAG, "Not attached to context ! (onLocationChanged)")
    }

    private fun checkGNSSPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                haveAccessToLocation = true
                Log.d(TAG, "ACCESS_FINE_LOCATION : Permission granted")

                requestLocation()
            }

            requireActivity().shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined. In this UI, include a
                // "cancel" or "no thanks" button that lets the user continue
                // using your app without granting the permission.
                val builder = AlertDialog.Builder(requireContext())
                builder.setPositiveButton(requireContext().getString(R.string.text_yes)) { _: DialogInterface, _: Int -> enableLocationSettings() }
                    .setNegativeButton(requireContext().getString(R.string.text_no)) { _: DialogInterface, _: Int -> gpsNotEnabled() }
                    .setMessage(requireContext().getString(R.string.alert_location_detail))
                    .setTitle(requireContext().getString(R.string.alert_location_required))
                val alertDialog = builder.create()
                alertDialog.show()
            }

            else -> {
                // You can directly ask for the permission.
                requestPermissions(permissionsGps, 1)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    haveAccessToLocation = true
                    Log.d(TAG, "ACCESS_FINE_LOCATION : Permission granted")
                    requestLocation()

                } else {
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.

                    val builder = AlertDialog.Builder(requireContext())
                    builder.setNeutralButton(requireContext().getString(R.string.ok)) { _: DialogInterface, _: Int -> gpsNotEnabled() }
                        .setMessage(getString(R.string.limited_use))
                        .setTitle(requireContext().getString(R.string.alert_location_required))
                    val alertDialog = builder.create()
                    alertDialog.show()
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun enableLocationSettings() {
        Log.i(TAG, "Enabling location in settings")
        val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(settingsIntent)
    }

    private fun gpsNotEnabled() {
        Log.w(TAG, "GPS not enabled")
        Toast.makeText(requireContext(),
            getString(R.string.app_cannot_function_without_gps), Toast.LENGTH_LONG)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateMeter() {
        mTxtOdometer?.text = String.format(
            odometerFormat,
            mSpeedMeasures.getDistanceTotalM() / 1000
        )
        mTxtSpeed?.text = String.format(
            requireContext().getString(R.string.speed_format),
            (mSpeedMeasures.getSpeedMS() * 3.6).toInt()
        )
        mTxtMaxSpeed?.text =
            String.format(
                requireContext().getString(R.string.max_speed_format),
                (mSpeedMeasures.getMaxSpeedMS() * 3.6).toInt()
            )
    }

    /**
     * Display real time clock and update timer
     */
    private val mHandlerClock = Handler()
    private val mRunnableClock: Runnable = object : Runnable {
        override fun run() {
            if (context != null) {
                val currentDate = Calendar.getInstance().time

                // Update current time
                val simpleDateFormat =
                    SimpleDateFormat(getString(R.string.time_pattern), Locale.getDefault())
                mTxtClock?.text = simpleDateFormat.format(currentDate)
                updateTimer(currentDate.time)

                // Update chronometer
                if (isChronometerRunning) {
                    val currentDurationMS = currentDate.time - startChronometer
                    // Update AVG Speed
                    val avgSpeed = mSpeedMeasures.getAverageSpeed(currentDurationMS) * 3.6
                    mTxtAvgSpeed?.text = String.format(
                        requireContext().getString(R.string.avg_speed_format),
                        (avgSpeed)
                    )
                    if (highlightAvgSpeed) {
                        if ((avgSpeed >= avgSpeedTarget + avgSpeedTargetRange) or (avgSpeed <= avgSpeedTarget - avgSpeedTargetRange))
                            mTxtAvgSpeed?.setTextColor(requireContext().getColor(R.color.avg_speed_critical))
                        else
                            mTxtAvgSpeed?.setTextColor(requireContext().getColor(R.color.avg_speed_ok))
                    } else
                        mTxtAvgSpeed?.setTextColor(requireContext().getColor(R.color.text))

                    // Update chrono
                    mTxtTimer?.text = String.format(
                        requireContext().getString(R.string.timer_format),
                        (currentDurationMS / 60000).toInt(),
                        ((currentDurationMS / 1000) % 60).toInt()
                    )

                } else if (mSpeedMeasures.getDistanceTotalM() > minimumDistanceToStartChrono) {
                    isChronometerRunning = true
                    startChronometer = currentDate.time
                }
            } else
                Log.w(TAG, "Not attached to context ! (mRunnableClock)")

            mHandlerClock.postDelayed(this, 1000)
        }
    }

    private fun updateTimer(currentTime: Long) {
        if (isChronometerRunning) {
            if (context != null) {
                val simpleDateFormat = SimpleDateFormat(
                    getString(R.string.chronometer_pattern),
                    Locale.getDefault()
                )
                mTxtTimer?.text = simpleDateFormat.format(Date(currentTime - startChronometer))
            } else
                Log.w(TAG, "Not attached to context ! (updateTimer)")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun raz() {
        val prefs = PreferenceHelper.defaultPreference(requireContext())
        mSpeedMeasures.raz()
        isChronometerRunning = false
        startChronometer = 0
        mTxtTimer?.text = requireContext().getString(R.string.zero_timer)
        mTxtOdometer?.text =
            if (prefs.odometerPrecision) requireContext().getString(R.string.zero_odometer) else requireContext().getString(
                R.string.zero_odometer_dec
            )
        mTxtSpeed?.text = requireContext().getString(R.string.zero_speed)
        mTxtMaxSpeed?.text = requireContext().getString(R.string.zero_max_speed)
        mTxtAvgSpeed?.text = requireContext().getString(R.string.zero_avg_speed)
        mTxtAvgSpeed?.setTextColor(requireContext().getColor(R.color.text))
    }

    private fun refreshRoadbookCases() {
        val index = mRbLoader.currentCase
        Log.d(TAG, "Current case is " + index + " over " + mRbLoader.casesSize)
        val currentIndex = mRbLoader.currentCase
        //The case C is the lower and should draw the current case, then caseB then caseA (display order from bottom to top)
        this.loadCase(mImgCaseA, currentIndex + 2)
        this.loadCase(mImgCaseB, currentIndex + 1)
        this.loadCase(mImgCaseC, currentIndex)
    }

    private fun loadCase(theCase: ImageView?, index: Int) {
        if (theCase != null) {
            if (mRbLoader.isRoadbookLoaded) {
                val caseFile = mRbLoader.getCase(index)
                try {
                    if (caseFile != null) theCase.setImageURI(caseFile.uri) else theCase.setImageResource(
                        R.drawable.case_empty
                    )
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Cannot set uri the Image View %s".format(caseFile?.uri.toString()),
                        e
                    )
                    theCase.setImageResource(
                        R.drawable.case_error
                    )
                }

            }
        }
    }

    private fun loadControllerConfiguration() {
        val prefs = PreferenceHelper.defaultPreference(requireContext())
        if ((prefs.controllerConfig != null) and (prefs.controllerConfig != "")) {
            val remoteConfig =
                Gson().fromJson(prefs.controllerConfig, ControllerConfigData::class.java)

            ActionCaseNext = KeyEvent.keyCodeFromString(remoteConfig.nextCase)
            ActionCasePrev = KeyEvent.keyCodeFromString(remoteConfig.prevCase)
            ActionIncreaseOdometer = KeyEvent.keyCodeFromString(remoteConfig.increaseOdo)
            ActionDecreaseOdometer = KeyEvent.keyCodeFromString(remoteConfig.decreaseOdo)
            ActionRAZ = KeyEvent.keyCodeFromString(remoteConfig.raz)
        }
    }

    // Manage key event from remote controller
    fun dispatchKeyEvent(event: KeyEvent?): Boolean {

        val keyCode = event?.keyCode
        val action = event?.action

        if (action == KeyEvent.ACTION_DOWN) {
            return when (keyCode) {
                ActionCaseNext -> {
                    true
                }

                ActionCasePrev -> {
                    true
                }

                ActionIncreaseOdometer -> {
                    if (!isIncreaseTotalTimerRunning) {
                        isIncreaseTotalTimerRunning = true
                        increaseTotalTimer.start()
                    }
                    true
                }

                ActionDecreaseOdometer -> {
                    if (!isDecreaseTotalTimerRunning) {
                        isDecreaseTotalTimerRunning = true
                        decreaseTotalTimer.start()
                    }
                    true
                }

                ActionRAZ -> {
                    if (!isRazTimerRunning) {
                        isRazTimerRunning = true
                        razTimer.start()
                    }
                    true
                }

                else -> false
            }
        } else if (action == KeyEvent.ACTION_UP) {
            return when (keyCode) {
                ActionCaseNext -> {
                    mRbLoader.goNextCase()
                    refreshRoadbookCases()
                    true
                }

                ActionCasePrev -> {
                    mRbLoader.goPrevCase()
                    refreshRoadbookCases()
                    true
                }

                ActionIncreaseOdometer -> {
                    isIncreaseTotalTimerRunning = false
                    increaseTotalTimer.cancel()
                    true
                }

                ActionDecreaseOdometer -> {
                    isDecreaseTotalTimerRunning = false
                    decreaseTotalTimer.cancel()
                    true
                }

                ActionRAZ -> {
                    razTimer.cancel()
                    isRazTimerRunning = false
                    true
                }

                else -> false
            }
        }

        return false
    }
}
