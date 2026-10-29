package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.NoRepeatPicker
import vn.danhvan.tieuhoc.data.VietnameseTones
import vn.danhvan.tieuhoc.data.WordBank

/**
 * Chặng 3, SỬA 2026-10-29:
 * - Đáp án hiện ở 1 Ô CHO 2 TỪ LUÔN: mỗi đáp án là 1 ô chứa cả 2 từ,
 *   ví dụ "bà, gà", không còn chọn lẻ từng từ rồi bấm Kiểm tra nữa.
 *   Bấm 1 ô là chấm luôn câu đó, và dừng đọc luôn.
 * - Đoạn văn trên bảng như cũ, giọng đọc tiếng Việt đọc câu hỏi + đoạn văn.
 * - Không lặp câu hỏi: dùng NoRepeatPicker.pickAt theo lịch sử câu hỏi,
 *   câu đã dùng trong lần thi này không hiện lại cho đến khi hết ngân hàng.
 */
@Composable
fun Stage3Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    questionIndex: Int,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    val pick = remember { picker.pickAt(3, questionIndex, WordBank.passages) { it.id } }
    LaunchedEffect(pick?.newCycle) { if (pick?.newCycle == true) onNewCycle() }
    val passage = pick?.item
    if (passage == null) {
        Text("Chưa có đoạn văn nào.", fontSize = 24.sp)
        return
    }
    val speakText = "Đoạn văn có 2 từ bé đọc được. Bé tìm 2 từ đó nhé! " + passage.doan
    if (!reviewOnly) {
        LaunchedEffect(passage.id) { tts.speak(speakText) }
    }
    var answered by remember { mutableStateOf(false) }
    var pickedOpt by remember { mutableStateOf<String?>(null) }
    // Mỗi đáp án là 1 cặp 2 từ: cặp đúng + 3 cặp gây nhiễu ghép từ 2 từ sai.
    val options = remember(passage.id) {
        val correctPair = passage.dapAn.joinToString(", ")
        val wrongs = if (passage.luaChonSai.size >= 2) {
            passage.luaChonSai.take(2)
        } else {
            WordBank.words.map { it.tu }.filter { it !in passage.dapAn }.shuffled().take(2)
        }
        val pairs = mutableListOf(correctPair)
        if (wrongs.size >= 2) {
            pairs += "${passage.dapAn[0]}, ${wrongs[0]}"
            pairs += "${passage.dapAn[1]}, ${wrongs[1]}"
            pairs += "${wrongs[0]}, ${wrongs[1]}"
        }
        pairs.shuffled()
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(KidBoard)
                .border(8.dp, KidBoardFrame, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(passage.doan, fontSize = 24.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
        Spacer(modifier = Modifier.weight(1f))
        SpeakButton(textToSpeak = speakText, tts = tts)
        Spacer(modifier = Modifier.height(12.dp))
        AnswerGrid2x2(
            options = options,
            enabled = !answered && !reviewOnly,
            selected = pickedOpt,
            correctOption = passage.dapAn.joinToString(", "),
            answered = answered || reviewOnly,
            onPick = { opt ->
                if (!answered && !reviewOnly) {
                    answered = true
                    pickedOpt = opt
                    tts.stop()
                    onAnswer(opt == passage.dapAn.joinToString(", "), passage.dapAn.joinToString(", "))
                }
            },
        )
    }
}

/**
 * Bộ ô chữ MỚI 2026-10-29, đúng 8 ô, không còn 9 ô:
 * - 3 phụ âm (1 đúng + 2 gây nhiễu).
 * - 3 nguyên âm gốc (1 đúng + 2 gây nhiễu).
 * - 2 dấu (từ có dấu: dấu đúng + 1 dấu gây nhiễu; từ không dấu: 2 dấu gây
 *   nhiễu, đúng là KHÔNG chọn dấu nào, không còn ô "không dấu").
 * Dấu hiển thị dạng KÝ HIỆU DẤU (◌̀, ◌́...), không phải chữ "huyền", "sắc".
 */
data class TileSet(
    val consonants: List<String>,
    val vowels: List<String>,
    val tones: List<String>,
    val correctConsonant: String,
    val correctVowel: String,
    val correctTone: String?,
    // 8 ô đã xáo trộn để xếp thành 2 hàng ngang, mỗi ô biết nó thuộc nhóm nào.
    val allTiles: List<Tile>,
)

data class Tile(val text: String, val kind: TileKind, val toneName: String? = null)

enum class TileKind { CONSONANT, VOWEL, TONE }

fun makeTileSet(wordTu: String, phuAm: String, nguyenAmCoDau: String): TileSet {
    val (baseVowel, tone) = VietnameseTones.decompose(nguyenAmCoDau)
    val correctTone: String? = if (tone == "không dấu") null else tone
    val consonants = shuffledOptions(
        phuAm,
        distractWords(phuAm, WordBank.initials, 2),
    )
    val vowels = shuffledOptions(
        baseVowel,
        distractWords(baseVowel, VietnameseTones.BASE_VOWELS, 2),
    )
    val tones: List<String> = if (correctTone != null) {
        shuffledOptions(
            correctTone,
            distractWords(correctTone, VietnameseTones.TONED_TONE_NAMES, 1),
        )
    } else {
        VietnameseTones.TONED_TONE_NAMES.shuffled().take(2)
    }
    check(consonants.size == 3 && vowels.size == 3 && tones.size == 2) {
        "Bộ ô chữ phải đúng 8 ô (3 + 3 + 2), đang có ${consonants.size} + ${vowels.size} + ${tones.size}"
    }
    val all = mutableListOf<Tile>()
    all += consonants.map { Tile(it, TileKind.CONSONANT) }
    all += vowels.map { Tile(it, TileKind.VOWEL) }
    all += tones.map { Tile(VietnameseTones.TONE_MARKS[it] ?: it, TileKind.TONE, it) }
    return TileSet(consonants, vowels, tones, phuAm, baseVowel, correctTone, all.shuffled())
}

/**
 * 2 Ô TO ở trên cùng, dùng CHUNG cho chặng 4 và chặng 7.
 * Ô 1 = phụ âm, ô 2 = nguyên âm kèm dấu (nếu có chọn dấu).
 * Chạm ô to đã đầy thì xóa ô đó để sửa lại, giống trước.
 */
@Composable
fun AssembleBoxes(
    selConsonant: String?,
    selVowel: String?,
    selTone: String?,
    enabled: Boolean = true,
    onClearConsonant: () -> Unit = {},
    onClearVowel: () -> Unit = {},
    resultColor: Color? = null,
) {
    val vowelWithTone = when {
        selVowel == null -> ""
        selTone == null -> selVowel
        else -> VietnameseTones.apply(selVowel, selTone)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier.weight(1f).height(72.dp)
                .shadow(4.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(KidYellow)
                .border(4.dp, KidOrange, RoundedCornerShape(18.dp))
                .clickable(enabled = enabled && selConsonant != null) { onClearConsonant() },
            contentAlignment = Alignment.Center,
        ) {
            Text(selConsonant ?: "", fontSize = 40.sp, fontWeight = FontWeight.Black)
        }
        Text("+", fontSize = 32.sp, fontWeight = FontWeight.Black)
        Box(
            modifier = Modifier.weight(1f).height(72.dp)
                .shadow(4.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(KidYellow)
                .border(4.dp, KidOrange, RoundedCornerShape(18.dp))
                .clickable(enabled = enabled && selVowel != null) { onClearVowel() },
            contentAlignment = Alignment.Center,
        ) {
            Text(vowelWithTone, fontSize = 40.sp, fontWeight = FontWeight.Black)
        }
    }
}

/**
 * Giao diện ghép chữ MỚI 2026-10-29, dùng chung chặng 4 và 7:
 * - 8 ô chữ và dấu xếp thành 2 HÀNG NGANG, không còn chia 3 hàng theo nhóm.
 * - Nút "✓ Hoàn thành" nằm CUỐI HÀNG 2, cùng hàng với các ô cuối.
 * - KHÔNG cần chọn đủ, KHÔNG cần chọn "không dấu": bấm ✓ lúc nào cũng được,
 *   nếu xếp thiếu hoặc sai thì tính là 1 câu sai (không phải chờ chọn đủ mới bấm).
 * - KHÔNG còn đoạn hướng dẫn dài hiện mọi lúc trong màn: luật chỉ đọc 1 lần
 *   trước khi vào thi ở màn hình chính, trong màn chỉ có ô ghép và ô chữ.
 * - Bấm lại ô đã chọn thì ô đó mất để sửa lại, như trước.
 */
@Composable
fun AssembleGame(
    tts: VietnameseTts,
    wordTu: String,
    phuAm: String,
    nguyenAmCoDau: String,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
    prompt: @Composable () -> Unit,
    speakButton: @Composable () -> Unit = {},
) {
    var selConsonant by remember { mutableStateOf<String?>(null) }
    var selVowel by remember { mutableStateOf<String?>(null) }
    var selTone by remember { mutableStateOf<String?>(null) }
    var checked by remember { mutableStateOf(false) }
    var wasCorrect by remember { mutableStateOf<Boolean?>(null) }
    val tiles = remember(wordTu) { makeTileSet(wordTu, phuAm, nguyenAmCoDau) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        prompt()
        Spacer(modifier = Modifier.height(10.dp))
        AssembleBoxes(
            selConsonant, selVowel, selTone,
            enabled = !checked && !reviewOnly,
            onClearConsonant = { selConsonant = null },
            onClearVowel = { selVowel = null },
            resultColor = if (checked) (if (wasCorrect == true) KidGreen else KidRed) else null,
        )
        Spacer(modifier = Modifier.height(10.dp))
        speakButton()
        Spacer(modifier = Modifier.weight(1f))
        // Hàng 1: 4 ô đầu. Hàng 2: 4 ô sau + nút ✓ ở cuối hàng.
        val row1 = tiles.allTiles.take(4)
        val row2 = tiles.allTiles.drop(4)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            for (tile in row1) {
                val isSelected = when (tile.kind) {
                    TileKind.CONSONANT -> tile.text == selConsonant
                    TileKind.VOWEL -> tile.text == selVowel
                    TileKind.TONE -> tile.toneName == selTone
                }
                BigButton(
                    text = tile.text,
                    onClick = {
                        when (tile.kind) {
                            TileKind.CONSONANT -> selConsonant = if (tile.text == selConsonant) null else tile.text
                            TileKind.VOWEL -> selVowel = if (tile.text == selVowel) null else tile.text
                            TileKind.TONE -> selTone = if (tile.toneName == selTone) null else tile.toneName
                        }
                    },
                    enabled = !checked && !reviewOnly,
                    color = if (isSelected) KidGreen else KidBlue,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            for (tile in row2) {
                val isSelected = when (tile.kind) {
                    TileKind.CONSONANT -> tile.text == selConsonant
                    TileKind.VOWEL -> tile.text == selVowel
                    TileKind.TONE -> tile.toneName == selTone
                }
                BigButton(
                    text = tile.text,
                    onClick = {
                        when (tile.kind) {
                            TileKind.CONSONANT -> selConsonant = if (tile.text == selConsonant) null else tile.text
                            TileKind.VOWEL -> selVowel = if (tile.text == selVowel) null else tile.text
                            TileKind.TONE -> selTone = if (tile.toneName == selTone) null else tile.toneName
                        }
                    },
                    enabled = !checked && !reviewOnly,
                    color = if (isSelected) KidGreen else KidBlue,
                    modifier = Modifier.weight(1f),
                )
            }
            // Nút hoàn thành nằm cuối hàng 2, luôn bấm được, thiếu cũng tính sai.
            BigButton(
                text = "✓",
                onClick = {
                    if (!checked && !reviewOnly) {
                        checked = true
                        tts.stop()
                        val assembled = if (selConsonant != null && selVowel != null) {
                            selConsonant!! + VietnameseTones.apply(selVowel!!, selTone ?: "không dấu")
                        } else {
                            ""
                        }
                        // Từ không dấu: đúng khi không chọn dấu nào. Từ có dấu:
                        // đúng khi dấu chọn đúng. Thiếu phụ âm/nguyên âm là sai.
                        wasCorrect = assembled == wordTu
                        onAnswer(assembled == wordTu, wordTu)
                    }
                },
                enabled = !checked && !reviewOnly,
                color = KidOrange,
                modifier = Modifier.weight(1f),
            )
        }
        // CHOT v13: khong hien chu dung/sai, chi mau o ghep + am thanh.
    }
}

/** Chặng 4: Ghép chữ + dấu theo giọng đọc. */
@Composable
fun Stage4Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    questionIndex: Int,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    val pick = remember { picker.pickAt(4, questionIndex, WordBank.words) { it.id } }
    LaunchedEffect(pick?.newCycle) { if (pick?.newCycle == true) onNewCycle() }
    val word = pick?.item
    if (word == null) {
        Text("Chưa có từ nào.", fontSize = 24.sp)
        return
    }
    if (!reviewOnly) {
        LaunchedEffect(word.id) { tts.speak(word.phatAm) }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        AssembleGame(
            tts = tts,
            wordTu = word.tu,
            phuAm = word.phuAm,
            nguyenAmCoDau = word.nguyenAmCoDau,
            reviewOnly = reviewOnly,
            onAnswer = onAnswer,
            prompt = {},
            speakButton = { SpeakButton(textToSpeak = word.phatAm, tts = tts) },
        )
    }
}

/**
 * Chặng 7: Nhìn hình tự xếp chữ.
 * - Giao diện xếp lại như chặng 4 (8 ô, 2 hàng, nút ✓ cuối hàng 2).
 * - Sai hay đúng, sau khi bấm ✓ đều ĐỌC LẠI ĐÁP ÁN 1 lần trước khi sang câu mới,
 *   và hiện đáp án đúng ở dưới ô hình (xem AssembleGame: dòng đáp án đúng).
 *   Nhiều hình khó đoán nên bé cần nghe lại từ đúng, không chỉ nhìn chữ.
 */
@Composable
fun Stage7Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    questionIndex: Int,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    val pick = remember { picker.pickAt(7, questionIndex, WordBank.pictureWords) { it.id } }
    LaunchedEffect(pick?.newCycle) { if (pick?.newCycle == true) onNewCycle() }
    val word = pick?.item
    if (word == null) {
        Text("Chưa có từ có hình nào.", fontSize = 24.sp)
        return
    }
    // Chỉ đọc hướng dẫn, không đọc từ đáp án (đọc từ sẽ lộ đáp án).
    if (!reviewOnly) {
        LaunchedEffect(word.id) { tts.speak("Hãy nhìn hình và xếp chữ thành từ") }
    }
    var spokenAnswer by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.shadow(6.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(Color.White).padding(8.dp)) {
            WordImage(wordId = word.id, modifier = Modifier.size(180.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        AssembleGame(
            tts = tts,
            wordTu = word.tu,
            phuAm = word.phuAm,
            nguyenAmCoDau = word.nguyenAmCoDau,
            reviewOnly = reviewOnly,
            onAnswer = { correct, correctText ->
                onAnswer(correct, correctText)
                // Sau khi chấm, đọc lại đáp án 1 lần, đúng hay sai đều đọc.
                if (!spokenAnswer) {
                    spokenAnswer = true
                    tts.speak(correctText)
                }
            },
            prompt = {},
        )
    }
}
