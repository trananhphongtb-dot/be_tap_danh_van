package vn.danhvan.tieuhoc.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import vn.danhvan.tieuhoc.R

/**
 * Âm thanh đáp án riêng biệt, theo yêu cầu người dùng 2026-10-29:
 * đáp án đúng thì 1 âm riêng (correct.wav), sai thì 1 âm khác (wrong.wav).
 * Chỉ phát âm hiệu ứng, không động vào VietnameseTts.kt, không có prefs âm thanh.
 */
class AnswerSounds(context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val correctId: Int = pool.load(context.applicationContext, R.raw.correct, 1)
    private val wrongId: Int = pool.load(context.applicationContext, R.raw.wrong, 1)

    /** Âm riêng cho đáp án đúng. */
    fun playCorrect() {
        pool.play(correctId, 1f, 1f, 1, 0, 1f)
    }

    /** Âm khác cho đáp án sai. */
    fun playWrong() {
        pool.play(wrongId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }
}
