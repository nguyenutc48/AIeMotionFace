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
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageView
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
import android.net.Uri

import android.provider.Settings
import android.speech.tts.UtteranceProgressListener
import org.json.JSONException
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var minionView: MinionView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private var isRecording = false
    private lateinit var webhookUrl: String
    private lateinit var micButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()
        setContentView(R.layout.activity_main)

        minionView = findViewById(R.id.minionView)
        micButton = findViewById(R.id.mic_speak_iv)

        // Yêu cầu quyền truy cập micro nếu chưa có
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        // Đặt webhook URL
        webhookUrl = "https://n8n.thiktek.com/webhook/voicechat"

        // Khởi tạo SpeechRecognizer
        initSpeechRecognizer()

        // Khởi tạo TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech.language = Locale("vi", "VN")
            }
        }
//        textToSpeech = TextToSpeech(this) { status ->
//            if (status != TextToSpeech.ERROR) {
//                textToSpeech.language = Locale("vi", "VN")
//                // Thiết lập UtteranceProgressListener sau khi khởi tạo thành công
//                textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
//                    override fun onStart(utteranceId: String?) {
//                        // Có thể thực hiện các hành động khi bắt đầu phát âm
//                    }
//
//                    override fun onDone(utteranceId: String?) {
//                        // Khi việc phát âm hoàn tất, bắt đầu nhận diện giọng nói
//                        startListening()
//                    }
//
//                    override fun onError(utteranceId: String?) {
//                        // Xử lý lỗi nếu có
//                    }
//                })
//            }
//        }

        // Thiết lập sự kiện cho nút micro
        micButton.setOnClickListener {
            if (isRecording) {
                // Nếu đang ghi âm, dừng lại và gửi kết quả
                stopListening()
                // Đổi màu mic về trạng thái không ghi âm
                micButton.setColorFilter(ContextCompat.getColor(this, R.color.mic_disabled_color)) // Giả sử màu mặc định là xám
            } else {
                // Bắt đầu ghi âm
                startListening()
                minionView.smile()
                // Đổi màu mic thành màu đang ghi âm
                micButton.setColorFilter(ContextCompat.getColor(this, R.color.mic_enabled_color)) // Giả sử màu khi đang ghi âm là xanh
            }
        }
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {
                minionView.smile()
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                minionView.lookStraight()
                // Sau khi dừng nhận diện, cập nhật giao diện
                runOnUiThread {
                    isRecording = false
                    micButton.setColorFilter(ContextCompat.getColor(applicationContext, R.color.mic_disabled_color))
                }
            }

            override fun onError(error: Int) {
                minionView.lookDown()
                // Khi gặp lỗi, cập nhật trạng thái
                runOnUiThread {
                    isRecording = false
                    micButton.setColorFilter(ContextCompat.getColor(applicationContext, R.color.mic_disabled_color))
                    Toast.makeText(applicationContext, "Lỗi nhận diện: $error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val text = matches[0]
                    // Xử lý giọng nói và gửi tới webhook
                    minionView.lookStraight()
                    // Thay thế AsyncTask bằng gọi hàm sendTextToWebhook
                    sendTextToWebhook(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Có thể hiển thị kết quả tạm thời nếu cần
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

        try {
            speechRecognizer.startListening(intent)
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
            // Xử lý lỗi khi không thể bắt đầu ghi âm
            Toast.makeText(this, "Không thể bắt đầu ghi âm: ${e.message}", Toast.LENGTH_SHORT).show()
            micButton.setColorFilter(ContextCompat.getColor(this, R.color.mic_disabled_color))
        }
    }

    private fun stopListening() {
        if (isRecording) {
            try {
                speechRecognizer.stopListening()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isRecording = false
            micButton.setColorFilter(ContextCompat.getColor(this, R.color.mic_disabled_color))
        }
    }

    private fun speakText(text: String) {
        // Thêm utteranceId để theo dõi tiến trình phát âm
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ttsUtterance")
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                runOnUiThread {
                    minionView.startTalking()
                }
            }

            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    minionView.stopTalking()
                    micButton.isEnabled = true
                }
            }

            override fun onError(utteranceId: String?) {
                runOnUiThread {
                    minionView.stopTalking()
                    micButton.isEnabled = true
                    Toast.makeText(applicationContext, "Lỗi phát âm", Toast.LENGTH_SHORT).show()
                }
            }
        })
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "ttsUtterance")
    }

    // Thay thế AsyncTask bằng Coroutines
    private fun sendTextToWebhook(userInput: String) {
        // Hiển thị trạng thái đang xử lý
        micButton.isEnabled = false
        Toast.makeText(applicationContext, "Đang xử lý...", Toast.LENGTH_SHORT).show()

        // Sử dụng coroutine để thực hiện network request
        CoroutineScope(Dispatchers.IO).launch {
            val result = try {
                val url = URL(webhookUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.doOutput = true
                connection.doInput = true
                connection.connectTimeout = 10000 // 10 giây timeout
                connection.readTimeout = 15000 // 15 giây read timeout

                // Tạo nội dung JSON để gửi đến webhook
                val jsonParam = JSONObject().apply {
                    put("text", userInput)
                    // Nếu n8n yêu cầu thêm tham số, thêm vào đây
                    // Ví dụ: put("source", "android_app")
                }

                // Gửi JSON lên webhook
                connection.outputStream.use { os ->
                    val input = jsonParam.toString().toByteArray(charset("UTF-8"))
                    os.write(input, 0, input.size)
                }

                // Xử lý phản hồi từ webhook
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Đọc response
                    BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { reader ->
                        val response = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        response.toString()
                    }
                } else {
                    connection.disconnect()
                    "Error response from webhook: $responseCode"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Error sending text to webhook: ${e.message}"
            }

            // Chuyển sang main thread để cập nhật UI
            withContext(Dispatchers.Main) {
                try {
                    if (result.startsWith("Error")) {
                        speakText("Không thể gửi văn bản tới webhook: $result")
                    } else {
                        // Phân tích kết quả JSON và trích xuất phản hồi
                        try {
                            val jsonObject = JSONObject(result)
                            // Điều chỉnh theo cấu trúc thực tế của n8n webhook response
                            val responseText = when {
                                jsonObject.has("response") -> jsonObject.getString("response")
                                jsonObject.has("text") -> jsonObject.getString("text")
                                jsonObject.has("message") -> jsonObject.getString("message")
                                jsonObject.has("data") -> {
                                    // n8n thường trả về dữ liệu trong trường "data"
                                    val data = jsonObject.get("data")
                                    if (data is JSONObject && data.has("output")) {
                                        data.getString("output")
                                    } else {
                                        data.toString()
                                    }
                                }
                                // Nếu jsonObject có trường "output" trực tiếp
                                jsonObject.has("output") -> jsonObject.getString("output")
                                else -> result
                            }
                            var textClear = responseText
                                .replace("\\*\\*", "")
                                .replace("\\*", "")
                                .replace("\\n\\n", ". ")
                                .replace("\\n", ". ")
                                .replace("-", "")
                                .replace("_", "")
                                .replace("#", "")
                                .replace("\\[", "")
                                .replace("\\]", "")
                                .replace("\\(", "")
                                .replace("\\)", "")
                                .replace("\\|", "")
                                .replace("`", "")
                                .trim().replace("\\s+", " ")
                            // Hiển thị và chuyển đổi giá trị thành giọng nói
                            speakText(textClear)
                        } catch (e: JSONException) {
                            // Nếu không phải JSON hợp lệ, đọc nguyên văn kết quả
                            speakText(result)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    speakText("Đã xảy ra lỗi khi xử lý phản hồi từ webhook")
                } finally {
                    // Đảm bảo micButton luôn được kích hoạt lại sau khi hoàn thành
                    micButton.isEnabled = true
                    minionView.stopTalking()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Giải phóng tài nguyên
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
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