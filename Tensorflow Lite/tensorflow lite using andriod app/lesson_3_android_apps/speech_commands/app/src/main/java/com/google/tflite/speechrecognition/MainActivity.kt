/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* Demonstrates how to run an audio recognition model in Android.

This example loads a simple speech recognition model trained by the tutorial at
https://www.tensorflow.org/tutorials/audio_training

The model files should be downloaded automatically from the TensorFlow website,
but if you have a custom model you can update the LABEL_FILENAME and
MODEL_FILENAME constants to point to your own files.

The example application displays a list view with all of the known audio labels,
and highlights each one when it thinks it has detected one through the
microphone. The averaging of results to give a more reliable signal happens in
the RecognizeCommands helper class.
*/

package com.google.tflite.speechrecognition

import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.TextView
import com.google.tflite.speechrecognition.tflite.RecognizeCommands
import com.google.tflite.speechrecognition.tflite.SpeechInterpreter
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * An activity that listens for audio and then uses a TensorFlow model to detect particular classes,
 * by default a small set of action words.
 */
class MainActivity : Activity() {


    private var selectedTextView: TextView? = null
    private var lastProcessingTimeMs: Long = 0
    private lateinit var speechInterpreter: SpeechInterpreter

    // Working variables.
    private var recordingBuffer = ShortArray(RECORDING_LENGTH)
    private var recordingOffset = 0
    private var shouldContinue = true
    private var recordingThread: Thread? = null
    private var shouldContinueRecognition = true
    private var recognitionThread: Thread? = null
    private val recordingBufferLock = ReentrantLock()

    private val labels = ArrayList<String>()
    private val displayedLabels = ArrayList<String>()
    private var tfLite: Interpreter? = null

    private var yesTextView: TextView? = null
    private var noTextView: TextView? = null
    private var upTextView: TextView? = null
    private var downTextView: TextView? = null
    private var leftTextView: TextView? = null
    private var rightTextView: TextView? = null
    private var onTextView: TextView? = null
    private var offTextView: TextView? = null
    private var stopTextView: TextView? = null
    private var goTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val actualLabelFilename = LABEL_FILENAME.split("file:///android_asset/".toRegex()).toTypedArray()[1]
        Log.i(LOG_TAG, "Reading labels from: $actualLabelFilename")
        val br: BufferedReader?
        try {
            br = BufferedReader(InputStreamReader(assets.open(actualLabelFilename)) as Reader?)
            var line: String?
            do {
                line = br.readLine()
                if (line != null) {
                    labels.add(line)
                    if (line[0] != '_') {
                        displayedLabels.add(line.substring(0, 1).toUpperCase() + line.substring(1))
                    }
                }
            } while (line != null)
            br.close()
        } catch (e: IOException) {
            throw RuntimeException("Problem reading label file!", e)
        }

        val actualModelFilename = MODEL_FILENAME.split("file:///android_asset/".toRegex()).toTypedArray()[1]
        speechInterpreter = SpeechInterpreter(assets, actualModelFilename)
        try {
            tfLite = speechInterpreter.getTfLite()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        tfLite!!.resizeInput(0, intArrayOf(RECORDING_LENGTH, 1))
        tfLite!!.resizeInput(1, intArrayOf(1))

        // Start the recording and recognition threads.
        requestMicrophonePermission()
        startRecording()
        startRecognition()

        yesTextView = findViewById(R.id.yes)
        noTextView = findViewById(R.id.no)
        upTextView = findViewById(R.id.up)
        downTextView = findViewById(R.id.down)
        leftTextView = findViewById(R.id.left)
        rightTextView = findViewById(R.id.right)
        onTextView = findViewById(R.id.on)
        offTextView = findViewById(R.id.off)
        stopTextView = findViewById(R.id.stop)
        goTextView = findViewById(R.id.go)

    }

    private fun requestMicrophonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_RECORD_AUDIO
                && grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecording()
            startRecognition()
        }
    }

    @Synchronized
    fun startRecording() {
        if (recordingThread != null) {
            return
        }
        shouldContinue = true
        recordingThread = Thread(
                Runnable { record() })
        recordingThread!!.start()
    }

    @Synchronized
    fun stopRecording() {
        if (recordingThread == null) {
            return
        }
        shouldContinue = false
        recordingThread = null
    }

    private fun record() {

        Log.e("amlan", "Recording in progress")
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)

        // Estimate the buffer size we'll need for this device.
        var bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2
        }
        val audioBuffer = ShortArray(bufferSize / 2)

        val record = AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize)

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!")
            return
        }

        record.startRecording()

        Log.v(LOG_TAG, "Start recording")

        // Loop, gathering audio data and copying it to a round-robin buffer.
        while (shouldContinue) {
            val numberRead = record.read(audioBuffer, 0, audioBuffer.size)
            val maxLength = recordingBuffer.size
            val newRecordingOffset = recordingOffset + numberRead
            val secondCopyLength = max(0, newRecordingOffset - maxLength)
            val firstCopyLength = numberRead - secondCopyLength
            // We store off all the data for the recognition thread to access. The ML
            // thread will copy out of this buffer into its own, while holding the
            // lock, so this should be thread safe.
            recordingBufferLock.lock()
            try {
                System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, firstCopyLength)
                System.arraycopy(audioBuffer, firstCopyLength, recordingBuffer, 0, secondCopyLength)
                recordingOffset = newRecordingOffset % maxLength
            } finally {
                recordingBufferLock.unlock()
            }
        }

        record.stop()
        record.release()
    }

    @Synchronized
    fun startRecognition() {
        if (recognitionThread != null) {
            return
        }
        shouldContinueRecognition = true
        recognitionThread = Thread(
                Runnable { recognize() })
        recognitionThread!!.start()
    }

    private fun recognize() {

        Log.e("amlan", "Start recognition")

        val inputBuffer = ShortArray(RECORDING_LENGTH)
        val floatInputBuffer = Array(RECORDING_LENGTH) { FloatArray(1) }
        val outputScores = Array(1) { FloatArray(labels.size) }
        val sampleRateList = intArrayOf(SAMPLE_RATE)

        // Loop, grabbing recorded data and running the recognition model on it.
        while (shouldContinueRecognition) {
            val startTime = Date().time
            // The recording thread places data in this round-robin buffer, so lock to
            // make sure there's no writing happening and then copy it to our own
            // local version.
            recordingBufferLock.lock()
            try {
                val maxLength = recordingBuffer.size
                val firstCopyLength = maxLength - recordingOffset
                val secondCopyLength = recordingOffset
                System.arraycopy(recordingBuffer, recordingOffset, inputBuffer, 0, firstCopyLength)
                System.arraycopy(recordingBuffer, 0, inputBuffer, firstCopyLength, secondCopyLength)
            } finally {
                recordingBufferLock.unlock()
            }

            // We need to feed in float values between -1.0f and 1.0f, so divide the
            // signed 16-bit inputs.
            for (i in 0 until RECORDING_LENGTH) {
                floatInputBuffer[i][0] = inputBuffer[i] / 32767.0f
            }

            val inputArray = arrayOf<Any>(floatInputBuffer, sampleRateList)
            val outputMap = HashMap<Int, Any>()
            outputMap[0] = outputScores

            // Run the model.
            tfLite!!.runForMultipleInputsOutputs(inputArray, outputMap)

            // Use the smoother to figure out if we've had a real recognition event.
            val currentTime = System.currentTimeMillis()
            val result: RecognizeCommands.RecognitionResult =
                    speechInterpreter.getRecognizer().processLatestResults(outputScores[0], currentTime)
            lastProcessingTimeMs = Date().time - startTime

            runOnUiThread { updateUI(result) }

            try {
                // We don't need to run too frequently, so snooze for a bit.
                Thread.sleep(MINIMUM_TIME_BETWEEN_SAMPLES_MS)
            } catch (e: InterruptedException) {
                // Ignore
            }

        }

        Log.v(LOG_TAG, "End recognition")
    }

    private fun updateUI(result: RecognizeCommands.RecognitionResult) {
        // If we do have a new command, highlight the right list entry.
        if (!result.foundCommand.startsWith("_") && result.isNewCommand) {
            var labelIndex: Int = -1
            for (i in 0 until labels.size) {
                if (labels[i] == result.foundCommand) {
                    labelIndex = i
                }
            }

            when (labelIndex - 2) {
                0 ->
                    selectedTextView = yesTextView
                1 ->
                    selectedTextView = noTextView
                2 ->
                    selectedTextView = upTextView
                3 ->
                    selectedTextView = downTextView
                4 ->
                    selectedTextView = leftTextView
                5 ->
                    selectedTextView = rightTextView
                6 ->
                    selectedTextView = onTextView
                7 ->
                    selectedTextView = offTextView
                8 ->
                    selectedTextView = stopTextView
                9 ->
                    selectedTextView = goTextView
            }
            highlightRecognizedText(result)
        }
    }

    private fun highlightRecognizedText(result: RecognizeCommands.RecognitionResult) {
        if (selectedTextView != null) {
            selectedTextView!!.setBackgroundColor(R.drawable.round_corner_text_bg_selected)
            val score = "${(result.score * 100).roundToInt()}%"
            selectedTextView!!.text = "${selectedTextView!!.text} \n  $score"
            selectedTextView!!.setTextColor(
                    ContextCompat.getColor(selectedTextView!!.context,android.R.color.holo_orange_light))
            selectedTextView!!.postDelayed({
                val origionalString: String =
                        selectedTextView!!.text.toString().replace(score, "").trim()
                selectedTextView!!.text = origionalString
                selectedTextView!!.setBackgroundResource(
                        R.drawable.round_corner_text_bg_unselected)
                selectedTextView!!.setTextColor(
                        ContextCompat.getColor(selectedTextView!!.context,android.R.color.darker_gray))
            }, 700)
        }
    }

    override fun onStop() {
        super.onStop()
        stopRecording()
    }

    companion object {
        private const val LOG_TAG = "MainActivity"
        private const val LABEL_FILENAME = "file:///android_asset/conv_actions_labels.txt"
        private const val SAMPLE_RATE = 16000
        private const val SAMPLE_DURATION_MS = 1000
        private const val RECORDING_LENGTH = SAMPLE_RATE * SAMPLE_DURATION_MS / 1000
        private const val MINIMUM_TIME_BETWEEN_SAMPLES_MS: Long = 30
        private const val REQUEST_RECORD_AUDIO = 13
        private const val MODEL_FILENAME = "file:///android_asset/conv_actions_frozen.tflite"
    }
}
