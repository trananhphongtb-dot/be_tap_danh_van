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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.NoRepeatPicker
import vn.danhvan.tieuhoc.data.VietnameseTones
import vn.danhvan.tieuhoc.data.WordBank
import kotlin.math.roundToInt

private data class FallingItem(
    val letter: String,
    val xFrac: Float,
    val speed: Float, // phần chiều cao rơi mỗi nhịp 50ms
    val y: Float, // 0.0 = trên cùng, 1.0 = dưới cùng
)

/**
 * Chặng 5: Bắt chữ rơi.
 * SỬA THEO NGƯỜI DÙNG 2026-10-29, KHÔNG dùng bản mô tả cũ cho chặng này:
 * - Chữ rơi THẲNG từ trên xuống (chỉ đổi y, không trôi ngang), PHẲNG, ĐƠN GIẢN:
 *   chỉ là vòng tròn màu trơn với 1 chữ cái, KHÔNG có hình em bé, KHÔNG có
 *   nhân vật nào trong màn chơi. Tốc độ vừa phải (rơi hết màn hình trong
 *   khoảng 5–8 giây tùy chữ).
 * - Giọng đọc tiếng Việt yêu cầu 1 chữ cái (phụ âm đầu hoặc nguyên âm gốc),
 *   có nút "Nghe lại".
 * - Chạm sai 1 chữ tính là 1 câu sai, chấm 1 lần cho cả lượt rồi sang lượt mới.
 * - Chữ YÊU CẦU rơi qua khỏi màn hình mà không bắt được cũng tính là 1 câu sai,
 *   áp dụng luật điểm chung (GameRules.applyAnswer: sai 2 câu trừ 1 ở câu sai
 *   thứ 2). Các chữ gây nhiễu rơi qua khỏi màn hình thì quay lại trên cùng,
 *   không tính sai. Chữ yêu cầu KHÔNG quay lại — hụt là hết lượt đó.
 * ĐÃ CHỐT 2026-10-29 (người dùng: "ok, rơi k bắt kụp tính là sai"):
 * chữ yêu cầu rơi hụt = 1 câu sai, áp dụng luật điểm chung (sai 2 câu trừ 1
 * ở câu sai thứ 2). Không còn phương án "hụt trừ ngay 1 điểm" — phương án đó đã bỏ.
 */
@Composable
fun Stage5Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    // Ngân hàng chữ: phụ âm đầu + nguyên âm gốc, không dùng chữ có dấu sẵn
    // để bé chỉ bắt đúng 1 chữ cái đơn.
    val letterPool = remember {
        (WordBank.initials + VietnameseTones.BASE_VOWELS).distinct()
    }
    val pick = remember {
        picker.pick(5, letterPool) { "letter:$it" }
    }
    LaunchedEffect(pick?.newCycle) { if (pick?.newCycle == true) onNewCycle() }
    val target = pick?.item
    if (target == null) {
        Text("Chưa có chữ nào.", fontSize = 24.sp)
        return
    }
    LaunchedEffect(target) { tts.speak(target) }
    var answered by remember { mutableStateOf(false) }
    val items = remember(target) {
        val distractors = letterPool.filter { it != target }.shuffled().take(5)
        val letters = (distractors + target).shuffled()
        letters.mapIndexed { index, letter ->
            FallingItem(
                letter = letter,
                xFrac = (index + 0.5f) / letters.size,
                // Tốc độ vừa phải: 0.006–0.010 / nhịp 50ms = hết màn hình trong ~5–8 giây.
                speed = 0.006f + (index % 3) * 0.002f,
                y = -0.15f * index,
            )
        }
    }
    var falling by remember { mutableStateOf(items) }
    // Vòng lặp rơi: mỗi 50ms đẩy các chữ THẲNG xuống (chỉ đổi y, không đổi x).
    // Chữ gây nhiễu tới đáy quay lại trên cùng; chữ YÊU CẦU tới đáy là hụt.
    LaunchedEffect(target, answered) {
        var current = items
        while (!answered) {
            delay(50)
            if (answered) break
            current = current.map { item ->
                val newY = item.y + item.speed
                if (newY > 1.05f && item.letter != target) {
                    item.copy(y = -0.1f) // gây nhiễu: quay lại, không tính sai
                } else {
                    item.copy(y = newY)
                }
            }
            falling = current
            val missedTarget = current.any { it.letter == target && it.y > 1.05f }
            if (missedTarget && !answered) {
                answered = true
                // Hụt = 1 câu sai, áp dụng luật điểm chung (sai 2 câu trừ 1).
                // Đã chốt với người dùng 2026-10-29, không trừ ngay 1 điểm.
                onAnswer(false, target)
                break
            }
        }
    }
    Column {
        Text("Nghe rồi bắt đúng chữ nhé!", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        SpeakButton(textToSpeak = target, tts = tts)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Chữ cần bắt là chữ mà giọng đọc vừa đọc.", fontSize = 22.sp)
        Text("Nếu chữ cần bắt rơi qua khỏi màn hình mà chưa bắt được thì tính là sai.", fontSize = 22.sp)
        Spacer(modifier = Modifier.height(12.dp))
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().height(420.dp)
                .background(KidCream),
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            for (item in falling) {
                val xPx = (item.xFrac * widthPx - 40f).roundToInt()
                val yPx = (item.y * heightPx).roundToInt()
                Box(
                    modifier = Modifier
                        .offset { IntOffset(xPx, yPx) }
                        .size(80.dp)
                        .clip(CircleShape)
                        // Tất cả chữ cùng 1 màu vàng phẳng — không để màu
                        // khác nhau lộ chữ yêu cầu, bé phải nghe mới biết bắt chữ nào.
                        .background(KidYellow)
                        .clickable(enabled = !answered) {
                            answered = true
                            onAnswer(item.letter == target, target)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.letter, fontSize = 40.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
