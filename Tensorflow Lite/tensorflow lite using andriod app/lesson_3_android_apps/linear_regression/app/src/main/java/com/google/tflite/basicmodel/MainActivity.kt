package com.google.tflite.basicmodel

import android.app.Activity
import android.content.res.AssetFileDescriptor
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {

    private lateinit var tflite: Interpreter
    private var tfliteoptions: Interpreter.Options = Interpreter.Options()
    private lateinit var tflitemodel: MappedByteBuffer
    private lateinit var resultTextView: TextView
    private lateinit var editText: EditText;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultTextView = findViewById(R.id.result);
        editText = findViewById(R.id.editText);
        try {
            tflitemodel = loadModelfile(this)
            tfliteoptions.setNumThreads(1)
            tflite = Interpreter(tflitemodel, tfliteoptions)

        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                doInference();
            }

        })


    }

    private fun doInference() {
        if (editText.text.isEmpty()) {
            resultTextView.setText("Result: 0")
            return
        }
        var intValue = editText.text.toString().toFloat();
        val inputVal: FloatArray = floatArrayOf(intValue)
        var outputVal: ByteBuffer = ByteBuffer.allocateDirect(4)
        outputVal.order(ByteOrder.nativeOrder())
        tflite.run(inputVal, outputVal)
        outputVal.rewind()
        var prediction: Float = outputVal.getFloat()
        resultTextView.setText("Result: " + prediction.toString())
    }

    private fun loadModelfile(activity: Activity): MappedByteBuffer {
        var fileDescriptor: AssetFileDescriptor = activity.assets.openFd("model.tflite")
        var inputStream: FileInputStream = FileInputStream(fileDescriptor.fileDescriptor);
        var fileChannel: FileChannel = inputStream.channel;
        var startOffset: Long = fileDescriptor.startOffset;
        var declaredLength: Long = fileDescriptor.declaredLength;
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
