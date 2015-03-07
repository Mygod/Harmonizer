package tk.mygod.harmonizer

import android.app.{Activity, AlertDialog}
import android.content.res.Configuration
import android.content.{Context, DialogInterface, Intent}
import android.media.{AudioFormat, AudioManager, AudioTrack}
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar.OnMenuItemClickListener
import android.support.v7.widget.{DefaultItemAnimator, LinearLayoutManager, RecyclerView, Toolbar}
import android.view.View.OnTouchListener
import android.view._
import android.view.inputmethod.InputMethodManager
import android.widget.{NumberPicker, EditText, TextView}

import scala.collection.mutable.ArrayBuffer

final class MainActivity extends Activity with OnMenuItemClickListener {
  var buffer: ArrayBuffer[Short] = null // recycling ArrayBuffer ;-)

  private def generateTrack(frequency: Double, samplingRate: Int): AudioTrack = {
    val s10 = samplingRate * 10
    val minute = samplingRate * 60
    var max = if (frequency <= 0) 2 else (samplingRate / frequency).toInt
    if (max < 16) max = 16 else if (max > minute) max = minute
    if (buffer == null) buffer = new ArrayBuffer[Short](max) else buffer.sizeHint(max)
    val k = 3.14159265358979323846264338327950288 * 2 * frequency / samplingRate
    var i = 0
    var last: Short = 32767
    while (i < max || i < 524288 && last < 32767 || i < s10 && last < 32760) {
      last = (32767 * Math.cos(k * i)).round.toShort
      buffer.append(last)
      i += 1
    }
    i -= 1
    val delta = i * 3 / 4
    val samples = new Array[Short](i)
    var s = 0
    for (j <- delta until i) {
      samples(s) = buffer(j)
      s += 1
    }
    for (j <- 0 until delta) {
      samples(s) = buffer(j)
      s += 1
    }
    buffer.clear
    val track = new AudioTrack(AudioManager.STREAM_MUSIC, samplingRate, AudioFormat.CHANNEL_OUT_MONO,
                               AudioFormat.ENCODING_PCM_16BIT, i << 1, AudioTrack.MODE_STATIC)
    track.write(samples, 0, i)
    track.setLoopPoints(0, i, -1)
    track
  }

  private class FavoriteItemViewHolder(private val view: View) extends RecyclerView.ViewHolder(view)
      with View.OnClickListener {
    private val text = itemView.findViewById(android.R.id.text1).asInstanceOf[TextView]
    private var item: FavoriteItem = null
    itemView.setOnClickListener(this)
    registerForContextMenu(itemView)

    def bind(item: FavoriteItem) {
      this.item = item
      text.setText(item.getFullName)
      view.setTag(item)
    }

    override def onClick(v: View) {
      frequencyText.setText(Utils.betterToString(item.frequency))
      if (drawerLayout != null) drawerLayout.closeDrawers
    }
  }

  private class FavoritesAdapter extends RecyclerView.Adapter[FavoriteItemViewHolder] {
    private val favorites = new ArrayBuffer[FavoriteItem]
    private val empty = findViewById(android.R.id.empty)
    private val pref = getSharedPreferences("favorites", Context.MODE_PRIVATE)

    {
      val size = pref.getInt("size", 0)
      if (size > 0) {
        empty.setVisibility(View.GONE)
        for (i <- 0 until size) favorites += new FavoriteItem(pref.getString(i + "_name", ""),
          java.lang.Double.longBitsToDouble(pref.getLong(i + "_freq", 0)))
      }
    }

    override def onCreateViewHolder(vg: ViewGroup, i: Int): FavoriteItemViewHolder = new FavoriteItemViewHolder(
      LayoutInflater.from(vg.getContext).inflate(android.R.layout.simple_list_item_1, vg, false))

    override def onBindViewHolder(vh: FavoriteItemViewHolder, i: Int) {
      vh.bind(favorites(i))
    }

    override def getItemCount = favorites.size

    def add(item: FavoriteItem) {
      val pos = favorites.size
      favorites += item
      update
      empty.setVisibility(View.GONE)
      notifyItemInserted(pos)
    }

    def remove(item: FavoriteItem) {
      val pos: Int = favorites.indexOf(item)
      favorites.remove(pos)
      update
      notifyItemRemoved(pos)
      if (favorites.size == 0) empty.setVisibility(View.VISIBLE)
    }

    def update {
      val oldSize = pref.getInt("size", 0)
      var size = 0
      val editor = pref.edit
      for (favorite <- favorites) {
        editor.putString(size + "_name", favorite.name)
        editor.putLong(size + "_freq", java.lang.Double.doubleToLongBits(favorite.frequency))
        size += 1
      }
      editor.putInt("size", size)
      for (size <- size until oldSize) {
        editor.remove(size + "_name")
        editor.remove(size + "_freq")
      }
      editor.apply
    }
  }

  private var savedFrequency = .0
  private var savedSamplingRate = 0
  private var savedTrack: AudioTrack = null
  private val muteTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_MONO,
                                         AudioFormat.ENCODING_PCM_16BIT, 32, AudioTrack.MODE_STATIC)
  muteTrack.write(new Array[Short](16), 0, 16)
  muteTrack.setLoopPoints(0, 16, -1)
  private lazy val frequencyText = findViewById(R.id.frequency_text).asInstanceOf[EditText]
  private var samplingRatePicker: NumberPicker = _
  private lazy val favoritesAdapter = new FavoritesAdapter
  private lazy val drawerLayout = findViewById(R.id.drawer_layout).asInstanceOf[DrawerLayout]
  private var drawerToggle: ActionBarDrawerToggle = null
  private var addFavoriteMenu: MenuItem = null
  private var selectedItem: FavoriteItem = null
  private var pressed = false

  private def hideInput(text: TextView) {
    getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]
                                                  .hideSoftInputFromWindow(text.getWindowToken, 0)
  }

  private def getFrequency = try frequencyText.getText.toString.toDouble
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

  protected override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.activity_main)
    val toolbar = findViewById(R.id.toolbar).asInstanceOf[Toolbar]
    toolbar.inflateMenu(R.menu.favorites)
    addFavoriteMenu = toolbar.getMenu.findItem(R.id.add_to_favorites)
    val opened = drawerLayout == null || drawerLayout.isDrawerOpen(GravityCompat.START)
    addFavoriteMenu.setVisible(opened)
    toolbar.setOnMenuItemClickListener(this)
    toolbar.setTitle(if (opened) R.string.favorites else R.string.app_name)
    if (drawerLayout != null) {
      drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                                               R.string.drawer_open, R.string.drawer_close) {
        override def onDrawerClosed(view: View) {
          super.onDrawerClosed(view)
          toolbar.setTitle(R.string.app_name)
          addFavoriteMenu.setVisible(false)
        }

        override def onDrawerOpened(view: View) {
          super.onDrawerOpened(view)
          toolbar.setTitle(R.string.favorites)
          addFavoriteMenu.setVisible(true)
          hideInput(frequencyText)
        }
      }
      drawerLayout.setDrawerListener(drawerToggle)
    }
    val favoriteList = findViewById(R.id.favorite).asInstanceOf[RecyclerView]
    favoriteList.setLayoutManager(new LinearLayoutManager(this))
    favoriteList.setItemAnimator(new DefaultItemAnimator)
    favoriteList.setOnTouchListener(new OnTouchListener {
      override def onTouch(v: View, event: MotionEvent): Boolean = false
    })
    favoriteList.setAdapter(favoritesAdapter)
    samplingRatePicker = findViewById(R.id.sampling_rate_picker).asInstanceOf[NumberPicker]
    try {
      val minField = classOf[AudioTrack].getDeclaredField("SAMPLE_RATE_HZ_MIN")
      minField.setAccessible(true)
      samplingRatePicker.setMinValue(minField.get(null).asInstanceOf[Int])
    } catch {
      case exc: NoSuchFieldException => samplingRatePicker.setMinValue(4000)
    }
    try {
      val maxField = classOf[AudioTrack].getDeclaredField("SAMPLE_RATE_HZ_MAX")
      maxField.setAccessible(true)
      val max = maxField.get(null).asInstanceOf[Int]
      samplingRatePicker.setMaxValue(max)
      samplingRatePicker.setValue(max)
    } catch {
      case exc: NoSuchFieldException => // 48000 are used before this field is introduced
        samplingRatePicker.setMaxValue(48000)
        samplingRatePicker.setValue(48000)
    }
    findViewById(R.id.beep_button).setOnTouchListener(new OnTouchListener {
      override def onTouch(v: View, event: MotionEvent): Boolean = {
        event.getAction match {
          case MotionEvent.ACTION_DOWN =>
            pressed = true
            new Thread {
              override def run {
                val frequency = getFrequency
                val samplingRate = samplingRatePicker.getValue
                if (savedFrequency != frequency || savedSamplingRate != samplingRate) {
                  savedFrequency = frequency
                  savedSamplingRate = samplingRate
                  if (savedTrack != null) {
                    savedTrack.stop
                    savedTrack.release
                    savedTrack = null
                  }
                  savedTrack = generateTrack(frequency, samplingRate)
                }
                if (savedTrack != null && pressed) savedTrack.play
              }
            }.start
          case MotionEvent.ACTION_UP => stop
          case _ =>
        }
        false
      }
    })
  }

  protected override def onPostCreate(savedInstanceState: Bundle) {
    super.onPostCreate(savedInstanceState)
    if (drawerToggle != null) drawerToggle.syncState
  }

  override def onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    if (drawerToggle != null) drawerToggle.onConfigurationChanged(newConfig)
  }

  override def onMenuItemClick(menuItem: MenuItem): Boolean = {
    if (menuItem.getItemId != R.id.add_to_favorites) return super.onOptionsItemSelected(menuItem)
    val text: EditText = new EditText(this)
    new AlertDialog.Builder(this).setTitle(R.string.add_favorite_dialog_title).setView(text)
      .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener {
      def onClick(dialog: DialogInterface, which: Int) {
        favoritesAdapter.add(new FavoriteItem(text.getText.toString, getFrequency))
        hideInput(text)
      }
    }).setNegativeButton(android.R.string.cancel, null).show
    text.requestFocus
    getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]
      .toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean =
    drawerToggle != null && drawerToggle.onOptionsItemSelected(item)

  override def onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo)
    getMenuInflater.inflate(R.menu.favorite_context_menu, menu)
    selectedItem = v.getTag.asInstanceOf[FavoriteItem]
  }

  override def onContextItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case R.id.share =>
        startActivity(Intent.createChooser(new Intent().setAction(Intent.ACTION_SEND).setType("text/plain")
          .putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.share_content), selectedItem.getFullName)),
                    getString(R.string.share_title)))
        true
      case R.id.remove =>
        favoritesAdapter.remove(selectedItem)
        true
      case _ => super.onContextItemSelected(item)
    }
  }
}