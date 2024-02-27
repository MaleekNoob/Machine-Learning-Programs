package com.google.tflite.speechrecognition.tflite

import android.content.res.AssetManager
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class SpeechInterpreter//Load TFlite

//Read Labels
(assets: AssetManager, fileName: String) {

    private var recognizeCommands: RecognizeCommands
    private var tflite: Interpreter
    private var labels: List<String>

    init {
        val byteBuffer = loadModelFile(assets, fileName)
        tflite = Interpreter(byteBuffer)
        val actualLabelFilename = LABEL_FILENAME.split("file:///android_asset/".toRegex()).toTypedArray()[1]
        labels = assets.open(actualLabelFilename).bufferedReader().useLines { it.toList() }
        Log.i(LOG_TAG, "Reading labels from: $actualLabelFilename")
        recognizeCommands = RecognizeCommands(
                labels,
                AVERAGE_WINDOW_DURATION_MS,
                DETECTION_THRESHOLD,
                SUPPRESSION_MS,
                MINIMUM_COUNT,
                MINIMUM_TIME_BETWEEN_SAMPLES_MS)
    }

    @Throws(IOException::class)
    private fun loadModelFile(assets: AssetManager, modelFilename: String): ByteBuffer {
        val fileDescriptor = assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun getTfLite(): Interpreter {
        return tflite
    }

    fun getRecognizer(): RecognizeCommands {
        return recognizeCommands
    }

    companion object {
        private const val LABEL_FILENAME = "file:///android_asset/conv_actions_labels.txt"
        private const val LOG_TAG: String = "SpeechInterpreter"
        private const val MINIMUM_TIME_BETWEEN_SAMPLES_MS: Long = 30
        private const val MINIMUM_COUNT = 3
        private const val SUPPRESSION_MS = 1500
        private const val DETECTION_THRESHOLD = 0.50f
        private const val AVERAGE_WINDOW_DURATION_MS: Long = 1000
    }
}