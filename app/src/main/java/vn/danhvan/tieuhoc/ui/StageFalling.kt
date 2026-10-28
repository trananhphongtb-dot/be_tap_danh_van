package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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

private data class FallingWord(
    val word: String,
    val xFrac: Float,
    val speed: Float,
    val y: Float,
    val color: Color,
)

private val FallingColors = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFF8F00),
    Color(0xFF8E24AA), Color(0xFF00897B), Color(0xFFD81B60), Color(0xFF3949AB),
)

/**
 * Chặng 5, SỬA LỚN 2026-10-29: BẮT CHỮ RƠI -> TỪ RƠI.
 * - Rơi là TỪ 2 chữ cái, không phải 1 chữ cái nữa.
 * - Từ rơi TỪ ĐỈNH CAO xuống, LẺ TẺ: không rơi hàng loạt 1 lúc nữa, mà
 *   cứ khoảng 1 giây lại thả thêm 1 từ ở vị trí ngang ngẫu nhiên, từ trước
 *   vẫn đang rơi, từ sau mới bắt đầu rơi.
 * - Mỗi từ 1 MÀU KHÁC NHAU cho phong phú, màu không lộ từ cần bắt vì
 *   từ cần bắt cũng chỉ là 1 trong các màu đó.
 * - Rơi LIÊN TỤC, không dừng, không sang màn mới khi chọn xong: bấm đúng
 *   hay sai đều đọc TỪ MỚI ngay, màn rơi vẫn ở đó, rơi tiếp như cũ, chỉ
 *   dừng hẳn khi đủ 10 điểm (hộp hoàn thành hiện lên).
 * - Từ cần bắt rơi được NỬA ĐƯỜNG mà bé chưa chọn được thì ĐỌC LẠI 1 lần.
 * - Từ cần bắt rơi qua khỏi màn hình mà chưa bắt được thì tính là 1 câu sai.
 * - Từ gây nhiễu rơi qua khỏi màn hình thì chỉ biến mất, không tính sai.
 * - Chặng này KHÔNG có câu trước/câu tiếp, vì nó là màn rơi liên tục.
 */
@Composable
fun Stage5Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    // Lượt rơi trong màn: mỗi lượt là 1 từ cần bắt, không dùng lịch sử
    // câu trước/câu tiếp của các chặng khác, vì màn rơi không rời màn.
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
    // Đọc từ mới khi bắt đầu lượt. Lượt sau đọc từ mới, không đọc từ cũ.
    LaunchedEffect(cycle, target) { tts.speak(target) }
    var answered by remember(cycle) { mutableStateOf(false) }
    var reReadDone by remember(cycle) { mutableStateOf(false) }
    // Danh sách từ sẽ thả lẻ tẻ trong lượt này: từ cần bắt + 5 từ gây nhiễu,
    // thứ tự xáo trộn, thả dần chứ không thả cùng lúc.
    val spawnList = remember(cycle, target) {
        val distractors = WordBank.words.map { it.tu }.filter { it != target }.shuffled().take(5)
        (distractors + target).shuffled()
    }
    var falling by remember(cycle) { mutableStateOf<List<FallingWord>>(emptyList()) }

    fun nextCycle() {
        if (!answered) {
            answered = true
            cycle++
        }
    }

    // Vòng lặp rơi + thả lẻ tẻ: mỗi 50ms đẩy từ xuống, mỗi ~1 giây thả 1 từ mới.
    // Dùng biến cục bộ trong coroutine cho danh sách và chỉ số thả, tránh đọc
    // state đã cũ trong vòng lặp; chỉ đẩy kết quả ra state để vẽ.
    LaunchedEffect(cycle) {
        var current = emptyList<FallingWord>()
        var spawnIdx = 0
        var tick = 0
        var reRead = false
        while (true) {
            delay(50)
            tick++
            // Thả lẻ tẻ: cứ 20 nhịp (1 giây) thả thêm 1 từ, ở x ngẫu nhiên.
            if (spawnIdx < spawnList.size && tick % 20 == 1) {
                val wordToSpawn = spawnList[spawnIdx]
                current = current + FallingWord(
                    word = wordToSpawn,
                    xFrac = (0.08f + (0.84f * kotlin.random.Random.nextFloat())),
                    speed = 0.005f + kotlin.random.Random.nextFloat() * 0.003f,
                    y = -0.12f,
                    color = FallingColors.random(),
                )
                spawnIdx++
            }
            current = current.map { it.copy(y = it.y + it.speed) }
            // Từ cần bắt rơi nửa đường mà chưa chọn được thì đọc lại 1 lần.
            val targetItem = current.firstOrNull { it.word == target }
            if (targetItem != null && targetItem.y >= 0.5f && !reRead) {
                reRead = true
                reReadDone = true
                tts.speak(target)
            }
            // Từ cần bắt rơi qua khỏi màn hình là hụt = 1 câu sai, sang lượt mới.
            val missed = current.any { it.word == target && it.y > 1.05f }
            if (missed) {
                onAnswer(false, target)
                nextCycle()
                break
            }
            // Từ gây nhiễu rơi qua khỏi màn hình thì chỉ biến mất.
            current = current.filter { it.y <= 1.05f }
            falling = current
        }
    }

    Column {
        Text("Nghe rồi chạm vào từ đang rơi đúng nhé!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        SpeakButton(textToSpeak = target, tts = tts)
        Spacer(modifier = Modifier.height(10.dp))
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().height(420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(KidCream),
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            for (item in falling) {
                val xPx = (item.xFrac * widthPx - 60f).roundToInt()
                val yPx = (item.y * heightPx).roundToInt()
                Box(
                    modifier = Modifier
                        .offset { IntOffset(xPx, yPx) }
                        .size(120.dp, 72.dp)
                        .clip(RoundedCornerShape(20.dp))
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
        Spacer(modifier = Modifier.height(8.dp))
        Text("Từ rơi liên tục, đủ 10 điểm mới dừng.", fontSize = 20.sp)
    }
}
