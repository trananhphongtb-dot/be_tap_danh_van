package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.R
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.ProgressStore
import vn.danhvan.tieuhoc.data.StageInfo
import kotlin.math.roundToInt

/**
 * Màn hình chính v15 - DÙNG ẢNH, KHÔNG VẼ BẰNG CODE NỮA (chốt 2026-10-29):
 * - Nền là ảnh Gemini anh gửi (nen_chinh.png), đã có sẵn bảng gỗ câu dẫn,
 *   thỏ lớn, đường mòn có hàng rào, cây, hoa, đá, nấm, giỏ cà rốt.
 * - Chỉ phủ nút số lên đúng vị trí các số trong ảnh để bấm được và để
 *   đổi trạng thái (qua rồi xanh lá có sao, mở xanh dương, khóa xám có
 *   ổ khóa, chặng 7 đĩa gỗ nâu). Nút phủ đúng tâm số trong ảnh nên
 *   số trong ảnh bị nút che, không bị trùng số.
 * - Giọng đọc câu dẫn vẫn đọc khi mở app, bảng luật vẫn là bảng gỗ lớn.
 */
val StageRules = mapOf(
    1 to "Bé hãy nghe giọng đọc, chọn đúng từ đó.",
    2 to "Bé hãy nhìn hình, chọn đúng từ ứng với hình.",
    3 to "Bé hãy nghe đoạn văn, tìm 2 từ bé đọc được, chọn đáp án có đúng 2 từ đó.",
    4 to "Bé hãy nghe từ, xếp chữ và dấu thành từ đó, rồi bấm nút tích.",
    5 to "Bé hãy nghe từ, chạm vào từ đang rơi đúng với từ vừa nghe.",
    6 to "Bé hãy nghe câu 3 từ, chọn đúng đoạn chữ đó.",
    7 to "Bé hãy nhìn hình, tự xếp chữ và dấu thành từ ứng với hình, rồi bấm nút tích.",
)

private val HomeIntro = "Bé ơi, hãy giúp bạn thỏ vượt chướng ngại vật giành được cà rốt nào!"

// Tâm các số trong ảnh Gemini, theo tỉ lệ màn hình (đo từ ảnh gốc).
private val HomeNodeFracs = listOf(
    0.147f to 0.235f,
    0.52f to 0.262f,
    0.879f to 0.31f,
    0.765f to 0.49f,
    0.47f to 0.667f,
    0.265f to 0.812f,
    0.83f to 0.885f,
)

@Composable
fun HomeScreen(
    stages: List<StageInfo>,
    store: ProgressStore,
    tts: VietnameseTts,
    onStage: (Int) -> Unit,
    autoOpenStage: Int? = null,
    onAutoOpenShown: () -> Unit = {},
) {
    var selectedStage by remember { mutableStateOf<StageInfo?>(null) }
    var lockedMessage by remember { mutableStateOf<String?>(null) }
    var introSpoken by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!introSpoken) {
            introSpoken = true
            tts.speak(HomeIntro)
        }
    }

    val displayStages = if (stages.size >= 7) stages.sortedBy { it.number }.take(7) else listOf(
        StageInfo(1, "Nghe chọn đúng từ", ""),
        StageInfo(2, "Hình chọn đúng từ", ""),
        StageInfo(3, "Đoạn văn dài", ""),
        StageInfo(4, "Ghép chữ và dấu theo giọng đọc", ""),
        StageInfo(5, "Từ rơi", ""),
        StageInfo(6, "Nghe câu 3 từ chọn đoạn chữ đúng", ""),
        StageInfo(7, "Nhìn hình tự xếp chữ", ""),
    )

    LaunchedEffect(autoOpenStage) {
        if (autoOpenStage != null) {
            val s = displayStages.firstOrNull { it.number == autoOpenStage }
            if (s != null && store.isUnlocked(s.number)) {
                selectedStage = s
            }
            onAutoOpenShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Nền ảnh Gemini, kéo vừa màn hình để không cắt mất số ở mép.
        Image(
            painter = painterResource(R.drawable.nen_chinh),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            val nodeHalfPx = with(density) { 32.dp.toPx() }
            for (stage in displayStages) {
                val idx = stage.number - 1
                if (idx !in HomeNodeFracs.indices) continue
                val frac = HomeNodeFracs[idx]
                val unlocked = store.isUnlocked(stage.number)
                val passed = store.isPassed(stage.number)
                val bg = if (passed) KidGreen else if (unlocked) KidBlue else Color(0xFF757575)
                val isWood = stage.number == 7
                Box(
                    modifier = Modifier
                        .offset {
                            val x = (frac.first * wPx - nodeHalfPx).roundToInt()
                            val y = (frac.second * hPx - nodeHalfPx).roundToInt()
                            IntOffset(x, y)
                        }
                        .size(64.dp).shadow(6.dp, CircleShape).clip(CircleShape)
                        .background(if (isWood) Color(0xFF8D6E63) else bg)
                        .border(3.dp, Color.White, CircleShape)
                        .clickable {
                            if (unlocked) {
                                selectedStage = stage
                            } else {
                                lockedMessage = "Phần ${stage.number} đang khóa. Bé qua phần trước đã nhé!"
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${stage.number}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                if (passed) {
                    Text(
                        "⭐", fontSize = 16.sp,
                        modifier = Modifier.offset {
                            val x = (frac.first * wPx + 16.dp.toPx()).roundToInt()
                            val y = (frac.second * hPx - nodeHalfPx).roundToInt()
                            IntOffset(x, y)
                        },
                    )
                }
                if (!unlocked) {
                    Text(
                        "🔒", fontSize = 16.sp,
                        modifier = Modifier.offset {
                            val x = (frac.first * wPx + 16.dp.toPx()).roundToInt()
                            val y = (frac.second * hPx - nodeHalfPx).roundToInt()
                            IntOffset(x, y)
                        },
                    )
                }
            }
        }
    }

    // Bảng luật LỚN bằng gỗ, không câu giải thích về giọng đọc.
    val picked = selectedStage
    if (picked != null) {
        val rule = StageRules[picked.number] ?: picked.moTa
        LaunchedEffect(picked.number) { tts.speak("Phần ${picked.number}. $rule") }
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0x80000000))
                .clickable { tts.stop(); selectedStage = null },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.88f)
                    .shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF5E6C8)).border(3.dp, Color(0xFF8D6E63), RoundedCornerShape(24.dp))
                    .padding(20.dp),
            ) {
                Text("Phần ${picked.number}", fontSize = 32.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(12.dp))
                Text(rule, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                Row {
                    BigButton(
                        text = "← Trang chủ",
                        onClick = { tts.stop(); selectedStage = null },
                        color = Color(0xFF757575), modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    BigButton(
                        text = "Vào thi →",
                        onClick = { tts.stop(); selectedStage = null; onStage(picked.number) },
                        color = KidGreen, modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    if (lockedMessage != null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0x80000000))
                .clickable { lockedMessage = null },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.88f)
                    .shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                    .background(Color.White).padding(20.dp),
            ) {
                Text("Phần đang khóa 🔒", fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(10.dp))
                Text(lockedMessage!!, fontSize = 22.sp)
                Spacer(modifier = Modifier.height(16.dp))
                BigButton(text = "Đã hiểu", onClick = { lockedMessage = null }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
