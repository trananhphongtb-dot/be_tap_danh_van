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

private val Lanes = listOf(0.12f, 0.31f, 0.50f, 0.69f, 0.88f)
private const val MaxFalling = 6

private val EllipseShape = GenericShape { size, _ ->
    addOval(Rect(0f, 0f, size.width, size.height))
}

/**
 * Phần 5, SỬA V20 2026-10-29:
 * - 6 chữ, tốc độ chậm 0.003, 5 làn, elip 84x52dp, chỉ khác màu.
 * - Mỗi lần rơi 2 chữ ở 2 làn cách nhau ít nhất 1 ô (chênh nhau ít
 *   nhất 2 làn). Khi 2 chữ đầu rơi được 1/4 đường (y >= 0.25) thì mới
 *   rơi 2 chữ tiếp, chứ không thả theo giờ cố định.
 * - Cặp mới KHÁC vị trí cặp trước: 2 làn của cặp mới không trùng 2 làn
 *   của cặp vừa rơi trước, và chọn ngẫu nhiên trong các cặp đủ điều
 *   kiện, nên không còn rơi dồn 1 bên như khi chọn theo thứ tự cố định.
 * - Đáp án nằm ở 1/2 màn hình phía trên (y < 0.5), chọn ngẫu nhiên
 *   trong các chữ đang rơi, không nhất thiết là chữ mới.
 * - Chữ đã chạm (đúng/sai) cấm rơi lại trong lần vào phần này.
 * - Phản hồi nói "Đúng rồi" / "Sai rồi" ở StageScreen, nên sau khi
 *   trả lời thì nghỉ 900ms mới đọc từ cần bắt tiếp theo, để câu nói
 *   không bị cắt ngang.
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
    var lastPairLanes by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var lastPairIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var lastAnswerMs by remember { mutableStateOf(0L) }

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

    // Chọn cặp làn: cách nhau ít nhất 1 ô, khác cặp trước, ngẫu nhiên.
    fun pickPair(list: List<FallingWord>): Pair<Int, Int>? {
        val eligible = Lanes.indices.filter { li ->
            val top = list.filter { it.lane == li }.minByOrNull { it.y }
            top == null || top.y > 0.12f
        }
        val allPairs = eligible.flatMap { a ->
            eligible.filter { b -> b - a >= 2 }.map { b -> a to b }
        }
        val diffFromLast = allPairs.filter { (a, b) ->
            val last = lastPairLanes
            last == null || (a != last.first && a != last.second && b != last.first && b != last.second)
        }
        return (diffFromLast.ifEmpty { allPairs }).randomOrNull()
    }

    fun spawnPairInto(list: List<FallingWord>): List<FallingWord> {
        val pair = pickPair(list) ?: return list
        var out = list
        for (li in listOf(pair.first, pair.second)) {
            out = out + spawnWord(li, out.map { it.word }.toSet())
        }
        lastPairLanes = pair
        lastPairIds = out.takeLast(2).map { it.id }.toSet()
        return out
    }

    fun endQuestion(correct: Boolean, answeredWord: String) {
        val cur = target ?: return
        target = null
        reReadDone = false
        tappedWords = tappedWords + answeredWord
        lastAnswerMs = System.currentTimeMillis()
        onAnswer(correct, cur)
        falling = falling.filter { it.word != answeredWord }
    }

    LaunchedEffect(isCompleted) {
        if (isCompleted) return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis()
            // Thả cặp mới khi: chưa có chữ nào, hoặc cặp vừa rơi trước
            // đã rơi được 1/4 đường, hoặc đã có chữ chạm đáy (để bù đủ 6).
            val lastPairY = falling.filter { it.id in lastPairIds }.minOfOrNull { it.y } ?: 1.0f
            if (falling.size < MaxFalling && (falling.isEmpty() || lastPairY >= 0.25f || firstBottomReached)) {
                if (falling.size <= MaxFalling - 2) {
                    falling = spawnPairInto(falling)
                } else {
                    val eligible = Lanes.indices.firstOrNull { li ->
                        val top = falling.filter { it.lane == li }.minByOrNull { it.y }
                        top == null || top.y > 0.12f
                    }
                    if (eligible != null) falling = falling + spawnWord(eligible, falling.map { it.word }.toSet())
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
                    lastAnswerMs = now
                    if (cur != null) onAnswer(false, cur)
                }
                newList = newList.filter { it.y <= 1.05f }
                while (newList.size < MaxFalling) {
                    val before = newList.size
                    if (newList.size <= MaxFalling - 2) {
                        newList = spawnPairInto(newList)
                    } else {
                        val eligible = Lanes.indices.firstOrNull { li ->
                            val top = newList.filter { it.lane == li }.minByOrNull { it.y }
                            top == null || top.y > 0.12f
                        } ?: break
                        newList = newList + spawnWord(eligible, newList.map { it.word }.toSet())
                    }
                    if (newList.size == before) break
                }
            }
            falling = newList
            // Nghỉ 900ms sau khi trả lời để nghe hết "Đúng rồi"/"Sai rồi".
            if (target == null && now - lastAnswerMs >= 900L) {
                if (usedTargets.size >= WordBank.words.size) usedTargets = emptySet()
                val eligible = newList.filter { it.y < 0.5f && it.word !in usedTargets && it.id !in firstSpawnIds }
                if (eligible.isNotEmpty()) {
                    val chosen = eligible.random()
                    target = chosen.word
                    usedTargets = usedTargets + chosen.word
                    reReadDone = false
                    tts.speak(chosen.word)
                }
            } else if (target != null) {
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
