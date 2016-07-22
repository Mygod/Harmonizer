package tk.mygod.harmonizer

import android.content._
import android.media.{AudioFormat, AudioManager, AudioTrack}
import android.util.Log
import android.widget.Toast
import tk.mygod.os.Build

/**
 * @author Mygod
 */
object AudioConfig extends BroadcastReceiver with SharedPreferences.OnSharedPreferenceChangeListener {
  private final val TAG = "AudioConfig"

  private var preferences: SharedPreferences = _
  var sampleRate: Int = _
  var format: Int = _
  private var sampleRateChanged: Boolean = _
  private var formatChanged = true
  private var initialized: Boolean = _

  def init(context: Context) = if (!initialized) {
    preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    preferences.registerOnSharedPreferenceChangeListener(this)
    val filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    filter.addAction(AudioManager.ACTION_HDMI_AUDIO_PLUG)
    filter.addAction(AudioManager.ACTION_HEADSET_PLUG)
    filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
    sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
    onReceive(context, null)
    context.registerReceiver(this, filter)
    initialized = true
  }

  override def onReceive(context: Context, intent: Intent) {
    sampleRateChanged = true
    val am = context.getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]
    if (Build.version >= 23) {
      var failed = true
      if (am.getProperty(AudioManager.PROPERTY_SUPPORT_MIC_NEAR_ULTRASOUND).toBoolean) {
        Log.i(TAG, "The default microphone audio source supports near-ultrasound frequencies (range of 18 - 21 kHz).")
        failed = false
      }
      if (am.getProperty(AudioManager.PROPERTY_SUPPORT_SPEAKER_NEAR_ULTRASOUND).toBoolean) {
        Log.i(TAG, "The default speaker audio path supports near-ultrasound frequencies (range of 18 - 21 kHz).")
        failed = false
      }
      if (failed) Toast.makeText(context, R.string.near_ultrasound_unsupported, Toast.LENGTH_LONG).show()
    }
  }
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
