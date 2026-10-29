package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.ProgressStore
import vn.danhvan.tieuhoc.data.StageInfo
import kotlin.math.roundToInt

/**
 * Màn hình chính, CHỐT 2026-10-29 sau khi anh duyệt đường v9:
 * - Câu dẫn trên cùng: "Bé ơi, hãy giúp bạn thỏ vượt chướng ngại vật
 *   giành được cà rốt nào!"
 * - Đường là 1 ĐƯỜNG CONG TỰ DO duy nhất, uốn như tay vẽ: đi ngang trên,
 *   cong xuống phải, chéo xuống trái tới giữa, vòng xuống trái dưới,
 *   rồi ngang dưới sang phải tới số 7. Các số NẰM TRÊN đường cong,
 *   không phải góc cua của đường, nên đường không gãy khúc ở mỗi số.
 *   Đường bắt đầu đúng từ số 1, kết thúc đúng ở số 7, không thừa đầu
 *   hay cuối. Dưới số 7 là 3 củ cà rốt.
 * - Chỉ ghi SỐ 1..7, không ghi tên chặng, không ghi hướng dẫn bé chạy
 *   hay cúp vàng gì cả. Số 1 sáng, số đã qua xanh lá, số khóa xám sẫm
 *   và có ổ khóa nhỏ ở góc, bấm không vào được.
 * - Nền trắng gọn: núi, mặt trời, mây, ít cây hoa ven đường.
 * - Bấm chặng đã mở: hiện bảng luật + đọc luật tiếng Việt, nút trái là
 *   "← Trang chủ", nút phải là "Vào thi →".
 * - Thi xong chặng trước bấm "Chặng tiếp theo": MainActivity quay về
 *   màn chính và tự mở bảng luật của chặng tiếp theo (autoOpenStage),
 *   không vào thẳng chặng.
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

// Vị trí các số trên đường cong tự do, theo tỉ lệ màn hình (x, y).
// Thứ tự đi: trái trên -> phải trên -> phải giữa -> giữa -> trái dưới -> phải dưới.
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

    val displayStages = if (stages.size >= 7) stages.sortedBy { it.number }.take(7) else listOf(
        StageInfo(1, "Nghe chọn đúng từ", ""),
        StageInfo(2, "Hình chọn đúng từ", ""),
        StageInfo(3, "Đoạn văn dài", ""),
        StageInfo(4, "Ghép chữ và dấu theo giọng đọc", ""),
        StageInfo(5, "Từ rơi", ""),
        StageInfo(6, "Nghe câu 3 từ chọn đoạn chữ đúng", ""),
        StageInfo(7, "Nhìn hình tự xếp chữ", ""),
    )

    // Chặng tiếp theo sau khi thi xong: tự mở bảng luật của chặng đó.
    LaunchedEffect(autoOpenStage) {
        if (autoOpenStage != null) {
            val s = displayStages.firstOrNull { it.number == autoOpenStage }
            if (s != null && store.isUnlocked(s.number)) {
                selectedStage = s
            }
            onAutoOpenShown()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Câu dẫn.
            Text(
                "Bé ơi, hãy giúp bạn thỏ vượt chướng ngại vật giành được cà rốt nào!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFF212121), RoundedCornerShape(16.dp))
                    .padding(12.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("☀️ ☁️ ⛰️ 🌳 🌸", fontSize = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            // Đường cong tự do + các số.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val density = LocalDensity.current
                val wPx = with(density) { maxWidth.toPx() }
                val hPx = with(density) { maxHeight.toPx() }
                val nodePx = with(density) { 32.dp.toPx() }
                val lockPx = with(density) { 18.dp.toPx() }
                val carrotPx = with(density) { 20.dp.toPx() }
                val carrotYPx = with(density) { 44.dp.toPx() }
                // Vẽ đường cong tự do qua các số, bằng Bezier mượt.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pts = HomeNodeFracs.map { Offset(it.first * size.width, it.second * size.height) }
                    for (i in 0 until pts.size - 1) {
                        val p0 = if (i == 0) pts[0] else pts[i - 1]
                        val p1 = pts[i]
                        val p2 = pts[i + 1]
                        val p3 = if (i + 2 < pts.size) pts[i + 2] else pts[i + 1]
                        // Catmull-Rom -> Bezier, vẽ từng đoạn nhỏ cho mượt.
                        var prev = p1
                        for (step in 1..20) {
                            val t = step / 20f
                            val x = 0.5f * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t * t + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t * t * t)
                            val y = 0.5f * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t * t + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t * t * t)
                            val cur = Offset(x, y)
                            if (step == 1) {
                                // Đoạn đầu đã ở p1, chỉ vẽ từ p1 tới cur.
                            }
                            drawLine(Color(0xFFD2AA6E), prev, cur, strokeWidth = 42f)
                            prev = cur
                        }
                    }
                }
                // Thỏ ở đầu đường, cạnh số 1.
                Text(
                    "🐰",
                    fontSize = 44.sp,
                    modifier = Modifier.offset {
                        val x = (HomeNodeFracs[0].first * wPx - 64f).roundToInt()
                        val y = (HomeNodeFracs[0].second * hPx - 56f).roundToInt()
                        IntOffset(x.coerceAtLeast(0), y.coerceAtLeast(0))
                    },
                )
                // Các số, chỉ số, không tên chặng.
                for (stage in displayStages) {
                    val idx = stage.number - 1
                    if (idx !in HomeNodeFracs.indices) continue
                    val frac = HomeNodeFracs[idx]
                    val unlocked = store.isUnlocked(stage.number)
                    val passed = store.isPassed(stage.number)
                    val bg = if (passed) KidGreen else if (unlocked) KidBlue else Color(0xFF5A5A5A)
                    Box(
                        modifier = Modifier
                            .offset {
                                val x = (frac.first * wPx - nodePx).roundToInt()
                                val y = (frac.second * hPx - nodePx).roundToInt()
                                IntOffset(x, y)
                            }
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(bg)
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
                            "🔒",
                            fontSize = 16.sp,
                            modifier = Modifier.offset {
                                val x = (frac.first * wPx + lockPx).roundToInt()
                                val y = (frac.second * hPx - nodePx).roundToInt()
                                IntOffset(x, y)
                            },
                        )
                    }
                }
                // Cà rốt ở cuối đường, cạnh số 7, đường không thừa sau số 7.
                val last = HomeNodeFracs[6]
                Text(
                    "🥕🥕🥕",
                    fontSize = 28.sp,
                    modifier = Modifier.offset {
                        val x = (last.first * wPx - carrotPx).roundToInt()
                        val y = (last.second * hPx + carrotYPx).roundToInt()
                        IntOffset(x, y)
                    },
                )
            }
            Text(
                "App chạy offline trên máy, không quảng cáo, không mua trong app, không chat tự do.",
                fontSize = 16.sp,
                color = Color(0xFF555555),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    val picked = selectedStage
    if (picked != null) {
        val rule = StageRules[picked.number] ?: picked.moTa
        LaunchedEffect(picked.number) { tts.speak(rule) }
        AlertDialog(
            onDismissRequest = {
                tts.stop()
                selectedStage = null
            },
            title = { Text("Chặng ${picked.number}", fontWeight = FontWeight.Black) },
            text = { Text(rule, fontSize = 22.sp) },
            confirmButton = {
                TextButton(onClick = {
                    tts.stop()
                    selectedStage = null
                    onStage(picked.number)
                }) { Text("Vào thi →", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    tts.stop()
                    selectedStage = null
                }) { Text("← Trang chủ") }
            },
        )
    }

    if (lockedMessage != null) {
        AlertDialog(
            onDismissRequest = { lockedMessage = null },
            title = { Text("Chặng đang khóa 🔒", fontWeight = FontWeight.Black) },
            text = { Text(lockedMessage!!, fontSize = 22.sp) },
            confirmButton = {
                TextButton(onClick = { lockedMessage = null }) { Text("Đã hiểu", fontWeight = FontWeight.Bold) }
            },
        )
    }
}
