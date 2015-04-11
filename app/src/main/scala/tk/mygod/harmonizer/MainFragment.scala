package tk.mygod.harmonizer

import android.app.AlertDialog
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
import android.widget.{EditText, TextView}
import tk.mygod.app.FragmentPlus
import tk.mygod.view.LocationObserver

import scala.collection.mutable.ArrayBuffer

/**
 * @author mygod
 */
class MainFragment extends FragmentPlus with OnMenuItemClickListener {
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

  private class FavoritesAdapter(private val empty: View) extends RecyclerView.Adapter[FavoriteItemViewHolder] {
    private val favorites = new ArrayBuffer[FavoriteItem]
    private val pref = getActivity.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    {
      val size = pref.getInt("size", 0)
      if (size > 0) {
        empty.setVisibility(View.GONE)
        for (i <- 0 until size) favorites += new FavoriteItem(pref.getString(i + "_name", ""),
          java.lang.Double.longBitsToDouble(pref.getLong(i + "_freq", 0)))
      }
    }

    override def onCreateViewHolder(vg: ViewGroup, i: Int) = new FavoriteItemViewHolder(
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
  private var savedTrack: AudioTrack = _
  private val muteTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_MONO,
    AudioFormat.ENCODING_PCM_16BIT, 32, AudioTrack.MODE_STATIC)
  muteTrack.write(new Array[Short](16), 0, 16)
  muteTrack.setLoopPoints(0, 16, -1)
  private var frequencyText: TextView = _
  private var favoritesAdapter: FavoritesAdapter = _
  private var drawerLayout: DrawerLayout = _
  private var drawerToggle: ActionBarDrawerToggle = _
  private var selectedItem: FavoriteItem = _
  private var pressed = false

  private def hideInput(text: TextView) {
    getActivity.getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]
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

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) = {
    val result = inflater.inflate(R.layout.fragment_main, container, false)
    frequencyText = result.findViewById(R.id.frequency_text).asInstanceOf[EditText]
    drawerLayout = result.findViewById(R.id.drawer_layout).asInstanceOf[DrawerLayout]
    val toolbar = result.findViewById(R.id.toolbar).asInstanceOf[Toolbar]
    toolbar.inflateMenu(R.menu.favorites)
    val menu = toolbar.getMenu
    val settingsMenu = menu.findItem(R.id.settings)
    val addFavoriteMenu = menu.findItem(R.id.add_to_favorites)
    result.findViewById(R.id.settings).setOnTouchListener(LocationObserver)
    val opened = drawerLayout == null || drawerLayout.isDrawerOpen(GravityCompat.START)
    settingsMenu.setVisible(!opened)
    addFavoriteMenu.setVisible(opened)
    toolbar.setOnMenuItemClickListener(this)
    toolbar.setTitle(if (opened) R.string.favorites else R.string.app_name)
    if (drawerLayout != null) {
      drawerToggle = new ActionBarDrawerToggle(getActivity, drawerLayout, toolbar,
        R.string.drawer_open, R.string.drawer_close) {
        override def onDrawerClosed(view: View) {
          super.onDrawerClosed(view)
          toolbar.setTitle(R.string.app_name)
          settingsMenu.setVisible(true)
          addFavoriteMenu.setVisible(false)
        }

        override def onDrawerOpened(view: View) {
          super.onDrawerOpened(view)
          toolbar.setTitle(R.string.favorites)
          settingsMenu.setVisible(false)
          addFavoriteMenu.setVisible(true)
          hideInput(frequencyText)
        }
      }
      drawerLayout.setDrawerListener(drawerToggle)
    }
    val favoriteList = result.findViewById(R.id.favorite).asInstanceOf[RecyclerView]
    favoriteList.setLayoutManager(new LinearLayoutManager(getActivity))
    favoriteList.setItemAnimator(new DefaultItemAnimator)
    favoriteList.setOnTouchListener(new OnTouchListener {
      def onTouch(v: View, event: MotionEvent) = false
    })
    favoritesAdapter = new FavoritesAdapter(result.findViewById(android.R.id.empty))
    favoriteList.setAdapter(favoritesAdapter)
    val button = result.findViewById(R.id.beep_button)
    button.setOnTouchListener(new OnTouchListener {
      def onTouch(v: View, event: MotionEvent): Boolean = {
        result.findViewById(R.id.scroller).asInstanceOf[ViewGroup].requestDisallowInterceptTouchEvent(true)
        event.getAction match {
          case MotionEvent.ACTION_DOWN =>
            pressed = true
            new Thread {
              override def run {
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
            }.start
          case MotionEvent.ACTION_UP => stop
          case _ =>
        }
        false
      }
    })
    result
  }

  override def onActivityCreated(savedInstanceState: Bundle) {
    super.onActivityCreated(savedInstanceState)
    if (drawerToggle != null) drawerToggle.syncState
  }

  override def onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    if (drawerToggle != null) drawerToggle.onConfigurationChanged(newConfig)
  }

  override def onMenuItemClick(menuItem: MenuItem): Boolean = {
    menuItem.getItemId match {
      case R.id.settings =>
        getActivity.asInstanceOf[MainActivity].showSettings(getActivity.findViewById(R.id.settings))
        true
      case R.id.add_to_favorites =>
        val text: EditText = new EditText(getActivity)
        new AlertDialog.Builder(getActivity).setTitle(R.string.add_favorite_dialog_title).setView(text)
          .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener {
          def onClick(dialog: DialogInterface, which: Int) {
            favoritesAdapter.add(new FavoriteItem(text.getText.toString, getFrequency))
            hideInput(text)
          }
        }).setNegativeButton(android.R.string.cancel, null).show
        true
      case _ => super.onOptionsItemSelected(menuItem)
    }
  }

  override def onOptionsItemSelected(item: MenuItem) = drawerToggle != null && drawerToggle.onOptionsItemSelected(item)

  override def onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo)
    getActivity.getMenuInflater.inflate(R.menu.favorite_context_menu, menu)
    selectedItem = v.getTag.asInstanceOf[FavoriteItem]
  }

  override def onContextItemSelected(item: MenuItem) = {
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