package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.GameRules
import kotlin.math.roundToInt

/** Nút to, chữ to, phù hợp trẻ mới học. Chiều cao 56dp để vừa 1 màn hình. */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = KidBlue,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

/** Nút phát lại giọng đọc, có ở mọi chặng. */
@Composable
fun SpeakButton(textToSpeak: String, tts: VietnameseTts, label: String = "🔊 Nghe lại") {
    BigButton(
        text = label,
        onClick = { tts.speak(textToSpeak) },
        color = KidOrange,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Đầu màn hình chặng, SỬA 2026-10-29:
 * - Thanh tiến trình ở BIÊN TRÊN: 1 đường chạy ngang, bé trai chạy dọc theo
 *   đường, cuối đường là cúp vàng. Đủ 10 điểm là bé tới đích (tới cúp).
 * - Điểm chỉ tính RIÊNG chặng đang thi, không cộng dồn sang chặng khác:
 *   vào chặng nào điểm bắt đầu lại từ 0 ở chặng đó.
 */
@Composable
fun ScoreHeader(stageName: String, score: Int, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BigButton(text = "← Về", onClick = onBack, color = Color(0xFF757575))
            Text(
                "Điểm: $score / ${GameRules.POINTS_TO_PASS}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(stageName, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(6.dp))
        // Đường chạy trên biên trên: bé chạy từ trái sang phải theo điểm.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val frac = (score.toFloat() / GameRules.POINTS_TO_PASS).coerceIn(0f, 1f)
            // Vạch đường.
            Box(
                modifier = Modifier.align(Alignment.CenterStart)
                    .fillMaxWidth().height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFBDBDBD)),
            )
            // Phần đường đã chạy qua.
            Box(
                modifier = Modifier.align(Alignment.CenterStart)
                    .fillMaxWidth(frac).height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(KidGreen),
            )
            // Bé trai chạy: vị trí theo điểm, trừ đi nửa chiều rộng của chữ.
            val boyX = (frac * widthPx - 22f).coerceIn(0f, (widthPx - 44f).coerceAtLeast(0f)).roundToInt()
            Text(
                "🏃",
                fontSize = 30.sp,
                modifier = Modifier.align(Alignment.CenterStart)
                    .offset { IntOffset(boyX, -8) },
            )
            // Cúp vàng ở cuối đường.
            Text(
                "🏆",
                fontSize = 30.sp,
                modifier = Modifier.align(Alignment.CenterEnd)
                    .offset { IntOffset(0, -8) },
            )
        }
    }
}

/** 4 đáp án xếp thành 2 HÀNG, mỗi hàng 2 ô, gọn vừa 1 màn hình. */
@Composable
fun AnswerGrid2x2(
    options: List<String>,
    enabled: Boolean,
    onPick: (String) -> Unit,
    selected: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in options.chunked(2)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (opt in row) {
                    BigButton(
                        text = opt,
                        onClick = { onPick(opt) },
                        enabled = enabled,
                        color = if (opt == selected) KidGreen else KidBlue,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Hàng lẻ ô cuối thì chèn khoảng trống cho cân.
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Hàng nút Câu trước / Câu tiếp, nằm dưới đáp án. */
@Composable
fun PrevNextRow(
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        BigButton(
            text = "← Câu trước",
            onClick = onPrev,
            enabled = canPrev,
            color = Color(0xFF757575),
            modifier = Modifier.weight(1f),
        )
        BigButton(
            text = "Câu tiếp →",
            onClick = onNext,
            enabled = canNext,
            color = KidGreen,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Dòng phản hồi sau 1 câu trả lời. Chặng 5 rơi liên tục không dùng dòng này. */
@Composable
fun FeedbackBar(feedback: String?, isCorrect: Boolean?, onNext: () -> Unit) {
    if (feedback == null) return
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        feedback,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = if (isCorrect == true) KidGreen else KidRed,
    )
    Spacer(modifier = Modifier.height(10.dp))
    BigButton(text = "Câu tiếp theo →", onClick = onNext, color = KidGreen, modifier = Modifier.fillMaxWidth())
}
