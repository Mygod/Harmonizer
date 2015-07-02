package tk.mygod.harmonizer

import android.app.Activity
import android.content.{Intent, Context}
import android.os.Bundle
import android.support.v7.widget.{AppCompatEditText, DefaultItemAnimator, LinearLayoutManager, RecyclerView}
import android.view._
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import tk.mygod.app.CircularRevealFragment
import tk.mygod.util.MethodWrappers._
import tk.mygod.view.LocationObserver

import scala.collection.mutable.ArrayBuffer

/**
 * @author Mygod
 */
class FavoritesFragment extends CircularRevealFragment {
  private lazy val mainFragment: MainFragment = getActivity.asInstanceOf[MainActivity].mainFragment

  private class FavoriteItemViewHolder(private val view: View) extends RecyclerView.ViewHolder(view)
    with View.OnClickListener {
    private var item: FavoriteItem = _
    private val text = itemView.findViewById(android.R.id.text1).asInstanceOf[TextView]
    itemView.setOnTouchListener(LocationObserver)
    itemView.setOnClickListener(this)
    registerForContextMenu(itemView)

    {
      val ta = getActivity.obtainStyledAttributes(Array(android.R.attr.selectableItemBackground))
      itemView.setBackground(ta.getDrawable(0))
      ta.recycle
    }

    def bind(item: FavoriteItem) {
      this.item = item
      text.setText(item.getFullName)
      view.setTag(item)
    }

    def onClick(v: View) {
      mainFragment.frequencyText.setText(Utils.betterToString(item.frequency))
      exit(v)
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

    def onCreateViewHolder(vg: ViewGroup, i: Int) = new FavoriteItemViewHolder(LayoutInflater.from(vg.getContext)
      .inflate(android.R.layout.simple_list_item_1, vg, false))
    def onBindViewHolder(vh: FavoriteItemViewHolder, i: Int) = vh.bind(favorites(i))
    def getItemCount = favorites.size

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
      if (favorites.isEmpty) empty.setVisibility(View.VISIBLE)
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

  private var favoritesAdapter: FavoritesAdapter = _
  private var selectedItem: FavoriteItem = _

  override def isFullscreen = true

  override def onAttach(activity: Activity) {
    super.onAttach(activity)
    activity.asInstanceOf[MainActivity].favoritesFragment = this
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) = {
    val result = inflater.inflate(R.layout.fragment_favorites, container, false)
    configureToolbar(result, R.string.favorites, 0)
    result.findViewById(R.id.favorite_name_text).asInstanceOf[AppCompatEditText].setOnEditorActionListener(
      (v: TextView, actionId: Int, event: KeyEvent) => if (actionId == EditorInfo.IME_ACTION_SEND) {
        favoritesAdapter.add(new FavoriteItem(v.getText.toString, mainFragment.getFrequency))
        v.setText(null)
        true
      } else false)
    val favoriteList = result.findViewById(R.id.favorite).asInstanceOf[RecyclerView]
    favoriteList.setLayoutManager(new LinearLayoutManager(getActivity))
    favoriteList.setItemAnimator(new DefaultItemAnimator)
    favoritesAdapter = new FavoritesAdapter(result.findViewById(android.R.id.empty))
    favoriteList.setAdapter(favoritesAdapter)
    result
  }

  override def onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo)
    selectedItem = v.getTag.asInstanceOf[FavoriteItem]
    getActivity.getMenuInflater.inflate(R.menu.context_menu_favorite, menu)
    menu.setHeaderTitle(selectedItem.getFullName)
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
