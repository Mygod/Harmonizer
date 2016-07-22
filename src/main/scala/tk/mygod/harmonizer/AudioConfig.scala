package tk.mygod.harmonizer

import android.content._
import android.media.{AudioFormat, AudioManager, AudioTrack}
import tk.mygod.content.ContextPlus
import tk.mygod.os.Build

/**
 * @author Mygod
 */
object AudioConfig extends BroadcastReceiver with SharedPreferences.OnSharedPreferenceChangeListener {
  private var preferences: SharedPreferences = _
  var sampleRate: Int = _
  var format: Int = _
  private var sampleRateChanged: Boolean = _
  private var formatChanged = true
  private var initialized: Boolean = _

  def init(context: ContextPlus) = if (!initialized) {
    preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    preferences.registerOnSharedPreferenceChangeListener(this)
    val filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    filter.addAction(AudioManager.ACTION_HDMI_AUDIO_PLUG)
    filter.addAction(AudioManager.ACTION_HEADSET_PLUG)
    filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
    sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
    context.registerReceiver(this, filter)
    initialized = true
  }

  override def onReceive(context: Context, intent: Intent): Unit = sampleRateChanged = true
  def onSharedPreferenceChanged(pref: SharedPreferences, key: String) = formatChanged = true
  def changed = {
    var result = false
    if (formatChanged) {
      val newValue = preferences.getString("audio.bitDepth", (if (Build.version >= 21)
        AudioFormat.ENCODING_PCM_FLOAT else AudioFormat.ENCODING_PCM_16BIT).toString).toInt
      if (newValue != format) {
        format = newValue
        result = true
      }
      formatChanged = false
    }
    if (sampleRateChanged) {
      val newValue = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
      if (newValue != sampleRate) {
        sampleRate = newValue
        result = true
      }
      sampleRateChanged = false
    }
    result
  }
}
