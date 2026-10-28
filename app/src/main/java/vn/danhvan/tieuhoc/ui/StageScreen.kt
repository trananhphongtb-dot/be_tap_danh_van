package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.audio.AnswerSounds
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.GameRules
import vn.danhvan.tieuhoc.data.NoRepeatPicker
import vn.danhvan.tieuhoc.data.ProgressStore
import vn.danhvan.tieuhoc.data.WordBank

/**
 * Khung chung mọi chặng: điểm, phản hồi, nút câu tiếp theo, hoàn thành 10 điểm.
 * Mỗi lần sang câu mới, nội dung chặng được key(round) nên trạng thái chọn
 * của câu cũ tự reset, không cần code reset riêng ở từng chặng.
 */
@Composable
fun StageScreen(
    stage: Int,
    stageName: String,
    tts: VietnameseTts,
    sounds: AnswerSounds,
    picker: NoRepeatPicker,
    store: ProgressStore,
    onBack: () -> Unit,
) {
    var score by remember { mutableIntStateOf(0) }
    var wrongCount by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var lastCorrect by remember { mutableStateOf<Boolean?>(null) }
    var completed by remember { mutableStateOf(false) }
    var newCycleMessage by remember { mutableStateOf<String?>(null) }

    fun answer(correct: Boolean, correctText: String) {
        if (feedback != null || completed) return
        // Âm riêng biệt: đúng 1 âm, sai 1 âm khác, phát trước khi hiện phản hồi.
        // Đây là điểm chấm duy nhất nên đủ cho cả 7 chặng, kể cả chặng 5 rơi hụt
        // và chặng 4/7 bấm tích hoàn thành, vì tất cả đều gọi onAnswer -> answer.
        if (correct) sounds.playCorrect() else sounds.playWrong()
        val (newScore, newWrong) = GameRules.applyAnswer(score, wrongCount, correct)
        score = newScore
        wrongCount = newWrong
        lastCorrect = correct
        feedback = if (correct) {
            "Đúng rồi! 🎉"
        } else {
            "Chưa đúng. Đáp án đúng là: $correctText"
        }
        if (newScore >= GameRules.POINTS_TO_PASS) {
            store.markPassed(stage)
            completed = true
        }
    }

    fun next() {
        round++
        feedback = null
        lastCorrect = null
        newCycleMessage = null
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        ScoreHeader(stageName = stageName, score = score, onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        if (newCycleMessage != null) {
            Text(
                newCycleMessage!!,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = KidOrange,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        key(round) {
            when (stage) {
                1 -> Stage1Content(tts, picker, onAnswer = { correct, correctText -> answer(correct, correctText) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                2 -> Stage2Content(tts, picker, onAnswer = { correct, correctText -> answer(correct, correctText) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                3 -> Stage3Content(tts, picker, onAnswer = { correct, correctText -> answer(correct, correctText) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                4 -> Stage4Content(tts, picker, onAnswer = { correct, correctText -> answer(correct, correctText) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                5 -> Stage5Content(tts, picker, onAnswer = { correct, correctText -> answer(correct, correctText) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                6 -> Stage6Content(tts, picker, onAnswer = { correct, correctText -> answer(correct, correctText) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                else -> Stage7Content(tts, picker, onAnswer = { correct, correctText -> answer(correct, correctText) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
            }
        }
        FeedbackBar(feedback = feedback, isCorrect = lastCorrect, onNext = { next() })
    }

    if (completed) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Hoàn thành chặng $stage! 🎉", fontWeight = FontWeight.Black) },
            text = { Text("Bé đã đủ 10 điểm. Chặng tiếp theo đã mở (nếu có).") },
            confirmButton = {
                TextButton(onClick = onBack) { Text("Về màn hình chính", fontWeight = FontWeight.Bold) }
            },
        )
    }
}

/** Lấy 3 từ gây nhiễu khác từ đúng, từ danh sách từ cho trước. */
fun distractWords(correct: String, pool: List<String>, count: Int = 3): List<String> =
    pool.filter { it != correct }.shuffled().take(count)

/** Trộn đáp án đúng + gây nhiễu, đảm bảo đáp án đúng luôn có trong danh sách. */
fun shuffledOptions(correct: String, distractors: List<String>): List<String> =
    (distractors + correct).shuffled()
