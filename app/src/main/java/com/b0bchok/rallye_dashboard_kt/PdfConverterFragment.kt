package com.b0bchok.rallye_dashboard_kt

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.b0bchok.rallye_dashboard_kt.databinding.PdfConverterFragmentBinding
import com.b0bchok.rallye_dashboard_kt.rd_loader.PdfConverter
import com.b0bchok.rallye_dashboard_kt.utils.FileUtils
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

        binding.txtPdfStatus.text = "PDF to convert : %s".format(pdf?.let {
            FileUtils(requireActivity()).getFileName(
                it
            )
        })

        binding.btLoadCfrrPreset.setOnClickListener {
            binding.imgPdfPreview.loadCFRRConfig()
        }

        binding.btLoadTrippyPreset.setOnClickListener {
            binding.imgPdfPreview.loadTrippyConfig()
        }

        binding.btConvertPdf.setOnClickListener {
            val destF = converter.convert(binding.imgPdfPreview.pageConfig)

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Pdf converted !")
                .setMessage("The pdf was converted to case by case image here %s".format(destF.toString()))
                .setPositiveButton(
                    "Ok"
                ) { _, _ ->
                    // return to menu
                    requireActivity().onBackPressed()
                }
            val dialog = builder.create()
            dialog.show()
        }

        converter = PdfConverter(pdf, requireContext(), requireActivity())
        converter.loadPDFPreview()
        binding.imgPdfPreview.setImageBitmap(converter.pageList[0])
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