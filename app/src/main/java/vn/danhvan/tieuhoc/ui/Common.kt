package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import vn.danhvan.tieuhoc.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.GameRules
import kotlin.math.roundToInt

/**
 * Nút 3D, CHỐT v13 2026-10-29:
 * - 1 dòng duy nhất, không xuống dòng, chữ dài tự nhỏ lại, tất cả nút
 *   cùng chiều cao 56dp nên không còn nút to nhỏ khác nhau.
 * - Bóng đổ + viền dưới đậm + highlight trên để nhìn nổi 3D.
 */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = KidBlue,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit? = null,
) {
    val fontSize = fontSize ?: if (text.length > 12) 20.sp else 24.sp
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.shadow(6.dp, RoundedCornerShape(18.dp)).height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, disabledContainerColor = color, disabledContentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Text(
            text, fontSize = fontSize, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Nút phát lại giọng đọc, có ở mọi chặng có giọng đọc. */
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
 * Đầu màn hình chặng, CHỐT v13:
 * - Nút quay lại nhỏ ở trái, tên chặng ở giữa, ĐIỂM Ở GÓC PHẢI cho cân.
 * - Thanh tiến trình dưới: thỏ chạy sang phải theo điểm, cuối là cà rốt.
 * - Điểm riêng từng chặng, không cộng dồn.
 */
@Composable
fun ScoreHeader(stageName: String, score: Int, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).shadow(4.dp, CircleShape).clip(CircleShape)
                    .background(Color(0xFF757575)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("←", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White) }
            Text(
                stageName, fontSize = 22.sp, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            // Điểm ở góc phải, dạng ô nhỏ cho cân với nút trái.
            Box(
                modifier = Modifier.shadow(4.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
                    .background(Color.White).padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Điểm: $score/${GameRules.POINTS_TO_PASS}",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val frac = (score.toFloat() / GameRules.POINTS_TO_PASS).coerceIn(0f, 1f)
            Box(
                modifier = Modifier.align(Alignment.CenterStart)
                    .fillMaxWidth().height(10.dp)
                    .clip(RoundedCornerShape(5.dp)).background(Color(0xFFBDBDBD)),
            )
            Box(
                modifier = Modifier.align(Alignment.CenterStart)
                    .fillMaxWidth(frac).height(10.dp)
                    .clip(RoundedCornerShape(5.dp)).background(KidGreen),
            )
            val rabbitX = (frac * widthPx - 22f).coerceIn(0f, (widthPx - 44f).coerceAtLeast(0f)).roundToInt()
            Text(
                "🐰", fontSize = 30.sp,
                modifier = Modifier.align(Alignment.CenterStart).offset { IntOffset(rabbitX, -8) },
            )
            Text(
                "🥕", fontSize = 30.sp,
                modifier = Modifier.align(Alignment.CenterEnd).offset { IntOffset(0, -8) },
            )
        }
    }
}

/** 4 đáp án xếp thành 2 HÀNG, mỗi hàng 2 ô, cùng cao, 1 dòng. */
@Composable
fun AnswerGrid2x2(
    options: List<String>,
    enabled: Boolean,
    onPick: (String) -> Unit,
    selected: String? = null,
    correctOption: String? = null,
    answered: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in options.chunked(2)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (opt in row) {
                    val btnColor = when {
                        answered && opt == correctOption -> KidGreen
                        answered && opt == selected -> KidRed
                        opt == selected -> KidGreen
                        else -> KidBlue
                    }
                    BigButton(
                        text = opt,
                        onClick = { onPick(opt) },
                        enabled = enabled,
                        color = btnColor,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Hàng nút Câu trước / Câu tiếp, nằm dưới đáp án, 1 dòng, cùng cao. */
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
            text = "← Phần trước",
            onClick = onPrev,
            enabled = canPrev,
            color = Color(0xFF757575),
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
        )
        BigButton(
            text = "Phần tiếp →",
            onClick = onNext,
            enabled = canNext,
            color = KidGreen,
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
        )
    }
}

/**
 * Dòng phản hồi, CHỐT v13: KHÔNG hiện chữ nữa, chỉ âm thanh + màu đáp án.
 * Hàm này giữ lại để code cũ không vỡ, nhưng luôn không vẽ gì.
 */
@Composable
fun FeedbackBar(feedback: String?, isCorrect: Boolean?, onNext: () -> Unit) {
    // Không hiện chữ "Đúng rồi!" / "Chưa đúng." nữa.
}

/**
 * Nền toàn màn mỗi chặng, DÙNG ẢNH AI, KHÔNG VẼ BẰNG CODE NỮA (chốt 2026-10-29):
 * 1 vườn thỏ, 2 trang trại bò, 3 vườn gà, 4 vườn ong, 5 ao vịt,
 * 6 nhà mèo, 7 rừng xanh. Ảnh DỌC (2026-10-29) nên vừa màn dọc,
 * không còn phóng to ảnh ngang gây mờ.
 */
@Composable
fun StageBackgroundFull(stage: Int) {
    val res = when (stage) {
        1 -> R.drawable.nen_chang1
        2 -> R.drawable.nen_chang2
        3 -> R.drawable.nen_chang3
        4 -> R.drawable.nen_chang4
        5 -> R.drawable.nen_chang5
        6 -> R.drawable.nen_chang6
        else -> R.drawable.nen_chang7
    }
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}

/** Giữ lại cho tương thích, StageScreen v13 dùng StageBackgroundFull. */
@Composable
fun StageBackdrop(stage: Int) {
    // Không vẽ dải chủ đề nhỏ nữa, nền toàn màn đã có ở StageBackgroundFull.
}
