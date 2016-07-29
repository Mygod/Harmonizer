package tk.mygod.harmonizer

import android.media.{AudioAttributes, AudioFormat, AudioManager, AudioTrack}
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.support.v7.widget.AppCompatEditText
import android.support.v7.widget.Toolbar.OnMenuItemClickListener
import android.view.{KeyEvent, MenuItem, MotionEvent}
import tk.mygod.app.{CircularRevealActivity, ToolbarActivity}
import tk.mygod.os.Build
import tk.mygod.view.LocationObserver

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag

object MainActivity {
  var instance: MainActivity = _
}

final class MainActivity extends ToolbarActivity with OnMenuItemClickListener with TypedFindView {
  import MainActivity._

  def createTrack(bufferSize: Int = 16, encoding: Int = AudioFormat.ENCODING_PCM_8BIT) =
    if (Build.version >= 21) new AudioTrack(
      new AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED | AudioAttributes.FLAG_LOW_LATENCY)
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .build(),
      new AudioFormat.Builder()
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .setEncoding(encoding)
        .setSampleRate(AudioConfig.sampleRate)
        .build(), bufferSize, AudioTrack.MODE_STATIC, 0)
    else new AudioTrack(AudioManager.STREAM_SYSTEM, AudioConfig.sampleRate, AudioFormat.CHANNEL_OUT_MONO, encoding,
      bufferSize, AudioTrack.MODE_STATIC)

  private lazy val am = systemService[AudioManager]
  private var savedFrequency: Double = _
  private var savedTrack: AudioTrack = _
  private lazy val muteTrack = {
    val track = createTrack()
    track.write(new Array[Byte](16), 0, 16)
    track.setLoopPoints(0, 16, -1)
    track
  }
  var frequencyText: AppCompatEditText = _
  private var pressed: Boolean = _
  private var muteNeedsNoting = true
  // recycling ArrayBuffer ;-)
  private var byteBuffer: ArrayBuffer[Byte] = _
  private var shortBuffer: ArrayBuffer[Short] = _
  private var floatBuffer: ArrayBuffer[Float] = _
  private def shift[T](buffer: ArrayBuffer[T], i: Int)(implicit m: ClassTag[T]) = {
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
    val s10 = AudioConfig.sampleRate * 10
    val minute = AudioConfig.sampleRate * 60
    var max = if (frequency <= 0) 2 else (AudioConfig.sampleRate / frequency).toInt
    if (max < 16) max = 16 else if (max > minute) max = minute
    val k = 3.14159265358979323846264338327950288 * 2 * frequency / AudioConfig.sampleRate
    var i = 0
    AudioConfig.format match {
      case AudioFormat.ENCODING_PCM_8BIT =>
        if (byteBuffer == null) byteBuffer = new ArrayBuffer[Byte](max) else byteBuffer.sizeHint(max)
        var last: Byte = -1
        while (i < max || last != -1 && (i < 524288 || i < s10 && last != -2)) {
          last = (127 * Math.cos(k * i) + 128).round.toByte
          byteBuffer.append(last)
          i += 1
        }
        i -= 1
        val track = createTrack(i)
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
        val track = createTrack(i << 1, AudioFormat.ENCODING_PCM_16BIT)
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
        val track = createTrack(i << 2, AudioFormat.ENCODING_PCM_FLOAT)
        track.write(shift(floatBuffer, i), 0, i, AudioTrack.WRITE_BLOCKING)
        floatBuffer.clear
        track.setLoopPoints(0, i, -1)
        track
      case _ => throw new IllegalArgumentException
    }
  }

  def getFrequency = try frequencyText.getText.toString.toDouble
    catch {
      case exc: Exception => 1000.0
    }

  private def stop {
    pressed = false
    if (savedTrack != null) savedTrack.pause
  }

  protected override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    instance = this
    AudioConfig.init(this)
    setContentView(R.layout.activity_main)
    frequencyText = findView(TR.frequency_text)
    configureToolbar()
    toolbar.inflateMenu(R.menu.menu_main)
    toolbar.setOnMenuItemClickListener(this)
    findViewById(R.id.settings).setOnTouchListener(LocationObserver)
    findViewById(R.id.favorites).setOnTouchListener(LocationObserver)
    findViewById(R.id.beep_button).setOnTouchListener((_, event) => {
      findView(TR.scroller).requestDisallowInterceptTouchEvent(true)
      event.getAction match {
        case MotionEvent.ACTION_DOWN =>
          pressed = true
          Future {
            val frequency = getFrequency
            if (AudioConfig.changed || savedFrequency != frequency) {
              savedFrequency = frequency
              if (savedTrack != null) {
                savedTrack.stop
                savedTrack.release
                savedTrack = null
              }
              savedTrack = generateTrack(frequency)
            }
            if (savedTrack != null && pressed) {
              savedTrack.play()
              if (muteNeedsNoting && am.getStreamVolume(AudioManager.STREAM_SYSTEM) <= 0)
                runOnUiThread(makeToast(R.string.volume_off).show)
              muteNeedsNoting = false
            }
          }
        case MotionEvent.ACTION_UP => stop
        case _ =>
      }
      false
    })
  }

  protected override def onDestroy() {
    instance = null
    super.onDestroy()
  }

  protected override def onResume() {
    super.onResume
    muteTrack.play()
  }

  protected override def onPause() {
    super.onPause
    muteTrack.pause()
    stop
  }

  def onMenuItemClick(menuItem: MenuItem) = {
    menuItem.getItemId match {
      case R.id.settings =>
        showSettings()
        true
      case R.id.favorites =>
        startActivity(CircularRevealActivity.putLocation(intent[FavoritesActivity],
          LocationObserver.getOnScreen(findViewById(R.id.favorites))),
          ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle)
        true
      case _ => super.onOptionsItemSelected(menuItem)
    }
  }

  override def onKeyUp(keyCode: Int, event: KeyEvent) = keyCode match {
    case KeyEvent.KEYCODE_MENU =>
      showSettings()
      true
    case _ => super.onKeyUp(keyCode, event)
  }

  def showSettings() = startActivity(CircularRevealActivity.putLocation(intent[SettingsActivity],
    LocationObserver.getOnScreen(findViewById(R.id.settings))),
    ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle)
}
