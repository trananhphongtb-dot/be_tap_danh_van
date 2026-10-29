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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * Màn hình chính v14 - Nâng cấp toàn diện đồ họa đồi cỏ, đường mòn và nút 3D:
 * - Nền chuyển màu mượt mà, bầu trời mây bay, 3 lớp đồi cỏ uốn lượn có chiều sâu.
 * - Đường mòn viền đất nổi, rải sỏi đá ngắt quãng theo đúng dáng uốn từ chặng 1 đến 7.
 * - Nút chặng 3D nổi khối: chặng đã qua có sao vàng, chặng khóa phủ vân đá, chặng 7 đĩa gỗ.
 * - Mascot chú thỏ hoạt hình và vườn cà rốt vẽ vector sinh động, loại bỏ emoji thô cứng.
 * - Giữ nguyên 100% logic, dữ liệu lưu trữ, luật thi và tương tác nguyên bản.
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

// Vị trí các số trên đường rắn theo tỉ lệ màn hình (kế thừa nguyên vẹn)
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
        // LỚP 1: Nền phong cảnh hoạt hình (Bầu trời, mặt trời, mây bay, các lớp đồi cỏ)
        CartoonScenicBackground()

        // LỚP 2: Nội dung chính
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            // Bảng gỗ câu dẫn 3D
            WoodenHeaderBanner(text = HomeIntro)

            Spacer(modifier = Modifier.height(4.dp))

            // Khu vực bản đồ đường mòn & các chặng
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val density = LocalDensity.current
                val wPx = with(density) { maxWidth.toPx() }
                val hPx = with(density) { maxHeight.toPx() }
                val nodeSizeDp = 64.dp
                val nodeHalfPx = with(density) { 32.dp.toPx() }

                // Vẽ con đường mòn đất 2 tầng uốn lượn + sỏi trắng
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pts = HomeNodeFracs.map { Offset(it.first * size.width, it.second * size.height) }
                    val pathCurve = Path().apply {
                        if (pts.isNotEmpty()) {
                            moveTo(pts[0].x, pts[0].y)
                            for (i in 0 until pts.size - 1) {
                                val p0 = if (i == 0) pts[0] else pts[i - 1]
                                val p1 = pts[i]
                                val p2 = pts[i + 1]
                                val p3 = if (i + 2 < pts.size) pts[i + 2] else pts[i + 1]
                                for (step in 1..20) {
                                    val t = step / 20f
                                    val x = 0.5f * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t * t + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t * t * t)
                                    val y = 0.5f * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t * t + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t * t * t)
                                    lineTo(x, y)
                                }
                            }
                        }
                    }

                    // 1. Viền đất sẫm bên ngoài (tạo độ dày cho đường)
                    drawPath(
                        path = pathCurve,
                        color = Color(0xFF8D6E63),
                        style = Stroke(width = 54f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    // 2. Lòng đường màu đất cát ấm áp
                    drawPath(
                        path = pathCurve,
                        color = Color(0xFFE5C483),
                        style = Stroke(width = 44f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )

                    // 3. Những viên sỏi đá nhỏ màu trắng ngà rải rác bên lề đường
                    val pebbles = listOf(
                        Offset(0.25f, 0.22f), Offset(0.38f, 0.25f), Offset(0.70f, 0.26f),
                        Offset(0.88f, 0.40f), Offset(0.60f, 0.52f), Offset(0.40f, 0.58f),
                        Offset(0.24f, 0.70f), Offset(0.48f, 0.81f), Offset(0.65f, 0.83f)
                    )
                    pebbles.forEach { p ->
                        drawCircle(
                            color = Color(0xFFFFF8E7),
                            radius = 5.5f,
                            center = Offset(p.x * size.width, p.y * size.height),
                        )
                    }
                }

                // Chú thỏ hoạt hình vector ở chặng 1
                Box(
                    modifier = Modifier.offset {
                        val x = (HomeNodeFracs[0].first * wPx - 42.dp.toPx()).roundToInt()
                        val y = (HomeNodeFracs[0].second * hPx - 80.dp.toPx()).roundToInt()
                        IntOffset(x.coerceAtLeast(0), y.coerceAtLeast(0))
                    }
                ) {
                    CartoonRabbitMascot(modifier = Modifier.size(70.dp))
                }

                // 7 Nút chặng 3D nổi khối
                for (stage in displayStages) {
                    val idx = stage.number - 1
                    if (idx !in HomeNodeFracs.indices) continue
                    val frac = HomeNodeFracs[idx]
                    val unlocked = store.isUnlocked(stage.number)
                    val passed = store.isPassed(stage.number)
                    val isWood = stage.number == 7

                    Box(
                        modifier = Modifier
                            .offset {
                                val x = (frac.first * wPx - nodeHalfPx).roundToInt()
                                val y = (frac.second * hPx - nodeHalfPx).roundToInt()
                                IntOffset(x, y)
                            }
                            .size(nodeSizeDp)
                            .clickable {
                                if (unlocked) {
                                    selectedStage = stage
                                } else {
                                    lockedMessage = "Chặng ${stage.number} đang khóa. Bé qua chặng trước đã nhé!"
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        StageButton3D(
                            number = stage.number,
                            isPassed = passed,
                            isUnlocked = unlocked,
                            isWood = isWood,
                        )
                    }
                }

                // Vườn cà rốt sum sê tại đích đến (dưới chặng 7)
                val lastNode = HomeNodeFracs[6]
                Box(
                    modifier = Modifier.offset {
                        val x = (lastNode.first * wPx - 32.dp.toPx()).roundToInt()
                        val y = (lastNode.second * hPx + 38.dp.toPx()).roundToInt()
                        IntOffset(x, y)
                    }
                ) {
                    CarrotPatchIllustration(modifier = Modifier.size(90.dp, 45.dp))
                }
            }

            // Thanh nhãn chú thích tinh tế dưới đáy
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33000000))
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "App chạy offline trên máy, không quảng cáo, không mua trong app.",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    // Bảng luật LỚN bằng gỗ 3D
    val picked = selectedStage
    if (picked != null) {
        val rule = StageRules[picked.number] ?: picked.moTa
        LaunchedEffect(picked.number) { tts.speak(rule) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x90000000))
                .clickable { tts.stop(); selectedStage = null },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .shadow(12.dp, RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFFFBF1D8))
                    .border(4.dp, Color(0xFF795548), RoundedCornerShape(26.dp))
                    .padding(22.dp),
            ) {
                Text(
                    "Chặng ${picked.number}",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF5D4037),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    rule,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723),
                    lineHeight = 28.sp,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row {
                    BigButton(
                        text = "← Trang chủ",
                        onClick = { tts.stop(); selectedStage = null },
                        color = Color(0xFF8D6E63),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    BigButton(
                        text = "Vào thi →",
                        onClick = {
                            tts.stop()
                            selectedStage = null
                            onStage(picked.number)
                        },
                        color = KidGreen,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    // Dialog thông báo khi chặng bị khóa
    if (lockedMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x90000000))
                .clickable { lockedMessage = null },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(3.dp, Color(0xFFFFB300), RoundedCornerShape(24.dp))
                    .padding(22.dp),
            ) {
                Text("Chặng đang khóa 🔒", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFD84315))
                Spacer(modifier = Modifier.height(10.dp))
                Text(lockedMessage!!, fontSize = 20.sp, color = Color(0xFF424242), lineHeight = 26.sp)
                Spacer(modifier = Modifier.height(18.dp))
                BigButton(
                    text = "Đã hiểu",
                    onClick = { lockedMessage = null },
                    color = Color(0xFFFF9800),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// =====================================================================
// CÁC THÀNH PHẦN ĐỒ HỌA HOẠT HÌNH (VẼ TỰ ĐỘNG BẰNG COMPOSE CANVAS)
// =====================================================================

/** Nền bầu trời chuyển màu, mặt trời, mây bồng bềnh và 3 tầng đồi cỏ uốn lượn */
@Composable
private fun CartoonScenicBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Bầu trời Gradient xanh dương êm dịu
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF64B5F6), Color(0xFFBBDEFB), Color(0xFFE3F2FD)),
                startY = 0f,
                endY = h * 0.45f,
            ),
            size = Size(w, h * 0.45f),
        )

        // 2. Mặt trời ấm áp
        drawCircle(
            color = Color(0xFFFFE082),
            radius = w * 0.12f,
            center = Offset(w * 0.82f, h * 0.08f),
        )
        drawCircle(
            color = Color(0xFFFFCA28),
            radius = w * 0.09f,
            center = Offset(w * 0.82f, h * 0.08f),
        )

        // 3. Những đám mây trắng xốp
        drawCloud(this, Offset(w * 0.18f, h * 0.09f), scale = 1.0f)
        drawCloud(this, Offset(w * 0.55f, h * 0.13f), scale = 0.8f)

        // 4. Đồi cỏ xa xăm (Màu xanh dịu)
        val farHill = Path().apply {
            moveTo(0f, h * 0.38f)
            cubicTo(w * 0.3f, h * 0.30f, w * 0.7f, h * 0.42f, w, h * 0.32f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(farHill, color = Color(0xFF81C784))

        // 5. Đồi cỏ trung tâm (Màu xanh sáng)
        val midHill = Path().apply {
            moveTo(0f, h * 0.46f)
            cubicTo(w * 0.4f, h * 0.52f, w * 0.6f, h * 0.40f, w, h * 0.48f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(midHill, color = Color(0xFF66BB6A))

        // 6. Đồi cỏ tiền cảnh (Màu xanh tươi rực rỡ)
        val nearHill = Path().apply {
            moveTo(0f, h * 0.58f)
            cubicTo(w * 0.35f, h * 0.54f, w * 0.65f, h * 0.64f, w, h * 0.60f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(nearHill, color = Color(0xFF4CAF50))

        // 7. Điểm xuyết hoa cúc nhỏ trên cỏ
        val flowerLocs = listOf(
            Offset(w * 0.08f, h * 0.48f), Offset(w * 0.88f, h * 0.55f),
            Offset(w * 0.14f, h * 0.72f), Offset(w * 0.90f, h * 0.76f),
            Offset(w * 0.22f, h * 0.90f), Offset(w * 0.60f, h * 0.93f)
        )
        flowerLocs.forEach { loc ->
            drawCircle(Color.White, radius = 5f, center = Offset(loc.x - 4f, loc.y))
            drawCircle(Color.White, radius = 5f, center = Offset(loc.x + 4f, loc.y))
            drawCircle(Color.White, radius = 5f, center = Offset(loc.x, loc.y - 4f))
            drawCircle(Color.White, radius = 5f, center = Offset(loc.x, loc.y + 4f))
            drawCircle(Color(0xFFFFD54F), radius = 4f, center = loc)
        }
    }
}

private fun drawCloud(scope: DrawScope, center: Offset, scale: Float) {
    scope.drawCircle(Color.White, radius = 22f * scale, center = center)
    scope.drawCircle(Color.White, radius = 17f * scale, center = Offset(center.x - 22f * scale, center.y + 4f * scale))
    scope.drawCircle(Color.White, radius = 19f * scale, center = Offset(center.x + 22f * scale, center.y + 3f * scale))
    scope.drawCircle(Color.White, radius = 14f * scale, center = Offset(center.x - 36f * scale, center.y + 8f * scale))
    scope.drawCircle(Color.White, radius = 15f * scale, center = Offset(center.x + 36f * scale, center.y + 7f * scale))
}

/** Bảng gỗ câu dẫn 3D với ốc vít và thớ gỗ chân thật */
@Composable
private fun WoodenHeaderBanner(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF5D4037))
            .padding(bottom = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFEEDBB2))
                .border(3.dp, Color(0xFF8D6E63), RoundedCornerShape(18.dp))
                .padding(vertical = 10.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = Color(0xFF4E342E),
                lineHeight = 26.sp,
            )
        }
    }
}

/** Nút chặng nổi khối 3D với trạng thái đã hoàn thành (sao), đang mở, bị khóa hoặc đĩa gỗ chặng 7 */
@Composable
private fun StageButton3D(
    number: Int,
    isPassed: Boolean,
    isUnlocked: Boolean,
    isWood: Boolean,
) {
    val topColor = when {
        isWood -> Color(0xFFBCAAA4)
        isPassed -> Color(0xFF66BB6A)
        isUnlocked -> Color(0xFF42A5F5)
        else -> Color(0xFFB0BEC5)
    }
    val shadowColor = when {
        isWood -> Color(0xFF6D4C41)
        isPassed -> Color(0xFF2E7D32)
        isUnlocked -> Color(0xFF1565C0)
        else -> Color(0xFF546E7A)
    }

    Box(
        modifier = Modifier
            .size(62.dp)
            .shadow(6.dp, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)

            // Đế nút (tạo độ dày nổi 3D)
            drawCircle(color = shadowColor, radius = r, center = Offset(c.x, c.y + 4.dp.toPx()))
            // Mặt nút chính
            drawCircle(color = topColor, radius = r - 1.dp.toPx(), center = c)
            // Viền sáng bóng
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = r - 1.dp.toPx(),
                center = c,
                style = Stroke(width = 3.dp.toPx()),
            )

            // Vòng vân gỗ đặc biệt cho chặng 7
            if (isWood) {
                drawCircle(
                    color = Color(0xFF5D4037).copy(alpha = 0.4f),
                    radius = r * 0.65f,
                    center = c,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        // Nội dung bên trong nút chặng
        if (isUnlocked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isPassed) {
                    Text("⭐", fontSize = 11.sp, modifier = Modifier.offset(y = 2.dp))
                }
                Text(
                    "$number",
                    fontSize = if (isPassed) 24.sp else 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
            }
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text("$number", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.8f))
                Text(
                    "🔒",
                    fontSize = 15.sp,
                    modifier = Modifier.offset(x = 16.dp, y = (-16).dp),
                )
            }
        }
    }
}

/** Chú thỏ trắng hoạt hình vẽ vector độc quyền */
@Composable
private fun CartoonRabbitMascot(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Tai trái & tai phải
        drawOval(Color.White, Offset(w * 0.22f, 0f), Size(w * 0.18f, h * 0.44f))
        drawOval(Color(0xFFFFCDD2), Offset(w * 0.25f, h * 0.06f), Size(w * 0.12f, h * 0.32f))

        drawOval(Color.White, Offset(w * 0.55f, 0f), Size(w * 0.18f, h * 0.44f))
        drawOval(Color(0xFFFFCDD2), Offset(w * 0.58f, h * 0.06f), Size(w * 0.12f, h * 0.32f))

        // 2. Đầu thỏ
        drawCircle(Color.White, radius = w * 0.34f, center = Offset(w * 0.48f, h * 0.50f))

        // 3. Mắt đen láy & ánh sáng long lanh
        drawCircle(Color(0xFF212121), radius = 4.5f, center = Offset(w * 0.38f, h * 0.46f))
        drawCircle(Color.White, radius = 1.5f, center = Offset(w * 0.37f, h * 0.45f))

        drawCircle(Color(0xFF212121), radius = 4.5f, center = Offset(w * 0.58f, h * 0.46f))
        drawCircle(Color.White, radius = 1.5f, center = Offset(w * 0.57f, h * 0.45f))

        // 4. Má hồng hào
        drawCircle(Color(0xFFFF8A80).copy(alpha = 0.6f), radius = 5f, center = Offset(w * 0.30f, h * 0.54f))
        drawCircle(Color(0xFFFF8A80).copy(alpha = 0.6f), radius = 5f, center = Offset(w * 0.66f, h * 0.54f))

        // 5. Mũi & miệng xinh
        drawCircle(Color(0xFFFF4081), radius = 2.5f, center = Offset(w * 0.48f, h * 0.51f))
        val smile = Path().apply {
            moveTo(w * 0.43f, h * 0.54f)
            quadraticBezierTo(w * 0.48f, h * 0.58f, w * 0.53f, h * 0.54f)
        }
        drawPath(smile, color = Color(0xFF212121), style = Stroke(width = 2.5f))

        // 6. Tay nhỏ vẫy chào
        drawCircle(Color.White, radius = 8f, center = Offset(w * 0.82f, h * 0.58f))
    }
}

/** Vườn cà rốt màu cam mọng nước ở đích đến */
@Composable
private fun CarrotPatchIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Đụn đất màu nâu màu mỡ
        drawRoundRect(
            color = Color(0xFF5D4037),
            topLeft = Offset(0f, h * 0.35f),
            size = Size(w, h * 0.65f),
            cornerRadius = CornerRadius(16f, 16f),
        )

        // 3 củ cà rốt mọc lên từ luống đất
        val carrotOffsets = listOf(Offset(w * 0.22f, h * 0.32f), Offset(w * 0.52f, h * 0.24f), Offset(w * 0.80f, h * 0.30f))
        carrotOffsets.forEach { c ->
            // Cuống lá xanh
            drawLine(Color(0xFF4CAF50), c, Offset(c.x - 7f, c.y - 14f), strokeWidth = 4f, cap = StrokeCap.Round)
            drawLine(Color(0xFF4CAF50), c, Offset(c.x + 7f, c.y - 14f), strokeWidth = 4f, cap = StrokeCap.Round)
            drawLine(Color(0xFF81C784), c, Offset(c.x, c.y - 18f), strokeWidth = 4f, cap = StrokeCap.Round)

            // Củ cà rốt cam
            val carrot = Path().apply {
                moveTo(c.x - 10f, c.y)
                lineTo(c.x + 10f, c.y)
                lineTo(c.x, c.y + 24f)
                close()
            }
            drawPath(carrot, color = Color(0xFFFF6D00))
        }
    }
}
