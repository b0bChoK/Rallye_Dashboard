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

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.b0bchok.rallye_dashboard_kt.databinding.PdfConverterFragmentBinding
import com.b0bchok.rallye_dashboard_kt.rd_loader.PdfConverter
import com.b0bchok.rallye_dashboard_kt.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfConverterFragment(var pdf: Uri? = null) : Fragment() {

    companion object {
        private const val TAG = "PdfConverterFragment"
    }

    private val TAG_PDF_PATH = "pdf-to-convert-value"

    private var _binding: PdfConverterFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var converter: PdfConverter

    private var presetMenuVisible: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (savedInstanceState != null) {
            pdf = savedInstanceState.getParcelable(TAG_PDF_PATH)

            Log.d(TAG, "Load saved file %s".format(pdf.toString()))
        }

        _binding = PdfConverterFragmentBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pageConfigChangedObserver = Observer<Boolean> { status ->
            enableSaveButton(status)
        }
        binding.imgPdfPreview.pageConfigChanged.observe(
            viewLifecycleOwner,
            pageConfigChangedObserver
        )

        binding.btConvertPdf.isEnabled = true

        binding.txtPdfStatus.text = getString(R.string.pdf_to_convert_s).format(pdf?.let {
            FileUtils(requireActivity()).getFileName(
                it
            )
        })

        binding.btLoadCfrrPreset.setOnClickListener {
            binding.imgPdfPreview.loadCFRRConfig()
            showPresetMenu()
        }

        binding.btLoadTrippyPreset.setOnClickListener {
            binding.imgPdfPreview.loadTrippyConfig()
            showPresetMenu()
        }

        binding.btLoadCustom.setOnClickListener {
            binding.imgPdfPreview.loadCustomConfig()
            showPresetMenu()
        }

        binding.btSavePresset.setOnClickListener {
            binding.imgPdfPreview.saveCustomConfig()
        }

        enableSaveButton(false)

        binding.btAddRow.setOnClickListener {
            if (binding.imgPdfPreview.numberLine < 20)
                binding.imgPdfPreview.changeNumberLine(binding.imgPdfPreview.numberLine + 1)
        }

        binding.btRemoveRow.setOnClickListener {
            if (binding.imgPdfPreview.numberLine > 6)
                binding.imgPdfPreview.changeNumberLine(binding.imgPdfPreview.numberLine - 1)
        }

        binding.btSelectPresset.setOnClickListener {
            showPresetMenu()
        }

        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        binding.btConvertPdf.setOnClickListener {
            // Run loading bar
            binding.loadBar.visibility = View.VISIBLE
            binding.btConvertPdf.isEnabled = false

            coroutineScope.launch(Dispatchers.IO) {
                val destF: File? =
                    converter.convert(binding.imgPdfPreview.updateCurrentPageConfig())
                // Handle the result 'destF' on the main thread
                withContext(Dispatchers.Main) {
                    binding.loadBar.visibility = View.GONE
                    // Update UI or perform other main-thread operations with 'destF'
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle(getString(R.string.pdf_converted))
                        .setMessage(
                            getString(R.string.the_pdf_was_converted_to_case_by_case_image_here_s).format(
                                destF.toString()
                            )
                        )
                        .setPositiveButton(
                            getString(R.string.ok),
                        ) { _, _ ->
                            // return to menu
                            requireActivity().onBackPressed()
                        }
                    val dialog = builder.create()
                    dialog.show()
                }
            }

        }

        converter = PdfConverter(pdf, requireContext(), requireActivity())
        converter.loadPDFPreview()
        binding.imgPdfPreview.setImageBitmap(converter.pageList[0])
    }

    private fun showPresetMenu() {
        if (!presetMenuVisible) {
            binding.btLoadCustom.visibility = View.VISIBLE
            binding.btLoadCfrrPreset.visibility = View.VISIBLE
            binding.btLoadTrippyPreset.visibility = View.VISIBLE
            binding.btSelectPresset.isSelected = true
        } else {
            binding.btLoadCustom.visibility = View.GONE
            binding.btLoadCfrrPreset.visibility = View.GONE
            binding.btLoadTrippyPreset.visibility = View.GONE
            binding.btSelectPresset.isSelected = false
        }
        presetMenuVisible = !presetMenuVisible
    }

    private fun enableSaveButton(enable: Boolean) {
        if (!enable) {
            binding.btSavePresset.isEnabled = false
            binding.btSavePresset.alpha = 0.5F
        } else {
            binding.btSavePresset.isEnabled = true
            binding.btSavePresset.alpha = 1F
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putParcelable(TAG_PDF_PATH, pdf)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}