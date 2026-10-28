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
import androidx.compose.runtime.DisposableEffect
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
 * Khung chung mọi chặng, SỬA LỚN 2026-10-29 theo góp ý sau khi anh chạy thử:
 * - Điểm RIÊNG từng chặng, vào chặng là điểm về 0, không cộng dồn chặng khác.
 * - Chọn đáp án xong là DỪNG ĐỌC LUÔN (tts.stop trước khi phát âm đúng/sai).
 * - Câu trước / Câu tiếp: lịch sử câu hỏi giữ trong NoRepeatPicker, quay lại
 *   câu trước thì chỉ xem lại, KHÔNG chấm điểm lại. Chặng 5 rơi liên tục
 *   không có câu trước/câu tiếp, vì nó không rời màn rơi.
 * - Thi xong 1 chặng: hiện cả 2 lựa chọn "Chặng tiếp theo" và "Về màn hình chính".
 * - Qua hết 7 chặng: pháo hoa chúc mừng rồi mới về màn hình chính.
 * - Chặng 5: rơi liên tục nên answer() không hiện thanh phản hồi chặn màn rơi;
 *   điểm vẫn tính, đủ 10 vẫn hiện hộp hoàn thành.
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
    onNextStage: () -> Unit,
) {
    var score by remember { mutableIntStateOf(0) }
    var wrongCount by remember { mutableIntStateOf(0) }
    var qIndex by remember { mutableIntStateOf(0) }
    // Các câu đã trả lời trong lần vào chặng này (theo chỉ số lịch sử).
    var answeredIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var lastCorrect by remember { mutableStateOf<Boolean?>(null) }
    var completed by remember { mutableStateOf(false) }
    var newCycleMessage by remember { mutableStateOf<String?>(null) }

    // Rời chặng là dừng đọc và xóa lịch sử trong bộ nhớ của lần vào này.
    DisposableEffect(stage) {
        onDispose {
            tts.stop()
            picker.clearHistory(stage)
        }
    }

    val isContinuous = stage == 5
    val historySize = picker.historySize(stage)
    val isReview = qIndex < historySize && qIndex in answeredIndices
    // Câu tiếp chỉ bấm được khi: đang xem lại (đi tiếp trong lịch sử hoặc
    // sang câu mới), hoặc câu hiện tại vừa trả lời xong. Chưa trả lời mà
    // bấm tiếp thì sẽ bỏ qua câu đó nên không cho.
    val canNext = isReview || qIndex in answeredIndices
    val canPrev = qIndex > 0

    fun answer(correct: Boolean, correctText: String) {
        // Câu xem lại: không chấm điểm lại, không đổi điểm, không trừ điểm.
        if (isReview || qIndex in answeredIndices) return
        if (completed) return
        // Chọn xong là dừng đọc luôn, không để giọng cũ đọc chồng lên câu mới.
        tts.stop()
        if (correct) sounds.playCorrect() else sounds.playWrong()
        val (newScore, newWrong) = GameRules.applyAnswer(score, wrongCount, correct)
        score = newScore
        wrongCount = newWrong
        answeredIndices = answeredIndices + qIndex
        lastCorrect = correct
        if (!isContinuous) {
            feedback = if (correct) {
                "Đúng rồi! 🎉"
            } else {
                "Chưa đúng. Đáp án đúng là: $correctText"
            }
        }
        if (newScore >= GameRules.POINTS_TO_PASS) {
            store.markPassed(stage)
            completed = true
        }
    }

    fun next() {
        tts.stop()
        qIndex++
        feedback = null
        lastCorrect = null
        newCycleMessage = null
    }

    fun prev() {
        if (qIndex <= 0) return
        tts.stop()
        qIndex--
        feedback = null
        lastCorrect = null
        newCycleMessage = null
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        ScoreHeader(stageName = stageName, score = score, onBack = {
            tts.stop()
            onBack()
        })
        Spacer(modifier = Modifier.height(10.dp))
        if (newCycleMessage != null) {
            Text(
                newCycleMessage!!,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = KidOrange,
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        if (isReview) {
            Text(
                "Câu này đã trả lời rồi, chỉ xem lại thôi, không tính điểm nữa.",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = KidOrange,
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        key(qIndex) {
            when (stage) {
                1 -> Stage1Content(tts, picker, qIndex, isReview, onAnswer = { c, t -> answer(c, t) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                2 -> Stage2Content(tts, picker, qIndex, isReview, onAnswer = { c, t -> answer(c, t) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                3 -> Stage3Content(tts, picker, qIndex, isReview, onAnswer = { c, t -> answer(c, t) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                4 -> Stage4Content(tts, picker, qIndex, isReview, onAnswer = { c, t -> answer(c, t) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                5 -> Stage5Content(tts, picker, onAnswer = { c, t -> answer(c, t) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                6 -> Stage6Content(tts, picker, qIndex, isReview, onAnswer = { c, t -> answer(c, t) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                else -> Stage7Content(tts, picker, qIndex, isReview, onAnswer = { c, t -> answer(c, t) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
            }
        }
        // Chặng 5 rơi liên tục: không có câu trước/câu tiếp, không có thanh
        // phản hồi chặn màn rơi; đủ điểm thì hộp hoàn thành tự hiện.
        if (!isContinuous) {
            Spacer(modifier = Modifier.height(14.dp))
            PrevNextRow(canPrev = canPrev, canNext = canNext, onPrev = { prev() }, onNext = { next() })
            FeedbackBar(feedback = feedback, isCorrect = lastCorrect, onNext = { next() })
        }
    }

    if (completed) {
        val allDone = store.allPassed()
        if (allDone) {
            // Qua hết 7 chặng: pháo hoa chúc mừng rồi về màn hình chính.
            AlertDialog(
                onDismissRequest = {},
                title = { Text("🎆🎇 Chúc mừng bé! 🎇🎆", fontWeight = FontWeight.Black) },
                text = {
                    Text(
                        "Bé đã hoàn thành hết 7 chặng! Pháo hoa chúc mừng bé, " +
                            "giờ bé quay về màn hình chính chơi lại nhé!",
                        fontSize = 22.sp,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        tts.stop()
                        onBack()
                    }) { Text("🎆 Về màn hình chính", fontWeight = FontWeight.Bold) }
                },
            )
        } else {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Hoàn thành chặng $stage! 🎉", fontWeight = FontWeight.Black) },
                text = { Text("Bé đã đủ 10 điểm. Chặng tiếp theo đã mở (nếu có).") },
                confirmButton = {
                    TextButton(onClick = {
                        tts.stop()
                        if (stage < 7) onNextStage() else onBack()
                    }) {
                        Text(
                            if (stage < 7) "Chặng tiếp theo →" else "Về màn hình chính",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        tts.stop()
                        onBack()
                    }) { Text("Về màn hình chính") }
                },
            )
        }
    }
}

/** Lấy 3 từ gây nhiễu khác từ đúng, từ danh sách từ cho trước. */
fun distractWords(correct: String, pool: List<String>, count: Int = 3): List<String> =
    pool.filter { it != correct }.shuffled().take(count)

/** Trộn đáp án đúng + gây nhiễu, đảm bảo đáp án đúng luôn có trong danh sách. */
fun shuffledOptions(correct: String, distractors: List<String>): List<String> =
    (distractors + correct).shuffled()
