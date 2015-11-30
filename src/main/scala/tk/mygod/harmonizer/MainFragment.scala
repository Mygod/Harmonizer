package tk.mygod.harmonizer

import android.app.Activity
import android.media.{AudioFormat, AudioManager, AudioTrack}
import android.os.Bundle
import android.support.v7.widget.AppCompatEditText
import android.support.v7.widget.Toolbar.OnMenuItemClickListener
import android.view._
import tk.mygod.app.ToolbarFragment
import tk.mygod.harmonizer.TypedResource._
import tk.mygod.view.LocationObserver

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

/**
 * @author Mygod
 */
class MainFragment extends ToolbarFragment with OnMenuItemClickListener {
  lazy val audioConfig = new AudioConfig(getActivity)
  // recycling ArrayBuffer ;-)
  var byteBuffer: ArrayBuffer[Byte] = _
  var shortBuffer: ArrayBuffer[Short] = _
  var floatBuffer: ArrayBuffer[Float] = _
  private def shift[T: Manifest](buffer: ArrayBuffer[T], i: Int) = {
    val delta = i * 3 / 4
    val result = new Array[T](i)
    var s = 0
    for (j <- delta until i) {
      result(s) = buffer(j)
      s += 1
    }
    for (j <- 0 until delta) {
      result(s) = buffer(j)
      s += 1
    }
    result
  }
  private def generateTrack(frequency: Double) = {
    val s10 = audioConfig.samplingRate * 10
    val minute = audioConfig.samplingRate * 60
    var max = if (frequency <= 0) 2 else (audioConfig.samplingRate / frequency).toInt
    if (max < 16) max = 16 else if (max > minute) max = minute
    val k = 3.14159265358979323846264338327950288 * 2 * frequency / audioConfig.samplingRate
    var i = 0
    audioConfig.format match {
      case AudioFormat.ENCODING_PCM_8BIT =>
        if (byteBuffer == null) byteBuffer = new ArrayBuffer[Byte](max) else byteBuffer.sizeHint(max)
        var last: Byte = -1
        while (i < max || last != -1 && (i < 524288 || i < s10 && last != -2)) {
          last = (127 * Math.cos(k * i) + 128).round.toByte
          byteBuffer.append(last)
          i += 1
        }
        i -= 1
        val track = new AudioTrack(AudioManager.STREAM_MUSIC, audioConfig.samplingRate, AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_8BIT, i, AudioTrack.MODE_STATIC)
        track.write(shift(byteBuffer, i), 0, i)
        byteBuffer.clear
        track.setLoopPoints(0, i, -1)
        track
      case AudioFormat.ENCODING_PCM_16BIT =>
        if (shortBuffer == null) shortBuffer = new ArrayBuffer[Short](max) else shortBuffer.sizeHint(max)
        var last: Short = 32767
        while (i < max || i < 524288 && last < 32767 || i < s10 && last < 32727) {
          last = (32767 * Math.cos(k * i)).round.toShort
          shortBuffer.append(last)
          i += 1
        }
        i -= 1
        val track = new AudioTrack(AudioManager.STREAM_MUSIC, audioConfig.samplingRate, AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT, i << 1, AudioTrack.MODE_STATIC)
        track.write(shift(shortBuffer, i), 0, i)
        shortBuffer.clear
        track.setLoopPoints(0, i, -1)
        track
      case AudioFormat.ENCODING_PCM_FLOAT =>
        if (floatBuffer == null) floatBuffer = new ArrayBuffer[Float](max) else floatBuffer.sizeHint(max)
        var last: Float = 1
        while (i < max || i < 524288 && last < 1 || i < s10 && last < 0.9999) {
          last = Math.cos(k * i).toFloat
          floatBuffer.append(last)
          i += 1
        }
        i -= 1
        val track = new AudioTrack(AudioManager.STREAM_MUSIC, audioConfig.samplingRate, AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_FLOAT, i << 2, AudioTrack.MODE_STATIC)
        track.write(shift(floatBuffer, i), 0, i, AudioTrack.WRITE_BLOCKING)
        floatBuffer.clear
        track.setLoopPoints(0, i, -1)
        track
      case _ => throw new IllegalArgumentException
    }
  }

  private var savedFrequency = .0
  private var savedTrack: AudioTrack = _
  private val muteTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_MONO,
    AudioFormat.ENCODING_PCM_16BIT, 32, AudioTrack.MODE_STATIC)
  muteTrack.write(new Array[Short](16), 0, 16)
  muteTrack.setLoopPoints(0, 16, -1)
  var frequencyText: AppCompatEditText = _
  private var pressed = false

  def getFrequency = try frequencyText.getText.toString.toDouble
    catch {
      case exc: Exception => 1000.0
    }

  private def stop {
    pressed = false
    if (savedTrack != null) savedTrack.pause
  }

  protected override def onResume {
    super.onResume
    muteTrack.play
  }

  protected override def onPause {
    super.onPause
    muteTrack.pause
    stop
  }

  override def isFullscreen = true

  override def onAttach(activity: Activity) {
    //noinspection ScalaDeprecation
    super.onAttach(activity)
    activity.asInstanceOf[MainActivity].mainFragment = this
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) = {
    val result = inflater.inflate(R.layout.fragment_main, container, false)
    frequencyText = result.findView(TR.frequency_text)
    configureToolbar(result, R.string.app_name)
    toolbar.inflateMenu(R.menu.menu_main)
    toolbar.setOnMenuItemClickListener(this)
    result.findViewById(R.id.settings).setOnTouchListener(LocationObserver)
    result.findViewById(R.id.favorites).setOnTouchListener(LocationObserver)
    result.findViewById(R.id.beep_button).setOnTouchListener((v: View, event: MotionEvent) => {
      result.findViewById(R.id.scroller).asInstanceOf[ViewGroup].requestDisallowInterceptTouchEvent(true)
      event.getAction match {
        case MotionEvent.ACTION_DOWN =>
          pressed = true
          Future {
            val frequency = getFrequency
            if (audioConfig.changed || savedFrequency != frequency) {
              savedFrequency = frequency
              if (savedTrack != null) {
                savedTrack.stop
                savedTrack.release
                savedTrack = null
              }
              savedTrack = generateTrack(frequency)
            }
            if (savedTrack != null && pressed) savedTrack.play
          }
        case MotionEvent.ACTION_UP => stop
        case _ =>
      }
      false
    })
    result
  }

  def onMenuItemClick(menuItem: MenuItem) = {
    val parent = getActivity.asInstanceOf[MainActivity]
    menuItem.getItemId match {
      case R.id.settings =>
        parent.showSettings
        true
      case R.id.favorites =>
        parent.showFavorites
        true
      case _ => super.onOptionsItemSelected(menuItem)
    }
  }
}
