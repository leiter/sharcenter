package cut.the.crap.data.storage

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class FileHelper(private val context: Context) {

    private fun defaultFileName(): String{
        return "ShareCare_${System.currentTimeMillis()}.txt"
    }
    fun writeToFile(fileName: String? = null, fileContent: String, append: Boolean = true): Boolean {
        val name = fileName ?: defaultFileName()
        val file = File(context.filesDir, name)
        if(!file.exists()){
            file.createNewFile()
        }
        val content =
            if (append && !fileContent.endsWith("\n")) "$fileContent\n"
            else fileContent
        return try {
            FileOutputStream(file, append).use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            Log.d("FileHelper", "File created successfully at: ${file.absolutePath}")
            true
        } catch (e: IOException) {
            Log.e("FileHelper", "Failed to create file: ${e.message}")
            false
        }
    }

    fun readFile(fileName: String): String {
        val file = File(context.filesDir, fileName)
        if(!file.exists()){
            file.createNewFile()
        }
        return file.readText()
    }

    fun copyFileToDownloads(context: Context, sourceFileName: String) {
        val sourceFile = File(context.filesDir, sourceFileName)
        if (!sourceFile.exists()) {
            Toast.makeText(context, "Source file does not exist", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = sourceFile.name

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, use MediaStore API
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        copy(sourceFile.inputStream(), outputStream)
                    }
                    Toast.makeText(context, "File copied to Downloads", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(context, "Failed to copy file", Toast.LENGTH_SHORT).show()
                }

            } else {
                // For Android 9 and below, copy directly to the Downloads directory
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destinationFile = File(downloadsDir, fileName)

                sourceFile.inputStream().use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        copy(inputStream, outputStream)
                    }
                }

                Toast.makeText(context, "File copied to Downloads", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("FileCopy", "Error copying file: ${e.message}")
            Toast.makeText(context, "Error copying file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copy(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
        }
        output.flush()
    }

}

fun saveFileToDownloads(outputStream: OutputStream?, fileContent: String): Result<Boolean> {

    try {
        val r = outputStream?.use {
            it.write(fileContent.toByteArray())
            it.flush()
        }
        return if (r != null) {
            Result.success(true)
        } else Result.failure(NullPointerException("OutputStream is null"))

        // Notify user
        //showToast(context, "File saved successfully!")
    } catch (e: Exception) {
        e.printStackTrace()
        if (e is CancellationException) throw e
        else return Result.failure(e)
        //showToast(context, "Failed to save file: ${e.localizedMessage}")
    }
}

fun provideOutputStream(fileName: String, context: Context): OutputStream? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // For Android 10 and above, use MediaStore
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        uri?.let { context.contentResolver.openOutputStream(it) }
    } else {
        // For older Android versions
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        FileOutputStream(file)
    }
}

fun showToast(context: Context, message: String) {
    // Display a toast message on the UI thread
    ContextCompat.getMainExecutor(context).execute {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}