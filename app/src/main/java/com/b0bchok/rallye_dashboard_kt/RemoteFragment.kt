package com.b0bchok.rallye_dashboard_kt

import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.b0bchok.rallye_dashboard_kt.controller.ControllerConfigData
import com.b0bchok.rallye_dashboard_kt.databinding.AdvancedMenuFragmentBinding
import com.b0bchok.rallye_dashboard_kt.databinding.RemoteFragmentBinding
import com.b0bchok.rallye_dashboard_kt.rd_loader.ConverterConfigData
import com.b0bchok.rallye_dashboard_kt.utils.PreferenceHelper
import com.b0bchok.rallye_dashboard_kt.utils.PreferenceHelper.controllerConfig
import com.b0bchok.rallye_dashboard_kt.utils.ReadJSONFromAssets
import com.google.gson.Gson

class RemoteFragment : Fragment() {
    companion object {
        private const val TAG = "RemoteFragment"
    }

    private val TAG_CONTROLLER_CONFIGURATION = "controller-configuration"

    private var listenEvent = false

    private var listenNextCase = false
    private var listenPrevCase = false
    private var listenIncreaseOdo = false
    private var listenDecreaseOdo = false
    private var listenRAZ = false

    var nextCase: Int = KeyEvent.KEYCODE_UNKNOWN
    var prevCase: Int = KeyEvent.KEYCODE_UNKNOWN
    var increaseOdo: Int = KeyEvent.KEYCODE_UNKNOWN
    var decreaseOdo: Int = KeyEvent.KEYCODE_UNKNOWN
    var raz: Int = KeyEvent.KEYCODE_UNKNOWN

    private var _binding: RemoteFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = RemoteFragmentBinding.inflate(inflater, container, false)

        if (savedInstanceState != null) {
            val jsonString = savedInstanceState.getString(TAG_CONTROLLER_CONFIGURATION)
            val remoteConfig = Gson().fromJson(jsonString, ControllerConfigData::class.java)
            loadConfig(remoteConfig)
        } else {
            val prefs = PreferenceHelper.defaultPreference(requireContext())
            if ((prefs.controllerConfig != null) and (prefs.controllerConfig != "")) {
                val remoteConfig =
                    Gson().fromJson(prefs.controllerConfig, ControllerConfigData::class.java)
                loadConfig(remoteConfig)
            } else {
                loadDefault()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btCaseNext.setOnClickListener {
            //listen next up action
            listenEvent = true
            listenNextCase = true
            updateStatus(getString(R.string.press_key_for_next_case))
        }

        binding.btCasePrev.setOnClickListener {
            //listen next up action
            listenEvent = true
            listenPrevCase = true
            updateStatus(getString(R.string.press_key_for_previous_case))
        }

        binding.btIncreaseOdo.setOnClickListener {
            //listen next up action
            listenEvent = true
            listenIncreaseOdo = true
            updateStatus(getString(R.string.press_key_for_increase_odometer))
        }

        binding.btDecreaseOdometer.setOnClickListener {
            //listen next up action
            listenEvent = true
            listenDecreaseOdo = true
            updateStatus(getString(R.string.press_key_for_decrease_odometer))
        }
        binding.btRaz.setOnClickListener {
            //listen next up action
            listenEvent = true
            listenRAZ = true
            updateStatus(getString(R.string.press_key_for_raz))
        }

        binding.btReset.setOnClickListener {
            loadDefault()
        }

        binding.btSave.setOnClickListener {
            val prefs = PreferenceHelper.defaultPreference(requireContext())
            prefs.controllerConfig = configToJson()

            it.isEnabled = false
        }

    }

    fun dispatchKeyEvent(event: KeyEvent?): Boolean {

        val keyCode = event?.keyCode
        val action = event?.action

        if (listenEvent and (keyCode == KeyEvent.KEYCODE_BACK)) {
            updateStatus(getString(R.string.select_action_to_configure))
            listenEvent = false
            return true
        }

        if (listenEvent and (keyCode != null)){

            if (action == KeyEvent.ACTION_UP) {

                checkCollision(keyCode)

                if(listenNextCase){
                    nextCase = keyCode!!
                    listenNextCase = false
                    binding.btCaseNext.text = KeyEvent.keyCodeToString(keyCode)
                }

                if(listenPrevCase){
                    prevCase = keyCode!!
                    listenPrevCase = false
                    binding.btCasePrev.text = KeyEvent.keyCodeToString(keyCode)
                }

                if(listenIncreaseOdo){
                    increaseOdo = keyCode!!
                    listenIncreaseOdo = false
                    binding.btIncreaseOdo.text = KeyEvent.keyCodeToString(keyCode)
                }

                if(listenDecreaseOdo){
                    decreaseOdo = keyCode!!
                    listenDecreaseOdo = false
                    binding.btDecreaseOdometer.text = KeyEvent.keyCodeToString(keyCode)
                }

                if(listenRAZ){
                    raz = keyCode!!
                    listenRAZ = false
                    binding.btRaz.text = KeyEvent.keyCodeToString(keyCode)
                }

                binding.txtStatus.text = getString(R.string.s_selected).format(KeyEvent.keyCodeToString(keyCode!!))
                updateStatusTimer.start()

                // Active save bouton
                binding.btSave.isEnabled = true

                listenEvent = false
            }
        } else return false

        return true
    }

    private fun updateStatus(txt: String) {
        binding.txtStatus.text = txt
        updateStatusTimer.cancel()
    }

    private val updateStatusTimer: CountDownTimer = object : CountDownTimer(3000, 500) {
        override fun onTick(p0: Long) {}

        override fun onFinish() {
            binding.txtStatus.text = getString(R.string.select_action_to_configure)
        }
    }

    private fun loadDefault() {
        val jsonString = ReadJSONFromAssets(requireContext(), "defaultControllerConfig.json")
        val remoteConfig = Gson().fromJson(jsonString, ControllerConfigData::class.java)

        loadConfig(remoteConfig)

        // Active save bouton
        binding.btSave.isEnabled = true
    }

    private fun loadConfig(config: ControllerConfigData) {
        nextCase = KeyEvent.keyCodeFromString(config.nextCase)
        binding.btCaseNext.text = config.nextCase

        prevCase = KeyEvent.keyCodeFromString(config.prevCase)
        binding.btCasePrev.text = config.prevCase

        increaseOdo = KeyEvent.keyCodeFromString(config.increaseOdo)
        binding.btIncreaseOdo.text = config.increaseOdo

        decreaseOdo = KeyEvent.keyCodeFromString(config.decreaseOdo)
        binding.btDecreaseOdometer.text = config.decreaseOdo

        raz = KeyEvent.keyCodeFromString(config.raz)
        binding.btRaz.text = config.raz
    }

    private fun configToJson() : String {
        var config: ControllerConfigData = ControllerConfigData(
            KeyEvent.keyCodeToString(nextCase),
            KeyEvent.keyCodeToString(prevCase),
            KeyEvent.keyCodeToString(increaseOdo),
            KeyEvent.keyCodeToString(decreaseOdo),
            KeyEvent.keyCodeToString(raz)
        )

        return Gson().toJson(config)
    }

    private fun checkCollision(keyCode: Int?) {
        if (nextCase == keyCode) {
            nextCase = KeyEvent.KEYCODE_UNKNOWN
            binding.btCaseNext.text = getString(R.string.set)
        }
        if (prevCase == keyCode) {
            prevCase = KeyEvent.KEYCODE_UNKNOWN
            binding.btCasePrev.text = getString(R.string.set)
        }
        if (increaseOdo == keyCode) {
            increaseOdo = KeyEvent.KEYCODE_UNKNOWN
            binding.btIncreaseOdo.text = getString(R.string.set)
        }
        if (decreaseOdo == keyCode) {
            decreaseOdo = KeyEvent.KEYCODE_UNKNOWN
            binding.btDecreaseOdometer.text = getString(R.string.set)
        }
        if (raz == keyCode) {
            raz = KeyEvent.KEYCODE_UNKNOWN
            binding.btRaz.text = getString(R.string.set)
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putString(TAG_CONTROLLER_CONFIGURATION, configToJson())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}