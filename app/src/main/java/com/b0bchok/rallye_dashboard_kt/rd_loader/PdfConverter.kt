package com.b0bchok.rallye_dashboard_kt.rd_loader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri

class PdfConverter(val pdf: Uri, val context: Context) {

    var pageList = ArrayList<Bitmap>()

    fun loadPDFPreview(){
        // https://developer.android.com/reference/kotlin/android/graphics/pdf/PdfRenderer
        // create a new renderer
        val fileDescriptor = context.contentResolver.openFileDescriptor(pdf, "r")
        val renderer = PdfRenderer(fileDescriptor!!)


        // let us just render all pages
        val pageCount = renderer.pageCount

        for (i in 0 until pageCount) {
            val page: PdfRenderer.Page = renderer.openPage(i)

            // say we render for showing on the screen
            val rendererPageWidth = page.width
            val rendererPageHeight = page.height
            val bitmap = Bitmap.createBitmap(
                rendererPageWidth,
                rendererPageHeight,
                Bitmap.Config.ARGB_8888
            )
            // Fill background
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(bitmap, 0.0F, 0.0F, null)
            // Render pdf page in bitmap
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pageList.add(bitmap)

            // close the page
            page.close()
        }

        // close the renderer
        renderer.close()
    }

    fun convert(param: ConverterConfigData) {

    }
}