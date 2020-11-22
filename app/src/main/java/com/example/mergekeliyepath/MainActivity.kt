package com.example.mergekeliyepath

import android.annotation.TargetApi
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class MainActivity : AppCompatActivity() {
    var file1: String? = null
    var file2: String? = null
    lateinit var button1: Button
    lateinit var button2: Button
    lateinit var button3:Button
    var myFileIntent: Intent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button1 = findViewById(R.id.pdf1)
        button2 = findViewById(R.id.pdf2)
        button3=findViewById(R.id.mergebutton)
        button1.setOnClickListener(View.OnClickListener {
            myFileIntent = Intent(Intent.ACTION_GET_CONTENT)
            myFileIntent!!.type = "*/*"
            startActivityForResult(myFileIntent, 10)
        })
        button2.setOnClickListener(View.OnClickListener {
            myFileIntent = Intent(Intent.ACTION_GET_CONTENT)
            myFileIntent!!.type = "*/*"
            startActivityForResult(myFileIntent, 20)
        })

        button3.setOnClickListener(){
            mergeDocumentsUsingAndroid(File(file1),2F,File(file2))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            10 -> if (resultCode == RESULT_OK) {
                file1 = data!!.data!!.path
            }
            20 -> if (resultCode == RESULT_OK) {
                file2 = data!!.data!!.path
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun mergeDocumentsUsingAndroid(outputFile: File,
                                   imageScale: Float,
                                   vararg inputFiles: File) {
        // We need to keep track of the current page when creating the output PDF.
        var currentOutputPage = 0

        // Create our output document to render into.
        val outputDocument = PdfDocument()

        val bitmapPaint = Paint()
        // By applying the image scale, we can render higher resolution bitmaps into
        // our output PDF. This will result in a bigger file size but higher quality.
        val scaleMatrix = Matrix().apply {
            postScale(imageScale, imageScale)
        }

        for (inputFile in inputFiles) {
            // For every one of our input files, we create a `PdfRenderer`.
            val currentDocument = PdfRenderer(ParcelFileDescriptor.open(inputFile,
                    ParcelFileDescriptor.MODE_READ_ONLY))
            for (pageIndex in 0 until currentDocument.pageCount) {
                val currentPage = currentDocument.openPage(pageIndex)
                // We create the page information based on the actual page we are currently
                // working on.
                val outputPageInfo = PdfDocument.PageInfo.Builder(
                        currentPage.width,
                        currentPage.height,
                        currentOutputPage
                ).create()
                val outputPage = outputDocument.startPage(outputPageInfo)

                // We need to create a bitmap for every page, since the size could be
                // different for every page.
                val pageBitmap = Bitmap.createBitmap(
                        (outputPageInfo.pageWidth * imageScale).toInt(),
                        (outputPageInfo.pageHeight * imageScale).toInt(),
                        Bitmap.Config.ARGB_8888)
                // Now we draw the actual page content onto the bitmap.
                currentPage.render(pageBitmap,
                        null,
                        scaleMatrix,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                // We draw the bitmap in the top-left corner since it will fill the entire page.
                outputPage.canvas.drawBitmap(pageBitmap,
                        Rect(0, 0, pageBitmap.width, pageBitmap.height),
                        outputPageInfo.contentRect,
                        bitmapPaint)

                // Once you're done with a page, make sure to close everything properly.
                outputDocument.finishPage(outputPage)
                currentPage.close()

                ++currentOutputPage
            }
        }

        // Once all pages are copied, write to the output file.
        outputDocument.writeTo(FileOutputStream(outputFile))
    }
}