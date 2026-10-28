package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.data.ProgressStore
import vn.danhvan.tieuhoc.data.StageInfo

/**
 * Màn hình chính: mở app là vào luôn, KHÔNG màn hình đăng nhập, KHÔNG tài khoản.
 * Danh sách 7 chặng từ dễ đến khó. Chặng sau mở khi chặng trước đủ 10 điểm.
 * Muốn mở hết thì đổi UnlockRules.UNLOCK_ALL thành true (xem GameRules.kt).
 */
@Composable
fun HomeScreen(
    stages: List<StageInfo>,
    store: ProgressStore,
    onStage: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Đánh Vần", fontSize = 40.sp, fontWeight = FontWeight.Black)
        Text(
            "Chơi mà học đánh vần, không cần đăng nhập.",
            fontSize = 22.sp,
            color = Color(0xFF555555),
        )
        Spacer(modifier = Modifier.height(4.dp))
        val displayStages = if (stages.size >= 7) stages.sortedBy { it.number }.take(7) else listOf(
            StageInfo(1, "Nghe chọn đúng từ", ""),
            StageInfo(2, "Hình chọn đúng từ", ""),
            StageInfo(3, "Đoạn văn dài", ""),
            StageInfo(4, "Ghép chữ và dấu theo giọng đọc", ""),
            StageInfo(5, "Bắt chữ rơi", ""),
            StageInfo(6, "Nghe câu 3 từ chọn đoạn chữ đúng", ""),
            StageInfo(7, "Nhìn hình tự xếp chữ", ""),
        )
        for (stage in displayStages) {
            val unlocked = store.isUnlocked(stage.number)
            val passed = store.isPassed(stage.number)
            val label = when {
                passed -> "✓ Chặng ${stage.number}: ${stage.ten}"
                unlocked -> "Chặng ${stage.number}: ${stage.ten}"
                else -> "🔒 Chặng ${stage.number}: ${stage.ten} (cần qua chặng trước)"
            }
            BigButton(
                text = label,
                onClick = { if (unlocked) onStage(stage.number) },
                enabled = unlocked,
                color = if (passed) KidGreen else if (unlocked) KidBlue else Color(0xFF9E9E9E),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Luật chung: mỗi chặng cần 10 điểm để qua. Đúng +1 điểm, sai 2 câu trừ 1 điểm ở câu sai thứ 2.",
            fontSize = 20.sp,
            color = Color(0xFF555555),
        )
        Text(
            "App chạy offline trên máy, không quảng cáo, không mua trong app, không chat tự do.",
            fontSize = 20.sp,
            color = Color(0xFF555555),
        )
    }
}
