package vn.danhvan.tieuhoc.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Giọng đọc BẮT BUỘC tiếng Việt (vi-VN) cho mọi từ, câu, đoạn văn trong app.
 * Không dùng giọng tiếng Anh, không để mặc định hệ thống.
 * Nếu máy thiếu dữ liệu giọng vi-VN, code vẫn giữ Locale vi-VN và không
 * chuyển sang tiếng Anh — người dùng cần cài giọng tiếng Việt trên máy.
 */
class VietnameseTts(context: Context) {
    private val viLocale = Locale("vi", "VN")

    private var tts: TextToSpeech? = null
    var ready = false
        private set

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(viLocale)
                // Chỉ ghi nhận sẵn sàng, không đổi sang Locale khác dù kết quả ra sao.
                ready = result != null
                tts?.setSpeechRate(0.9f)
            }
        }
    }

    /** Đọc 1 đoạn văn bản tiếng Việt. Nút "Nghe lại" ở mọi chặng gọi hàm này. */
    fun speak(text: String) {
        val engine = tts ?: return
        // Đảm bảo lại Locale vi-VN trước mỗi lần đọc, tránh máy tự đổi Locale.
        engine.language = viLocale
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "danhvan")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
