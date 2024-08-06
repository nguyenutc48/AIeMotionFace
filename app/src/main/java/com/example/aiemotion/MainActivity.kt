package com.example.aiemotion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.aiemotion.ui.theme.AiEmotionTheme
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.RelativeLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import android.widget.Toast

class MainActivity : ComponentActivity() {
    private lateinit var minionView: MinionView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private var isRecording = false
    private lateinit var apiUrl: String
    private var isListeningForKeyword = true
    private var idleTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()
        setContentView(R.layout.activity_main)

        minionView = findViewById(R.id.minionView)

        // Yêu cầu quyền truy cập micro nếu chưa có
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        // Đọc API URL từ file cấu hình
        apiUrl = "http://localhost:11434/api/generate"

        // Khởi tạo SpeechRecognizer
        initSpeechRecognizer()

        // Khởi tạo TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech.language = Locale("vi", "VN")
            }
        }

        // Bắt đầu lắng nghe từ khóa
        startListeningForKeyword()

//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
//        speechRecognizer.setRecognitionListener(object : RecognitionListener {
//            override fun onReadyForSpeech(params: Bundle?) {}
//            override fun onBeginningOfSpeech() {
//                minionView.smile()
//            }
//            override fun onRmsChanged(rmsdB: Float) {}
//            override fun onBufferReceived(buffer: ByteArray?) {}
//            override fun onEndOfSpeech() {
//                minionView.lookStraight()
//            }
//            override fun onError(error: Int) {
//                minionView.lookDown()
//            }
//
//            override fun onResults(results: Bundle?) {
//                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                if (matches != null) {
//                    val text = matches[0]
//                    minionView.startListening()
//                    SendTextToApiTask().execute(text)
//                }
//                stopListening()
//                minionView.stopListening()
//            }
//
//            override fun onPartialResults(partialResults: Bundle?) {
//                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//
//            }
//
//            override fun onEvent(eventType: Int, params: Bundle?) {}
//        })
//
//        // Khởi tạo TextToSpeech
//        textToSpeech = TextToSpeech(this) { status ->
//            if (status != TextToSpeech.ERROR) {
//                textToSpeech.language = Locale("vi", "VN")
//            }
//        }

        // Ví dụ về chuỗi biểu cảm
//        lifecycleScope.launch {
//            while(true) {
//                minionView.startTalking()
//                delay(3000) // Nói trong 3 giây
//                minionView.stopTalking()
//                minionView.lookLeft()
//                delay(1000)
//                minionView.lookRight()
//                delay(1000)
//            }
//        }
    }
    private fun startListeningForKeyword() {
        isListeningForKeyword = true
        startListening()
        Toast.makeText(this, "Lắng nghe từ khóa 'xin chào", Toast.LENGTH_SHORT).show()
    }
    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {
                if (isListeningForKeyword) {
                    minionView.smile()
                }
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                minionView.lookStraight()
                if (isListeningForKeyword) {
                    // Nếu đang lắng nghe từ khóa và kết thúc ghi âm, khởi động lại lắng nghe từ khóa
                    startListeningForKeyword()
                }
            }

            override fun onError(error: Int) {
                minionView.lookDown()
                // Khi gặp lỗi, quay lại lắng nghe từ khóa
                startListeningForKeyword()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    val text = matches[0]
                    if (isListeningForKeyword && (text.contains("xin chào", true) || text.contains("chào", true))) {
                        // Từ khóa được phát hiện
                        isListeningForKeyword = false
                        minionView.smile()
                        stopListening()

                        // Bắt đầu ghi âm giọng nói để chuyển sang text và gửi tới API
                        startListeningForCommand()
                    } else if (!isListeningForKeyword) {
                        // Xử lý giọng nói và gửi tới API
                        stopListening()
                        minionView.lookStraight()
                        SendTextToApiTask().execute(text)
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    val text = matches[0]
                    if (isListeningForKeyword && (text.contains("xin chào", true) || text.contains("chào", true))) {
                        // Từ khóa được phát hiện
                        isListeningForKeyword = false
                        minionView.smile()
                        stopListening()

                        // Bắt đầu ghi âm giọng nói để chuyển sang text và gửi tới API
                        startListeningForCommand()
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    private fun startListeningForCommand() {
        // Bắt đầu lắng nghe giọng nói trong 1 phút, nếu không có kết quả sẽ quay lại chế độ lắng nghe từ khóa
        startListening()

        idleTimer?.cancel() // Hủy bỏ timer trước đó nếu có
        idleTimer = Timer()
        idleTimer?.schedule(object : TimerTask() {
            override fun run() {
                if (!isListeningForKeyword) {
                    runOnUiThread {
                        minionView.lookStraight()
                        speakText("Không có giọng nói nào được nhận diện, quay lại chế độ lắng nghe từ khóa.")
                        startListeningForKeyword()
                    }
                }
            }
        }, 60000) // 1 phút
    }
    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        speechRecognizer.startListening(intent)
        isRecording = true
    }

    private fun stopListening() {
        if (isRecording) {
            speechRecognizer.stopListening()
            isRecording = false
        }
    }

    private fun loadConfig(fileName: String): String? {
        return try {
            val properties = Properties()
            assets.open(fileName).use { inputStream ->
                properties.load(inputStream)
            }
            properties.getProperty("api_url")
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun speakText(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private inner class SendTextToApiTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String): String {
            val question = params[0]
            return try {
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.doOutput = true
                connection.doInput = true

                // Tạo nội dung JSON theo định dạng yêu cầu
                val jsonParam = JSONObject().apply {
                    put("model", "qwen2")
                    put("prompt", question)
                    put("stream", false)
                }

                // Gửi JSON lên API
                val os: OutputStream = connection.outputStream
                os.write(jsonParam.toString().toByteArray(charset("UTF-8")))
                os.close()

                // Xử lý phản hồi từ API
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val br = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    br.close()
                    response.toString()
                } else {
                    "Error response from API"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Error sending text to API"
            }
        }

//        override fun onPostExecute(result: String) {
//            if (result.startsWith("Error")) {
//                speakText("Không thể gửi văn bản tới API, sử dụng văn bản mặc định.")
//            } else {
//                speakText(result)
//            }
//        }

        override fun onPostExecute(result: String) {
            if (result.startsWith("Error")) {
                speakText("Không thể gửi văn bản tới API, sử dụng văn bản mặc định.")
            } else {
                minionView.startTalking()
                // Trích xuất giá trị "response" từ phản hồi JSON
                val jsonObject = JSONObject(result)
                val responseText = jsonObject.getString("response")

                // Hiển thị và chuyển đổi giá trị thành giọng nói
                speakText(responseText)
                minionView.stopTalking()
            }
        }
    }
}



@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AiEmotionTheme {
        Greeting("Android")
    }
}