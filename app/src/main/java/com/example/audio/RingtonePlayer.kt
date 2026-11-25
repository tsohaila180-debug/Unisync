package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.example.model.VoiceNoteItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.PI
import kotlin.math.sin

data class RingtoneOption(
    val id: String,
    val label: String,
    val freqs: List<Double>,
    val noteDurationMs: Int
)

object RingtonePlayer {
    val options = listOf(
        RingtoneOption("crystal_chime", "Crystal Chime", listOf(784.0, 988.0, 1175.0, 1568.0), 180),
        RingtoneOption("academic_bell", "Academic Bell", listOf(523.25, 659.25, 783.99, 659.25), 220),
        RingtoneOption("gentle_harp", "Gentle Harp", listOf(440.0, 554.37, 659.25, 880.0), 250),
        RingtoneOption("urgent_pulse", "Urgent Pulse", listOf(880.0, 0.0, 880.0, 1046.5), 140),
        RingtoneOption("digital_radar", "Digital Radar", listOf(1046.5, 1318.5, 1046.5, 1568.0), 150),
        RingtoneOption("soft_piano", "Soft Piano", listOf(392.0, 493.88, 587.33, 783.99), 240)
    )

    private var playbackJob: Job? = null
    @Volatile
    private var activeTrack: AudioTrack? = null

    private fun buildTonePhraseBuffer(
        option: RingtoneOption,
        volumePercent: Int,
        trailingSilenceMs: Int = 0
    ): ShortArray {
        val sampleRate = 22050
        val gain = (volumePercent.coerceIn(0, 100) / 100.0) * 0.50
        val samplesPerNote = (sampleRate * option.noteDurationMs) / 1000
        val melodySamples = samplesPerNote * option.freqs.size
        val silenceSamples = (sampleRate * trailingSilenceMs) / 1000
        val buffer = ShortArray(melodySamples + silenceSamples)
        option.freqs.forEachIndexed { noteIdx, freq ->
            val offset = noteIdx * samplesPerNote
            for (i in 0 until samplesPerNote) {
                val env = when {
                    i < 200 -> i / 200.0
                    i > samplesPerNote - 400 -> (samplesPerNote - i) / 400.0
                    else -> 1.0
                }.coerceIn(0.0, 1.0)
                val sampleVal = if (freq <= 0.0) 0.0 else sin(2.0 * PI * freq * i / sampleRate)
                buffer[offset + i] = (sampleVal * env * gain * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return buffer
    }

    /**
     * Plays a single preview phrase (used by Preview Tone button & Pomodoro phase chime).
     */
    fun playTone(ringtoneId: String, volumePercent: Int, scope: CoroutineScope) {
        stop()
        val option = options.firstOrNull { it.id == ringtoneId } ?: options.first()
        playbackJob = scope.launch(Dispatchers.IO) {
            var track: AudioTrack? = null
            try {
                val sampleRate = 22050
                val buffer = buildTonePhraseBuffer(option, volumePercent, trailingSilenceMs = 0)
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                activeTrack = track
                track.write(buffer, 0, buffer.size)
                track.play()
                val totalDurationMs = option.noteDurationMs.toLong() * option.freqs.size
                delay(totalDurationMs + 120L)
            } catch (e: Exception) {
                Log.w("RingtonePlayer", "Unable to play ringtone preview: ${e.message}")
            } finally {
                releaseTrackSafely(track)
                if (activeTrack === track) activeTrack = null
            }
        }
    }

    /**
     * Continuously loops the selected ringtone phrase until stop() is called by Dismiss or Snooze.
     */
    fun startAlarmLoop(ringtoneId: String, volumePercent: Int, scope: CoroutineScope) {
        stop()
        val option = options.firstOrNull { it.id == ringtoneId } ?: options.first()
        playbackJob = scope.launch(Dispatchers.IO) {
            var track: AudioTrack? = null
            try {
                val sampleRate = 22050
                val phraseBuffer = buildTonePhraseBuffer(option, volumePercent, trailingSilenceMs = 420)
                val minBufBytes = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufBytes.coerceAtLeast(phraseBuffer.size * 2))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                activeTrack = track
                track.play()

                while (isActive) {
                    val written = track.write(phraseBuffer, 0, phraseBuffer.size)
                    if (written < 0) break
                }
            } catch (e: Exception) {
                Log.w("RingtonePlayer", "Unable to loop alarm ringtone: ${e.message}")
            } finally {
                releaseTrackSafely(track)
                if (activeTrack === track) activeTrack = null
            }
        }
    }

    @Synchronized
    fun stop() {
        val currentTrack = activeTrack
        activeTrack = null
        playbackJob?.cancel()
        playbackJob = null
        releaseTrackSafely(currentTrack)
    }

    @Synchronized
    private fun releaseTrackSafely(track: AudioTrack?) {
        if (track == null) return
        try {
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.pause()
                track.flush()
                track.stop()
            }
        } catch (_: Exception) {
        } finally {
            try {
                track.release()
            } catch (_: Exception) {}
        }
    }
}

class VoiceNoteManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var activeFilePath: String? = null
    private var recordingStartMillis: Long = 0L

    private fun notesDir(): File {
        val dir = File(context.filesDir, "voice_notes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listSavedNotes(): List<VoiceNoteItem> {
        return notesDir().listFiles()
            ?.filter { it.extension == "m4a" && it.length() > 0L }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                VoiceNoteItem(
                    id = file.nameWithoutExtension,
                    title = file.nameWithoutExtension.replace("_", " "),
                    filePath = file.absolutePath,
                    durationLabel = "${(file.length() / 2048L).coerceAtLeast(1L)}s",
                    createdAtMillis = file.lastModified()
                )
            } ?: emptyList()
    }

    fun startRecording(titlePrefix: String = "StudyNote"): Boolean {
        return try {
            stopRecording()
            val safeName = "${titlePrefix.replace("[^A-Za-z0-9]".toRegex(), "_")}_${System.currentTimeMillis() % 100000}"
            val outFile = File(notesDir(), "$safeName.m4a")
            activeFilePath = outFile.absolutePath
            recordingStartMillis = System.currentTimeMillis()
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioSamplingRate(44100)
            mr.setAudioEncodingBitRate(96000)
            mr.setOutputFile(outFile.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            true
        } catch (e: Exception) {
            Log.e("VoiceNoteManager", "Failed to start recording", e)
            false
        }
    }

    fun stopRecording(): VoiceNoteItem? {
        val mr = recorder ?: return null
        val path = activeFilePath
        val durationSec = ((System.currentTimeMillis() - recordingStartMillis) / 1000L).coerceAtLeast(1L)
        return try {
            mr.stop()
            mr.release()
            recorder = null
            if (path != null) {
                val f = File(path)
                VoiceNoteItem(
                    id = UUID.randomUUID().toString(),
                    title = f.nameWithoutExtension.replace("_", " "),
                    filePath = path,
                    durationLabel = "${durationSec}s",
                    createdAtMillis = System.currentTimeMillis()
                )
            } else null
        } catch (e: Exception) {
            Log.w("VoiceNoteManager", "Stop recording error: ${e.message}")
            try {
                mr.release()
            } catch (_: Exception) {}
            recorder = null
            null
        }
    }

    fun playNote(filePath: String, onCompletion: () -> Unit) {
        stopPlayback()
        try {
            val mp = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    stopPlayback()
                    onCompletion()
                }
                start()
            }
            player = mp
        } catch (e: Exception) {
            Log.e("VoiceNoteManager", "Failed to play voice note", e)
            onCompletion()
        }
    }

    fun stopPlayback() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        } finally {
            player = null
        }
    }

    fun deleteNote(filePath: String) {
        stopPlayback()
        try {
            File(filePath).delete()
        } catch (_: Exception) {}
    }
}
