/*
Copyright (c) 2024 DEWAS Albert

This license replace the general license of the software
and apply to all file in this directory "rb_loader".

Permission is hereby granted, free of charge, to any person obtaining
a copy of this package and associated documentation files (the "package"),
to deal in the package without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom
the package is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall
be included in all copies or substantial portions of the package.

THE package IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE package.
 */

package com.b0bchok.rallye_dashboard_kt.rd_loader

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import com.b0bchok.rallye_dashboard_kt.utils.FileUtils
import java.io.File
import java.nio.file.Files

class PdfConverter(private val pdf: Uri?, private val context: Context, val activity: Activity) {

    companion object {
        private const val TAG = "PDFConverter"
    }

    var pageList = ArrayList<Bitmap>()

    fun loadPDFPreview() {
        // https://developer.android.com/reference/kotlin/android/graphics/pdf/PdfRenderer
        // create a new renderer
        val fileDescriptor = pdf?.let { context.contentResolver.openFileDescriptor(it, "r") }
        val renderer = PdfRenderer(fileDescriptor!!)


        // let us just render all pages
        val pageCount = renderer.pageCount

        for (i in 0 until pageCount) {
            val page: PdfRenderer.Page = renderer.openPage(i)

            // say we render for showing on the screen
            // increase resolution to better display
            val rendererPageWidth = page.width * 3
            val rendererPageHeight = page.height * 3
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

    // https://www.linkedin.com/pulse/how-use-bitmapcreatebitmap-crop-image-homan-huang/
    fun convert(param: ConverterConfigData): File? {

        var pageHeight = 0
        var pageWidth = 0

        val inputFileName = pdf?.let { FileUtils(activity).getFileName(it)?.split('.')?.get(0) }

        var caseList = ArrayList<Bitmap>()

        // Extract case
        for (p in pageList) {

            pageHeight = p.height
            pageWidth = p.width

            val topMargin = (pageHeight * param.topMargin).toInt()
            val bottomMargin = (pageHeight * param.bottomMargin).toInt()

            val colAStart = (pageWidth * param.leftMargin).toInt()
            val colAWidth = (pageWidth * param.columnAWidth).toInt() - colAStart

            val rightMargin = (pageWidth * param.rightMargin).toInt()
            val colBStart = (pageWidth - (pageWidth * param.columnBWidth)).toInt()
            val colBWidth = ((pageWidth * param.columnBWidth) - rightMargin).toInt()

            val caseHeight = (pageHeight - (topMargin + bottomMargin)) / param.lineNumber

            // Crop left column
            for (i in (param.lineNumber - 1) downTo 0) {
                var caseImg = Bitmap.createBitmap(
                    p,
                    colAStart,
                    topMargin + (i * caseHeight),
                    colAWidth,
                    caseHeight
                )
                caseList.add(caseImg)
            }

            // Crop right column
            for (i in (param.lineNumber - 1) downTo 0) {
                var caseImg = Bitmap.createBitmap(
                    p,
                    colBStart,
                    topMargin + (i * caseHeight),
                    colBWidth,
                    caseHeight
                )
                caseList.add(caseImg)
            }

        }

        // Save case
        val destF = createAppDirectoryInDownloads(context, inputFileName)

        if (destF != null) {
            var caseNumber = 1
            for (i in caseList) {
                val caseName = "%03d_%s.jpg".format(caseNumber, inputFileName)
                File(destF, caseName).writeBitmap(i)
                caseNumber++
            }
        }

        return destF
    }

    private fun createAppDirectoryInDownloads(context: Context, inputFileName: String?): File? {
        val downloadsDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appDirectory = File(downloadsDirectory, "%s_rb".format(inputFileName))

        if (!appDirectory.exists()) {
            Files.createDirectory(appDirectory.toPath())
//            val directoryCreated = appDirectory.mkdir()
//            if (!directoryCreated) {
//                Log.e(TAG, "Failed to create the directory %s".format(appDirectory.toString()))
//                return null
//            }
        }

        return appDirectory
    }

    private fun File.writeBitmap(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 80
    ) {
        outputStream().use { out ->
            bitmap.compress(format, quality, out)
            out.flush()
        }
    }
}