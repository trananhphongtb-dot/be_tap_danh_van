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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private enum class FallShape { SQUARE, CIRCLE, TRIANGLE }

private data class FallingWord(
    val id: Int,
    val word: String,
    val xFrac: Float,
    val speed: Float,
    val y: Float,
    val color: Color,
    val shape: FallShape,
    val isTarget: Boolean,
)

private val FallingColors = listOf(
    Color(0xFF1E88E5), Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFFFFB300),
    Color(0xFF43A047),
)

private val TriangleShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

/**
 * Phần 5, SỬA 2026-10-29 theo phản hồi của anh:
 * - Không còn rơi 1 chữ một lần. Lúc đầu thả thưa dần, nhưng khi chữ
 *   đầu tiên đang rơi thì trên màn phải có đủ 10 chữ đang rơi, và luôn
 *   giữ đủ 10 chữ: chữ nào rơi qua màn thì thả chữ mới thay ngay.
 * - Mỗi lượt có đúng 1 chữ cần bắt trong 10 chữ đang rơi, 9 chữ còn lại
 *   là gây nhiễu. Chữ mới thả có thể là từ đã rơi ở lượt trước (chỉ từ
 *   cần bắt là lấy không lặp theo luật chung, từ gây nhiễu lấy ngẫu nhiên
 *   từ cả ngân hàng, không loại trừ lượt trước).
 * - Chạm đúng chữ cần bắt = +1 điểm, chạm nhầm = 1 sai, để chữ cần bắt
 *   rơi qua màn = 1 sai. Xong 1 lượt thì đọc từ mới, thả lại từ đầu.
 * - Cộng điểm đã sửa ở StageScreen.answer: phần 5 không dùng qIndex,
 *   nên mỗi lượt đều được chấm, không còn kẹt ở câu đầu.
 */
@Composable
fun Stage5Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    var cycle by remember { mutableIntStateOf(0) }
    val pick = remember(cycle) {
        picker.pick(5, WordBank.words) { it.id }
    }
    LaunchedEffect(pick?.newCycle) { if (pick?.newCycle == true) onNewCycle() }
    val targetWord = pick?.item
    if (targetWord == null) {
        Text("Chưa có từ nào.", fontSize = 24.sp)
        return
    }
    val target = targetWord.tu
    LaunchedEffect(cycle, target) { tts.speak(target) }
    var answered by remember(cycle) { mutableStateOf(false) }
    var falling by remember(cycle) { mutableStateOf<List<FallingWord>>(emptyList()) }
    var nextId by remember(cycle) { mutableIntStateOf(0) }

    fun makeWord(isTarget: Boolean, used: Set<String>): FallingWord {
        val w = if (isTarget) target else {
            // Gây nhiễu: ngẫu nhiên từ ngân hàng, tránh trùng chữ đang rơi
            // trong cùng lượt, nhưng được phép là từ của lượt trước.
            WordBank.words.map { it.tu }.filter { it != target && it !in used }.random()
        }
        val item = FallingWord(
            id = nextId++,
            word = w,
            xFrac = 0.14f + kotlin.random.Random.nextFloat() * 0.72f,
            speed = 0.004f + kotlin.random.Random.nextFloat() * 0.003f,
            y = -0.12f,
            color = FallingColors.random(),
            shape = FallShape.values().random(),
            isTarget = isTarget,
        )
        return item
    }

    fun endRound(correct: Boolean) {
        if (answered) return
        answered = true
        onAnswer(correct, target)
        cycle++
    }

    LaunchedEffect(cycle) {
        var reRead = false
        var targetSpawned = false
        var lastSpawnMs = 0L
        var startMs = System.currentTimeMillis()
        while (!answered) {
            val now = System.currentTimeMillis()
            // Thả thưa lúc đầu: chữ cần bắt thả đầu tiên, sau đó mỗi
            // 300ms thả thêm 1 chữ, cho tới khi đủ 10 chữ đang rơi.
            if (falling.size < 10 && now - lastSpawnMs >= 300L) {
                lastSpawnMs = now
                val used = falling.map { it.word }.toSet()
                val item = if (!targetSpawned) {
                    targetSpawned = true
                    makeWord(true, used)
                } else {
                    makeWord(false, used)
                }
                falling = falling + item
            }
            // Rơi xuống 1 nhịp.
            var newList = falling.map { it.copy(y = it.y + it.speed) }
            val targetItem = newList.firstOrNull { it.isTarget }
            if (targetItem != null && targetItem.y >= 0.5f && !reRead) {
                reRead = true
                tts.speak(target)
            }
            // Chữ cần bắt rơi qua màn: 1 sai, hết lượt.
            if (targetItem != null && targetItem.y > 1.05f) {
                endRound(false)
                break
            }
            // Chữ gây nhiễu rơi qua: biến mất, thả chữ mới thay ngay để
            // luôn giữ đủ 10 chữ đang rơi (khi đã đủ 10).
            val exited = newList.filter { !it.isTarget && it.y > 1.05f }
            if (exited.isNotEmpty()) {
                newList = newList.filter { it.y <= 1.05f }
                val used = newList.map { it.word }.toSet()
                for (e in exited) {
                    if (newList.size < 10) {
                        newList = newList + makeWord(false, used + newList.map { it.word }.toSet())
                    }
                }
            }
            falling = newList
            delay(50)
            // Tránh vòng lặp vô hạn nếu có lỗi: sau 60s mà chưa xong lượt thì thôi.
            if (System.currentTimeMillis() - startMs > 60000L) break
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SpeakButton(textToSpeak = target, tts = tts)
        Spacer(modifier = Modifier.height(10.dp))
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            for (item in falling) {
                val xPx = (item.xFrac * widthPx - 55f).roundToInt()
                val yPx = (item.y * heightPx).roundToInt()
                val shapeModifier = when (item.shape) {
                    FallShape.CIRCLE -> Modifier.clip(CircleShape)
                    FallShape.TRIANGLE -> Modifier.clip(TriangleShape)
                    else -> Modifier.clip(RoundedCornerShape(20.dp))
                }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(xPx, yPx) }
                        .size(110.dp, 68.dp)
                        .then(shapeModifier)
                        .background(item.color)
                        .clickable(enabled = !answered) {
                            endRound(item.isTarget)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.word, fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
    }
}
