package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.ProgressStore
import vn.danhvan.tieuhoc.data.StageInfo
import kotlin.math.roundToInt

/**
 * Màn hình chính, CHỐT v13 2026-10-29, theo phong cách ảnh Gemini anh gửi:
 * - Nền đồng cỏ xanh toàn màn, trời xanh, cây, hoa, không nền trắng trơn.
 * - Bảng gỗ câu dẫn trên cùng, GIỌNG ĐỌC câu dẫn khi mở app.
 * - Đường mòn đất có hàng rào, uốn theo nét đỏ anh vẽ lần cuối (v11):
 *   trái trên -> phải trên -> phải giữa -> chéo xuống giữa -> trái dưới
 *   -> phải dưới, bắt đầu đúng từ 1, kết thúc đúng ở 7, cà rốt dưới 7.
 * - Chỉ số 1..7, không tên chặng. Số 1 xanh, số đã qua xanh lá, số khóa
 *   đá xám có ổ khóa nhỏ, số 7 đĩa gỗ.
 * - Bảng luật LỚN bằng gỗ, không phải hộp thoại hệ thống nhỏ, không câu
 *   giải thích về giọng đọc, chỉ tiêu đề + luật thật + 2 nút 3D.
 */
val StageRules = mapOf(
    1 to "Nghe giọng đọc, chọn đúng từ đó.",
    2 to "Nhìn hình, chọn đúng từ ứng với hình.",
    3 to "Nghe đoạn văn, tìm 2 từ bé đọc được, chọn đáp án có đúng 2 từ đó.",
    4 to "Nghe từ, xếp chữ và dấu thành từ đó, rồi bấm nút tích.",
    5 to "Nghe từ, chạm vào từ đang rơi đúng với từ vừa nghe.",
    6 to "Nghe câu 3 từ, chọn đúng đoạn chữ đó.",
    7 to "Nhìn hình, tự xếp chữ và dấu thành từ ứng với hình, rồi bấm nút tích.",
)

private val HomeIntro = "Bé ơi, hãy giúp bạn thỏ vượt chướng ngại vật giành được cà rốt nào!"

// Vị trí các số trên đường rắn v11, theo tỉ lệ màn hình.
private val HomeNodeFracs = listOf(
    0.17f to 0.25f,
    0.46f to 0.23f,
    0.90f to 0.31f,
    0.74f to 0.46f,
    0.34f to 0.62f,
    0.30f to 0.77f,
    0.82f to 0.84f,
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

    // Mở app là đọc câu dẫn thỏ 1 lần.
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
        // Nền đồng cỏ toàn màn.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color(0xFF87CEEB), Offset(0f, 0f), size)
            drawRect(Color(0xFF7BC950), Offset(0f, size.height * 0.45f), size.copy(height = size.height * 0.55f))
            drawCircle(Color(0xFFFFD54F), Offset(size.width * 0.85f, size.height * 0.10f), size.width * 0.08f)
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Bảng gỗ câu dẫn.
            Box(
                modifier = Modifier.fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF5E6C8)).border(3.dp, Color(0xFF8D6E63), RoundedCornerShape(20.dp))
                    .padding(14.dp),
            ) {
                Text(
                    HomeIntro, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, color = Color(0xFF5D4037),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val density = LocalDensity.current
                val wPx = with(density) { maxWidth.toPx() }
                val hPx = with(density) { maxHeight.toPx() }
                val nodePx = with(density) { 32.dp.toPx() }
                val lockPx = with(density) { 18.dp.toPx() }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pts = HomeNodeFracs.map { Offset(it.first * size.width, it.second * size.height) }
                    for (i in 0 until pts.size - 1) {
                        val p0 = if (i == 0) pts[0] else pts[i - 1]
                        val p1 = pts[i]
                        val p2 = pts[i + 1]
                        val p3 = if (i + 2 < pts.size) pts[i + 2] else pts[i + 1]
                        var prev = p1
                        for (step in 1..20) {
                            val t = step / 20f
                            val x = 0.5f * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t * t + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t * t * t)
                            val y = 0.5f * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t * t + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t * t * t)
                            val cur = Offset(x, y)
                            drawLine(Color(0xFFD2AA6E), prev, cur, strokeWidth = 42f)
                            prev = cur
                        }
                    }
                }
                // Thỏ lớn ở đầu đường, cạnh số 1.
                Text(
                    "🐰", fontSize = 64.sp,
                    modifier = Modifier.offset {
                        val x = (HomeNodeFracs[0].first * wPx - 70f).roundToInt()
                        val y = (HomeNodeFracs[0].second * hPx - 90f).roundToInt()
                        IntOffset(x.coerceAtLeast(0), y.coerceAtLeast(0))
                    },
                )
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
                                val x = (frac.first * wPx - nodePx).roundToInt()
                                val y = (frac.second * hPx - nodePx).roundToInt()
                                IntOffset(x, y)
                            }
                            .size(64.dp).shadow(6.dp, CircleShape).clip(CircleShape)
                            .background(if (isWood) Color(0xFF8D6E63) else bg)
                            .border(3.dp, Color.White, CircleShape)
                            .clickable {
                                if (unlocked) {
                                    selectedStage = stage
                                } else {
                                    lockedMessage = "Chặng ${stage.number} đang khóa. Bé qua chặng trước đã nhé!"
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${stage.number}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    if (!unlocked) {
                        Text(
                            "🔒", fontSize = 16.sp,
                            modifier = Modifier.offset {
                                val x = (frac.first * wPx + lockPx).roundToInt()
                                val y = (frac.second * hPx - nodePx).roundToInt()
                                IntOffset(x, y)
                            },
                        )
                    }
                }
                val last = HomeNodeFracs[6]
                Text(
                    "🥕🥕🥕", fontSize = 28.sp,
                    modifier = Modifier.offset {
                        val x = (last.first * wPx - 20.dp.toPx()).roundToInt()
                        val y = (last.second * hPx + 44.dp.toPx()).roundToInt()
                        IntOffset(x, y)
                    },
                )
            }
            Text(
                "App chạy offline trên máy, không quảng cáo, không mua trong app, không chat tự do.",
                fontSize = 16.sp, color = Color(0xFF333333), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Bảng luật LỚN bằng gỗ, không phải hộp thoại hệ thống nhỏ.
    val picked = selectedStage
    if (picked != null) {
        val rule = StageRules[picked.number] ?: picked.moTa
        LaunchedEffect(picked.number) { tts.speak(rule) }
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
                Text("Chặng ${picked.number}", fontSize = 32.sp, fontWeight = FontWeight.Black)
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
                Text("Chặng đang khóa 🔒", fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(10.dp))
                Text(lockedMessage!!, fontSize = 22.sp)
                Spacer(modifier = Modifier.height(16.dp))
                BigButton(text = "Đã hiểu", onClick = { lockedMessage = null }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
