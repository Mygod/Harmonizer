package tk.mygod.harmonizer

import android.content.{Context, SharedPreferences}
import android.media.{AudioFormat, AudioManager, AudioTrack}
import tk.mygod.content.ContextPlus
import tk.mygod.os.Build

/**
 * @author Mygod
 */
object AudioConfig extends SharedPreferences.OnSharedPreferenceChangeListener {
  private var preferences: SharedPreferences = _
  val sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
  private var _format: Int = _
  private var _changed = true

  def init(context: ContextPlus) {
    preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    preferences.registerOnSharedPreferenceChangeListener(this)
  }
  def format = preferences.getString("audio.bitDepth", (if (Build.version >= 21)
    AudioFormat.ENCODING_PCM_FLOAT else AudioFormat.ENCODING_PCM_16BIT).toString).toInt

  def onSharedPreferenceChanged(pref: SharedPreferences, key: String) = _changed = true
  def changed = if (_changed) {
    var result = false
    val newValue = format
    if (newValue != _format) {
      _format = newValue
      result = true
    }
    _changed = false
    result
  } else false
}
