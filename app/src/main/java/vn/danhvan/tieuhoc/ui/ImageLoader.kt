package vn.danhvan.tieuhoc.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Hiện hình trong assets/hinh/<id>.webp, ví dụ DV006.webp cho từ bò.
 *  Hình đã nén từ PNG gốc (88MB) sang WebP 800px (3.84MB) ngày 2026-10-29,
 *  PNG gốc vẫn còn ở workspace/research/danh-van/hinh/. */
@Composable
fun WordImage(wordId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(wordId) {
        try {
            context.assets.open("hinh/$wordId.webp").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = "Hình minh họa", modifier = modifier)
    } else {
        Box(
            modifier = modifier.size(200.dp).background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center,
        ) {
            // Hình thiếu: vẫn hiện khung trống, không crash app.
        }
    }
}
