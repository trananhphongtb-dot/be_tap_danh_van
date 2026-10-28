package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.NoRepeatPicker
import vn.danhvan.tieuhoc.data.VietnameseTones
import vn.danhvan.tieuhoc.data.WordBank

/**
 * Chặng 3: Đoạn văn dài (từ 10 từ trở lên, đúng 2 từ đọc được).
 * SỬA THEO NGƯỜI DÙNG 2026-10-29, KHÔNG dùng bản mô tả cũ cho chặng này:
 * - Hiện đoạn văn trên 1 BẢNG (nền bảng xanh + khung bảng), KHÔNG dùng
 *   hình trang sách, KHÔNG có hình sách mở ở đây.
 * - Giọng đọc tiếng Việt đọc đoạn văn + câu hỏi ("Đoạn văn có 2 từ bé
 *   đọc được..."), có nút "Nghe lại" phát lại đúng câu đó.
 * - Bé CHỌN 2 từ trong các lựa chọn, KHÔNG bấm trực tiếp vào từ trong đoạn:
 *   đoạn trên bảng là Text thường, không có Modifier.clickable, không chọn được.
 * Bấm "Kiểm tra" để chấm 1 lần cho cả câu.
 */
@Composable
fun Stage3Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    val pick = remember { picker.pick(3, WordBank.passages) { it.id } }
    LaunchedEffect(pick?.newCycle) { if (pick?.newCycle == true) onNewCycle() }
    val passage = pick?.item
    if (passage == null) {
        Text("Chưa có đoạn văn nào.", fontSize = 24.sp)
        return
    }
    // Giọng đọc đọc cả câu hỏi lẫn đoạn văn, nút "Nghe lại" phát lại đúng câu này.
    val speakText = "Đoạn văn có 2 từ bé đọc được. Bé tìm 2 từ đó nhé! " + passage.doan
    LaunchedEffect(passage.id) { tts.speak(speakText) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var checked by remember { mutableStateOf(false) }
    val options = remember(passage.id) {
        val distractors = if (passage.luaChonSai.size >= 2) {
            passage.luaChonSai.take(2)
        } else {
            // Tránh lấy trùng 1 trong 2 từ đáp án đúng làm gây nhiễu.
            WordBank.words.map { it.tu }.filter { it !in passage.dapAn }.shuffled().take(2)
        }
        (passage.dapAn + distractors).shuffled()
    }
    Column {
        Text("Đoạn văn có 2 từ bé đọc được. Bé tìm 2 từ đó nhé!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        // BẢNG: đoạn văn hiện trong 1 bảng duy nhất, không phải trang sách.
        // Text trong bảng không bấm được — đáp án chỉ chọn ở các nút bên dưới.
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(KidBoard)
                .border(8.dp, KidBoardFrame, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Text(passage.doan, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
        SpeakButton(textToSpeak = speakText, tts = tts)
        Spacer(modifier = Modifier.height(20.dp))
        Text("Chọn đúng 2 từ:", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (opt in options) {
                val isSelected = opt in selected
                BigButton(
                    text = if (isSelected) "✓ $opt" else opt,
                    onClick = {
                        if (!checked) {
                            selected = if (isSelected) selected - opt else {
                                // Chỉ cho chọn tối đa 2 từ.
                                if (selected.size < 2) selected + opt else selected
                            }
                        }
                    },
                    enabled = !checked,
                    color = if (isSelected) KidGreen else KidBlue,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        BigButton(
            text = "Kiểm tra",
            onClick = {
                if (!checked) {
                    checked = true
                    onAnswer(selected == passage.dapAn.toSet(), passage.dapAn.joinToString(", "))
                }
            },
            enabled = !checked && selected.size == 2,
            color = KidOrange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Tạo bộ ô chữ cho 1 từ: ĐÚNG 9 ô — 3 phụ âm, 3 nguyên âm gốc, 3 dấu,
 * mỗi bộ có 1 ô đúng + 2 ô gây nhiễu. Dùng chung cho chặng 4 và chặng 7;
 * 2 chặng chỉ khác gợi ý (chặng 4 nghe giọng đọc, chặng 7 nhìn hình).
 * Từ không dấu thì dấu đúng là "không dấu".
 */
data class TileSet(
    val consonants: List<String>,
    val vowels: List<String>,
    val tones: List<String>,
    val correctConsonant: String,
    val correctVowel: String,
    val correctTone: String,
)

fun makeTileSet(wordTu: String, phuAm: String, nguyenAmCoDau: String): TileSet {
    val (baseVowel, tone) = VietnameseTones.decompose(nguyenAmCoDau)
    val consonants = shuffledOptions(
        phuAm,
        distractWords(phuAm, WordBank.initials, 2),
    )
    val vowels = shuffledOptions(
        baseVowel,
        distractWords(baseVowel, VietnameseTones.BASE_VOWELS, 2),
    )
    val tones = shuffledOptions(
        tone,
        distractWords(tone, VietnameseTones.TONE_NAMES, 2),
    )
    // Khóa đúng 9 ô: nếu 1 bộ thiếu / thừa ô thì lỗi ngay lúc chạy,
    // thay vì im lặng hiện sai số ô cho bé.
    check(consonants.size == 3 && vowels.size == 3 && tones.size == 3) {
        "Bộ ô chữ phải đúng 9 ô (3 + 3 + 3), đang có ${consonants.size} + ${vowels.size} + ${tones.size}"
    }
    return TileSet(consonants, vowels, tones, phuAm, baseVowel, tone)
}

/**
 * Một hàng ô chữ. Việc "bấm lại thì mất" do nơi gọi xử lý trong lambda
 * onSelect: chạm lại ô đã chọn (tile == selected) thì đặt lựa chọn về null,
 * thay vì giữ nguyên — xem Stage4Content / Stage7Content.
 */
@Composable
fun TileRow(
    title: String,
    tiles: List<String>,
    selected: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (tile in tiles) {
            BigButton(
                text = tile,
                onClick = { onSelect(tile) },
                enabled = enabled,
                color = if (tile == selected) KidGreen else KidBlue,
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

/**
 * 2 Ô TO ở trên cùng của phần ghép chữ, dùng CHUNG cho chặng 4 và chặng 7
 * để 2 chặng không lệch nhau (sửa 2026-10-29 theo người dùng).
 * - Ô to 1 = phụ âm đã chọn, chưa chọn thì trống.
 * - Ô to 2 = nguyên âm đã chọn kèm dấu đã chọn, dùng VietnameseTones.apply
 *   (không tự viết bảng dấu mới). Chưa chọn nguyên âm thì trống; chưa chọn
 *   dấu thì nguyên âm trơn (ví dụ chọn o chưa chọn dấu thì ô hiện "o").
 * - Chạm 1 ô chữ ở dưới là chữ đó "nhảy lên" ô to tương ứng: phụ âm -> ô 1,
 *   nguyên âm -> ô 2, dấu -> tự điền vào nguyên âm trong ô 2 (o + huyền = ò,
 *   a + sắc = á, "không dấu" thì nguyên âm giữ nguyên). Chạm ô khác cùng nhóm
 *   thì thay thế ô to đó, vì mỗi nhóm chỉ giữ 1 lựa chọn (selConsonant /
 *   selVowel / selTone) — không cần logic riêng cho việc thay thế.
 * - Hàng Dấu LUÔN hiện đủ 3 ô, kể cả từ không dấu: lúc đó dấu đúng là
 *   "không dấu" + 2 dấu gây nhiễu (xem makeTileSet, check 3 + 3 + 3),
 *   và bước chọn dấu không bao giờ bị bỏ qua — nút "✓ Hoàn thành" chỉ bật khi
 *   đã chọn đủ phụ âm + nguyên âm + dấu.
 * - Bấm lại thì mất (sửa 2026-10-29): chạm trực tiếp vào ô to 1 đã đầy thì
 *   xóa phụ âm; chạm vào ô to 2 đã đầy thì xóa nguyên âm, dấu đã chọn vẫn
 *   giữ để áp cho nguyên âm mới (trừ khi bé chạm lại ô dấu, lúc đó dấu mất
 *   và nguyên âm trở về dạng trơn). Sau khi chấm (checked) thì các ô to
 *   không bấm được nữa.
 */
@Composable
fun AssembleBoxes(
    selConsonant: String?,
    selVowel: String?,
    selTone: String?,
    enabled: Boolean = true,
    onClearConsonant: () -> Unit = {},
    onClearVowel: () -> Unit = {},
) {
    // Ô 2: chưa có nguyên âm thì trống; có nguyên âm mà chưa có dấu thì trơn;
    // có cả dấu thì ghép bằng VietnameseTones.apply (o + huyền = ò).
    val vowelWithTone = when {
        selVowel == null -> ""
        selTone == null -> selVowel
        else -> VietnameseTones.apply(selVowel, selTone)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Ô to 1: phụ âm. Chạm vào ô đã đầy thì xóa phụ âm (khi chưa chấm).
        Box(
            modifier = Modifier.weight(1f).height(96.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(KidYellow)
                .border(4.dp, KidOrange, RoundedCornerShape(20.dp))
                .clickable(enabled = enabled && selConsonant != null) { onClearConsonant() },
            contentAlignment = Alignment.Center,
        ) {
            Text(selConsonant ?: "", fontSize = 48.sp, fontWeight = FontWeight.Black)
        }
        Text("+", fontSize = 40.sp, fontWeight = FontWeight.Black)
        // Ô to 2: nguyên âm + dấu tự điền vào. Chạm vào ô đã đầy thì chỉ xóa
        // nguyên âm; dấu đã chọn vẫn giữ để áp cho nguyên âm mới.
        Box(
            modifier = Modifier.weight(1f).height(96.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(KidYellow)
                .border(4.dp, KidOrange, RoundedCornerShape(20.dp))
                .clickable(enabled = enabled && selVowel != null) { onClearVowel() },
            contentAlignment = Alignment.Center,
        ) {
            Text(vowelWithTone, fontSize = 48.sp, fontWeight = FontWeight.Black)
        }
    }
    Text(
        "Chạm vào ô to đã đầy, hoặc chạm lại ô chữ đã chọn, để xóa và sửa lại.",
        fontSize = 18.sp,
    )
    Spacer(modifier = Modifier.height(16.dp))
}

/**
 * Chặng 4: Ghép chữ + dấu theo giọng đọc.
 * SỬA THEO NGƯỜI DÙNG 2026-10-29, KHÔNG dùng bản mô tả cũ cho chặng này:
 * Đúng 9 ô — 3 phụ âm (1 đúng + 2 gây nhiễu), 3 nguyên âm (1 đúng + 2 gây
 * nhiễu), 3 dấu (1 đúng + 2 gây nhiễu). Bé chọn 1 phụ âm + 1 nguyên âm + 1 dấu.
 * Cách xếp này GIỐNG chặng 7, chỉ khác gợi ý: chặng 4 nghe giọng đọc,
 * chặng 7 nhìn hình. Từ không dấu thì dấu đúng là ô "không dấu".
 */
@Composable
fun Stage4Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    val pick = remember { picker.pick(4, WordBank.words) { it.id } }
    LaunchedEffect(pick?.newCycle) { if (pick?.newCycle == true) onNewCycle() }
    val word = pick?.item
    if (word == null) {
        Text("Chưa có từ nào.", fontSize = 24.sp)
        return
    }
    LaunchedEffect(word.id) { tts.speak(word.phatAm) }
    var selConsonant by remember { mutableStateOf<String?>(null) }
    var selVowel by remember { mutableStateOf<String?>(null) }
    var selTone by remember { mutableStateOf<String?>(null) }
    var checked by remember { mutableStateOf(false) }
    val tiles = remember(word.id) { makeTileSet(word.tu, word.phuAm, word.nguyenAmCoDau) }
    Column {
        Text("Nghe rồi ghép chữ thành từ nhé!", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        SpeakButton(textToSpeak = word.phatAm, tts = tts)
        Spacer(modifier = Modifier.height(20.dp))
        Text("9 ô chữ — chọn 1 phụ âm + 1 nguyên âm + 1 dấu:", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        // 2 ô to ở trên: chạm ô dưới nào thì chữ đó nhảy lên ô to tương ứng.
        // Bấm lại thì mất: chạm lại ô đã chọn thì lựa chọn đó về null, ô to
        // trống lại và nút "✓ Hoàn thành" tắt cho đến khi chọn đủ lại.
        // Chưa bấm nút tích thì chưa tính điểm, dù đã chọn đủ 3 ô.
        AssembleBoxes(
            selConsonant, selVowel, selTone,
            enabled = !checked,
            onClearConsonant = { selConsonant = null },
            onClearVowel = { selVowel = null },
        )
        TileRow("Phụ âm:", tiles.consonants, selConsonant, !checked) {
            selConsonant = if (it == selConsonant) null else it
        }
        TileRow("Nguyên âm:", tiles.vowels, selVowel, !checked) {
            selVowel = if (it == selVowel) null else it
        }
        // Hàng Dấu luôn hiện đủ 3 ô, kể cả từ không dấu (dấu đúng = "không dấu").
        // Chạm lại dấu đã chọn thì dấu mất, nguyên âm trong ô to 2 về dạng trơn.
        TileRow("Dấu:", tiles.tones, selTone, !checked) {
            selTone = if (it == selTone) null else it
        }
        val ready = selConsonant != null && selVowel != null && selTone != null
        val assembled = if (ready) {
            selConsonant!! + VietnameseTones.apply(selVowel!!, selTone!!)
        } else {
            ""
        }
        Spacer(modifier = Modifier.height(12.dp))
        BigButton(
            text = "✓ Hoàn thành",
            onClick = {
                // Chỉ bấm nút tích này mới gọi onAnswer 1 lần và tính điểm.
                if (!checked && ready) {
                    checked = true
                    onAnswer(assembled == word.tu, word.tu)
                }
            },
            enabled = !checked && ready,
            color = KidOrange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Chặng 7: Nhìn hình tự xếp chữ (chặng mới chốt, ở cuối sau chặng 6).
 * Hiện hình, KHÔNG đọc từ lên (đọc từ sẽ lộ đáp án), chỉ đọc hướng dẫn chung.
 * Bé tự xếp từ từ 3 phụ âm + 3 nguyên âm + 3 dấu, giống chặng 4 nhưng gợi ý là hình.
 */
@Composable
fun Stage7Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    val pick = remember { picker.pick(7, WordBank.pictureWords) { it.id } }
    LaunchedEffect(pick?.newCycle) { if (pick?.newCycle == true) onNewCycle() }
    val word = pick?.item
    if (word == null) {
        Text("Chưa có từ có hình nào.", fontSize = 24.sp)
        return
    }
    // Chỉ đọc hướng dẫn, không đọc từ đáp án.
    LaunchedEffect(word.id) { tts.speak("Hãy nhìn hình và xếp chữ thành từ") }
    var selConsonant by remember { mutableStateOf<String?>(null) }
    var selVowel by remember { mutableStateOf<String?>(null) }
    var selTone by remember { mutableStateOf<String?>(null) }
    var checked by remember { mutableStateOf(false) }
    val tiles = remember(word.id) { makeTileSet(word.tu, word.phuAm, word.nguyenAmCoDau) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nhìn hình rồi tự xếp chữ nhé!", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        WordImage(wordId = word.id, modifier = Modifier.size(260.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            // 2 ô to ở trên, dùng chung composable với chặng 4.
            // Bấm lại thì mất, giống chặng 4; chưa bấm nút tích thì chưa tính điểm.
            AssembleBoxes(
                selConsonant, selVowel, selTone,
                enabled = !checked,
                onClearConsonant = { selConsonant = null },
                onClearVowel = { selVowel = null },
            )
            TileRow("Phụ âm:", tiles.consonants, selConsonant, !checked) {
                selConsonant = if (it == selConsonant) null else it
            }
            TileRow("Nguyên âm:", tiles.vowels, selVowel, !checked) {
                selVowel = if (it == selVowel) null else it
            }
            // Hàng Dấu luôn hiện đủ 3 ô, kể cả từ không dấu (dấu đúng = "không dấu").
            // Chạm lại dấu đã chọn thì dấu mất, nguyên âm trong ô to 2 về dạng trơn.
            TileRow("Dấu:", tiles.tones, selTone, !checked) {
                selTone = if (it == selTone) null else it
            }
            val ready = selConsonant != null && selVowel != null && selTone != null
            val assembled = if (ready) {
                selConsonant!! + VietnameseTones.apply(selVowel!!, selTone!!)
            } else {
                ""
            }
            Spacer(modifier = Modifier.height(12.dp))
            BigButton(
                text = "✓ Hoàn thành",
                onClick = {
                    // Chỉ bấm nút tích này mới gọi onAnswer 1 lần và tính điểm.
                    if (!checked && ready) {
                        checked = true
                        onAnswer(assembled == word.tu, word.tu)
                    }
                },
                enabled = !checked && ready,
                color = KidOrange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
