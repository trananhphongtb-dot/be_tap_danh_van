package vn.danhvan.tieuhoc.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KidBlue = Color(0xFF1E88E5)
val KidOrange = Color(0xFFFF8F00)
val KidGreen = Color(0xFF43A047)
val KidRed = Color(0xFFE53935)
val KidCream = Color(0xFFFFF8E7)
val KidYellow = Color(0xFFFFCA28)
// Bảng lớp học cho chặng 3: nền bảng xanh đậm, khung bảng màu gỗ,
// chữ trắng trên bảng. Không dùng hình trang sách cho chặng 3.
val KidBoard = Color(0xFF1B5E20)
val KidBoardFrame = Color(0xFF8D6E63)

@Composable
fun DanhVanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = KidBlue,
            secondary = KidOrange,
            background = KidCream,
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = Color(0xFF212121),
            onSurface = Color(0xFF212121),
        ),
        content = content,
    )
}
