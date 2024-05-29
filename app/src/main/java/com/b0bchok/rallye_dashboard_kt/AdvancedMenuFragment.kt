package com.b0bchok.rallye_dashboard_kt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import com.b0bchok.rallye_dashboard_kt.PreferenceHelper.roadbookUri
import com.b0bchok.rallye_dashboard_kt.databinding.AdvancedMenuFragmentBinding


class AdvancedMenuFragment : Fragment() {

    companion object {
        private const val TAG = "OpenRoadbookFragment"
    }

    private lateinit var mRbLoader: RoadbookLoader

    private var _binding: AdvancedMenuFragmentBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = AdvancedMenuFragmentBinding.inflate(inflater, container, false)

        mRbLoader = ViewModelProvider(requireActivity())[RoadbookLoader::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btSelectRb.setOnClickListener {
            openFolderPrompt.launch(null)
        }

        binding.btConvertPdf.setOnClickListener {
            openPdfPrompt.launch("application/pdf")
        }

        binding.btLoadCfrrPreset.setOnClickListener {
            binding.imgPdfPreview.loadCFRRConfig()
        }

        binding.btLoadTrippyPreset.setOnClickListener {
            binding.imgPdfPreview.loadTrippyConfig()
        }

        Log.d(TAG, "Rb loaded ? %b".format(mRbLoader.isRoadbookLoaded))
        if(mRbLoader.isRoadbookLoaded)
        {
            binding.txtRoadbookStatus.text = String.format(getString(R.string.roadbook_loaded),mRbLoader.getRoadbookName())
        } else {
            binding.txtRoadbookStatus.text = getString(R.string.no_roadbook_loaded)
        }

    }

    private val openFolderPrompt =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }
            requireActivity().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            Log.d(TAG, "Open document tree $uri")

            DocumentFile.fromTreeUri(requireContext(), uri)?.let {
                //Save roadbook uri
                val prefs = PreferenceHelper.defaultPreference(requireContext())
                prefs.roadbookUri = uri.toString()
                mRbLoader.setRoadbookDir(it)
            }

            // return to dashboard
            requireActivity().onBackPressed()
        }

    private val openPdfPrompt = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            return@registerForActivityResult
        }
        requireActivity().contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        Log.d(TAG, "Select pdf $uri")

        // https://developer.android.com/reference/kotlin/android/graphics/pdf/PdfRenderer
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}