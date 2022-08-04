package info.plateaukao.einkbro.util

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter

class PdfDocumentAdapter(
    private val pathName: String,
    private val superAdapter: PrintDocumentAdapter,
    private val onFinish: () -> Unit
) : PrintDocumentAdapter() {

    override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: android.os.CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: Bundle?
    ) {
        superAdapter.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, extras)
    }

    override fun onFinish() {
        superAdapter.onFinish()
        onFinish.invoke()
    }

    override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: android.os.CancellationSignal?,
            callback: WriteResultCallback?
    ) {
        superAdapter.onWrite(pages, destination, cancellationSignal, callback)

//        try {
//            // copy file from the input stream to the output stream
//            FileInputStream(File(pathName)).use { inStream ->
//                FileOutputStream(destination?.fileDescriptor).use { outStream ->
//                    inStream.copyTo(outStream)
//                }
//            }
//
//            if (cancellationSignal?.isCanceled == true) {
//                callback?.onWriteCancelled()
//            } else {
//                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
//            }
//
//        } catch (e: Exception) {
//            callback?.onWriteFailed(e.message)
//        }
    }
}
