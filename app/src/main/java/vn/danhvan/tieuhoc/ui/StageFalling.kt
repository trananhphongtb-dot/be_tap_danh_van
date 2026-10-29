package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.NoRepeatPicker
import vn.danhvan.tieuhoc.data.WordBank
import kotlin.math.roundToInt

private data class FallingWord(
    val id: Int,
    val word: String,
    val xFrac: Float,
    val speed: Float,
    val y: Float,
    val color: Color,
)

private val FallingColors = listOf(
    Color(0xFF1E88E5), Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFFFFB300),
    Color(0xFF43A047),
)

// Hình elip gọn cho mọi chữ, chỉ khác màu, không còn vuông/tròn/tam giác.
private val EllipseShape = GenericShape { size, _ ->
    addOval(Rect(0f, 0f, size.width, size.height))
}

/**
 * Phần 5, CHỐT MỚI 2026-10-29 (thay kiểu hết lượt xóa màn của V16):
 * - Dòng rơi LIÊN TỤC, không reset theo lượt. Chữ nào rơi qua đáy thì
 *   biến mất, chữ mới thả lên thay ngay.
 * - Lúc đầu thả thưa dần (mỗi 300ms 1 chữ), nhưng từ khi chữ đầu tiên
 *   rơi xuống đáy trở đi, trên màn LUÔN có đủ 10 chữ đang rơi.
 * - Câu hỏi sau lấy luôn chữ đang rơi: từ cần bắt được chọn trong các
 *   chữ đang rơi trên màn (còn ở nửa trên, chưa dùng làm đáp án trong
 *   lần vào phần này), KHÔNG nhất thiết phải là chữ mới thả.
 * - Chạm đúng chữ cần bắt = +1, chạm nhầm = 1 sai (và chữ nhầm đó biến
 *   mất, câu mới chọn chữ khác đang rơi), để chữ cần bắt rơi qua đáy
 *   = 1 sai, câu mới cũng chọn chữ khác đang rơi. Dòng rơi không dừng.
 * - Cộng điểm chuẩn ở StageScreen.answer: phần 5 không dùng qIndex,
 *   mỗi câu được chấm 1 lần, không kẹt ở câu đầu như lỗi cũ.
 */
@Composable
fun Stage5Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
    isCompleted: Boolean = false,
) {
    var falling by remember { mutableStateOf<List<FallingWord>>(emptyList()) }
    var target by remember { mutableStateOf<String?>(null) }
    var usedTargets by remember { mutableStateOf<Set<String>>(emptySet()) }
    var nextId by remember { mutableStateOf(0) }
    var firstBottomReached by remember { mutableStateOf(false) }
    var reReadDone by remember { mutableStateOf(false) }

    fun spawnWord(used: Set<String>): FallingWord {
        val w = WordBank.words.map { it.tu }.filter { it !in used }.random()
        val item = FallingWord(
            id = nextId++,
            word = w,
            xFrac = 0.14f + kotlin.random.Random.nextFloat() * 0.72f,
            speed = 0.004f + kotlin.random.Random.nextFloat() * 0.003f,
            y = -0.12f,
            color = FallingColors.random(),
        )
        return item
    }

    fun endQuestion(correct: Boolean, answeredWord: String) {
        val cur = target ?: return
        target = null
        reReadDone = false
        onAnswer(correct, cur)
        // Chữ vừa trả lời (đúng hoặc nhầm) biến mất khỏi dòng rơi.
        falling = falling.filter { it.word != answeredWord }
    }

    // Vòng lặp duy nhất cho cả dòng rơi và câu hỏi, dừng khi hoàn thành.
    LaunchedEffect(isCompleted) {
        if (isCompleted) return@LaunchedEffect
        var lastSpawnMs = 0L
        while (true) {
            val now = System.currentTimeMillis()
            // Thả chữ: thưa lúc đầu, đủ 10 thì thôi; sau khi chữ đầu
            // chạm đáy thì thả ngay khi thiếu, luôn giữ đủ 10.
            if (falling.size < 10 && (firstBottomReached || now - lastSpawnMs >= 300L)) {
                lastSpawnMs = now
                falling = falling + spawnWord(falling.map { it.word }.toSet())
            }
            // Rơi xuống 1 nhịp.
            var newList = falling.map { it.copy(y = it.y + it.speed) }
            // Chữ rơi qua đáy: biến mất. Nếu đó là chữ cần bắt thì 1 sai.
            val exited = newList.filter { it.y > 1.05f }
            if (exited.isNotEmpty()) {
                if (exited.any { it.word == target }) {
                    firstBottomReached = true
                    val cur = target
                    target = null
                    reReadDone = false
                    if (cur != null) onAnswer(false, cur)
                } else {
                    firstBottomReached = true
                }
                newList = newList.filter { it.y <= 1.05f }
                // Thả chữ mới thay ngay để luôn giữ đủ 10 (sau chữ đầu chạm đáy).
                if (firstBottomReached) {
                    while (newList.size < 10) {
                        newList = newList + spawnWord(newList.map { it.word }.toSet())
                    }
                }
            }
            falling = newList
            // Chọn câu hỏi mới trong các chữ đang rơi, còn ở nửa trên
            // màn và chưa dùng làm đáp án trong lần vào phần này.
            if (target == null) {
                if (usedTargets.size >= WordBank.words.size) usedTargets = emptySet()
                val eligible = newList.filter { it.y < 0.5f && it.word !in usedTargets }
                if (eligible.isNotEmpty()) {
                    val chosen = eligible.random()
                    target = chosen.word
                    usedTargets = usedTargets + chosen.word
                    reReadDone = false
                    tts.speak(chosen.word)
                }
            } else {
                // Đọc lại 1 lần khi chữ cần bắt rơi tới nửa màn.
                val targetItem = newList.firstOrNull { it.word == target }
                if (targetItem != null && targetItem.y >= 0.5f && !reReadDone) {
                    reReadDone = true
                    tts.speak(target!!)
                }
            }
            delay(50)
        }
    }

    val curTarget = target
    Column(modifier = Modifier.fillMaxSize()) {
        SpeakButton(textToSpeak = curTarget ?: "", tts = tts)
        Spacer(modifier = Modifier.height(10.dp))
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            for (item in falling) {
                val xPx = (item.xFrac * widthPx - 52f).roundToInt()
                val yPx = (item.y * heightPx).roundToInt()
                Box(
                    modifier = Modifier
                        .offset { IntOffset(xPx, yPx) }
                        .size(104.dp, 60.dp)
                        .clip(EllipseShape)
                        .background(item.color)
                        .clickable(enabled = curTarget != null && !isCompleted) {
                            val t = target ?: return@clickable
                            if (item.word == t) {
                                endQuestion(true, item.word)
                            } else {
                                endQuestion(false, item.word)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.word, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
    }
}
