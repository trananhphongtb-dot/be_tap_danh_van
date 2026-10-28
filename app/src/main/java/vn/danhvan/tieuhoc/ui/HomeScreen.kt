package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.ProgressStore
import vn.danhvan.tieuhoc.data.StageInfo

/**
 * Màn hình chính, SỬA LỚN 2026-10-29 theo góp ý sau khi anh chạy thử:
 * - Không còn danh sách nút dọc xấu cũ. Thay bằng ĐƯỜNG ZICZÁC: các chặng
 *   xếp so le trái / phải dọc theo 1 con đường, như đường đua.
 * - Đầu đường là 1 bé trai tập chạy (🏃), cuối đường là cúp vàng (🏆).
 * - Chặng chưa qua thì KHÓA (🔒, nút xám, bấm không vào được).
 * - Bấm vào chặng đã mở thì HIỆN LUẬT CHƠI của chặng đó và ĐỌC LUẬT BẰNG
 *   TIẾNG VIỆT, rồi mới có nút "Vào thi". Luật chỉ nói cách chơi, KHÔNG
 *   nói luật tính điểm (không nhắc 10 điểm, trừ điểm...).
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

@Composable
fun HomeScreen(
    stages: List<StageInfo>,
    store: ProgressStore,
    tts: VietnameseTts,
    onStage: (Int) -> Unit,
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("👦 Đánh Vần", fontSize = 36.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text(
            "Bé chạy theo đường, qua từng chặng nhé!",
            fontSize = 20.sp,
            color = Color(0xFF555555),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Đầu đường: bé trai tập chạy.
        Text("🏃 Bé bắt đầu chạy ở đây", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("↓", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        // Đường ziczác: chặng lẻ lệch trái, chặng chẵn lệch phải.
        for (stage in displayStages) {
            val unlocked = store.isUnlocked(stage.number)
            val passed = store.isPassed(stage.number)
            val alignLeft = stage.number % 2 == 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (alignLeft) Arrangement.Start else Arrangement.End,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (passed) KidGreen else if (unlocked) KidBlue else Color(0xFF9E9E9E))
                        .clickable {
                            if (unlocked) {
                                selectedStage = stage
                            } else {
                                lockedMessage = "Chặng ${stage.number} đang khóa. Bé qua chặng trước đã nhé!"
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        if (passed) "✓" else if (unlocked) "${stage.number}" else "🔒",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                    Text(
                        stage.ten,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            // Đoạn đường nối xuống chặng sau, trừ chặng cuối.
            if (stage.number < 7) {
                Text(if (alignLeft) "  ↘" else "↙  ", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Cuối đường: cúp vàng.
        Text("🏆", fontSize = 48.sp)
        Text("Cúp vàng ở cuối đường!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "App chạy offline trên máy, không quảng cáo, không mua trong app, không chat tự do.",
            fontSize = 18.sp,
            color = Color(0xFF555555),
            textAlign = TextAlign.Center,
        )
    }

    // Bấm chặng đã mở: hiện luật + đọc luật bằng tiếng Việt, rồi mới vào thi.
    val picked = selectedStage
    if (picked != null) {
        val rule = StageRules[picked.number] ?: picked.moTa
        LaunchedEffect(picked.number) { tts.speak(rule) }
        AlertDialog(
            onDismissRequest = {
                tts.stop()
                selectedStage = null
            },
            title = { Text("Chặng ${picked.number}: ${picked.ten}", fontWeight = FontWeight.Black) },
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
                }) { Text("Đóng") }
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
