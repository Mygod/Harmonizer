package tk.mygod.harmonizer

import android.content.{Context, SharedPreferences}
import android.media.{AudioFormat, AudioTrack}
import tk.mygod.os.Build

/**
 * @author Mygod
 */
object AudioConfig extends SharedPreferences.OnSharedPreferenceChangeListener {
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

  private var preferences: SharedPreferences = _
  private var _samplingRate, _format: Int = _
  private var _changed = true

  def init(context: Context) {
    preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    preferences.registerOnSharedPreferenceChangeListener(this)
  }
  def samplingRate = {
    val result = preferences.getInt("audio.samplingRate", 2147483647)
    if (result > AudioConfig.maxSamplingRate) AudioConfig.maxSamplingRate
    else if (result < AudioConfig.minSamplingRate) AudioConfig.minSamplingRate else result
  }
  def format = preferences.getString("audio.bitDepth", (if (Build.version >= 21)
    AudioFormat.ENCODING_PCM_FLOAT else AudioFormat.ENCODING_PCM_16BIT).toString).toInt

  def onSharedPreferenceChanged(pref: SharedPreferences, key: String) = _changed = true
  def changed = if (_changed) {
    var result = false
    var i = samplingRate
    if (i != _samplingRate) {
      _samplingRate = i
      result = true
    }
    i = format
    if (i != _format) {
      _format = i
      result = true
    }
    _changed = false
    result
  } else false
}
