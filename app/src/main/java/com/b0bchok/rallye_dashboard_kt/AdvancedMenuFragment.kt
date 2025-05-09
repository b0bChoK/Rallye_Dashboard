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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.b0bchok.rallye_dashboard_kt.databinding.AdvancedMenuFragmentBinding
import com.b0bchok.rallye_dashboard_kt.rd_loader.RoadbookLoader
import com.b0bchok.rallye_dashboard_kt.utils.PreferenceHelper
import com.b0bchok.rallye_dashboard_kt.utils.PreferenceHelper.roadbookUri


class AdvancedMenuFragment : Fragment() {

    companion object {
        private const val TAG = "AdvancedMenuFragment"

        // Request code for selecting a PDF document.
        private const val PICK_PDF_FILE = 2
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
            //openPdfPrompt.launch("application/pdf")
            openPdfFile()
        }

        binding.btSettings.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment(), "settings")
                .addToBackStack(null)
                .commit()
        }

        binding.btRemoteConfig.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RemoteFragment(), "controller")
                .addToBackStack(null)
                .commit()
        }

        Log.d(TAG, "Rb loaded ? %b".format(mRbLoader.isRoadbookLoaded))
        if (mRbLoader.isRoadbookLoaded) {
            binding.txtRoadbookStatus.text =
                String.format(getString(R.string.roadbook_loaded), mRbLoader.getRoadbookName())
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
            try {
                requireActivity().onBackPressed()
            } catch (e : Exception) {
                Log.e(TAG, "Cannot invoke back button !")
                binding.txtRoadbookStatus.text =
                    String.format(getString(R.string.roadbook_loaded), mRbLoader.getRoadbookName())
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle(getString(R.string.roadbook_loaded_dialog))
                    .setMessage(getString(R.string.roadbook_loaded_dialog_text).format(binding.txtRoadbookStatus.text))
                    .setPositiveButton(
                        getString(R.string.ok)
                    ) { _, _ ->
                        // return to menu
                        requireActivity().onBackPressed()
                    }
                val dialog = builder.create()
                dialog.show()
            }
        }

    fun openPdfFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }

        startActivityForResult(intent, PICK_PDF_FILE)
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?
    ) {
        if (requestCode == PICK_PDF_FILE
            && resultCode == Activity.RESULT_OK
        ) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                // Perform operations on the document using its URI.
                Log.d(TAG, "Select pdf $uri")

                requireActivity().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, PdfConverterFragment(uri), "pdfConverter")
                    .addToBackStack(null)
                    .commit()
            }

        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}