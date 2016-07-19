package tk.mygod.harmonizer

import android.content.Context
import android.media.{AudioFormat, AudioTrack}
import tk.mygod.os.Build

/**
 * @author Mygod
 */
object AudioConfig {
  var minSamplingRate = 4000
  var maxSamplingRate = 48000 // 48000 are used before this field is introduced
  try {
    val minField = classOf[AudioTrack].getDeclaredField("SAMPLE_RATE_HZ_MIN")
    minField.setAccessible(true)
    minSamplingRate = minField.get(null).asInstanceOf[Int]
  } catch {
    case exc: NoSuchFieldException =>
  }
  try {
    val maxField = classOf[AudioTrack].getDeclaredField("SAMPLE_RATE_HZ_MAX")
    maxField.setAccessible(true)
    maxSamplingRate = maxField.get(null).asInstanceOf[Int]
  } catch {
    case exc: NoSuchFieldException =>
  }
}

final class AudioConfig(context: Context) {
  private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
  var samplingRate, format: Int = _

  def getSamplingRate = {
    val result = preferences.getInt("audio.samplingRate", 2147483647)
    if (result > AudioConfig.maxSamplingRate) AudioConfig.maxSamplingRate
    else if (result < AudioConfig.minSamplingRate) AudioConfig.minSamplingRate else result
  }
  def getFormat = preferences.getString("audio.bitDepth", (if (Build.version >= 21)
    AudioFormat.ENCODING_PCM_FLOAT else AudioFormat.ENCODING_PCM_16BIT).toString).toInt

  def changed = {
    var result = false
    var i = getSamplingRate
    if (i != samplingRate) {
      samplingRate = i
      result = true
    }
    i = getFormat
    if (i != format) {
      format = i
      result = true
    }
    result
  }
}
