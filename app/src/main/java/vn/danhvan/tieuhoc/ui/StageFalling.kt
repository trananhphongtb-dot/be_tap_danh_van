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
    val lane: Int,
    val speed: Float,
    val y: Float,
    val color: Color,
)

private val FallingColors = listOf(
    Color(0xFF1E88E5), Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFFFFB300),
    Color(0xFF43A047),
)

// 5 làn cố định, cách mép màn để elip không bị cắt.
private val Lanes = listOf(0.12f, 0.31f, 0.50f, 0.69f, 0.88f)

private val EllipseShape = GenericShape { size, _ ->
    addOval(Rect(0f, 0f, size.width, size.height))
}

/**
 * Phần 5, SỬA V18 2026-10-29 theo phản hồi của anh:
 * - 5 LÀN CỐ ĐỊNH, CÙNG TỐC ĐỘ, elip nhỏ 84x52dp. Chữ trong cùng làn
 *   cách nhau ít nhất 0.12 màn (khoảng 90dp) nên KHÔNG đè nhau, chữ ở
 *   mép cũng không bị cắt, lúc nào cũng nguyên hình elip, chỉ khác màu.
 * - Dòng rơi liên tục, không reset theo lượt. Từ khi chữ đầu tiên chạm
 *   đáy trở đi, luôn giữ đủ 10 chữ đang rơi.
 * - Chữ đầu tiên thả luôn là chữ gây nhiễu, KHÔNG phải đáp án, nên đáp
 *   án không còn luôn rơi trước. Đáp án chọn NGẪU NHIÊN trong các chữ
 *   đang rơi (nửa trên, chưa dùng làm đáp án), không nhất thiết là chữ mới.
 * - Chữ nào bé đã chạm (đúng hoặc sai) thì CẤM rơi lại trong lần vào
 *   phần này. Chỉ chữ chưa chạm mà rơi qua đáy mới được rơi lại.
 * - Chạm đúng +1, chạm nhầm 1 sai (chữ nhầm biến mất), để chữ cần bắt
 *   rơi qua đáy 1 sai, câu mới lại chọn chữ khác đang rơi.
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
    var tappedWords by remember { mutableStateOf<Set<String>>(emptySet()) }
    var nextId by remember { mutableStateOf(0) }
    var firstBottomReached by remember { mutableStateOf(false) }
    var reReadDone by remember { mutableStateOf(false) }
    var firstSpawnId by remember { mutableStateOf<Int?>(null) }

    fun spawnWord(lane: Int, used: Set<String>): FallingWord {
        val w = WordBank.words.map { it.tu }
            .filter { it !in used && it !in tappedWords }.random()
        val item = FallingWord(
            id = nextId++,
            word = w,
            lane = lane,
            speed = 0.005f,
            y = -0.12f,
            color = FallingColors.random(),
        )
        if (firstSpawnId == null) firstSpawnId = item.id
        return item
    }

    fun endQuestion(correct: Boolean, answeredWord: String) {
        val cur = target ?: return
        target = null
        reReadDone = false
        tappedWords = tappedWords + answeredWord
        onAnswer(correct, cur)
        falling = falling.filter { it.word != answeredWord }
    }

    LaunchedEffect(isCompleted) {
        if (isCompleted) return@LaunchedEffect
        var lastSpawnMs = 0L
        while (true) {
            val now = System.currentTimeMillis()
            // Thả chữ vào làn nào mà chữ trên cùng của làn đó đã rơi
            // xuống ít nhất 0.12 màn, để không đè nhau trong cùng làn.
            if (falling.size < 10 && (firstBottomReached || now - lastSpawnMs >= 300L)) {
                val used = falling.map { it.word }.toSet()
                val lane = Lanes.indices.firstOrNull { li ->
                    val top = falling.filter { it.lane == li }.minByOrNull { it.y }
                    top == null || top.y > 0.12f
                }
                if (lane != null) {
                    lastSpawnMs = now
                    falling = falling + spawnWord(lane, used)
                } else if (firstBottomReached) {
                    // Làn nào cũng kẹt thì chờ nhịp sau, không thả đè lên.
                    lastSpawnMs = now
                }
            }
            var newList = falling.map { it.copy(y = it.y + it.speed) }
            val exited = newList.filter { it.y > 1.05f }
            if (exited.isNotEmpty()) {
                firstBottomReached = true
                if (exited.any { it.word == target }) {
                    val cur = target
                    target = null
                    reReadDone = false
                    if (cur != null) onAnswer(false, cur)
                }
                newList = newList.filter { it.y <= 1.05f }
                // Thả chữ mới thay ngay để luôn giữ đủ 10.
                while (newList.size < 10) {
                    val used = newList.map { it.word }.toSet()
                    val lane = Lanes.indices.firstOrNull { li ->
                        val top = newList.filter { it.lane == li }.minByOrNull { it.y }
                        top == null || top.y > 0.12f
                    } ?: break
                    newList = newList + spawnWord(lane, used)
                }
            }
            falling = newList
            if (target == null) {
                if (usedTargets.size >= WordBank.words.size) usedTargets = emptySet()
                // Chữ đầu tiên thả không được làm đáp án của câu đầu.
                val eligible = newList.filter { it.y < 0.5f && it.word !in usedTargets && it.id != firstSpawnId }
                if (eligible.isNotEmpty()) {
                    val chosen = eligible.random()
                    target = chosen.word
                    usedTargets = usedTargets + chosen.word
                    reReadDone = false
                    tts.speak(chosen.word)
                }
            } else {
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
                val xPx = (Lanes[item.lane] * widthPx - 42f).roundToInt()
                val yPx = (item.y * heightPx).roundToInt()
                Box(
                    modifier = Modifier
                        .offset { IntOffset(xPx, yPx) }
                        .size(84.dp, 52.dp)
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
                    Text(item.word, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
    }
}
