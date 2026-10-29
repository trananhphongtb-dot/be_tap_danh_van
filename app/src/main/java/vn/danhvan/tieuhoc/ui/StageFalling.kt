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
    val word: String,
    val xFrac: Float,
    val speed: Float,
    val y: Float,
    val color: Color,
    val shape: FallShape,
)

// Màu theo góp ý: xanh dương, đỏ, tím, vàng, thêm xanh lá cho đa dạng.
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
 * Chặng 5, CHỐT 2026-10-29 sau khi anh duyệt đường v9:
 * - Mỗi lúc chỉ rơi 1 TỪ, không rơi nhiều từ cùng lúc, không rơi lẻ tẻ
 *   chồng nhau như trước.
 * - Từ rơi từ 1 trong 3 vị trí ngẫu nhiên trên đỉnh: trái / giữa / phải.
 * - Từ nằm trong hình đa dạng: vuông, tròn, tam giác, màu đa dạng:
 *   xanh dương, đỏ, tím, vàng (thêm xanh lá).
 * - Cách chơi: nghe từ, từ rơi xuống, nếu là từ cần bắt thì chạm vào,
 *   nếu là từ gây nhiễu thì để nó rơi qua, từ mới sẽ rơi tiếp.
 *   Từ cần bắt rơi qua khỏi màn hình mà chưa bắt được thì tính 1 câu sai.
 *   Từ cần bắt rơi nửa đường mà chưa chọn được thì đọc lại 1 lần.
 * - Rơi liên tục, không dừng, chỉ dừng hẳn khi đủ 10 điểm.
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
    var falling by remember(cycle) { mutableStateOf<FallingWord?>(null) }

    fun nextCycle() {
        if (!answered) {
            answered = true
            cycle++
        }
    }

    // Vòng lặp 1 từ: thả 1 từ, rơi tới hết, rồi mới thả từ tiếp theo
    // trong cùng lượt nếu từ đó là gây nhiễu rơi qua.
    LaunchedEffect(cycle) {
        var reRead = false
        while (true) {
            // Chọn từ rơi: 50% là từ cần bắt, 50% là từ gây nhiễu.
            val isTarget = kotlin.random.Random.nextBoolean()
            val wordToDrop = if (isTarget) {
                target
            } else {
                WordBank.words.map { it.tu }.filter { it != target }.random()
            }
            val slot = listOf(0.18f, 0.50f, 0.82f).random()
            var item = FallingWord(
                word = wordToDrop,
                xFrac = slot,
                speed = 0.005f + kotlin.random.Random.nextFloat() * 0.003f,
                y = -0.12f,
                color = FallingColors.random(),
                shape = FallShape.values().random(),
            )
            var dropped = false
            while (!dropped) {
                delay(50)
                item = item.copy(y = item.y + item.speed)
                if (item.word == target && item.y >= 0.5f && !reRead) {
                    reRead = true
                    tts.speak(target)
                }
                if (item.y > 1.05f) {
                    if (item.word == target) {
                        onAnswer(false, target)
                        nextCycle()
                        dropped = true
                        break
                    } else {
                        // Gây nhiễu rơi qua: chỉ biến mất, thả từ mới.
                        dropped = true
                        break
                    }
                }
                falling = item
            }
            if (answered) break
            falling = null
        }
    }

    // CHOT v13: khong khung, tu roi ngay tren nen toan man.
    Column(modifier = Modifier.fillMaxSize()) {
        SpeakButton(textToSpeak = target, tts = tts)
        Spacer(modifier = Modifier.height(10.dp))
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            // 3 vị trí rơi trên đỉnh, vẽ mờ để bé biết từ sẽ rơi ở đâu.
            Text("↓        ↓        ↓", fontSize = 20.sp, modifier = Modifier.align(Alignment.TopCenter))
            val item = falling
            if (item != null) {
                val xPx = (item.xFrac * widthPx - 60f).roundToInt()
                val yPx = (item.y * heightPx).roundToInt()
                val shapeModifier = when (item.shape) {
                    FallShape.CIRCLE -> Modifier.clip(CircleShape)
                    FallShape.TRIANGLE -> Modifier.clip(TriangleShape)
                    else -> Modifier.clip(RoundedCornerShape(20.dp))
                }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(xPx, yPx) }
                        .size(120.dp, 72.dp)
                        .then(shapeModifier)
                        .background(item.color)
                        .clickable(enabled = !answered) {
                            val correct = item.word == target
                            onAnswer(correct, target)
                            nextCycle()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.word, fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
    }
}
