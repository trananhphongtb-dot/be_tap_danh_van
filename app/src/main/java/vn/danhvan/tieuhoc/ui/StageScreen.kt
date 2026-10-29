package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
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
 * Khung chung mọi chặng, CHỐT v13 2026-10-29:
 * - Nền toàn màn theo chặng, không dải chủ đề nhỏ nữa.
 * - Đáp án ở đáy: nội dung chiếm trọng số còn lại, nút Trước/Tiếp ở đáy.
 * - Không hiện chữ "Đúng rồi!" / "Chưa đúng.", chỉ âm thanh + màu đáp án.
 * - Điểm riêng từng chặng, vào chặng là điểm về 0.
 * - Chọn đáp án xong là DỪNG ĐỌC LUÔN.
 * - Câu trước / Câu tiếp: chỉ xem lại, không chấm điểm lại.
 * - Chặng 5 rơi liên tục, không có câu trước/câu tiếp.
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
    var answeredIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var lastCorrect by remember { mutableStateOf<Boolean?>(null) }
    var completed by remember { mutableStateOf(false) }
    var newCycleMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(stage) {
        onDispose {
            tts.stop()
            picker.clearHistory(stage)
        }
    }

    val isContinuous = stage == 5
    val historySize = picker.historySize(stage)
    val isReview = qIndex < historySize && qIndex in answeredIndices
    val canNext = isReview || qIndex in answeredIndices
    val canPrev = qIndex > 0

    fun answer(correct: Boolean, correctText: String) {
        // Phần 5 rơi liên tục: mỗi lượt là 1 lần chấm riêng, KHÔNG dùng
        // qIndex/answeredIndices như các phần khác. Lỗi cũ ở đây: qIndex
        // của phần 5 không bao giờ tăng, nên sau câu đầu mọi lần chạm sau
        // đều bị chặn, điểm không cộng nữa, mãi không qua bài.
        if (stage != 5 && (isReview || qIndex in answeredIndices)) return
        if (completed) return
        tts.stop()
        // V20: bỏ chuông đúng/sai, nói luôn "Đúng rồi" / "Sai rồi".
        // sounds vẫn được truyền vào nhưng không dùng nữa.
        if (correct) tts.speak("Đúng rồi") else tts.speak("Sai rồi")
        val (newScore, newWrong) = GameRules.applyAnswer(score, wrongCount, correct)
        score = newScore
        wrongCount = newWrong
        if (stage != 5) answeredIndices = answeredIndices + qIndex
        lastCorrect = correct
        if (newScore >= GameRules.POINTS_TO_PASS) {
            store.markPassed(stage)
            completed = true
        }
    }

    fun next() {
        tts.stop()
        qIndex++
        lastCorrect = null
        newCycleMessage = null
    }

    fun prev() {
        if (qIndex <= 0) return
        tts.stop()
        qIndex--
        lastCorrect = null
        newCycleMessage = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StageBackgroundFull(stage)
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            ScoreHeader(stageName = "Phần $stage", score = score, onBack = {
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
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                key(qIndex) {
                    when (stage) {
                        1 -> Stage1Content(tts, picker, qIndex, isReview, onAnswer = { c, t -> answer(c, t) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                        2 -> Stage2Content(tts, picker, qIndex, isReview, onAnswer = { c, t -> answer(c, t) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                        3 -> Stage3Content(tts, picker, qIndex, isReview, onAnswer = { c, t -> answer(c, t) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                        4 -> Stage4Content(tts, picker, qIndex, isReview, onAnswer = { c, t -> answer(c, t) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                        5 -> Stage5Content(tts, picker, onAnswer = { c, t -> answer(c, t) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" }, isCompleted = completed)
                        6 -> Stage6Content(tts, picker, qIndex, isReview, onAnswer = { c, t -> answer(c, t) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                        else -> Stage7Content(tts, picker, qIndex, isReview, onAnswer = { c, t -> answer(c, t) }, onNewCycle = { newCycleMessage = "Đã hết câu mới, bắt đầu vòng mới!" })
                    }
                }
            }
            if (!isContinuous) {
                Spacer(modifier = Modifier.height(14.dp))
                PrevNextRow(canPrev = canPrev, canNext = canNext, onPrev = { prev() }, onNext = { next() })
            }
        }
    }

    if (completed) {
        val allDone = store.allPassed()
        // Hộp thông báo đồ họa, không dùng hộp hệ thống nhỏ nữa.
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0x80000000)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.88f)
                    .shadow(12.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFFFFF8E1)).border(4.dp, Color(0xFFFFB300), RoundedCornerShape(28.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (allDone) {
                    Text("🎆", fontSize = 64.sp)
                    Text("Chúc mừng bé!", fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Bé đã hoàn thành hết 7 phần! Giờ bé quay về màn hình chính chơi lại nhé!",
                        fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    BigButton(
                        text = "🏠 Về nhà",
                        onClick = { tts.stop(); onBack() },
                        color = KidBlue, modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text("🏆", fontSize = 64.sp)
                    Text("Hoàn thành Phần $stage!", fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Bé đã đủ 10 điểm. Phần tiếp theo đã mở!",
                        fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        BigButton(
                            text = "🏠 Về nhà",
                            onClick = { tts.stop(); onBack() },
                            color = Color(0xFF757575), modifier = Modifier.weight(1f),
                        )
                        BigButton(
                            text = if (stage < 7) "Chơi tiếp →" else "🏠 Về nhà",
                            onClick = {
                                tts.stop()
                                if (stage < 7) onNextStage() else onBack()
                            },
                            color = KidGreen, modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** Lấy 3 từ gây nhiễu khác từ đúng, từ danh sách từ cho trước. */
fun distractWords(correct: String, pool: List<String>, count: Int = 3): List<String> =
    pool.filter { it != correct }.shuffled().take(count)

/** Trộn đáp án đúng + gây nhiễu, đảm bảo đáp án đúng luôn có trong danh sách. */
fun shuffledOptions(correct: String, distractors: List<String>): List<String> =
    (distractors + correct).shuffled()
