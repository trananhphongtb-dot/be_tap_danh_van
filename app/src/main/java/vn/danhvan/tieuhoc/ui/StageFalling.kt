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

private const val MaxFalling = 6

private val EllipseShape = GenericShape { size, _ ->
    addOval(Rect(0f, 0f, size.width, size.height))
}

/**
 * Phần 5, SỬA V19 2026-10-29 theo phản hồi của anh:
 * - Chỉ 6 chữ cùng lúc (giảm mật độ), tốc độ chậm 0.003 (trước 0.005).
 * - Mỗi lần thả 2 từ ở 2 làn cách nhau ít nhất 1 ô (chỉ số làn chênh
 *   nhau ít nhất 2), cùng tốc độ nên cặp đó rơi cùng nhau, rơi qua đáy
 *   cùng nhau, rồi thả cặp mới thay.
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
    var firstSpawnIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    fun spawnWord(lane: Int, used: Set<String>): FallingWord {
        val w = WordBank.words.map { it.tu }
            .filter { it !in used && it !in tappedWords }.random()
        val item = FallingWord(
            id = nextId++,
            word = w,
            lane = lane,
            speed = 0.003f,
            y = -0.12f,
            color = FallingColors.random(),
        )
        if (falling.isEmpty()) firstSpawnIds = firstSpawnIds + item.id
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
            // Thả từng lần 2 từ ở 2 làn cách nhau ít nhất 1 ô (chênh
            // nhau ít nhất 2 làn), mỗi 800ms 1 lần, để không đè nhau.
            // Thưa lúc đầu, đủ 6 thì thôi; sau khi chữ đầu chạm đáy thì
            // thả ngay khi thiếu để luôn giữ 6.
            if (falling.size < MaxFalling && (firstBottomReached || now - lastSpawnMs >= 800L)) {
                val eligibleLanes = Lanes.indices.filter { li ->
                    val top = falling.filter { it.lane == li }.minByOrNull { it.y }
                    top == null || top.y > 0.12f
                }
                val pair = listOf(0 to 2, 1 to 3, 2 to 4, 0 to 3, 1 to 4, 0 to 4)
                    .firstOrNull { (a, b) -> a in eligibleLanes && b in eligibleLanes }
                if (pair != null && falling.size <= MaxFalling - 2) {
                    lastSpawnMs = now
                    var list = falling
                    for (li in listOf(pair.first, pair.second)) {
                        list = list + spawnWord(li, list.map { it.word }.toSet())
                    }
                    falling = list
                } else if (eligibleLanes.isNotEmpty() && falling.size < MaxFalling) {
                    lastSpawnMs = now
                    falling = falling + spawnWord(eligibleLanes.first(), falling.map { it.word }.toSet())
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
                // Thả chữ mới thay ngay để luôn giữ 6, ưu tiên thả cặp
                // ở 2 làn cách nhau ít nhất 1 ô khi thiếu từ 2 chữ trở lên.
                while (newList.size < MaxFalling) {
                    val eligibleLanes = Lanes.indices.filter { li ->
                        val top = newList.filter { it.lane == li }.minByOrNull { it.y }
                        top == null || top.y > 0.12f
                    }
                    val pair = listOf(0 to 2, 1 to 3, 2 to 4, 0 to 3, 1 to 4, 0 to 4)
                        .firstOrNull { (a, b) -> a in eligibleLanes && b in eligibleLanes }
                    if (pair != null && newList.size <= MaxFalling - 2) {
                        for (li in listOf(pair.first, pair.second)) {
                            newList = newList + spawnWord(li, newList.map { it.word }.toSet())
                        }
                    } else if (eligibleLanes.isNotEmpty()) {
                        newList = newList + spawnWord(eligibleLanes.first(), newList.map { it.word }.toSet())
                    } else break
                }
            }
            falling = newList
            if (target == null) {
                if (usedTargets.size >= WordBank.words.size) usedTargets = emptySet()
                // Chữ đầu tiên thả không được làm đáp án của câu đầu.
                val eligible = newList.filter { it.y < 0.5f && it.word !in usedTargets && it.id !in firstSpawnIds }
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
