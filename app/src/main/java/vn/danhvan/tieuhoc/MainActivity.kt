package vn.danhvan.tieuhoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import vn.danhvan.tieuhoc.audio.AnswerSounds
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.NoRepeatPicker
import vn.danhvan.tieuhoc.data.ProgressStore
import vn.danhvan.tieuhoc.data.WordBank
import vn.danhvan.tieuhoc.ui.DanhVanTheme
import vn.danhvan.tieuhoc.ui.HomeScreen
import vn.danhvan.tieuhoc.ui.StageScreen

/**
 * Mở app là vào thi luôn: KHÔNG màn hình đăng nhập, KHÔNG tài khoản,
 * KHÔNG Firebase, KHÔNG online, KHÔNG xếp hạng, KHÔNG quảng cáo.
 * Giọng đọc tiếng Việt (vi-VN) dùng chung cho mọi chặng.
 */
class MainActivity : ComponentActivity() {
    private lateinit var tts: VietnameseTts
    private lateinit var sounds: AnswerSounds
    private lateinit var store: ProgressStore
    private lateinit var picker: NoRepeatPicker

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WordBank.load(this)
        store = ProgressStore(this)
        picker = NoRepeatPicker(store)
        tts = VietnameseTts(this)
        sounds = AnswerSounds(this)

        setContent {
            DanhVanTheme {
                var currentStage by remember { mutableStateOf<Int?>(null) }
                val stage = currentStage
                if (stage == null) {
                    HomeScreen(
                        stages = WordBank.stages,
                        store = store,
                        onStage = { currentStage = it },
                    )
                } else {
                    BackHandler { currentStage = null }
                    val stageName = WordBank.stages
                        .firstOrNull { it.number == stage }?.ten
                        ?: "Chặng $stage"
                    StageScreen(
                        stage = stage,
                        stageName = "Chặng $stage: $stageName",
                        tts = tts,
                        sounds = sounds,
                        picker = picker,
                        store = store,
                        onBack = { currentStage = null },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations && ::tts.isInitialized) tts.shutdown()
        if (!isChangingConfigurations && ::sounds.isInitialized) sounds.release()
    }
}
