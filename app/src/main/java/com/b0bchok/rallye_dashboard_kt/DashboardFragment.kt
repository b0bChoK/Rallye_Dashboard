package com.b0bchok.rallye_dashboard_kt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.b0bchok.rallye_dashboard_kt.databinding.DashboardFragmentBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min


class DashboardFragment : Fragment(), LocationListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        private const val TAG = "DashboardFragment"
        private val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private val TAG_IS_CHRONOMTER_RUNNING = "is-chronometer-running"
    private val TAG_CHRONOMETER_VALUE = "chronometer-value"

    private var _binding: DashboardFragmentBinding? = null;
    private val binding get() = _binding!!;

    private lateinit var locationManager: LocationManager
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DashboardFragmentBinding.inflate(inflater, container, false);

        if (savedInstanceState != null) {
            isChronometerRunning = savedInstanceState.getBoolean(TAG_IS_CHRONOMTER_RUNNING)
            startChronometer = savedInstanceState.getLong(TAG_CHRONOMETER_VALUE)
        }

        mRbLoader = ViewModelProvider(this)[RoadbookLoader::class.java]
        mSpeedMeasures = ViewModelProvider(this)[SpeedMeasures::class.java]

        locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (sharedPreferences != null) {
            minimumDistanceToStartChrono =
                sharedPreferences.getString("chronometer_distance", "40")!!.toInt()
            distanceIncrementation =
                sharedPreferences.getString("odometer_increment", "10")!!.toInt()

            if (sharedPreferences.getBoolean("odometer_precision", true))
                odometerFormat = requireContext().getString(R.string.odometer_format_10m)
            else
                odometerFormat = requireContext().getString(R.string.odometer_format_100m)
        }

        checkPermissions()

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
        _binding = null
    }

    private val increaseTotalTimer: CountDownTimer = object : CountDownTimer(Long.MAX_VALUE, 500) {
        override fun onTick(l: Long) {
            mSpeedMeasures.increaseTotalDistance(distanceIncrementation.toFloat())
            updateMeter()
        }
        override fun onFinish() {}
    }
    private val decreaseTotalTimer: CountDownTimer = object : CountDownTimer(Long.MAX_VALUE, 500) {
        override fun onTick(l: Long) {
            mSpeedMeasures.decreaseTotalDistance(distanceIncrementation.toFloat())
            updateMeter()
        }
        override fun onFinish() {}
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

        updateButton()

        mImgCaseA = binding.imageCaseA
        mImgCaseB = binding.imageCaseB
        mImgCaseC = binding.imageCaseC

        binding.btSelectRoadbook.setOnLongClickListener {
            openFolderPrompt.launch(null)
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
                increaseTotalTimer.start();
            }
            if (event.action == MotionEvent.ACTION_UP) {
                increaseTotalTimer.cancel();
            }
            true
        }
        binding.btDecreaseDist.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                decreaseTotalTimer.start();
            }
            if (event.action == MotionEvent.ACTION_UP) {
                decreaseTotalTimer.cancel();
            }
            true
        }

        binding.btOpenConfig.setOnClickListener {
            Toast.makeText(
                requireContext(),
                requireContext().getString(R.string.click_setup),
                Toast.LENGTH_SHORT
            )
                .show()
        }

        binding.btOpenConfig.setOnLongClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment(), "settings")
                .addToBackStack(null)
                .commit()
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
        if (sharedPreferences != null) {
            minimumDistanceToStartChrono =
                sharedPreferences.getString("chronometer_distance", "40")!!.toInt()
            distanceIncrementation =
                sharedPreferences.getString("odometer_increment", "10")!!.toInt()
            if (sharedPreferences.getBoolean("odometer_precision", false))
                odometerFormat = requireContext().getString(R.string.odometer_format_10m)
            else
                odometerFormat = requireContext().getString(R.string.odometer_format_100m)
        }

        updateButton()
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

    private fun checkPermissions() {
        val anyDenied = permissions.any {
            ContextCompat.checkSelfPermission(requireContext(), it) ==
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

            val builder = AlertDialog.Builder(requireContext())
            builder.setPositiveButton(requireContext().getString(R.string.text_yes)) { _: DialogInterface, _: Int -> enableLocation() }
                .setNegativeButton(requireContext().getString(R.string.text_no)) { _: DialogInterface, _: Int -> gpsNotEnabled() }
                .setMessage(requireContext().getString(R.string.alert_location_detail))
                .setTitle(requireContext().getString(R.string.alert_location_required))
            val alertDialog = builder.create()
            alertDialog.show()

        }

        Log.d(TAG, "Check permission")

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Start update location")

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500,
                10.0f,
                this
            )
        }
    }

    private fun enableLocation() {
        val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(settingsIntent)
    }

    private fun gpsNotEnabled() {
        Toast.makeText(requireContext(), "App cannot function without GPS", Toast.LENGTH_LONG)
            .show()
    }

    override fun onLocationChanged(p0: Location) {
        mSpeedMeasures.updateLocation(p0)

        // Update odometer and speed
        if (context != null)
            updateMeter()
        else
            Log.w(TAG, "Not attached to context ! (onLocationChanged)")
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
                    mTxtAvgSpeed?.text = String.format(
                        requireContext().getString(R.string.avg_speed_format),
                        (mSpeedMeasures.getAverageSpeed(currentDurationMS) * 3.6)
                    )

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
        mSpeedMeasures.raz()
        isChronometerRunning = false
        startChronometer = 0
        mTxtTimer?.text = requireContext().getString(R.string.zero_timer)
        mTxtOdometer?.text = requireContext().getString(R.string.zero_odometer)
        mTxtSpeed?.text = requireContext().getString(R.string.zero_speed)
        mTxtMaxSpeed?.text = requireContext().getString(R.string.zero_max_speed)
        mTxtAvgSpeed?.text = requireContext().getString(R.string.zero_avg_speed)
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
            Log.d(TAG, "Open document tree $uri")
            DocumentFile.fromTreeUri(requireContext(), uri)?.let { mRbLoader.setRoadbookDir(it) }
            mRbLoader.goCase(0)
            refreshRoadbookCases()
        }

    fun onKeyUp(keyCode: Int): Boolean {
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
                increaseTotalTimer.cancel()
                true
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                decreaseTotalTimer.cancel();
                true
            }

            else -> false
        }
    }

    fun onKeyDown(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                increaseTotalTimer.start()
                true
            }

            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                decreaseTotalTimer.start()
                true
            }

            else -> false
        }
    }

    @Override
    fun onKeyLongPress(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                raz()
                true
            }

            else -> false
        }
    }

    // https://stuff.mit.edu/afs/sipb/project/android/docs/training/managing-audio/volume-playback.html
    // tester pour éviter d'appeler les fonctoins media du systeme
//    fun dispatchKeyEvent(event: KeyEvent?): Boolean {
//        return super.dispatchKeyEvent(event)
//    }
}