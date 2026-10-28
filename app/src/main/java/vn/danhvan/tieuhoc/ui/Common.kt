package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.GameRules

/** Nút to, chữ to, phù hợp trẻ mới học. */
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
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Text(text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
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

/** Đầu màn hình chặng: tên chặng, điểm, thanh tiến trình tới 10 điểm. */
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
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(stageName, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { score.toFloat() / GameRules.POINTS_TO_PASS },
            modifier = Modifier.fillMaxWidth().height(14.dp),
        )
    }
}

/** Dòng phản hồi sau 1 câu trả lời, kèm nút sang câu tiếp theo. */
@Composable
fun FeedbackBar(feedback: String?, isCorrect: Boolean?, onNext: () -> Unit) {
    if (feedback == null) return
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        feedback,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = if (isCorrect == true) KidGreen else KidRed,
    )
    Spacer(modifier = Modifier.height(12.dp))
    BigButton(text = "Câu tiếp theo →", onClick = onNext, color = KidGreen, modifier = Modifier.fillMaxWidth())
}
